import { useCallback, useRef, useState, type DragEvent, type ChangeEvent } from 'react'

interface DropZoneProps {
  onFile: (file: File) => void
  /** Currently selected file (for preview) */
  file: File | null
  disabled?: boolean
}

const ALLOWED_EXTENSIONS = ['jpg', 'jpeg', 'png', 'webp']
const MAX_SIZE = 10 * 1024 * 1024 // 10MB

function validateFile(file: File): string | null {
  const ext = file.name.split('.').pop()?.toLowerCase() ?? ''
  if (!ALLOWED_EXTENSIONS.includes(ext)) {
    return `Unsupported format: .${ext}. Allowed: ${ALLOWED_EXTENSIONS.join(', ')}`
  }
  if (file.size > MAX_SIZE) {
    return `File too large: ${(file.size / 1024 / 1024).toFixed(1)}MB (max 10MB)`
  }
  return null
}

export default function DropZone({ onFile, file, disabled }: DropZoneProps) {
  const [dragging, setDragging] = useState(false)
  const [preview, setPreview] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)
  const dragCounter = useRef(0)

  const handleFile = useCallback(
    (f: File) => {
      setError(null)
      const err = validateFile(f)
      if (err) {
        setError(err)
        return
      }
      onFile(f)
      // Generate preview
      const reader = new FileReader()
      reader.onload = () => setPreview(reader.result as string)
      reader.readAsDataURL(f)
    },
    [onFile],
  )

  const onDragEnter = (e: DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    dragCounter.current++
    if (e.dataTransfer.items?.length > 0) setDragging(true)
  }

  const onDragLeave = (e: DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    dragCounter.current--
    if (dragCounter.current === 0) setDragging(false)
  }

  const onDragOver = (e: DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
  }

  const onDrop = (e: DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setDragging(false)
    dragCounter.current = 0
    const files = e.dataTransfer.files
    if (files.length > 0) handleFile(files[0])
  }

  const onInputChange = (e: ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files
    if (files && files.length > 0) handleFile(files[0])
  }

  const clearFile = () => {
    setPreview(null)
    setError(null)
    if (inputRef.current) inputRef.current.value = ''
  }

  return (
    <div className="w-full">
      {/* Drop area */}
      <div
        onDragEnter={onDragEnter}
        onDragLeave={onDragLeave}
        onDragOver={onDragOver}
        onDrop={onDrop}
        onClick={() => !disabled && inputRef.current?.click()}
        className={`
          relative border-2 border-dashed rounded-xl p-8 text-center cursor-pointer
          transition-colors duration-200 select-none
          ${dragging
            ? 'border-blue-500 bg-blue-50'
            : 'border-gray-300 hover:border-gray-400 hover:bg-gray-50'
          }
          ${disabled ? 'opacity-50 cursor-not-allowed' : ''}
        `}
      >
        <input
          ref={inputRef}
          type="file"
          accept=".jpg,.jpeg,.png,.webp"
          className="hidden"
          onChange={onInputChange}
          disabled={disabled}
        />

        {preview && file ? (
          <div className="flex flex-col items-center gap-3">
            <img
              src={preview}
              alt="Preview"
              className="max-h-48 max-w-full rounded-lg object-contain shadow-sm"
            />
            <div className="text-sm text-gray-600">
              <p className="font-medium truncate max-w-xs">{file.name}</p>
              <p>{(file.size / 1024).toFixed(1)} KB</p>
            </div>
            <button
              type="button"
              onClick={(e) => { e.stopPropagation(); clearFile() }}
              className="text-xs text-red-500 hover:text-red-700 underline"
            >
              Remove / Re-select
            </button>
          </div>
        ) : (
          <div className="text-gray-500">
            <svg className="mx-auto mb-3 w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            <p className="font-medium">Drag & drop an image here</p>
            <p className="text-sm mt-1">or click to browse</p>
            <p className="text-xs mt-2 text-gray-400">JPEG, PNG, WebP — max 10MB</p>
          </div>
        )}
      </div>

      {error && (
        <p className="mt-2 text-sm text-red-500">{error}</p>
      )}
    </div>
  )
}
