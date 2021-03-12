package main

// Command IDs
const (
	CSetUUID        = iota + 1
	CSendCameraList = iota + 1
)

type SendCameraList struct {
	ID   int      `json:"id"`
	List []Client `json:"list"`
}
