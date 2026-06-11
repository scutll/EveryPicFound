import { request, buildFormData } from './client'
import type { UploadResponse } from '../types'

/**
 * Upload a single image to the system.
 */
export async function uploadImage(imageFile: File): Promise<UploadResponse> {
  const fd = buildFormData({ imageFile })
  return request<UploadResponse>('/api/images/upload', {
    method: 'POST',
    body: fd,
  })
}
