package main

import (
	"net/http"
	"strconv"
)

func wsHandler(ch chan []byte) http.HandlerFunc {
	var cl = ClientList{Clients: map[string]*Client{}}

	return func(w http.ResponseWriter, r *http.Request) {
		t := r.URL.Query().Get("t")
		i, err := strconv.Atoi(t)
		if err != nil {
			http.Error(w, err.Error(), http.StatusBadRequest)
			return
		}

		ws, err := upgrader.Upgrade(w, r, nil)
		if err != nil {
			http.Error(w, err.Error(), http.StatusBadRequest)
			return
		}

		cl.handleNewClient(ws, ch, i)
	}
}
