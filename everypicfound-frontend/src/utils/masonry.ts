import type { SearchResultItem } from '../types'

/**
 * Distribute items across N columns using the "shortest column first" algorithm.
 * Returns an array of N arrays, each containing the items assigned to that column.
 */
export function distributeToColumns(
  items: SearchResultItem[],
  columnCount: number,
  columnWidth: number,
): SearchResultItem[][] {
  if (columnCount <= 0) return [items]

  const columns: SearchResultItem[][] = Array.from({ length: columnCount }, () => [])
  const heights: number[] = new Array(columnCount).fill(0)

  for (const item of items) {
    // Find the shortest column
    let minIdx = 0
    for (let i = 1; i < columnCount; i++) {
      if (heights[i] < heights[minIdx]) {
        minIdx = i
      }
    }

    columns[minIdx].push(item)

    // Calculate card height: maintain aspect ratio
    const aspectRatio = item.height && item.width ? item.height / item.width : 1
    const cardHeight = columnWidth * aspectRatio + 40 // 40px for info bar
    heights[minIdx] += cardHeight + 12 // 12px gap
  }

  return columns
}

/**
 * Determine the optimal column count based on container width.
 */
export function calcColumnCount(
  containerWidth: number,
  columnWidth: number = 220,
  minColumns: number = 2,
  maxColumns: number = 6,
): number {
  if (containerWidth <= 0) return minColumns
  const count = Math.floor(containerWidth / (columnWidth + 12)) // 12px gap
  return Math.max(minColumns, Math.min(maxColumns, count))
}
