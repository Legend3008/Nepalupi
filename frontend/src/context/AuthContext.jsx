import { createContext, useContext, useState, useEffect, useCallback } from 'react';

const AuthContext = createContext(null);

const STORAGE_KEYS = {
  TOKEN: 'nupi_token',
  USER: 'nupi_user',
  MPIN_SET: 'nupi_mpin_set',
  PHONE: 'nupi_phone',
};

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [mpinSet, setMpinSet] = useState(false);

  useEffect(() => {
    const savedToken = localStorage.getItem(STORAGE_KEYS.TOKEN);
    const savedUser = localStorage.getItem(STORAGE_KEYS.USER);
    const savedMpin = localStorage.getItem(STORAGE_KEYS.MPIN_SET);

    if (savedToken && savedUser) {
      setToken(savedToken);
      setUser(JSON.parse(savedUser));
      setIsAuthenticated(true);
      setMpinSet(savedMpin === 'true');
    }
    setIsLoading(false);
  }, []);

  const login = useCallback((userData, authToken) => {
    setUser(userData);
    setToken(authToken);
    setIsAuthenticated(true);
    localStorage.setItem(STORAGE_KEYS.TOKEN, authToken);
    localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(userData));
  }, []);

  const setMpinComplete = useCallback(() => {
    setMpinSet(true);
    localStorage.setItem(STORAGE_KEYS.MPIN_SET, 'true');
  }, []);

  const savePhone = useCallback((phone) => {
    localStorage.setItem(STORAGE_KEYS.PHONE, phone);
  }, []);

  const getPhone = useCallback(() => {
    return localStorage.getItem(STORAGE_KEYS.PHONE) || '';
  }, []);

  const updateUser = useCallback((updates) => {
    setUser(prev => {
      const updated = { ...prev, ...updates };
      localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(updated));
      return updated;
    });
  }, []);

  const logout = useCallback(() => {
    setUser(null);
    setToken(null);
    setIsAuthenticated(false);
    setMpinSet(false);
    Object.values(STORAGE_KEYS).forEach(key => localStorage.removeItem(key));
  }, []);

  return (
    <AuthContext.Provider value={{
      user, token, isLoading, isAuthenticated, mpinSet,
      login, logout, updateUser, setMpinComplete, savePhone, getPhone
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
