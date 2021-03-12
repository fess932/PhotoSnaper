package ws

import (
	"encoding/json"
	"log"
)

type BSetRemoteDescription struct {
	ID                string `json:"id"`
	RemoteDescription struct {
		SDP  string `json:"sdp"`
		Type string `json:"type"`
	} `json:"remoteDescription"`
}

func (c *Client) handleOffer(m json.RawMessage) {
	var b BSetRemoteDescription
	if err := json.Unmarshal(m, &b); err != nil {
		log.Println("err handle set remote unmarshal", err)
	}

	camera := c.Pool.getClientByID(b.ID)

	rDescr, err := json.Marshal(b.RemoteDescription)
	if err != nil {
		log.Println("err while marshal remote decsr", err)
		return
	}

	camera.Owner = c.ID
	camera.Conn.WriteJSON(Message{
		Type: MTypeOffer,
		Body: rDescr,
	})
}

func (c *Client) handleAnswer(m json.RawMessage) {
	var b BSetRemoteDescription
	if err := json.Unmarshal(m, &b); err != nil {
		log.Println("err handle set remote unmarshal", err)
	}

	log.Println(b)

	viewer := c.Pool.getClientByID(c.Owner)

	rDescr, err := json.Marshal(b.RemoteDescription)
	if err != nil {
		log.Println("err while marshal remote decsr", err)
		return
	}

	viewer.Conn.WriteJSON(Message{
		Type: MTypeAnswer,
		Body: rDescr,
	})
}
