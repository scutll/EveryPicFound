import { useState } from 'react'
import { useSearch } from './hooks/useSearch'
import SearchBar from './components/SearchBar'
import WaterfallGrid from './components/WaterfallGrid'
import ResultInfo from './components/ResultInfo'
import UploadPage from './components/UploadPage'

type Page = 'search' | 'upload'

export default function App() {
  const [page, setPage] = useState<Page>('search')
  const { state, searchWithImage, searchWithText, searchWithHybrid } = useSearch()

  return (
    <div className="min-h-screen flex flex-col bg-gray-100">
      {/* Navbar */}
      <nav className="sticky top-0 z-40 bg-white border-b border-gray-200 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 h-14 flex items-center justify-between">
          {/* Logo */}
          <button
            onClick={() => setPage('search')}
            className="flex items-center gap-2 text-lg font-bold text-gray-800 hover:text-blue-600 transition-colors"
          >
            <svg className="w-7 h-7 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            EveryPicFound
          </button>

          {/* Nav tabs */}
          <div className="flex items-center gap-1">
            <button
              onClick={() => setPage('search')}
              className={`
                px-4 py-1.5 rounded-lg text-sm font-medium transition-colors duration-200
                ${page === 'search'
                  ? 'bg-blue-100 text-blue-700'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
                }
              `}
            >
              🔍 Search
            </button>
            <button
              onClick={() => setPage('upload')}
              className={`
                px-4 py-1.5 rounded-lg text-sm font-medium transition-colors duration-200
                ${page === 'upload'
                  ? 'bg-blue-100 text-blue-700'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
                }
              `}
            >
              ⬆️ Upload
            </button>
          </div>
        </div>
      </nav>

      {/* Main content */}
      <main className="flex-1 py-8 px-4">
        {page === 'search' && (
          <div className="space-y-8">
            <div className="text-center">
              <h1 className="text-3xl font-bold text-gray-800">Find Images</h1>
              <p className="text-gray-500 mt-1 text-sm">
                Search by text, image, or combine both
              </p>
            </div>

            <SearchBar
              onSearchImage={searchWithImage}
              onSearchText={searchWithText}
              onSearchHybrid={searchWithHybrid}
              loading={state.status === 'loading'}
            />

            {/* Error */}
            {state.status === 'error' && (
              <div className="max-w-3xl mx-auto bg-red-50 border border-red-200 text-red-700
                              rounded-lg px-4 py-3 text-sm text-center">
                {state.error}
              </div>
            )}

            {/* Results */}
            <ResultInfo
              response={state.response}
              loading={state.status === 'loading'}
            />

            {state.status === 'success' && state.response && state.response.items.length > 0 && (
              <WaterfallGrid items={state.response.items} />
            )}

            {state.status === 'success' && state.response && state.response.items.length === 0 && (
              <div className="text-center py-16 text-gray-400">
                <svg className="w-16 h-16 mx-auto mb-4 opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                    d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <p className="text-lg">No results found</p>
                <p className="text-sm mt-1">Try a different query or image</p>
              </div>
            )}
          </div>
        )}

        {page === 'upload' && (
          <UploadPage />
        )}
      </main>

      {/* Footer */}
      <footer className="text-center py-4 text-xs text-gray-400 border-t border-gray-200 bg-white">
        EveryPicFound — Image Search Engine
      </footer>
    </div>
  )
}
