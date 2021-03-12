export const TYPE_VIEWER = 1
export const TYPE_CAMERA = 2

// const host = '10.51.2.113:5000'
const host = 'localhost:5000'
// const host = '192.168.1.135:5000'

class WSClient {
  _socket: null | WebSocket = null
  _type: null | Number = null

  constructor(clientType: Number) {
    this._type = clientType
  }

  async setup() {
    try {
      this._socket = await new WebSocket(`ws://${host}/ws?t=${this._type}`)
    } catch (e) {
      console.log('WAT', e)
    }
  }

  onOpen(cb) {
    this._socket.onopen = cb
  }

  onMessage(cb) {
    this._socket.onmessage = cb
  }

  onErr(cb) {
    this._socket.onerror = cb
  }

  onClose(cb) {
    this._socket.onclose = cb
  }

  Send(message: Message) {
    console.log('send:', message.toJSON())
    this._socket.send(message.toJSON())
  }

  hello(this: WebSocket, ev: Event): any {
    console.log('hello!', this, ev)
  }
}

let wsClient = null

export async function getClient(clientType) {
  if (wsClient === null) {
    wsClient = new WSClient(clientType)
    await wsClient.setup()
  }
  return wsClient
}

export const MTYPE_MESSAGE = 1
export const MTYPE_OFFER = 2
export const MTYPE_ANSWER = 3
export const MTYPE_CAMERA_LIST = 4
export const MTYPE_NEW_ICE_CANDIDATE = 5

export class Message {
  _body: String | Object
  _type: Number
  constructor(type: Number, body: String | Object) {
    this._body = body
    this._type = type
  }
  toJSON(): string {
    return JSON.stringify({ type: this._type, body: this._body })
  }
}

export async function removeClient() {
  wsClient = null
}
//
// const ws = new WSClient()
//
// export default ws
