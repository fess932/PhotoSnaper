package main

import (
	"log"

	"github.com/google/uuid"
	"github.com/gorilla/websocket"
)

const (
	TCLIENT = iota + 1
	TCAMERA
)

type Client struct {
	UUID string `json:"uuid"` // для идентификации одним клиентом другого
	Type int    `json:"type"`
}

type ClientList struct {
	Clients map[string]*Client
}

func (c *ClientList) get(uuid string) Client {
	if cl, ok := c.Clients[uuid]; ok {
		return *cl
	}
	return Client{}
}

func (c *ClientList) getCameraList() []Client {
	var cameraList []Client
	for _, v := range c.Clients {
		if v.Type == TCAMERA {
			cameraList = append(cameraList, *v)
		}
	}
	log.Printf("cam list: %#v", cameraList)
	return cameraList
}
func (c *ClientList) addClient(client *Client) {
	c.Clients[client.UUID] = client
}

func (c *ClientList) handleNewClient(ws *websocket.Conn, ch chan []byte, t int) {
	stdoutDone := make(chan struct{})

	go pumpStdout(ws, ch, stdoutDone)
	go ping(ws, stdoutDone)
	go pumpStdin(ws)

	switch t {
	case TCLIENT:
		c.addClient(&Client{uuid.New().String(), TCLIENT})
		ws.WriteJSON(SendCameraList{ID: CSendCameraList, List: c.getCameraList()})
	case TCAMERA:
		c.addClient(&Client{uuid.New().String(), TCAMERA})

		log.Println("camera")
	default:
		log.Println("no such type:", t)
	}
}

func sendMessage(m Message, ch chan []byte) {
	ch <- m.encode()
}
