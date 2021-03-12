export async function getVideoStream(): Promise<MediaStream> {
  try {
    const constraints = {
      video: {
        width: 640,
        heigth: 480,
      },
    }
    const mediaStream = await navigator.mediaDevices.getUserMedia(constraints)
    console.log('got media stream')
    return mediaStream
  } catch (e) {
    console.log('get video stream err:', e)
  }
}

// async addViewerICECandidate(e) {
//   if (e.candidate) {
//     console.log(e.candidate.toJSON())
//     this.wsClient.Send(
//       new Message(MTYPE_ADD_VIEWER_ICE_CANDIDATE, {
//         candidate: e.candidate.toJSON(),
//       })
//     )
//   }
// },
