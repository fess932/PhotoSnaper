<template>
  <div class="p-6 flex flex-col space-y-4">
    <div class="text-2xl">Current type client: {{ currentType }}</div>

    <div class="space-y-2 space-x-2">
      <Btn @click="connectToCamera" value="Connect" :disabled="status"></Btn>
      <Btn @click="disconnectPeers" value="Disconnect"></Btn>
      <Btn value="connect as viewer" @click="connectViewer"></Btn>
      <Btn value="Connect as camera" @click="connectCamera"></Btn>
      <Btn value="Send message from local" @click="sendMessageWS"></Btn>
    </div>

    <div v-if="currentCamera">Current camera {{ currentCamera }}</div>
    <div>
      <div>remote stream</div>
      <video autoplay playsinline :srcObject="remoteStream"></video>
    </div>
    <div class="space-y-1 space-x-1">
      <Btn
        :value="camera.id"
        v-for="camera in cameraList"
        @click="selectCamera(camera)"
        :class="{ 'bg-blue-200': camera === currentCamera }"
      >
      </Btn>
    </div>
    <div>
      <div class="flex space-x-4 border-2 rounded-t border-indigo-600">
        <label class="flex space-x-4 flex-grow p-2">
          <span> Enter a message:</span>
          <input
            @keydown.enter="sendMessage"
            v-model="message"
            class="flex-grow text-base"
            type="text"
            name="message"
            placeholder="Message text"
            inputmode="latin"
            :disabled="!status"
          />
        </label>

        <button
          @click="sendMessage"
          name="sendButton"
          class="disabled:opacity-50 px-4 py-1 text-base border-l-2 border-indigo-600 focus:outline-none"
          :disabled="!status"
        >
          Send
        </button>
      </div>

      <div class="messagebox">
        <p>Messages received:</p>
        <p v-for="(msg, index) in inputMessages" :key="index">
          {{ index }}-{{ msg }}
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import Btn from './Btn.vue'
import {
  getClient,
  Message,
  MTYPE_ANSWER,
  MTYPE_CAMERA_LIST,
  MTYPE_MESSAGE,
  MTYPE_NEW_ICE_CANDIDATE,
  MTYPE_OFFER,
  removeClient,
  TYPE_CAMERA,
  TYPE_VIEWER,
} from '../api/ws'
import { getVideoStream } from './utils'

