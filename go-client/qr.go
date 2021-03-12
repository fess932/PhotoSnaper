package main

import (
	"encoding/json"
	"log"
	"net"
	"net/http"

	"github.com/go-chi/cors"

	"github.com/go-chi/chi/v5"
	"github.com/skip2/go-qrcode"
)

type QResp struct {
	Name string `json:"name"`
	QR   []byte `json:"qr"`
}

func main() {
	r := chi.NewRouter()
	r.Use(cors.Handler(cors.Options{
		AllowedOrigins:   []string{"*"},
		AllowedMethods:   []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowedHeaders:   []string{"Accept", "Authorization", "Content-Type", "X-CSRF-Token"},
		ExposedHeaders:   []string{"Link"},
		AllowCredentials: true,
		MaxAge:           300,
	}))

	r.Get("/", func(w http.ResponseWriter, r *http.Request) {

		w.Write(GetIpv4WithNames())
	})

	http.ListenAndServe(":4000", r)
}

type Host struct {
	Ipv4 string
	Name string
}

func GetIpv4WithNames() []byte {
	var hs []Host

	ifaces, err := net.Interfaces()
	if err == nil {
		for _, i := range ifaces {
			addrs, err := i.Addrs()

			if err != nil {
				continue
			}
			for _, addr := range addrs {
				var ip net.IP

				switch v := addr.(type) {
				case *net.IPNet:
					ip = v.IP
				case *net.IPAddr:
					ip = v.IP
				}
				if ip == nil {
					continue
				}

				ipAdr := ip.To4()
				if ipAdr == nil {
					continue
				}

				if !ipAdr.IsGlobalUnicast() {
					continue
				}

				hs = append(hs, Host{
					Ipv4: ipAdr.String(),
					Name: i.Name,
				})
			}
		}
	}

	var qr []QResp

	for _, v := range hs {
		png, err := qrcode.Encode(v.Ipv4, qrcode.High, 256)
		if err != nil {
			log.Fatal(err)
		}
		qr = append(qr, QResp{QR: png, Name: v.Name})
	}

	resp, err := json.Marshal(qr)
	if err != nil {
		log.Fatalln(err)
	}

	return resp
}
