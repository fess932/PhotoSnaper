package ws

import "encoding/json"

const (
	MType_Message = iota + 1
	MTypeOffer
	MTypeAnswer
	MtypeCameralist
	MTypeNewICECandidate
)

type Message struct {
	Type int             `json:"type"`
	Body json.RawMessage `json:"body"`
}
