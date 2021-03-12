package main

import (
	"encoding/json"
	"log"
	"net/http"
	"time"

	"github.com/gorilla/websocket"
)

const (
	// Time allowed to write a message to the peer.
	writeWait = 10 * time.Second

	// Maximum message size allowed from peer.
	maxMessageSize = 8192

	// Time allowed to read the next pong message from the peer.
	pongWait = 10 * time.Second

	// Send pings to peer with this period. Must be less than pongWait.
	pingPeriod = (pongWait * 9) / 10

	// Time to wait before force close on connection.
	closeGracePeriod = 10 * time.Second
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

// входящий поток
func pumpStdin(ws *websocket.Conn) {
	defer ws.Close()
	ws.SetReadLimit(maxMessageSize)
	ws.SetReadDeadline(time.Now().Add(pongWait))
	ws.SetPongHandler(func(string) error { ws.SetReadDeadline(time.Now().Add(pongWait)); return nil })

	for {
		_, message, err := ws.ReadMessage()
		if err != nil {
			break
		}
		ParseMessage(message)
	}
}

// исходящий поток
func pumpStdout(ws *websocket.Conn, ch chan []byte, done chan struct{}) {
	for {
		select {
		case cmd := <-ch:
			log.Println("stdout = ", string(cmd))
			if err := ws.WriteMessage(websocket.TextMessage, cmd); err != nil {
				log.Println(err)
			}
		case <-done:
			return
		}
	}
}

func ping(ws *websocket.Conn, done chan struct{}) {
	ticker := time.NewTicker(pingPeriod)
	defer ticker.Stop()
	for {
		select {
		case <-ticker.C:
			if err := ws.WriteControl(websocket.PingMessage, []byte{}, time.Now().Add(pongWait)); err != nil {
				close(done)
				if err == websocket.ErrCloseSent {
					log.Println("close sent, close socket")
				}
				log.Println("ping: ", err)
				return
			}

		case <-done:
			log.Println("return with done from ping")
			return
		}
	}
}

func internalError(ws *websocket.Conn, msg string, err error) {
	log.Println(msg, err)
	ws.WriteMessage(websocket.TextMessage, []byte("internal server errror."))
}

type TypeMessage int

const (
	MTYPE_MESSAGE = iota + 1
	MTYPE_COMMAND
)

type Message struct {
	Type TypeMessage `json:"type"`
	Data string      `json:"data"`
}

func (m Message) encode() []byte {
	bin, err := json.Marshal(m)
	if err != nil {
		log.Println("err in message encode", err)
	}
	return bin
}

func ParseMessage(rawMessage []byte) {
	println(string(rawMessage))

	var message Message
	if err := json.Unmarshal(rawMessage, &message); err != nil {
		log.Println("parse messsage err:", err)
	}

	switch message.Type {
	case MTYPE_MESSAGE:
		log.Printf("message: %#v", message.Data)
	case MTYPE_COMMAND:
		log.Println("need make command:", message.Data)
	}
}
