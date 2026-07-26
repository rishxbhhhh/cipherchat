import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import api from '../api/client';

const AuthContext = createContext(null);

function parseJwt(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return { email: payload.sub, role: payload.role || 'USER' };
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      const parsed = parseJwt(token);
      if (parsed) {
        setUser({ email: parsed.email, role: parsed.role, token });
      }
    }
    setLoading(false);
  }, []);

  const login = useCallback(async (username, password) => {
    const email = username.includes('@') ? username : username + '@cc.io';
    const res = await api.post('/auth/login', { email, password });
    const { accessToken, refreshToken } = res.data;
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    const parsed = parseJwt(accessToken);
    setUser({ email: parsed?.email || email, role: parsed?.role || 'USER', token: accessToken });
    return res.data;
  }, []);

  const register = useCallback(async (username, password) => {
    const email = username.includes('@') ? username : username + '@cc.io';
    await api.post('/auth/register', { email, password });
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout');
    } catch { /* ignore */ }
    localStorage.clear();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be inside AuthProvider');
  return ctx;
}
