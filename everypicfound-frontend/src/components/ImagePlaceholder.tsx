interface ImagePlaceholderProps {
  width: number
  height: number
  columnWidth: number
}

/**
 * Skeleton placeholder that matches the aspect ratio of the real image.
 */
export default function ImagePlaceholder({ width, height, columnWidth }: ImagePlaceholderProps) {
  const aspectRatio = height && width ? height / width : 1
  const cardHeight = columnWidth * aspectRatio

  return (
    <div
      className="skeleton rounded-lg"
      style={{ width: columnWidth, height: Math.max(cardHeight, 80) }}
    />
  )
}