export default {
  components: { Btn },
  data() {
    return {
      wsClient: null,
      peerConnection: null,
      dataChannel: null,

      status: false,
      message: '',
      inputMessages: ['test message'],

      currentType: 0,
      currentCamera: null,
      cameraList: [],
      videoStream: null,
      remoteStream: null,
    }
  },
  methods: {
    async createPeerConnection() {
      console.log('create peer connection')
      const peerConnection = new RTCPeerConnection()
      this.peerConnection = peerConnection
      // *** events *** ///
      peerConnection.onicecandidate = this.handleICECandidateEvent

      peerConnection.onicecandidateerror = this.handleICECandidateEvent
      peerConnection.onicegatheringstatechange = this.handleICEGatheringStateChangeEvent
      peerConnection.oniceconnectionstatechange = this.handleICEConnectionStateChangeEvent
      peerConnection.ontrack = this.handleTrackEvent
      peerConnection.onnegotiationneeded = this.handleNegotiationNeededEvent
      peerConnection.onremovetrack = this.handleRemoveTrackEvent
      peerConnection.onsignalingstatechange = this.handleSignalingStateChangeEvent
    },

    handleICECandidateEvent(e) {
      console.log('ice candidate event')

      if (e.candidate) {
        console.log('ice candidate event:', e.candidate.toJSON())
        this.wsClient.Send(
          new Message(MTYPE_NEW_ICE_CANDIDATE, {
            id: this.currentCamera ? this.currentCamera.id : '', // если нет айди то не отправляем
            candidate: e.candidate.toJSON(),
          })
        )
      }
    },
    handleICEConnectionStateChangeEvent(e) {
      console.log('handle ice status change event')
    },
    handleICEGatheringStateChangeEvent(e) {
      console.log('handle gathering event')
    },

    handleTrackEvent(e) {
      console.log('handle track event', e)
      this.remoteStream.addTrack(e.track, this.remoteStream)
    },
    handleNegotiationNeededEvent(e) {
      console.log('negotiation needed event')
      // todo: startConnection()
    },
    handleRemoveTrackEvent(e) {
      console.log(e)
    },
    handleSignalingStateChangeEvent(e) {
      console.log('signaling status change')
    },
    /////////////////////////////////////////////////////////
    sendMessage() {
      if (this.message !== '') {
        this.dataChannel.send(this.message)
        this.message = ''
      }
    },
    addIceCandidate(body) {
      this.peerConnection
        .addIceCandidate(body)
        .catch(this.handleAddCandidateError)
    },

    ///// VIEWER
    async connectViewer() {
      this.currentType = TYPE_VIEWER
      this.wsClient = await getClient(this.currentType)
      this.wsClient.onOpen(this.wsOnOpen)
      this.wsClient.onMessage(this.wsOnMessage)
      this.wsClient.onErr(this.wsOnErr)
      this.wsClient.onClose(this.wsOnClose)
    },
    selectCamera(camera) {
      this.currentCamera = camera
    },
    async connectToCamera() {
      this.remoteStream = new MediaStream()
      await this.createPeerConnection()
      await this.startConnection()
    },
    async startConnection() {
      try {
        const offer = await this.peerConnection.createOffer({
          offerToReceiveVideo: true,
        })
        await this.peerConnection.setLocalDescription(offer)
        this.wsClient.Send(
          new Message(MTYPE_OFFER, {
            id: this.currentCamera.id,
            remoteDescription: this.peerConnection.localDescription.toJSON(),
          })
        )
      } catch (e) {
        console.log('some error in start connection: ', e)
      }
    },
    async handleAnswer(answer) {
      console.log('handle answer connect', answer)
      try {
        await this.peerConnection.setRemoteDescription(answer)
      } catch (e) {
        console.log('err on handle answer', e)
        return
      }
      console.log('handle answer success, sdp connected!')
    },
    ////// END VIEWER

    ///// CAMERA
    async connectCamera() {
      this.currentType = TYPE_CAMERA
      this.videoStream = await getVideoStream()
      this.wsClient = await getClient(this.currentType)
      this.wsClient.onOpen(this.wsOnOpen)
      this.wsClient.onMessage(this.wsOnMessage)
      this.wsClient.onErr(this.wsOnErr)
      this.wsClient.onClose(this.wsOnClose)

      // 1 create peer connection
      await this.createPeerConnection()
    },
    async handleOffer(offer) {
      console.log('handle offer connect', offer)

      try {
        await this.peerConnection.setRemoteDescription(offer)
        this.videoStream.getTracks().forEach((track) => {
          console.log('peer connection add track', track)
          this.peerConnection.addTrack(track, this.videoStream)
        })
        const answer = await this.peerConnection.createAnswer()
        await this.peerConnection.setLocalDescription(answer)
        this.wsClient.Send(
          new Message(MTYPE_ANSWER, {
            remoteDescription: this.peerConnection.localDescription,
          })
        )
      } catch (e) {
        console.log('err handle offer', e)
      }
    },
    ////// END CAMERA

    //// UTILS
    handleAddCandidateError(e) {
      console.log('Oh noes! addICECandidate failed!', e)
    },

    disconnectPeers() {
      this.dataChannel.close()
      this.peerConnection.close()

      this.dataChannel = null
      this.peerConnection = null

      this.status = false
      this.message = ''
    },

    //// WEBSOCKET SIGNAL CHANNEL
    wsOnMessage(ev) {
      // console.log('message from other side')
      const m = JSON.parse(ev.data)

      switch (m.type) {
        case MTYPE_MESSAGE:
          console.log('just text message:', m.body)
          return
        case MTYPE_CAMERA_LIST:
          this.cameraList = m.body
          // TODO: проверка на наличие камеры в новом листе, если нет выбранной камеры, удалить камеру текущую
          if (this.cameraList === null) {
            this.currentCamera = null
          }
          return

        case MTYPE_ANSWER:
          console.log('message answer')
          this.handleAnswer(m.body)
          return

        case MTYPE_OFFER:
          console.log('message offer')
          this.handleOffer(m.body)
          return

        case MTYPE_NEW_ICE_CANDIDATE:
          this.addIceCandidate(m.body)
          return

        default:
          console.log('message not right message type')
          return
      }
    },
    async sendMessageWS() {
      this.wsClient.Send(new Message(MTYPE_MESSAGE, this.message))
    },
    wsOnOpen(ev) {
      //console.log('on open in vue', ev)
    },
    wsOnErr(ev) {
      console.log('on err in vue', ev)
    },
    wsOnClose(ev) {
      this.wsClient = null
      this.currentCamera = null
      this.cameraList = null
      removeClient()

      if (ev.reason === '') {
        console.log('error on connection to websocket, wrong auth')
        return
      }
      console.log('error on connection', ev)
    },
  },
  async mounted() {},
}
</script>
