import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { CurrentUser } from '../types';

interface AuthState {
  user: CurrentUser | null;
  loading: boolean;
}

const initialState: AuthState = { user: null, loading: true };

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setUser(state, action: PayloadAction<CurrentUser | null>) {
      state.user = action.payload;
      state.loading = false;
    },
    setLoading(state, action: PayloadAction<boolean>) {
      state.loading = action.payload;
    },
    clearUser(state) {
      state.user = null;
      state.loading = false;
    },
  },
});

export const { setUser, setLoading, clearUser } = authSlice.actions;
export default authSlice.reducer;
