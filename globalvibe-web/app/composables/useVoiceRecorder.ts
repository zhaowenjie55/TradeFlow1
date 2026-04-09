export const useVoiceRecorder = () => {
  const isRecording = ref(false)
  const errorMessage = ref('')
  const elapsedSeconds = ref(0)
  const maxDurationSeconds = 8

  let mediaRecorder: MediaRecorder | null = null
  let mediaStream: MediaStream | null = null
  let chunks: BlobPart[] = []
  let pendingResolve: ((file: File) => void) | null = null
  let pendingReject: ((error: Error) => void) | null = null
  let timerId: ReturnType<typeof setInterval> | null = null

  const cleanupStream = () => {
    mediaStream?.getTracks().forEach(track => track.stop())
    mediaStream = null
  }

  const stopTimer = () => {
    if (timerId) {
      clearInterval(timerId)
      timerId = null
    }
  }

  const resetRecorder = () => {
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      try {
        mediaRecorder.stop()
      } catch {
        // Ignore stop failures during teardown.
      }
    }
    mediaRecorder = null
    chunks = []
    pendingResolve = null
    pendingReject = null
    isRecording.value = false
    elapsedSeconds.value = 0
    stopTimer()
    cleanupStream()
  }

  const startRecording = async () => {
    if (isRecording.value) return
    if (!import.meta.client || typeof MediaRecorder === 'undefined') {
      throw new Error('Current browser does not support audio recording.')
    }

    errorMessage.value = ''
    chunks = []
    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true })

    const preferredMimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
      ? 'audio/webm;codecs=opus'
      : 'audio/webm'

    mediaRecorder = new MediaRecorder(mediaStream, { mimeType: preferredMimeType })
    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        chunks.push(event.data)
      }
    }
    mediaRecorder.onerror = () => {
      errorMessage.value = '录音失败，请检查浏览器麦克风权限。'
    }
    mediaRecorder.onstop = () => {
      isRecording.value = false
      stopTimer()
      const blob = new Blob(chunks, { type: mediaRecorder?.mimeType || 'audio/webm' })
      cleanupStream()
      chunks = []
      if (blob.size === 0) {
        pendingReject?.(new Error('录音内容为空，请重试。'))
      } else {
        pendingResolve?.(new File([blob], `voice-query-${Date.now()}.webm`, { type: blob.type }))
      }
      pendingResolve = null
      pendingReject = null
      mediaRecorder = null
    }

    mediaRecorder.start()
    isRecording.value = true
    elapsedSeconds.value = 0
    stopTimer()
    timerId = setInterval(() => {
      elapsedSeconds.value += 1
      if (elapsedSeconds.value >= maxDurationSeconds && mediaRecorder?.state === 'recording') {
        mediaRecorder.stop()
      }
    }, 1000)
  }

  const stopRecording = async () => {
    if (!mediaRecorder || !isRecording.value) {
      throw new Error('当前没有正在进行的录音。')
    }

    return await new Promise<File>((resolve, reject) => {
      pendingResolve = resolve
      pendingReject = reject
      mediaRecorder?.stop()
    })
  }

  return {
    isRecording,
    errorMessage,
    elapsedSeconds,
    maxDurationSeconds,
    startRecording,
    stopRecording,
    resetRecorder,
  }
}
