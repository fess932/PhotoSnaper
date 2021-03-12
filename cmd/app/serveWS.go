package main

import (
	"fmt"
	"net/http"
	"photo-sender-server/internal/ws"
	"strconv"

	"github.com/google/uuid"
)

// 1) connect viewer to camera via WebRTC, signal ower websocket
// 2) snap photo, signal via websocket, data via post request, send status upload over websocket
func serveWS(pool *ws.Pool, w http.ResponseWriter, r *http.Request) {
	t, err := getClientType(r)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	conn, err := ws.Upgrade(w, r)
	if err != nil {
		fmt.Fprintf(w, "%+V\n", err)
	}

	c := &ws.Client{
		Conn: conn,
		Pool: pool,
		ID:   uuid.New().String(),
		Type: t,
	}

	pool.Register <- c

	go c.Read()
}

func getClientType(r *http.Request) (int, error) {
	t := r.URL.Query().Get("t")
	i, err := strconv.Atoi(t)
	if err != nil {
		return 0, err
	}
	return i, nil
}
