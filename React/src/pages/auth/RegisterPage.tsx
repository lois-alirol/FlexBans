import React, { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import type { StatusMessage } from '@/types/auth';

import { useAuth } from '@hooks/useAuth';

import authService from '@services/authService';

import { AuthLayout } from '@components/auth/AuthLayout';
import { InputField } from '@components/auth/InputField';
import { StatusDisplay } from '@components/auth/StatusDisplay';
import { OAuthButtons } from '@components/auth/OAuthButtons';
import { useTranslation } from "react-i18next";

const getPasswordFeedback = (password: string, t: any) => {
  const criteria = {
    length: password.length >= 8,
    uppercase: /[A-Z]/.test(password),
    lowercase: /[a-z]/.test(password),
    special: /[!@#$%^&*(),.?":{}|<>]/.test(password),
  };

  let feedback = '';
  if (password.length > 0 && !criteria.length) feedback += `• ${t("register.password-criteria.length")}\n`;
  if (password.length > 0 && !criteria.uppercase) feedback += `• ${t("register.password-criteria.uppercase")}\n`;
  if (password.length > 0 && !criteria.lowercase) feedback += `• ${t("register.password-criteria.lowercase")}\n`;
  if (password.length > 0 && !criteria.special) feedback += `• ${t("register.password-criteria.special")}\n`;

  const isValid = criteria.length && criteria.uppercase && criteria.lowercase && criteria.special;
  return { feedback: feedback.trim(), isValid };
};

const Register: React.FC = () => {
  const { t } = useTranslation();
  const { isAuthenticated, user, refreshUserVerification } = useAuth();

  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [status, setStatus] = useState<StatusMessage>({ text: '', type: 'hidden' });

  const { feedback: passwordFeedback, isValid: isPasswordValid } = getPasswordFeedback(password, t);
  const passwordsMatch = password === confirmPassword;
  const passwordsMatchError = confirmPassword.length > 0 && !passwordsMatch ? t("register.passwords-no-match") : null;
  const isFormValid = isPasswordValid && passwordsMatch && username.length > 0;

  useEffect(() => {
      if (isAuthenticated && user?.isVerified) {
          navigate('/');
      }
      else if (isAuthenticated && !user?.isVerified) {
          navigate('/verify');
      }
  }, [isAuthenticated, navigate]);

  const handleRegisterSubmit = async (e: FormEvent) => {
    e.preventDefault();

    if (!isFormValid) {
      setStatus({ text: t("register.fix-errors"), type: 'error' });
      return;
    }

    setStatus({ text: t("register.registering"), type: 'hidden' });

    try {
      await authService.register(username, password);
      await authService.login(username, password, false);
      await refreshUserVerification();

      setStatus({
        text: t("register.success"),
        type: 'success'
      });

      setTimeout(() => {
        navigate('/verify');
      }, 1000);

    } catch (error) {
      console.error('Registration Error:', error);

      const errorMessage = (error instanceof Error)
          ? error.message
          : t("register.unknown-error");

      setStatus({ text: errorMessage, type: 'error' });
    }
  };

  const handleOAuthClick = (service: string) => {
    alert(t("register.oauth-redirect", { service }));
  };

  return (
      <AuthLayout title={t("register.title")} pageTitle={t("register.page-title")}>
        <form onSubmit={handleRegisterSubmit} className="space-y-6">

          <StatusDisplay status={status} />

          <InputField
              id="username"
              label={t("register.username")}
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
          />

          <InputField
              id="password"
              label={t("register.password")}
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              error={passwordFeedback}
              showToggle
          />

          <InputField
              id="confirm-password"
              label={t("register.confirm-password")}
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              error={passwordsMatchError}
              showToggle
          />

          <button
              type="submit"
              disabled={!isFormValid}
              className={`bg-server-color w-full text-text-primary font-bold py-3 rounded-xl transition duration-300 transform hover:scale-[1.01] active:scale-[0.99] custom-button-glow ${!isFormValid ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            {t("register.button")}
          </button>

          <OAuthButtons action="register" onOAuthClick={handleOAuthClick} />
        </form>

        <p className={`mt-6 text-center text-sm text-modal-text-secondary`}>
          {t("register.already-have-account")}
          <Link to="/login"
                className="text-server-color ml-1 font-semibold hover:underline transition-colors duration-300"
          >
            {t("register.login-link")}
          </Link>.
        </p>
      </AuthLayout>
  );
};

export default Register;