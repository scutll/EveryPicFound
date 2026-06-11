import { useState } from 'react'
import type { SearchType } from '../types'
import ImageSearchTab from './ImageSearchTab'
import TextSearchTab from './TextSearchTab'
import HybridSearchTab from './HybridSearchTab'

interface SearchBarProps {
  onSearchImage: (file: File, topK?: number) => void
  onSearchText: (queryText: string, topK?: number) => void
  onSearchHybrid: (file: File, queryText: string, topK?: number) => void
  loading: boolean
}

const TABS: { key: SearchType; label: string; icon: string }[] = [
  { key: 'TEXT', label: 'Text Search', icon: '🔤' },
  { key: 'IMAGE', label: 'Image Search', icon: '🖼️' },
  { key: 'HYBRID', label: 'Hybrid Search', icon: '🔀' },
]

export default function SearchBar({ onSearchImage, onSearchText, onSearchHybrid, loading }: SearchBarProps) {
  const [activeTab, setActiveTab] = useState<SearchType>('TEXT')

  return (
    <div className="w-full max-w-3xl mx-auto">
      {/* Tab buttons */}
      <div className="flex justify-center gap-1 mb-5">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`
              px-4 py-2 rounded-t-lg text-sm font-medium transition-colors duration-200
              ${activeTab === tab.key
                ? 'bg-white text-blue-600 border-t border-x border-gray-200'
                : 'text-gray-500 hover:text-gray-700 hover:bg-gray-100'
              }
            `}
          >
            <span className="mr-1.5">{tab.icon}</span>
            {tab.label}
          </button>
        ))}
      </div>

      {/* Tab content */}
      <div className="bg-white rounded-b-xl rounded-tr-xl border border-gray-200 p-6 shadow-sm">
        {activeTab === 'IMAGE' && (
          <ImageSearchTab onSearch={onSearchImage} loading={loading} />
        )}
        {activeTab === 'TEXT' && (
          <TextSearchTab onSearch={onSearchText} loading={loading} />
        )}
        {activeTab === 'HYBRID' && (
          <HybridSearchTab onSearch={onSearchHybrid} loading={loading} />
        )}
      </div>
    </div>
  )
}
