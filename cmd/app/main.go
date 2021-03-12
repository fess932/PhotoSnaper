package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"math/rand"
	"net"
	"net/http"
	"os"
	"photo-sender-server/internal/ws"
	"time"

	"github.com/skip2/go-qrcode"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/cors"
)

const port = ":5000"

func main() {
	log.SetFlags(log.Ltime | log.Lshortfile)
	cmd := make(chan []byte, 1)
	go serve(cmd)

	scanner := bufio.NewScanner(os.Stdin)
	for scanner.Scan() {
		println("scan...")
		bytes := scanner.Bytes()

		if len(cmd) > 0 {
			println("cmd chan full")
		}
		if len(cmd) == 0 {
			cmd <- bytes
		}
	}
}

func serve(ch chan []byte) {
	r := chi.NewRouter()
	r.Use(cors.Handler(cors.Options{
		AllowedOrigins:   []string{"*"},
		AllowedMethods:   []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowedHeaders:   []string{"Accept", "Authorization", "Content-Type", "X-CSRF-Token"},
		ExposedHeaders:   []string{"Link"},
		AllowCredentials: true,
		MaxAge:           300,
	}))

	r.HandleFunc("/ws2", wsHandler(ch))

	pool := ws.NewPool()
	go pool.Start()
	r.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) {
		serveWS(pool, w, r)
	})

	r.Get("/", func(w http.ResponseWriter, r *http.Request) {
		log.Printf("get : %v", time.Now())
		w.Write([]byte("welcome"))
	})

	r.Get("/qr", func(w http.ResponseWriter, r *http.Request) {
		w.Write(GetIpv4WithNames())
	})

	r.Get("/snap", func(w http.ResponseWriter, r *http.Request) {
		name := r.URL.Query().Get("name")
		if name != "" {
			if len(ch) == 0 {
				println("send snap")
				w.Write([]byte(fmt.Sprintf("name: %v", name)))
				ch <- []byte("snap")
				return
			}
			if len(ch) > 0 {
				println("очередь забита")
				w.Write([]byte("очередь запросов забита, телефон недоступен"))
				return
			}

			return
		}

		w.Write([]byte("not found name"))
	})

	r.HandleFunc("/upload", uploadHandler)

	log.Println("serve at port", port)
	if err := http.ListenAndServe(port, r); err != nil {
		log.Fatal(err)
	}
}

const MaxUploadSize = 1024 * 1024 * 10

func uploadHandler(w http.ResponseWriter, req *http.Request) {
	m, err := req.MultipartReader()
	if err != nil {
		log.Fatalln(err)
	}

	length := req.ContentLength
	cur := 0
	for {
		part, err := m.NextPart()
		if err == io.EOF {
			break
		}
		var read int64
		var p float32
		rnd := rand.Int()
		dst, err := os.OpenFile(fmt.Sprintf("./uploads/photo_%v_%v.jpg", cur, rnd), os.O_WRONLY|os.O_CREATE, os.ModePerm)
		if err != nil {
			log.Fatalln(err)
		}
		for {
			buffer := make([]byte, 100000)
			cBytes, err := part.Read(buffer)
			if err == io.EOF {
				fmt.Println("SUCCESS!!!")
				break
			}
			read = read + int64(cBytes)
			//fmt.Printf("read bytes per time: %v \n", cBytes)
			//fmt.Printf("read bytes total: %v \n", read)
			p = float32(read) / float32(length) * 100
			fmt.Printf("progress: %v \n", p)
			dst.Write(buffer[0:cBytes])
		}
	}
}

type Host struct {
	Ipv4 string
	Name string
}

type QResp struct {
	Name string `json:"name"`
	QR   []byte `json:"qr"`
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
		png, err := qrcode.Encode(fmt.Sprintf("%v%v", v.Ipv4, port), qrcode.High, 256)
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
