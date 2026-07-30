import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useColumnVisibility } from './useColumnVisibility';

describe('useColumnVisibility', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('starts from the given defaults when nothing is stored', () => {
    const { result } = renderHook(() => useColumnVisibility(1, 'dc-list', { unit: false }));
    const [model] = result.current;
    expect(model).toEqual({ unit: false });
  });

  it('persists changes to localStorage scoped by user and page', () => {
    const { result } = renderHook(() => useColumnVisibility(7, 'dc-list', {}));

    act(() => {
      const [, onChange] = result.current;
      onChange({ status: false });
    });

    expect(result.current[0]).toEqual({ status: false });
    expect(localStorage.getItem('col_vis_7_dc-list')).toBe(JSON.stringify({ status: false }));
  });

  it('merges stored overrides on top of defaults on next mount', () => {
    localStorage.setItem('col_vis_7_dc-list', JSON.stringify({ status: false }));

    const { result } = renderHook(() => useColumnVisibility(7, 'dc-list', { status: true, unit: true }));

    expect(result.current[0]).toEqual({ status: false, unit: true });
  });

  it('scopes the storage key to "anon" when no userId is given', () => {
    const { result } = renderHook(() => useColumnVisibility(undefined, 'dc-list', {}));

    act(() => {
      const [, onChange] = result.current;
      onChange({ unit: false });
    });

    expect(localStorage.getItem('col_vis_anon_dc-list')).toBe(JSON.stringify({ unit: false }));
  });
});
