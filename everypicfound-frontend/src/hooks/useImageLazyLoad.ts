import { useCallback, useRef, useState } from 'react'

interface UseImageLazyLoadOptions {
  /** Root margin for IntersectionObserver (default: '200px') */
  rootMargin?: string
}

/**
 * Hook that returns a ref callback and a loaded flag.
 * The image starts loading only when the element enters the viewport.
 */
export function useImageLazyLoad(opts: UseImageLazyLoadOptions = {}) {
  const { rootMargin = '200px' } = opts
  const [shouldLoad, setShouldLoad] = useState(false)
  const observerRef = useRef<IntersectionObserver | null>(null)

  const refCallback = useCallback(
    (el: HTMLDivElement | null) => {
      // Cleanup previous observer
      if (observerRef.current) {
        observerRef.current.disconnect()
        observerRef.current = null
      }

      if (!el) return

      // If already triggered, keep it
      if (shouldLoad) {
        return
      }

      const observer = new IntersectionObserver(
        (entries) => {
          for (const entry of entries) {
            if (entry.isIntersecting) {
              setShouldLoad(true)
              observer.disconnect()
              observerRef.current = null
            }
          }
        },
        { rootMargin },
      )

      observer.observe(el)
      observerRef.current = observer
    },
    [rootMargin, shouldLoad],
  )

  return { refCallback, shouldLoad }
}
