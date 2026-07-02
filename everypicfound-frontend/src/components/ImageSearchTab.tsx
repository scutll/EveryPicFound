import { useState } from 'react'
import DropZone from './DropZone'

interface ImageSearchTabProps {
  onSearch: (file: File, topK?: number) => void
  loading: boolean
}

export default function ImageSearchTab({ onSearch, loading }: ImageSearchTabProps) {
  const [file, setFile] = useState<File | null>(null)
  const [topK, setTopK] = useState(30)

  const handleSearch = () => {
    if (file) onSearch(file, topK)
  }

  return (
    <div className="flex flex-col gap-4">
      <DropZone onFile={setFile} file={file} disabled={loading} />

      <div className="flex items-center gap-3 justify-center">
        <label className="text-sm text-gray-600 flex items-center gap-1">
          TopK:
          <input
            type="number"
            min={1}
            max={50}
            value={topK}
            onChange={(e) => setTopK(Number(e.target.value))}
            className="w-16 px-2 py-1 border border-gray-300 rounded text-sm text-center"
          />
        </label>

        <button
          onClick={handleSearch}
          disabled={!file || loading}
          className="px-6 py-2 bg-blue-600 text-white rounded-lg font-medium
                     hover:bg-blue-700 disabled:opacity-40 disabled:cursor-not-allowed
                     transition-colors duration-200"
        >
          {loading ? 'Searching...' : 'Search by Image'}
        </button>
      </div>
    </div>
  )
}
