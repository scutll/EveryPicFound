import type { SearchResponse } from '../types'

interface ResultInfoProps {
  response: SearchResponse | null
  loading: boolean
}

export default function ResultInfo({ response, loading }: ResultInfoProps) {
  if (loading) {
    return (
      <div className="text-center py-8 text-gray-500">
        <div className="inline-flex items-center gap-2">
          <svg className="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
          <span>Searching...</span>
        </div>
      </div>
    )
  }

  if (!response && !loading) return null

  return (
    <div className="text-center py-3 text-sm text-gray-500">
      {response && (
        <span>
          <strong>{response.total}</strong> results &middot;{' '}
          <span className="text-gray-400">{response.costMs}ms</span>
        </span>
      )}
    </div>
  )
}
