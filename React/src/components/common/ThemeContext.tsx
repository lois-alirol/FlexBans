import React, { useState, useEffect, useMemo } from 'react';
import { ThemeContext } from '../../hooks/useTheme';

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const getInitialTheme = (): boolean => {
    const storedTheme = localStorage.getItem('isDarkMode');
    if (storedTheme !== null) {
      return storedTheme === 'true';
    }
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  };

  const [isDarkMode, setInternalIsDarkMode] = useState<boolean>(getInitialTheme);
  const currentTheme: 'dark' | 'light' = isDarkMode ? 'dark' : 'light';

  useEffect(() => {
    localStorage.setItem('isDarkMode', String(isDarkMode));

    if (isDarkMode) {
      document.body.classList.add('dark');
    } else {
      document.body.classList.remove('dark');
    }
    
    return () => {};
  }, [isDarkMode]);

  const setIsDarkMode = (isDark: boolean) => {
    setInternalIsDarkMode(isDark);
  };
  
  const contextValue = useMemo(() => ({
    isDarkMode,
    setIsDarkMode,
    currentTheme,
  }), [isDarkMode, currentTheme]);

  return (
    <ThemeContext.Provider value={contextValue}>
      {children}
    </ThemeContext.Provider>
  );
};