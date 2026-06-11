import { useState } from 'react'
import DropZone from './DropZone'

interface HybridSearchTabProps {
  onSearch: (file: File, queryText: string, topK?: number) => void
  loading: boolean
}

export default function HybridSearchTab({ onSearch, loading }: HybridSearchTabProps) {
  const [file, setFile] = useState<File | null>(null)
  const [queryText, setQueryText] = useState('')
  const [topK, setTopK] = useState(30)

  const handleSearch = () => {
    if (file && queryText.trim()) {
      onSearch(file, queryText.trim(), topK)
    }
  }

  const canSearch = file !== null && queryText.trim().length > 0 && !loading

  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Image drop zone */}
        <div>
          <p className="text-sm text-gray-500 mb-2 font-medium">Query Image</p>
          <DropZone onFile={setFile} file={file} disabled={loading} />
        </div>

        {/* Text input */}
        <div>
          <p className="text-sm text-gray-500 mb-2 font-medium">Query Text</p>
          <textarea
            value={queryText}
            onChange={(e) => setQueryText(e.target.value)}
            placeholder="Describe additional details..."
            maxLength={500}
            rows={4}
            disabled={loading}
            className="w-full px-4 py-3 border border-gray-300 rounded-lg text-sm resize-none
                       focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent
                       disabled:opacity-50 disabled:cursor-not-allowed"
          />
          <div className="text-right text-xs text-gray-400 mt-1">
            {queryText.length}/500
          </div>
        </div>
      </div>

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
          disabled={!canSearch}
          className="px-6 py-2 bg-blue-600 text-white rounded-lg font-medium
                     hover:bg-blue-700 disabled:opacity-40 disabled:cursor-not-allowed
                     transition-colors duration-200"
        >
          {loading ? 'Searching...' : 'Search Hybrid'}
        </button>
      </div>
    </div>
  )
}
