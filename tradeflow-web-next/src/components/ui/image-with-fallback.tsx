"use client"

import Image from "next/image"
import { ImageIcon } from "lucide-react"
import { useState } from "react"

import { cn } from "@/lib/utils"

interface ImageWithFallbackProps {
  src?: string | null
  alt: string
  className?: string
  wrapperClassName?: string
  iconClassName?: string
  sizes?: string
  priority?: boolean
}

export function ImageWithFallback({
  src,
  alt,
  className,
  wrapperClassName,
  iconClassName,
  sizes,
  priority,
}: ImageWithFallbackProps) {
  if (!src) {
    return (
      <div
        className={cn(
          "flex items-center justify-center overflow-hidden bg-[linear-gradient(135deg,rgba(241,245,249,0.95),rgba(226,232,240,0.88))] text-[var(--tf-text-subtle)]",
          wrapperClassName,
        )}
        aria-label={alt}
      >
        <ImageIcon className={cn("size-7 opacity-70", iconClassName)} />
      </div>
    )
  }

  return (
    <FallbackImage
      key={src}
      src={src}
      alt={alt}
      className={className}
      wrapperClassName={wrapperClassName}
      iconClassName={iconClassName}
      sizes={sizes}
      priority={priority}
    />
  )
}

function FallbackImage({
  src,
  alt,
  className,
  wrapperClassName,
  iconClassName,
  sizes,
  priority,
}: ImageWithFallbackProps & { src: string }) {
  const [failed, setFailed] = useState(false)

  if (failed) {
    return (
      <div
        className={cn(
          "flex items-center justify-center overflow-hidden bg-[linear-gradient(135deg,rgba(241,245,249,0.95),rgba(226,232,240,0.88))] text-[var(--tf-text-subtle)]",
          wrapperClassName,
        )}
        aria-label={alt}
      >
        <ImageIcon className={cn("size-7 opacity-70", iconClassName)} />
      </div>
    )
  }

  return (
    <div className={cn("relative overflow-hidden", wrapperClassName)}>
      <Image
        src={src}
        alt={alt}
        fill
        unoptimized
        referrerPolicy="no-referrer"
        sizes={sizes ?? "160px"}
        priority={priority}
        className={cn("object-cover", className)}
        onError={() => setFailed(true)}
      />
    </div>
  )
}
