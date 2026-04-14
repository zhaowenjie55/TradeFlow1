"use client"

import { useCallback, useEffect, useRef, useState } from "react"

export function useVoiceRecorder() {
  const [isRecording, setIsRecording] = useState(false)
  const [errorMessage, setErrorMessage] = useState("")
  const [elapsedSeconds, setElapsedSeconds] = useState(0)
  const maxDurationSeconds = 8

  const mediaRecorderRef = useRef<MediaRecorder | null>(null)
  const mediaStreamRef = useRef<MediaStream | null>(null)
  const chunksRef = useRef<BlobPart[]>([])
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const pendingResolveRef = useRef<((file: File) => void) | null>(null)
  const pendingRejectRef = useRef<((error: Error) => void) | null>(null)

  const cleanupStream = useCallback(() => {
    mediaStreamRef.current?.getTracks().forEach((track) => track.stop())
    mediaStreamRef.current = null
  }, [])

  const stopTimer = useCallback(() => {
    if (timerRef.current) {
      clearInterval(timerRef.current)
      timerRef.current = null
    }
  }, [])

  const resetRecorder = useCallback(() => {
    const currentRecorder = mediaRecorderRef.current
    if (currentRecorder && currentRecorder.state !== "inactive") {
      try {
        currentRecorder.stop()
      } catch {
        // ignore
      }
    }
    mediaRecorderRef.current = null
    chunksRef.current = []
    pendingResolveRef.current = null
    pendingRejectRef.current = null
    setIsRecording(false)
    setElapsedSeconds(0)
    stopTimer()
    cleanupStream()
  }, [cleanupStream, stopTimer])

  const startRecording = useCallback(async () => {
    if (isRecording) return
    if (typeof window === "undefined" || typeof MediaRecorder === "undefined") {
      throw new Error("Current browser does not support audio recording.")
    }

    setErrorMessage("")
    chunksRef.current = []
    mediaStreamRef.current = await navigator.mediaDevices.getUserMedia({ audio: true })
    const preferredMimeType = MediaRecorder.isTypeSupported("audio/webm;codecs=opus")
      ? "audio/webm;codecs=opus"
      : "audio/webm"

    const recorder = new MediaRecorder(mediaStreamRef.current, { mimeType: preferredMimeType })
    mediaRecorderRef.current = recorder

    recorder.ondataavailable = (event) => {
      if (event.data.size > 0) chunksRef.current.push(event.data)
    }
    recorder.onerror = () => {
      setErrorMessage("录音失败，请检查浏览器麦克风权限。")
    }
    recorder.onstop = () => {
      setIsRecording(false)
      stopTimer()
      const blob = new Blob(chunksRef.current, { type: recorder.mimeType || "audio/webm" })
      cleanupStream()
      chunksRef.current = []
      if (blob.size === 0) {
        pendingRejectRef.current?.(new Error("录音内容为空，请重试。"))
      } else {
        pendingResolveRef.current?.(
          new File([blob], `voice-query-${Date.now()}.webm`, { type: blob.type }),
        )
      }
      pendingResolveRef.current = null
      pendingRejectRef.current = null
      mediaRecorderRef.current = null
    }

    recorder.start()
    setIsRecording(true)
    setElapsedSeconds(0)
    stopTimer()
    timerRef.current = setInterval(() => {
      setElapsedSeconds((prev) => {
        const next = prev + 1
        if (next >= maxDurationSeconds && mediaRecorderRef.current?.state === "recording") {
          mediaRecorderRef.current.stop()
        }
        return next
      })
    }, 1000)
  }, [cleanupStream, isRecording, stopTimer])

  const stopRecording = useCallback(async () => {
    if (!mediaRecorderRef.current || !isRecording) {
      throw new Error("当前没有正在进行的录音。")
    }
    return new Promise<File>((resolve, reject) => {
      pendingResolveRef.current = resolve
      pendingRejectRef.current = reject
      mediaRecorderRef.current?.stop()
    })
  }, [isRecording])

  useEffect(() => () => resetRecorder(), [resetRecorder])

  return {
    isRecording,
    errorMessage,
    elapsedSeconds,
    maxDurationSeconds,
    startRecording,
    stopRecording,
    resetRecorder,
    setErrorMessage,
  }
}
