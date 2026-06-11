import { useCallback, useEffect, useRef, useState } from 'react'
import type { SearchResultItem } from '../types'
import { distributeToColumns, calcColumnCount } from '../utils/masonry'
import ImageCard from './ImageCard'
import Lightbox from './Lightbox'

interface WaterfallGridProps {
  items: SearchResultItem[]
}

const COLUMN_WIDTH = 220
const GAP = 12

export default function WaterfallGrid({ items }: WaterfallGridProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const [columnCount, setColumnCount] = useState(4)

  // Lightbox state
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null)

  // Recalculate column count on resize
  useEffect(() => {
    const el = containerRef.current
    if (!el) return

    const observer = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const width = entry.contentRect.width
        setColumnCount(calcColumnCount(width, COLUMN_WIDTH, 2, 6))
      }
    })

    observer.observe(el)
    // Initial calc
    setColumnCount(calcColumnCount(el.clientWidth, COLUMN_WIDTH, 2, 6))

    return () => observer.disconnect()
  }, [])

  const columns = distributeToColumns(items, columnCount, COLUMN_WIDTH)

  const openLightbox = useCallback((item: SearchResultItem) => {
    const idx = items.findIndex((i) => i.imageId === item.imageId)
    if (idx >= 0) setLightboxIndex(idx)
  }, [items])

  const closeLightbox = useCallback(() => setLightboxIndex(null), [])
  const prevImage = useCallback(() => {
    setLightboxIndex((prev) => {
      if (prev === null) return null
      return prev > 0 ? prev - 1 : prev
    })
  }, [])
  const nextImage = useCallback(() => {
    setLightboxIndex((prev) => {
      if (prev === null) return null
      return prev < items.length - 1 ? prev + 1 : prev
    })
  }, [items.length])

  if (items.length === 0) return null

  return (
    <>
      <div
        ref={containerRef}
        className="w-full px-2"
        style={{ paddingTop: 4 }}
      >
        <div className="flex justify-center" style={{ gap: GAP }}>
          {columns.map((colItems, colIdx) => (
            <div
              key={colIdx}
              className="flex flex-col"
              style={{ width: COLUMN_WIDTH, gap: GAP }}
            >
              {colItems.map((item) => (
                <ImageCard
                  key={item.imageId}
                  item={item}
                  columnWidth={COLUMN_WIDTH}
                  onClick={openLightbox}
                />
              ))}
            </div>
          ))}
        </div>
      </div>

      {/* Lightbox */}
      {lightboxIndex !== null && (
        <Lightbox
          items={items}
          currentIndex={lightboxIndex}
          onClose={closeLightbox}
          onPrev={prevImage}
          onNext={nextImage}
        />
      )}
    </>
  )
}
