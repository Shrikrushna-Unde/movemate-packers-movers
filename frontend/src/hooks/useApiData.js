import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * A small custom hook for "fetch something, track loading and errors, allow a reload".
 *
 * Every data page needs the same four pieces of state. Writing them out in each component is
 * about fifteen lines of identical code per page, and the cleanup step is the one people
 * forget. A custom hook is just a function that calls other hooks - which is the whole point
 * of hooks: behaviour becomes reusable without inheritance or wrapper components.
 *
 * In a production codebase you would likely reach for TanStack Query instead, which adds
 * caching, deduplication and background refetching. Using it here would hide the mechanics
 * this project is meant to demonstrate - but knowing when to reach for it is the better answer
 * in an interview.
 *
 * @param {Function} fetcher an async function that returns the data
 * @param {Array}    deps    re-run when any of these change (same contract as useEffect)
 */
export function useApiData(fetcher, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  /*
   * The fetcher is usually an inline arrow function, so it is a NEW function on every render.
   * Putting it in the dependency array would cause an infinite fetch loop. Storing it in a ref
   * gives the effect access to the latest version without making it a dependency - the
   * standard solution to the "stale closure vs infinite loop" dilemma.
   */
  const fetcherRef = useRef(fetcher);
  fetcherRef.current = fetcher;

  const [reloadToken, setReloadToken] = useState(0);

  /** Call after a mutation to refresh the data. */
  const reload = useCallback(() => setReloadToken((n) => n + 1), []);

  useEffect(() => {
    // The cancellation flag. Without it, a response that arrives after the user has navigated
    // away calls setState on an unmounted component - a memory leak, and in React 18
    // StrictMode an easy one to trigger locally.
    let cancelled = false;

    setLoading(true);
    setError(null);

    fetcherRef
      .current()
      .then((result) => {
        if (!cancelled) setData(result);
      })
      .catch((err) => {
        // Our axios interceptor already normalised this into { status, message, fieldErrors }.
        if (!cancelled) setError(err?.message || 'Could not load data');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, reloadToken]);

  return { data, loading, error, reload, setData };
}
