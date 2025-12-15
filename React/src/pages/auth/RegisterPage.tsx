import React, { useEffect, useState, type FormEvent } from 'react';
import type { StatusMessage } from '../../types/auth';
import { AuthLayout } from '../../components/auth/AuthLayout';
import { InputField } from '../../components/auth/InputField';
import { StatusDisplay } from '../../components/auth/StatusDisplay';
import { OAuthButtons } from '../../components/auth/OAuthButtons';
import { useServerConfig } from '../../hooks/useServerConfig';
import authService from '../../services/authService';
import { useAuth } from '../../hooks/useAuth';
import { Link, useNavigate } from 'react-router-dom';
import { useTheme } from '../../hooks/useTheme';

const getPasswordFeedback = (password: string) => {
  const criteria = {
    length: password.length >= 8,
    uppercase: /[A-Z]/.test(password),
    lowercase: /[a-z]/.test(password),
    special: /[!@#$%^&*(),.?":{}|<>]/.test(password),
  };

  let feedback = '';
  if (password.length > 0 && !criteria.length) feedback += '• At least 8 characters long.\n';
  if (password.length > 0 && !criteria.uppercase) feedback += '• At least one uppercase letter.\n';
  if (password.length > 0 && !criteria.lowercase) feedback += '• At least one lowercase letter.\n';
  if (password.length > 0 && !criteria.special) feedback += '• At least one special character.\n';

  const isValid = criteria.length && criteria.uppercase && criteria.lowercase && criteria.special;
  return { feedback: feedback.trim(), isValid };
};

const Register: React.FC = () => {
  const { serverConfig } = useServerConfig();
  const { isAuthenticated } = useAuth();
  const { currentTheme } = useTheme();
  
  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [status, setStatus] = useState<StatusMessage>({ text: '', type: 'hidden' });

  const { feedback: passwordFeedback, isValid: isPasswordValid } = getPasswordFeedback(password);
  const passwordsMatch = password === confirmPassword;
  const passwordsMatchError = confirmPassword.length > 0 && !passwordsMatch ? 'Passwords do not match.' : null;
  const isFormValid = isPasswordValid && passwordsMatch && username.length > 0;
  
  const isDark = currentTheme === 'dark';

  const buttonStyle = {
    backgroundColor: serverConfig.serverColor,
    '--server-color': serverConfig.serverColor,
  } as React.CSSProperties;

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/'); 
    }
  }, [isAuthenticated, navigate]);

  const handleRegisterSubmit = async (e: FormEvent) => {
    e.preventDefault();

    if (!isFormValid) {
      setStatus({ text: 'Please fix the errors in the form.', type: 'error' });
      return;
    }

    setStatus({ text: 'Registering...', type: 'hidden' });

    try {
      await authService.register(username, password); 
      
      setStatus({ 
          text: 'Registration successful! Redirecting...', 
          type: 'success' 
      });

      setTimeout(() => {
          navigate('/verify'); 
      }, 1500);
      
    } catch (error) { 
      console.error('Registration Error:', error);
      
      const errorMessage = (error instanceof Error) 
        ? error.message 
        : 'Unknown error occurred during registration.';
        
      setStatus({ text: errorMessage, type: 'error' });
    }
  };

  const handleOAuthClick = (service: string) => {
    alert(`Redirecting to ${service} for registration...`);
  };

  return (
    <AuthLayout title="Register for" pageTitle="Register">
      <form onSubmit={handleRegisterSubmit} className="space-y-6">

        <StatusDisplay status={status} />

        <InputField
          id="username"
          label="Username"
          type="text"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />

        <InputField
          id="password"
          label="Password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={passwordFeedback}
          showToggle
        />

        <InputField
          id="confirm-password"
          label="Confirm Password"
          type="password"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          error={passwordsMatchError}
          showToggle
        />

        <button
          type="submit"
          disabled={!isFormValid}
          className={`w-full text-white font-bold py-3 rounded-xl transition duration-300 transform hover:scale-[1.01] active:scale-[0.99] custom-button-glow ${!isFormValid ? 'opacity-50 cursor-not-allowed' : ''}`}
          style={buttonStyle}
        >
          Register
        </button>

        <OAuthButtons action="register" onOAuthClick={handleOAuthClick} />
      </form>

      <p className={`mt-6 text-center text-sm ${isDark ? 'text-[#a1a1aa]' : 'text-[#52525b]'}`}>
        Already have an account?
        <Link to="/login"
          className="ml-1 font-semibold hover:underline transition-colors duration-300"
          style={{color: serverConfig.serverColor}}
        >
          Log in here
        </Link>.
      </p>
    </AuthLayout>
  );
};

export default Register;