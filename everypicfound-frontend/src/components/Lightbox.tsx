import { useEffect, useCallback, useState } from 'react'
import type { SearchResultItem } from '../types'

interface LightboxProps {
  items: SearchResultItem[]
  currentIndex: number
  onClose: () => void
  onPrev: () => void
  onNext: () => void
}

export default function Lightbox({ items, currentIndex, onClose, onPrev, onNext }: LightboxProps) {
  const [fullLoaded, setFullLoaded] = useState(false)
  const item = items[currentIndex]

  // Keyboard navigation
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      switch (e.key) {
        case 'Escape':
          onClose()
          break
        case 'ArrowLeft':
          onPrev()
          break
        case 'ArrowRight':
          onNext()
          break
      }
    },
    [onClose, onPrev, onNext],
  )

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = ''
    }
  }, [handleKeyDown])

  // Reset loaded state when current image changes
  useEffect(() => {
    setFullLoaded(false)
  }, [currentIndex])

  if (!item) return null

  const canPrev = currentIndex > 0
  const canNext = currentIndex < items.length - 1

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/85"
      onClick={onClose}
    >
      {/* Close button */}
      <button
        onClick={onClose}
        className="absolute top-4 right-4 z-10 text-white/80 hover:text-white p-2 rounded-full
                   hover:bg-white/10 transition-colors"
        aria-label="Close"
      >
        <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>

      {/* Prev button */}
      {canPrev && (
        <button
          onClick={(e) => { e.stopPropagation(); onPrev() }}
          className="absolute left-4 z-10 text-white/80 hover:text-white p-2 rounded-full
                     hover:bg-white/10 transition-colors"
          aria-label="Previous"
        >
          <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
        </button>
      )}

      {/* Next button */}
      {canNext && (
        <button
          onClick={(e) => { e.stopPropagation(); onNext() }}
          className="absolute right-4 z-10 text-white/80 hover:text-white p-2 rounded-full
                     hover:bg-white/10 transition-colors"
          aria-label="Next"
        >
          <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </button>
      )}

      {/* Image container */}
      <div
        className="max-w-[90vw] max-h-[85vh] flex flex-col items-center"
        onClick={(e) => e.stopPropagation()}
      >
        <img
          src={item.imageUrl}
          alt={item.originalFileName ?? item.fileName}
          className={`
            max-w-full max-h-[75vh] object-contain rounded-lg shadow-2xl
            transition-opacity duration-300
            ${fullLoaded ? 'opacity-100' : 'opacity-0'}
          `}
          onLoad={() => setFullLoaded(true)}
        />

        {/* Info bar */}
        <div className="mt-3 text-white/70 text-sm flex flex-wrap items-center gap-x-4 gap-y-1 justify-center">
          <span className="font-medium text-white">{item.originalFileName ?? item.fileName}</span>
          <span>Score: {(item.score * 100).toFixed(1)}%</span>
          <span>{item.width} × {item.height}</span>
          <span className="text-white/50">{item.mimeType}</span>
          <span className="text-white/40">{currentIndex + 1} / {items.length}</span>
        </div>
      </div>
    </div>
  )
}
