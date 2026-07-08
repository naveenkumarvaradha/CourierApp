import { useState, useCallback } from 'react';
import type { GridColumnVisibilityModel } from '@mui/x-data-grid';

/**
 * Persists DataGrid column visibility per user per page in localStorage.
 * Key format: `col_vis_{userId}_{pageKey}`
 */
export function useColumnVisibility(
  userId: number | undefined,
  pageKey: string,
  defaults: GridColumnVisibilityModel = {}
): [GridColumnVisibilityModel, (m: GridColumnVisibilityModel) => void] {
  const storageKey = `col_vis_${userId ?? 'anon'}_${pageKey}`;

  const [model, setModel] = useState<GridColumnVisibilityModel>(() => {
    try {
      const stored = localStorage.getItem(storageKey);
      return stored ? { ...defaults, ...JSON.parse(stored) } : defaults;
    } catch {
      return defaults;
    }
  });

  const onChange = useCallback(
    (newModel: GridColumnVisibilityModel) => {
      setModel(newModel);
      try {
        localStorage.setItem(storageKey, JSON.stringify(newModel));
      } catch {
        // quota exceeded — silently ignore
      }
    },
    [storageKey]
  );

  return [model, onChange];
}
