// ============================================================
// Unified API response wrapper (backend: Result<T>)
// ============================================================
export interface ApiResult<T> {
  code: number
  message: string
  data: T | null
  requestId: string
}

// ============================================================
// Search types
// ============================================================
export type SearchType = 'IMAGE' | 'TEXT' | 'HYBRID'

export interface SearchResultItem {
  imageId: number
  imageUrl: string
  fileName: string
  originalFileName: string
  score: number
  width: number
  height: number
  mimeType: string
}

export interface SearchResponse {
  searchType: SearchType
  total: number
  items: SearchResultItem[]
  costMs: number
}

// ============================================================
// Image source abstraction — supports future thumbnail + full-resolution split
// Currently all three fields point to the same imageUrl.
// ============================================================
export interface ImageSource {
  /** Thumbnail for waterfall display (future use, currently = imageUrl) */
  thumbnailUrl?: string
  /** Current display URL (= imageUrl) */
  displayUrl: string
  /** Full-resolution URL for lightbox (= imageUrl, future may differ) */
  fullUrl: string
}

export function toImageSource(item: SearchResultItem): ImageSource {
  return {
    thumbnailUrl: undefined,
    displayUrl: item.imageUrl,
    fullUrl: item.imageUrl,
  }
}

// ============================================================
// Upload types
// ============================================================
export interface UploadResponse {
  imageId: number
  originalFileName: string
  imageUrl: string
  imageStatus: string
  vectorStatus: string
}

// ============================================================
// Search request params
// ============================================================
export interface TextSearchParams {
  queryText: string
  topK?: number
}

export interface ImageSearchParams {
  queryImage: File
  topK?: number
}

export interface HybridSearchParams {
  queryImage: File
  queryText: string
  topK?: number
}

// ============================================================
// UI state
// ============================================================
export type SearchStatus = 'idle' | 'loading' | 'success' | 'error'

export interface SearchState {
  status: SearchStatus
  response: SearchResponse | null
  error: string | null
}
