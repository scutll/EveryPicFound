import type { ApiResult } from '../types'

/**
 * Thin fetch wrapper that unwraps the backend's ApiResult<T> envelope.
 *  - code === 0 → returns data
 *  - code !== 0 → throws with server message
 *  - network error → throws with descriptive message
 */
export async function request<T>(
  url: string,
  options?: RequestInit,
): Promise<T> {
  let resp: Response
  try {
    resp = await fetch(url, options)
  } catch {
    throw new Error('Network error — please check your connection.')
  }

  // The image file endpoint returns raw binary, not JSON
  const contentType = resp.headers.get('content-type') ?? ''
  if (!contentType.includes('application/json')) {
    if (!resp.ok) {
      throw new Error(`Request failed: ${resp.status} ${resp.statusText}`)
    }
    return resp as unknown as T
  }

  let body: ApiResult<T>
  try {
    body = await resp.json()
  } catch {
    throw new Error('Failed to parse server response.')
  }

  if (body.code !== 0) {
    throw new Error(body.message ?? `Server error (code: ${body.code})`)
  }

  return body.data as T
}

/**
 * Build a FormData body for multipart uploads.
 */
export function buildFormData(fields: Record<string, string | File | number | undefined>): FormData {
  const fd = new FormData()
  for (const [key, value] of Object.entries(fields)) {
    if (value === undefined || value === null) continue
    if (value instanceof File) {
      fd.append(key, value)
    } else {
      fd.append(key, String(value))
    }
  }
  return fd
}
