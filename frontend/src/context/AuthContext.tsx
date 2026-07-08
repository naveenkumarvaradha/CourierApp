import { createContext, useCallback, useContext, useEffect, useMemo, useState, ReactNode } from 'react';
import { authApi } from '../api/endpoints';
import { tokenStore } from '../api/client';
import { useAppDispatch } from '../store/hooks';
import { setUser as setUserAction, clearUser, setLoading as setLoadingAction } from '../store/authSlice';
import { baseApi } from '../store/api/baseApi';
import type { CurrentUser } from '../types';

export class MfaRequiredError extends Error {
  constructor(public readonly mfaPendingToken: string) {
    super('MFA required');
  }
}

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
    if (!tokenStore.getAccess()) {
      setLoading(false);
      dispatch(setLoadingAction(false));
      return;
    }
    try {
      const me = await authApi.me();
      setUser(me);
      dispatch(setUserAction(me));
    } catch {
      tokenStore.clear();
      setUser(null);
      dispatch(clearUser());
    } finally {
      setLoading(false);
    }
  }, [dispatch]);

  useEffect(() => {
    loadUser();
  }, [loadUser]);

  const login = useCallback(async (companyCode: string, username: string, password: string) => {
    const tokens = await authApi.login(companyCode, username, password);
    if (tokens.mfaRequired && tokens.mfaPendingToken) {
      throw new MfaRequiredError(tokens.mfaPendingToken);
    }
    tokenStore.set(tokens.accessToken!, tokens.refreshToken!);
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
      tokenStore.clear();
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
