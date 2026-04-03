import React, { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import type { StatusMessage } from '@/types/auth';

import { useServerConfig } from '@hooks/useServerConfig';
import { useAuth } from '@hooks/useAuth';

import { AuthLayout } from '@components/auth/AuthLayout';
import { InputField } from '@components/auth/InputField';
import { StatusDisplay } from '@components/auth/StatusDisplay';
import { OAuthButtons } from '@components/auth/OAuthButtons';
import { useTranslation } from "react-i18next";

const Login: React.FC = () => {
  const { serverConfig } = useServerConfig();
  const { login } = useAuth();
  const { t } = useTranslation();

  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [stayLoggedIn, setStayLoggedIn] = useState(false);
  const [status, setStatus] = useState<StatusMessage>({
    text: '',
    type: 'hidden'
  });

  const buttonStyle = {
    backgroundColor: serverConfig ? serverConfig.serverColor : ''
  } as React.CSSProperties;

  const handleLoginSubmit = async (e: FormEvent) => {
    e.preventDefault();

    setStatus({
      text: t("login.status.logging-in"),
      type: 'hidden'
    });

    try {

      const response = await login(
          username,
          password,
          stayLoggedIn
      );

      if (!response.user.isVerified) {

        setStatus({
          text: t("login.status.2fa-required"),
          type: 'success'
        });

        navigate('/verify');

      } else {

        setStatus({
          text: t("login.status.success"),
          type: 'success'
        });

        setTimeout(() => {
          navigate('/');
        }, 1500);

      }

    } catch (error) {

      let errorMessage =
          t("login.status.unknown-error");

      if (error instanceof Error)
        errorMessage = error.message;
      else if (typeof error === 'string')
        errorMessage = error;

      setStatus({
        text: errorMessage,
        type: 'error'
      });

    }
  };

  const handleOAuthClick = (service: string) => {
    alert(t("login.oauth.redirect", { service }));
  };

  return (
      <AuthLayout title={t("login.title")} pageTitle={t("login.page-title")}>

        <form
            onSubmit={handleLoginSubmit}
            className="space-y-6"
        >

          <StatusDisplay status={status} />

          <InputField
              id="username"
              label={t("login.username")}
              type="text"
              value={username}
              onChange={(e) =>
                  setUsername(e.target.value)
              }
              error={status.type === 'error' ? ' ' : null}
          />

          <InputField
              id="password"
              label={t("login.password")}
              type="password"
              value={password}
              onChange={(e) =>
                  setPassword(e.target.value)
              }
              error={status.type === 'error' ? ' ' : null}
              showToggle
          />

          <div className="flex items-center">
            <input
                type="checkbox"
                id="stay-logged-in"
                checked={stayLoggedIn}
                onChange={(e) =>
                    setStayLoggedIn(e.target.checked)
                }
                className="mr-2 h-4 w-4 rounded"
                style={{
                  accentColor: serverConfig ? serverConfig.serverColor : ''
                }}
            />

            <label
                htmlFor="stay-logged-in"
                className={`text-sm text-text-secondary`}
            >
              {t("login.stay-logged-in")}
            </label>
          </div>

          <button
              type="submit"
              className="w-full text-text-primary font-semibold py-3 rounded-xl transition duration-200 hover:brightness-110 active:brightness-95"
              style={buttonStyle}
          >
            {t("login.button")}
          </button>

          <OAuthButtons
              action="login"
              onOAuthClick={handleOAuthClick}
          />

        </form>

        <p
            className={`mt-6 text-center text-sm text-text-secondary`}
        >

          {t("login.no-account")}

          <Link
              to="/register"
              className="ml-1 font-semibold hover:underline"
              style={{
                color: serverConfig ? serverConfig.serverColor : ''
              }}
          >
            {t("login.register-link")}
          </Link>

        </p>

      </AuthLayout>
  );
};

export default Login;