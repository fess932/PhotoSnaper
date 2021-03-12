package ws

import (
	"log"

	"github.com/gorilla/websocket"
)

const (
	TVIEWER = iota + 1
	TCAMERA
)

type Client struct {
	ID    string `json:"id"`
	Type  int    `json:"-"`
	Owner string `json:"-"`

	Conn *websocket.Conn `json:"-"`
	Pool *Pool           `json:"-"`
}

func (c *Client) Read() {
	defer func() {
		c.Pool.Unregister <- c
		c.Conn.Close()
	}()

	for {
		var m Message
		err := c.Conn.ReadJSON(&m)
		if err != nil {
			log.Println("err on client read json:", err)
			return
		}

		c.handleReadMessage(m)

		//message := Message{Type: messageType, Body: string(p)}
	}
}

func (c *Client) handleReadMessage(m Message) {
	switch m.Type {
	case MTypeOffer:
		c.handleOffer(m.Body)
	case MTypeAnswer:
		c.handleAnswer(m.Body)

	case MTypeNewICECandidate:
		c.handleNewICECandidate(m.Body)

	default:
		log.Println("not implemented type:", m.Type)
	}
}
