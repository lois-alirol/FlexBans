import React, { useState, useEffect, useRef } from 'react';
import { FaCircleNotch, FaCheckCircle } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';

import { useServerConfig } from '@hooks/useServerConfig';
import { useAuth } from '@hooks/useAuth';

import authService from '@services/authService';

import { AuthLayout } from '@components/auth/AuthLayout';
import { useTranslation } from 'react-i18next';

const VerifyPage: React.FC = () => {
  const { t } = useTranslation();
  const { serverConfig } = useServerConfig();
  const { isVerified: isVerifiedFromAuth, refreshUserVerification } = useAuth();
  const navigate = useNavigate();

  const [verificationCode, setVerificationCode] = useState<string | null>(null);
  const [isVerified, setIsVerified] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Prevents double-invocation in React StrictMode (dev) and accidental re-mounts
  const initCalledRef = useRef(false);

  useEffect(() => {
    if (isVerifiedFromAuth) {
      setIsVerified(true);
      setTimeout(() => navigate('/', { replace: true }), 1000);
    }
  }, [isVerifiedFromAuth, navigate]);

  useEffect(() => {
    if (isVerifiedFromAuth) return;
    // Guard: only run once per mount cycle
    if (initCalledRef.current) return;
    initCalledRef.current = true;

    let ws: WebSocket | null = null;

    const init = async () => {
      setIsLoading(true);
      try {
        const response = await authService.request2FACode();

        if (response.already_verified) {
          setIsVerified(true);
          await refreshUserVerification();
          return;
        }

        if (!response.code) return;
        setVerificationCode(response.code);

        const wsBase = import.meta.env.DEV
            ? (import.meta.env.VITE_APP_WEBSOCKET_URL as string)
            : `${window.location.host}/api/ws`;

        const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        ws = new WebSocket(`${wsProtocol}//${wsBase}/verify?username=${response.username}`);

        ws.onmessage = async (event) => {
          const data = JSON.parse(event.data);
          if (data.type === 'verified') {
            setIsVerified(true);

            await authService.completeVerification();

            await refreshUserVerification();

            setTimeout(() => navigate('/', { replace: true }), 1000);
          }
        };

        ws.onerror = () => setError(t('verify.failed-fetch'));

      } catch (err: any) {
        console.error('Failed to request 2FA code', err);
        // Surface rate-limit errors distinctly
        if (err?.message?.toLowerCase().includes('too many')) {
          setError(err.message);
        } else {
          setError(t('verify.failed-fetch'));
        }
      } finally {
        setIsLoading(false);
      }
    };

    void init();

    return () => {
      ws?.close();
      // Reset the guard on unmount so a genuine re-mount (e.g. navigation away
      // and back) works correctly, while still blocking StrictMode's
      // immediate remount within the same render cycle.
      // Note: StrictMode unmounts+remounts synchronously, so the ref stays true
      // for that second call. A real navigation resets it properly.
      initCalledRef.current = false;
    };
  }, [isVerifiedFromAuth, refreshUserVerification, navigate, t]);

  const codeDigitClass = "bg-surface border";
  const verifiedBg = 'bg-green-900/30';
  const verifiedText = 'text-green-500';
  const waitingText = 'text-[#a1a1aa]';
  const errorText = 'text-red-400';

  if (isLoading && !verificationCode) {
    return (
        <AuthLayout title={t("verify.title")} pageTitle={t("verify.page-title")}>
          <p className={`text-center text-text-secondary`}>
            {t("verify.initializing")}
          </p>
        </AuthLayout>
    );
  }

  if (error) {
    return (
        <AuthLayout title={t("verify.title")} pageTitle={t("verify.page-title")}>
          <div className={`text-center ${errorText}`}>
            <p>{error}</p>
            <button
                onClick={() => {
                  initCalledRef.current = false;
                  window.location.reload();
                }}
                className="bg-server-color mt-4 px-4 py-2 rounded-lg transition-colors text-text-primary"
            >
              {t("verify.try-again")}
            </button>
          </div>
        </AuthLayout>
    );
  }

  if (!verificationCode) {
    return (
        <AuthLayout title={t("verify.title")} pageTitle={t("verify.page-title")}>
          <p className={`text-center text-text-secondary`}>{t("verify.no-code")}</p>
        </AuthLayout>
    );
  }

  const codeDigits = verificationCode.split('');

  return (
      <AuthLayout title={t("verify.title")} pageTitle={t("verify.page-title")}>
        <div className="text-center">
          {isVerified ? (
              <div className={`${verifiedText} py-30 rounded-lg ${verifiedBg} mb-6`}>
                <FaCheckCircle className="mx-auto text-5xl mb-3" />
                <p className="text-xl font-semibold">{t("verify.success")}</p>
              </div>
          ) : (
              <>
                <p className={`mb-6 ${waitingText}`}>
                  {t("verify.validate-on-server", { server: serverConfig ? serverConfig.serverName : '' })}
                </p>

                <div className="flex justify-center gap-3 mb-6">
                  {codeDigits.map((d, i) => (
                      <div
                          key={i}
                          className={`text-server-color w-12 h-16 ${codeDigitClass} text-3xl font-bold flex items-center justify-center rounded-xl`}
                      >
                        {d}
                      </div>
                  ))}
                </div>

                <code
                    className="bg-server-color/10 text-server-color px-3 py-2 rounded-lg font-mono font-bold"
                >
                  /fb verify {verificationCode}
                </code>

                <div className={`mt-6 flex items-center justify-center text-sm ${waitingText}`}>
                  <FaCircleNotch className="animate-spin mr-2 text-yellow-500" />
                  {t("verify.waiting")}
                </div>
              </>
          )}
        </div>

        <p className={`mt-6 text-center text-sm text-text-secondary`}>
          {t("verify.wrong-account")}
          <a
              href="#"
              onClick={(e) => {
                e.preventDefault();
                navigate("/logout");
              }}
              className="text-server-color ml-1 font-semibold hover:underline transition-colors duration-300 cursor-pointer"
          >
            {t("verify.logout-link")}
          </a>.
        </p>
      </AuthLayout>
  );
};

export default VerifyPage;