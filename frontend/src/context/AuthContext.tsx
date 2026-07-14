import { createContext, useCallback, useContext, useEffect, useMemo, useState, ReactNode } from 'react';
import { authApi } from '../api/endpoints';
import { useAppDispatch } from '../store/hooks';
import { setUser as setUserAction, clearUser, setLoading as setLoadingAction } from '../store/authSlice';
import { baseApi } from '../store/api/baseApi';
import type { CurrentUser } from '../types';

interface AuthContextValue {
  user: CurrentUser | null;
  loading: boolean;
  login: (companyCode: string, username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  hasPermission: (code: string) => boolean;
  hasAnyPermission: (codes: string[]) => boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const dispatch = useAppDispatch();
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);

  const loadUser = useCallback(async () => {
    // No client-readable token to check — the access_token cookie (if any) is httpOnly, so
    // we just attempt the call and treat a 401 as "not logged in".
    try {
      const me = await authApi.me();
      setUser(me);
      dispatch(setUserAction(me));
    } catch {
      setUser(null);
      dispatch(clearUser());
    } finally {
      setLoading(false);
      dispatch(setLoadingAction(false));
    }
  }, [dispatch]);

  useEffect(() => {
    loadUser();
  }, [loadUser]);

  const login = useCallback(async (companyCode: string, username: string, password: string) => {
    const result = await authApi.login(companyCode, username, password);
    if (result.mfaRequired) {
      // MFA verification isn't wired up in this UI yet — surface a clear error rather than
      // silently proceeding as if login succeeded.
      throw new Error('This account requires MFA verification, which isn\'t supported here yet.');
    }
    const me = await authApi.me();
    setUser(me);
    dispatch(setUserAction(me));
  }, [dispatch]);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // best-effort
    } finally {
      setUser(null);
      dispatch(clearUser());
      dispatch(baseApi.util.resetApiState());
      window.location.href = '/login';
    }
  }, [dispatch]);

  const hasPermission = useCallback(
    (code: string) => !!user && user.permissions.includes(code),
    [user]
  );

  const hasAnyPermission = useCallback(
    (codes: string[]) => !!user && codes.some((c) => user.permissions.includes(c)),
    [user]
  );

  const value = useMemo(
    () => ({ user, loading, login, logout, hasPermission, hasAnyPermission }),
    [user, loading, login, logout, hasPermission, hasAnyPermission]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
