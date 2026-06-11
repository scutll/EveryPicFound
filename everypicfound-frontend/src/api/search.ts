import { request, buildFormData } from './client'
import type { SearchResponse, TextSearchParams } from '../types'

const SEARCH_BASE = '/api/search'

/**
 * Search by image (multipart upload).
 */
export async function searchByImage(
  imageFile: File,
  topK?: number,
): Promise<SearchResponse> {
  const fd = buildFormData({ queryImage: imageFile, topK })
  return request<SearchResponse>(`${SEARCH_BASE}/image`, {
    method: 'POST',
    body: fd,
  })
}

/**
 * Search by text (JSON body).
 */
export async function searchByText(
  params: TextSearchParams,
): Promise<SearchResponse> {
  return request<SearchResponse>(`${SEARCH_BASE}/text`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params),
  })
}

/**
 * Hybrid search: image + text (multipart).
 */
export async function searchByHybrid(
  imageFile: File,
  queryText: string,
  topK?: number,
): Promise<SearchResponse> {
  const fd = buildFormData({ queryImage: imageFile, queryText, topK })
  return request<SearchResponse>(`${SEARCH_BASE}/hybrid`, {
    method: 'POST',
    body: fd,
  })
}
