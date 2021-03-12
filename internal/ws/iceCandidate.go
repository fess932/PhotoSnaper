package ws

import (
	"encoding/json"
	"log"
)

type BNewICECandidate struct {
	ID        string          `json:"id"`
	Candidate json.RawMessage `json:"candidate"`
}

func (c *Client) handleNewICECandidate(m json.RawMessage) {
	var b BNewICECandidate
	if err := json.Unmarshal(m, &b); err != nil {
		log.Println("err handle set remote unmarshal", err)
	}

	id := ""

	if c.Owner != "" {
		id = c.Owner
	}
	if b.ID != "" {
		id = b.ID
	}

	camera := c.Pool.getClientByID(id)
	if camera == nil {
		log.Println("not found ice candidate id then message:", b.ID, c.Owner, id, string(m))
		return
	}

	camera.Conn.WriteJSON(Message{
		Type: MTypeNewICECandidate,
		Body: b.Candidate,
	})
}
