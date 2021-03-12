export const connectPeers = () => {
  let localConnection = null
  let remoteConnection = null

  let sendChannel = null
  let receiveChannel = null

  localConnection = new RTCPeerConnection()
  console.log('lel:', localConnection)

  localConection.cre
  sendChannel = localConnection.createDataChannel('send channel')
  sendChannel.onopen = handleSendChannelStatusChange
  sendChannel.onclose = handleSendChannelStatusChange

  // remote connection
  remoteConnection = new RTCPeerConnection()
  remoteConnection.ondatachannel = receiveChannelCallback

  // ICE
  localConnection.onicecandidate = (e) =>
    !e.candidate ||
    remoteConnection.addIceCandidate(e.candidate).catch(handleAddCandidateError)

  remoteConnection.onicecandidate = (e) =>
    !e.candidate ||
    localConnection.addIceCandidate(e.candidate).catch(handleAddCandidateError)

  // create offer
  localConnection
    .createOffer()
    .then((offer) => localConection.setLocalDescription(offer))
    .then(() =>
      remoteConnection.setRemoteDescription(localConection.localDescription)
    )
    .then(() => remoteConnection.createAnswer())
    .then((answer) => remoteConnection.setLocalDescription(answer))
    .then(() =>
      localConection.setRemoteDescription(remoteConnection.localDescription)
    )
    .catch(handleCreateDescriptionError)
}

function receiveChannelCallback(event) {
  receiveChannel = event.channel
  receiveChannel.onmessage = handleReceiveMessage
  receiveChannel.onopen = handleReceiveChannelStatusChange
  receiveChannel.onclose = handleReceiveChannelStatusChange
}

function handleSendChannelStatusChange(event) {
  if (sendChannel) {
    let state = sendChannel.readyState

    if (state === 'open') {
    }
  }
}

function handleLocalAddCandidateSuccess() {}
