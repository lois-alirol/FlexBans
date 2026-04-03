import React, { useState, useEffect, useMemo, useRef } from 'react';
import { ThemeContext } from '@hooks/useTheme';

export const ThemeProvider: React.FC<{
  children: React.ReactNode;
  defaultTheme?: string
}> = ({ children, defaultTheme = 'light' }) => {

  const getInitialTheme = (): string => {
    const storedTheme = localStorage.getItem('app-theme');
    if (storedTheme) return storedTheme;

    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : defaultTheme;
  };

  const [theme, setTheme] = useState<string>(getInitialTheme);
  const previousTheme = useRef<string>(theme);

  useEffect(() => {
    const root = window.document.documentElement;

    root.classList.remove(previousTheme.current);
    root.classList.add(theme);
    root.setAttribute('data-theme', theme);

    localStorage.setItem('app-theme', theme);
    previousTheme.current = theme;
  }, [theme]);

  const contextValue = useMemo(() => ({
    theme,
    setTheme,
  }), [theme]);

  return (
      <ThemeContext.Provider value={contextValue}>
        {children}
      </ThemeContext.Provider>
  );
};