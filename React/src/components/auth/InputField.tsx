import React, { useState, type ChangeEventHandler } from 'react';
import { FaEye, FaEyeSlash } from 'react-icons/fa';
import { useServerConfig } from '../../hooks/useServerConfig';
import { useTheme } from '../../hooks/useTheme';

interface InputFieldProps {
  id: string;
  label: string;
  type: 'text' | 'password';
  value: string;
  onChange: ChangeEventHandler<HTMLInputElement>;
  error?: string | null;
  showToggle?: boolean;
}

export const InputField: React.FC<InputFieldProps> = ({
  id,
  label,
  type,
  value,
  onChange,
  error,
  showToggle = false,
}) => {
  const { serverConfig } = useServerConfig();
  const { currentTheme } = useTheme();
  
  const isDark = currentTheme === 'dark';

  const [showPassword, setShowPassword] = useState(false);
  const inputType = type === 'password' && showPassword && showToggle ? 'text' : type;

  const inputBgClass = isDark ? 'bg-[#1c1c1c] text-white' : 'bg-white text-gray-900 border-gray-300';
  const labelColorClass = isDark ? 'text-white' : 'text-gray-700';
  const toggleColorClass = isDark ? 'text-[#a1a1aa] hover:text-white' : 'text-gray-500 hover:text-gray-700';

  const inputStyle = {
    borderColor: error ? '#ef4444' : (isDark ? '#4b5563' : '#d4d4d8'),
    '--tw-ring-color': serverConfig.serverColor
  } as React.CSSProperties;

  return (
    <div>
      <label htmlFor={id} className={`block text-sm font-semibold mb-2 ${labelColorClass}`}>{label}</label>
      <div className="relative">
        <input
          type={inputType}
          id={id}
          value={value}
          onChange={onChange}
          required
          className={`w-full border rounded-xl py-3 px-4 focus:outline-none focus:ring-2 transition duration-300 ${showToggle ? 'pr-12' : ''} ${inputBgClass}`}
          style={inputStyle}
        />
        {showToggle && type === 'password' && (
          <button
            type="button"
            onClick={() => setShowPassword(!showPassword)}
            aria-label={showPassword ? 'Hide password' : 'Show password'}
            className={`absolute inset-y-0 right-0 flex items-center justify-center w-10 focus:outline-none transition duration-300 ${toggleColorClass}`}
          >
            {showPassword ? <FaEyeSlash /> : <FaEye />}
          </button>
        )}
      </div>
      {error && <p className="text-sm text-red-400 whitespace-pre-line mt-2">{error}</p>}
    </div>
  );
};