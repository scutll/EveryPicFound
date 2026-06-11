import { useState, useCallback } from 'react'
import type { SearchResultItem, ImageSource } from '../types'
import { toImageSource } from '../types'
import { useImageLazyLoad } from '../hooks/useImageLazyLoad'
import ImagePlaceholder from './ImagePlaceholder'

interface ImageCardProps {
  item: SearchResultItem
  columnWidth: number
  onClick: (item: SearchResultItem) => void
}

export default function ImageCard({ item, columnWidth, onClick }: ImageCardProps) {
  const [loaded, setLoaded] = useState(false)
  const { refCallback, shouldLoad } = useImageLazyLoad({ rootMargin: '400px' })

  const source: ImageSource = toImageSource(item)
  const aspectRatio = item.height && item.width ? item.height / item.width : 1
  const cardHeight = columnWidth * aspectRatio

  const handleClick = useCallback(() => {
    if (loaded) onClick(item)
  }, [loaded, item, onClick])

  return (
    <div
      ref={refCallback}
      className="waterfall-card group cursor-pointer rounded-lg overflow-hidden bg-white shadow-sm
                 hover:shadow-md transition-shadow duration-200"
      style={{ width: columnWidth }}
      onClick={handleClick}
    >
      {/* Image area */}
      <div
        className="relative overflow-hidden bg-gray-100"
        style={{ width: columnWidth, height: Math.max(cardHeight, 80) }}
      >
        {!shouldLoad && (
          <ImagePlaceholder
            width={item.width}
            height={item.height}
            columnWidth={columnWidth}
          />
        )}

        {shouldLoad && !loaded && (
          <ImagePlaceholder
            width={item.width}
            height={item.height}
            columnWidth={columnWidth}
          />
        )}

        {shouldLoad && (
          <img
            src={source.displayUrl}
            alt={item.originalFileName ?? item.fileName}
            className={`
              absolute inset-0 w-full h-full object-cover img-fade-in
              ${loaded ? 'loaded' : ''}
            `}
            onLoad={() => setLoaded(true)}
            onError={(e) => {
              // Show a fallback on error
              (e.target as HTMLImageElement).style.display = 'none'
            }}
            loading="lazy"
          />
        )}

        {/* Score badge */}
        <div className="absolute top-2 right-2 bg-black/55 text-white text-xs px-2 py-0.5 rounded-full
                        opacity-0 group-hover:opacity-100 transition-opacity duration-200">
          {(item.score * 100).toFixed(0)}%
        </div>

        {/* Hover overlay: click to enlarge hint */}
        <div className="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors duration-200
                        flex items-center justify-center opacity-0 group-hover:opacity-100">
          <div className="bg-white/90 rounded-full p-2 shadow-lg">
            <svg className="w-5 h-5 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM10 7v3m0 0v3m0-3h3m-3 0H7" />
            </svg>
          </div>
        </div>
      </div>

      {/* Info bar */}
      <div className="px-2 py-1.5 text-xs text-gray-500 flex items-center justify-between">
        <span className="truncate max-w-[70%]" title={item.originalFileName}>
          {item.originalFileName ?? item.fileName}
        </span>
        <span className="text-gray-400">
          {item.width}×{item.height}
        </span>
      </div>
    </div>
  )
}
