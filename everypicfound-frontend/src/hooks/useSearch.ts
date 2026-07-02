import type { SearchResponse, SearchState } from '../types'
import { useCallback, useState } from 'react'
import { searchByImage, searchByText, searchByHybrid } from '../api/search'

interface UseSearchReturn {
  state: SearchState
  searchWithImage: (file: File, topK?: number) => Promise<void>
  searchWithText: (queryText: string, topK?: number) => Promise<void>
  searchWithHybrid: (file: File, queryText: string, topK?: number) => Promise<void>
  reset: () => void
}

const IDLE: SearchState = { status: 'idle', response: null, error: null }

export function useSearch(): UseSearchReturn {
  const [state, setState] = useState<SearchState>(IDLE)

  const runSearch = useCallback(
    async (
      fn: () => Promise<SearchResponse>,
    ) => {
      setState({ status: 'loading', response: null, error: null })
      try {
        const response = await fn()
        setState({ status: 'success', response, error: null })
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Unknown error'
        setState({ status: 'error', response: null, error: message })
      }
    },
    [],
  )

  const searchWithImage = useCallback(
    (file: File, topK?: number) => runSearch(() => searchByImage(file, topK)),
    [runSearch],
  )

  const searchWithText = useCallback(
    (queryText: string, topK?: number) => runSearch(() => searchByText({ queryText, topK })),
    [runSearch],
  )

  const searchWithHybrid = useCallback(
    (file: File, queryText: string, topK?: number) =>
      runSearch(() => searchByHybrid(file, queryText, topK)),
    [runSearch],
  )

  const reset = useCallback(() => setState(IDLE), [])

  return { state, searchWithImage, searchWithText, searchWithHybrid, reset }
}
