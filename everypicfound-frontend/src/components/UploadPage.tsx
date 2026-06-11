import { useState } from 'react'
import { uploadImage } from '../api/upload'
import type { UploadResponse } from '../types'
import DropZone from './DropZone'

interface HistoryEntry {
  imageId: number
  originalFileName: string
  imageUrl: string
  timestamp: number
}

function loadHistory(): HistoryEntry[] {
  try {
    const raw = localStorage.getItem('uploadHistory')
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveHistory(entry: HistoryEntry) {
  const history = loadHistory()
  // Deduplicate by imageId, keep most recent first
  const filtered = history.filter((h) => h.imageId !== entry.imageId)
  filtered.unshift(entry)
  localStorage.setItem('uploadHistory', JSON.stringify(filtered.slice(0, 20)))
}

export default function UploadPage() {
  const [file, setFile] = useState<File | null>(null)
  const [uploading, setUploading] = useState(false)
  const [result, setResult] = useState<UploadResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [history, setHistory] = useState<HistoryEntry[]>(loadHistory)
  const [copied, setCopied] = useState(false)

  const handleUpload = async () => {
    if (!file) return
    setUploading(true)
    setError(null)
    setResult(null)
    try {
      const data = await uploadImage(file)
      setResult(data)
      const entry: HistoryEntry = {
        imageId: data.imageId,
        originalFileName: data.originalFileName,
        imageUrl: data.imageUrl,
        timestamp: Date.now(),
      }
      saveHistory(entry)
      setHistory(loadHistory())
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed')
    } finally {
      setUploading(false)
    }
  }

  const copyUrl = async (url: string) => {
    try {
      await navigator.clipboard.writeText(url)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      // Fallback
      const input = document.createElement('input')
      input.value = url
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="text-center">
        <h2 className="text-xl font-semibold text-gray-800">Upload Image</h2>
        <p className="text-sm text-gray-500 mt-1">Add images to the searchable database</p>
      </div>

      <DropZone onFile={setFile} file={file} disabled={uploading} />

      <div className="flex justify-center">
        <button
          onClick={handleUpload}
          disabled={!file || uploading}
          className="px-8 py-2.5 bg-green-600 text-white rounded-lg font-medium
                     hover:bg-green-700 disabled:opacity-40 disabled:cursor-not-allowed
                     transition-colors duration-200"
        >
          {uploading ? (
            <span className="inline-flex items-center gap-2">
              <svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
              </svg>
              Uploading...
            </span>
          ) : 'Upload'}
        </button>
      </div>

      {/* Error */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 text-sm">
          {error}
        </div>
      )}

      {/* Success result */}
      {result && (
        <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm space-y-3">
          <h3 className="font-semibold text-gray-800">Upload Success ✓</h3>
          <div className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
            <span className="text-gray-500">Image ID:</span>
            <span className="font-mono">{result.imageId}</span>
            <span className="text-gray-500">File:</span>
            <span className="truncate">{result.originalFileName}</span>
            <span className="text-gray-500">Image Status:</span>
            <span className="text-green-600">{result.imageStatus}</span>
            <span className="text-gray-500">Vector Status:</span>
            <span className="text-amber-600">{result.vectorStatus}</span>
            <span className="text-gray-500">URL:</span>
            <span className="font-mono text-xs truncate">{result.imageUrl}</span>
          </div>
          <div className="flex gap-2 pt-2">
            <button
              onClick={() => copyUrl(result.imageUrl)}
              className="px-4 py-1.5 text-sm border border-gray-300 rounded-lg
                         hover:bg-gray-50 transition-colors"
            >
              {copied ? 'Copied!' : 'Copy URL'}
            </button>
          </div>
        </div>
      )}

      {/* Upload history */}
      {history.length > 0 && (
        <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm">
          <h3 className="font-semibold text-gray-800 mb-3">Recent Uploads</h3>
          <div className="space-y-2">
            {history.slice(0, 10).map((entry) => (
              <div
                key={entry.imageId}
                className="flex items-center justify-between text-sm py-1 px-2 rounded hover:bg-gray-50"
              >
                <span className="truncate max-w-[60%]" title={entry.originalFileName}>
                  {entry.originalFileName}
                </span>
                <span className="text-xs text-gray-400 font-mono">#{entry.imageId}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
