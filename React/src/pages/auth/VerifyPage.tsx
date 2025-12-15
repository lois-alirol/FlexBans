import React, { useState, useEffect } from 'react';
import { FaCircleNotch, FaCheckCircle } from 'react-icons/fa';
import { AuthLayout } from '../../components/auth/AuthLayout';
import { useServerConfig } from '../../hooks/useServerConfig';
import authService from '../../services/authService';
import { useAuth } from '../../hooks/useAuth';
import { Link, useNavigate } from 'react-router-dom';
import { useTheme } from '../../hooks/useTheme';

const VerifyPage: React.FC = () => {
  const { serverConfig } = useServerConfig();
  const { isVerified: isVerifiedFromAuth } = useAuth();
  const { currentTheme } = useTheme();

  const navigate = useNavigate();

  const [verificationCode, setVerificationCode] = useState<string | null>(null);
  const [isVerified, setIsVerified] = useState(false);
  const [isPolling, setIsPolling] = useState(true);

  const serverColor = serverConfig.serverColor;
  const isDark = currentTheme === 'dark';

  useEffect(() => {
    if (isVerifiedFromAuth) {
      navigate('/')
    }  
  }, [isVerifiedFromAuth, navigate]);

  useEffect(() => {
    const fetchCode = async () => {
      try {
        const { code } = await authService.request2FACode();
        setVerificationCode(code);
      } catch (err) {
        console.error('Failed to request 2FA code', err);
      }
    };

    fetchCode();
  }, []);

  useEffect(() => {
    if (!verificationCode || isVerified || !isPolling) return;

    const poll = async () => {
      try {
        const res = await authService.poll2FAStatus(verificationCode);

        if (res.verified) {
          setIsVerified(true);
          setIsPolling(false);
          navigate('/');
        }
      } catch (err) {
        console.error('Polling error', err);
      }
    };

    const id = setInterval(poll, 5000);
    poll();

    return () => clearInterval(id);
  }, [verificationCode, isVerified, isPolling, navigate]);

  if (!verificationCode) {
    return (
      <AuthLayout title="Verify" pageTitle="Verify">
        <p className={`text-center ${isDark ? '' : 'text-[#3f3f46]'}`}>Requesting verification code…</p>
      </AuthLayout>
    );
  }

  const codeDigits = verificationCode.split('');
  
  const codeDigitClass = isDark 
    ? "bg-[#1c1c1c] border" 
    : "bg-gray-100 border-gray-300 text-gray-800";
  
  const verifiedBg = isDark ? 'bg-green-900/30' : 'bg-green-100';
  const verifiedText = isDark ? 'text-green-500' : 'text-green-700';
  const waitingText = isDark ? 'text-[#a1a1aa]' : 'text-[#52525b]';

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
                  style={{ color: isDark ? serverColor : serverColor }}
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
        <Link to="/logout"
          className="ml-1 font-semibold hover:underline transition-colors duration-300"
          style={{color: serverConfig.serverColor}}
        >
          Log out here
        </Link>.
      </p>
    </AuthLayout>
  );
};

export default VerifyPage;