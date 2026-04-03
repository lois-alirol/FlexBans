import React, { useState, type ChangeEventHandler } from 'react';
import { FaEye, FaEyeSlash } from 'react-icons/fa';

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
    const [showPassword, setShowPassword] = useState(false);

    const inputType = type === 'password' && showPassword && showToggle ? 'text' : type;
    const inputClasses = 'bg-surface-elevated border border-surface-border text-text-primary focus:ring-server-color focus:ring-opacity-50'

    return (
        <div className="space-y-2">
            <label
                htmlFor={id}
                className="block text-sm font-semibold uppercase tracking-wide text-text-secondary"
            >
                {label}
            </label>

            <div className="relative">
                <input
                    type={inputType}
                    id={id}
                    value={value}
                    onChange={onChange}
                    required
                    className={`w-full rounded-lg px-4 py-3 text-base transition transform focus:-translate-y-px outline-none focus:ring-2 ${inputClasses}`}
                />

                {showToggle && type === 'password' && (
                    <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="absolute inset-y-0 right-0 flex items-center justify-center w-12 text-text-secondary hover:text-text-primary transition-colors"
                    >
                        {showPassword ? <FaEyeSlash /> : <FaEye />}
                    </button>
                )}
            </div>

            {error && (
                <p className="text-xs text-text-error font-medium animate-pulse mt-1">
                    {error}
                </p>
            )}
        </div>
    );
};