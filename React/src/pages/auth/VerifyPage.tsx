import React, { useState, useEffect } from 'react';
import { FaCircleNotch, FaCheckCircle } from 'react-icons/fa';
import { AuthLayout } from '../../components/auth/AuthLayout';
import { useServerConfig } from '../../hooks/useServerConfig';
import authService from '../../services/authService';
import { useAuth } from '../../hooks/useAuth';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '../../hooks/useTheme';

const VerifyPage: React.FC = () => {
  const { serverConfig } = useServerConfig();
  const { isVerified: isVerifiedFromAuth, refreshUserVerification } = useAuth();
  const { currentTheme } = useTheme();

  const navigate = useNavigate();

  const [verificationCode, setVerificationCode] = useState<string | null>(null);
  const [isVerified, setIsVerified] = useState(false);
  const [isPolling, setIsPolling] = useState(true);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const serverColor = serverConfig.serverColor;
  const isDark = currentTheme === 'dark';

  useEffect(() => {
    if (isVerifiedFromAuth) {
      setIsVerified(true);
      setIsPolling(false);
      setTimeout(() => {
        navigate('/', { replace: true });
      }, 1000);
    }
  }, [isVerifiedFromAuth, navigate]);

  useEffect(() => {
    const fetchCode = async () => {
      setIsLoading(true);
      try {
        const response = await authService.request2FACode();

        if (response.already_verified) {
          setIsVerified(true);
          setIsPolling(false);
          await refreshUserVerification();
          return;
        }

        if (response.code) {
          setVerificationCode(response.code);
        }
      } catch (err) {
        console.error('Failed to request 2FA code', err);
        setError('Failed to get verification code. Please try again.');
      } finally {
        setIsLoading(false);
      }
    };

    if (!isVerifiedFromAuth) {
      fetchCode();
    }
  }, [isVerifiedFromAuth, refreshUserVerification]);

  useEffect(() => {
    if (!verificationCode || isVerified || !isPolling) return;

    const checkVerification = async () => {
      try {
        const response = await authService.request2FACode();

        if (response.already_verified) {
          setIsVerified(true);
          setIsPolling(false);

          await refreshUserVerification();
        }
      } catch (err) {}
    };

    const id = setInterval(checkVerification, 5000);
    checkVerification();

    return () => clearInterval(id);
  }, [verificationCode, isVerified, isPolling, refreshUserVerification]);

  const codeDigitClass = isDark
      ? "bg-[#1c1c1c] border"
      : "bg-gray-100 border-gray-300 text-gray-800";

  const verifiedBg = isDark ? 'bg-green-900/30' : 'bg-green-100';
  const verifiedText = isDark ? 'text-green-500' : 'text-green-700';
  const waitingText = isDark ? 'text-[#a1a1aa]' : 'text-[#52525b]';
  const errorText = isDark ? 'text-red-400' : 'text-red-600';

  if (isLoading && !verificationCode) {
    return (
        <AuthLayout title="Verify" pageTitle="Verify">
          <p className={`text-center ${isDark ? '' : 'text-[#3f3f46]'}`}>
            Initializing verification…
          </p>
        </AuthLayout>
    );
  }

  if (error) {
    return (
        <AuthLayout title="Verify" pageTitle="Verify">
          <div className={`text-center ${errorText}`}>
            <p>{error}</p>
            <button
                onClick={() => window.location.reload()}
                className="mt-4 px-4 py-2 rounded-lg transition-colors text-white"
                style={{ backgroundColor: serverColor }}
            >
              Try Again
            </button>
          </div>
        </AuthLayout>
    );
  }

  if (!verificationCode) {
    return (
        <AuthLayout title="Verify" pageTitle="Verify">
          <p className={`text-center ${isDark ? '' : 'text-[#3f3f46]'}`}>Unable to get verification code</p>
        </AuthLayout>
    );
  }

  const codeDigits = verificationCode.split('');

  return (
      <AuthLayout title="Verify for" pageTitle="Verify">
        <div className="text-center">
          {isVerified ? (
              <div className={`${verifiedText} py-30 rounded-lg ${verifiedBg} mb-6`}>
                <FaCheckCircle className="mx-auto text-5xl mb-3" />
                <p className="text-xl font-semibold">Verification Successful!</p>
              </div>
          ) : (
              <>
                <p className={`mb-6 ${waitingText}`}>
                  Validate your account on <strong>{serverConfig.serverName}</strong>.
                </p>

                <div className="flex justify-center gap-3 mb-6">
                  {codeDigits.map((d, i) => (
                      <div
                          key={i}
                          className={`w-12 h-16 ${codeDigitClass} text-3xl font-bold flex items-center justify-center rounded-xl`}
                          style={{ color: serverColor }}
                      >
                        {d}
                      </div>
                  ))}
                </div>

                <code
                    className="px-3 py-2 rounded-lg font-mono font-bold"
                    style={{ backgroundColor: serverColor + '20', color: serverColor }}
                >
                  /fb verify {verificationCode}
                </code>

                <div className={`mt-6 flex items-center justify-center text-sm ${waitingText}`}>
                  <FaCircleNotch className="animate-spin mr-2 text-yellow-500" />
                  Waiting for in-game verification…
                </div>
              </>
          )}
        </div>

        <p className={`mt-6 text-center text-sm ${isDark ? 'text-[#a1a1aa]' : 'text-[#52525b]'}`}>
          Wrong account?
          <a
              href="#"
              onClick={(e) => {
                e.preventDefault();
                navigate("/logout");
              }}
              className="ml-1 font-semibold hover:underline transition-colors duration-300 cursor-pointer"
              style={{ color: serverConfig.serverColor }}
          >
            Log out here
          </a>.
        </p>
      </AuthLayout>
  );
};

export default VerifyPage;