import React, { useState, type FormEvent } from 'react';
import type { StatusMessage } from '../../types/auth';
import { AuthLayout } from '../../components/auth/AuthLayout';
import { InputField } from '../../components/auth/InputField';
import { StatusDisplay } from '../../components/auth/StatusDisplay';
import { OAuthButtons } from '../../components/auth/OAuthButtons';
import { useServerConfig } from '../../hooks/useServerConfig';
import { useAuth } from '../../hooks/useAuth';
import { Link, useNavigate } from 'react-router-dom';
import { useTheme } from '../../hooks/useTheme';

const Login: React.FC = () => {
  const { serverConfig } = useServerConfig();
  const { login } = useAuth();
  const { currentTheme } = useTheme();
  
  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [stayLoggedIn, setStayLoggedIn] = useState(false);
  const [status, setStatus] = useState<StatusMessage>({ text: '', type: 'hidden' });

  const isDark = currentTheme === 'dark';

  const buttonStyle = {
    backgroundColor: serverConfig.serverColor,
    '--server-color': serverConfig.serverColor,
  } as React.CSSProperties;

  const handleLoginSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setStatus({ text: 'Logging in...', type: 'hidden' });

    try {
      const response = await login(username, password, stayLoggedIn);
            
      if (!response.user.isVerified) {
          setStatus({ text: 'Login successful! Two-Factor Authentication required. Redirecting...', type: 'success' });
          navigate('/verify');
      } else {
          setStatus({ text: 'Login successful! Redirecting...', type: 'success' });

          setTimeout(() => {
            navigate('/');
          }, 1500);
      }
    } 
    catch (error) {
      let errorMessage = 'An unknown error occurred.';

      if (error instanceof Error) {
        errorMessage = error.message;
      } else if (typeof error === 'string') {
        errorMessage = error;
      }

      setStatus({ 
        text: errorMessage, 
        type: 'error' 
      });
    }
  }

  const handleOAuthClick = (service: string) => {
    alert(`Redirecting to ${service} for authentication...`);
  };

  return (
    <AuthLayout title="Login to" pageTitle="Login">
      <form onSubmit={handleLoginSubmit} className="space-y-6">

        <StatusDisplay status={status} />

        <InputField
          id="username"
          label="Username"
          type="text"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          error={status.type === 'error' ? ' ' : null}
        />

        <InputField
          id="password"
          label="Password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={status.type === 'error' ? ' ' : null}
          showToggle
        />

        <div className="flex items-center">
          <input
            type="checkbox"
            id="stay-logged-in"
            checked={stayLoggedIn}
            onChange={(e) => setStayLoggedIn(e.target.checked)}
            className="mr-2 h-4 w-4 rounded"
            style={{accentColor: serverConfig.serverColor}}
          />
          <label 
            htmlFor="stay-logged-in" 
            className={`text-sm font-medium ${isDark ? 'text-[#a1a1aa]' : 'text-[#52525b]'}`}
          >
            Stay logged in
          </label>
        </div>

        <button
          type="submit"
          className="w-full text-white font-bold py-3 rounded-xl transition duration-300 transform hover:scale-[1.01] active:scale-[0.99] custom-button-glow"
          style={buttonStyle}
        >
          Log In
        </button>

        <OAuthButtons action="login" onOAuthClick={handleOAuthClick} />
      </form>

      <p className={`mt-6 text-center text-sm ${isDark ? 'text-[#a1a1aa]' : 'text-[#52525b]'}`}>
        Don't have an account?
        <Link
          to="/register"
          className="ml-1 font-semibold hover:underline transition-colors duration-300"
          style={{ color: serverConfig.serverColor }}
        >
          Register here
        </Link>
      </p>
    </AuthLayout>
  );
};

export default Login;