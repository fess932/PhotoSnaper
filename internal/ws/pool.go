package ws

import (
	"log"
)

type Pool struct {
	Register   chan *Client
	Unregister chan *Client
	Clients    map[*Client]bool
	Broadcast  chan Message
}

func NewPool() *Pool {
	return &Pool{
		Register:   make(chan *Client),
		Unregister: make(chan *Client),
		Clients:    make(map[*Client]bool),
	}
}

func (p *Pool) getClientsByType(t int) []Client {
	var cs []Client

	for k, _ := range p.Clients {
		if k.Type == t {
			cs = append(cs, *k)
		}
	}

	return cs
}

func (p *Pool) sendToClientsByType(m interface{}, t int) {
	for k, _ := range p.Clients {
		if k.Type == t {
			if err := k.Conn.WriteJSON(m); err != nil {
				log.Println("write json send to clients: ", err)
			}
		}
	}
}

func (p *Pool) getClientByID(id string) *Client {
	for v := range p.Clients {
		if v.ID == id {
			return v
		}
	}
	return nil
}

func (p *Pool) handleOnRegister(c *Client) {
	switch c.Type {
	case TVIEWER:
		bm := BMessage{
			Type: MtypeCameralist,
			Body: p.getClientsByType(TCAMERA),
		}
		if err := c.Conn.WriteJSON(bm); err != nil {
			log.Println("err on write json", err)
		}

	case TCAMERA:
		bm := BMessage{
			Type: MtypeCameralist,
			Body: p.getClientsByType(TCAMERA),
		}
		p.sendToClientsByType(bm, TVIEWER)

	default:
		log.Println("wrong type")
	}

	// todo: если новый клиент имеет тип камера послать всем вьюверам
	// новый список камер
	// break ??
}

func (p *Pool) handleOnUnregister(c *Client) {
	switch c.Type {
	case TVIEWER:
		// delete owner from camera if camera connected to disconnected client
		if camera := p.getClientByID(c.ID); camera != nil {
			camera.Owner = ""
		}
		// если клиент имеет тип клиент вьювер тогда если к нему была подключена камера, отключить эту камеру
		// и сделать ее свободной для подключения
		//bm := BMessage{
		//	Type: MType_CameraList,
		//	Body: p.getClientsByType(TCAMERA),
		//}
		//if err := c.Conn.WriteJSON(bm); err != nil {
		//	log.Println("err on write json", err)
		//}
		// TODO: If disconnect camera owner

	case TCAMERA:
		// если клиент имеет тип камера
		// если камера была подключена к вьюверу сообщить этому вьюверу что камера отключена и отключить ее
		// насильно
		// если камера не была подключена то сообщить новый список клиентов-камер клиентам вьюверам
		log.Println("camera disconnect")
		bm := BMessage{
			Type: MtypeCameralist,
			Body: p.getClientsByType(TCAMERA),
		}
		p.sendToClientsByType(bm, TVIEWER)
	default:
		log.Println("wrong type")
	}
}

type BMessage struct {
	Type int      `json:"type"`
	Body []Client `json:"body"`
}

func (p *Pool) Start() {
	for {
		select {
		case client := <-p.Register:
			p.Clients[client] = true
			log.Println("Size of connection pool: ", len(p.Clients))
			p.handleOnRegister(client)

		case client := <-p.Unregister:
			delete(p.Clients, client)
			log.Println("Size of connection pool: ", len(p.Clients))
			p.handleOnUnregister(client)
		}
	}
}
