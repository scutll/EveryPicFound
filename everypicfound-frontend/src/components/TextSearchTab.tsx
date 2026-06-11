import { useState } from 'react'

interface TextSearchTabProps {
  onSearch: (queryText: string, topK?: number) => void
  loading: boolean
}

export default function TextSearchTab({ onSearch, loading }: TextSearchTabProps) {
  const [queryText, setQueryText] = useState('')
  const [topK, setTopK] = useState(30)

  const handleSearch = () => {
    const trimmed = queryText.trim()
    if (trimmed) onSearch(trimmed, topK)
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !loading && queryText.trim()) {
      handleSearch()
    }
  }

  return (
    <div className="flex flex-col gap-4 items-center">
      <div className="w-full max-w-xl">
        <textarea
          value={queryText}
          onChange={(e) => setQueryText(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Describe what you're looking for..."
          maxLength={500}
          rows={3}
          disabled={loading}
          className="w-full px-4 py-3 border border-gray-300 rounded-lg text-sm resize-none
                     focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent
                     disabled:opacity-50 disabled:cursor-not-allowed"
        />
        <div className="text-right text-xs text-gray-400 mt-1">
          {queryText.length}/500
        </div>
      </div>

      <div className="flex items-center gap-3">
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
          disabled={!queryText.trim() || loading}
          className="px-6 py-2 bg-blue-600 text-white rounded-lg font-medium
                     hover:bg-blue-700 disabled:opacity-40 disabled:cursor-not-allowed
                     transition-colors duration-200"
        >
          {loading ? 'Searching...' : 'Search by Text'}
        </button>
      </div>
    </div>
  )
}
