import React, { useState, useRef, useEffect } from 'react';
import { FaUserCircle } from 'react-icons/fa';
import { useAuth } from '../../hooks/useAuth';
import {useNavigate} from "react-router-dom";

interface MyAccountProps {
    currentTheme: 'dark' | 'light';
}

const MyAccount: React.FC<MyAccountProps> = ({ currentTheme }) => {
    const { user, isAuthenticated, logout } = useAuth();
    const navigate = useNavigate();

    const accountName = isAuthenticated && user ? user.username : '?';
    const isAdmin = user?.permissions?.includes('flexbans.web.admin');

    const baseTheme =
        currentTheme === 'dark'
            ? 'text-white hover:bg-[#2f2f2f] focus:ring-gray-600'
            : 'text-gray-800 hover:bg-gray-100 focus:ring-gray-300';

    const [isOpen, setIsOpen] = useState(false);
    const dropdownRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, []);

    return (
        <div className="flex items-center gap-2 relative">
            <div className="relative" ref={dropdownRef}>
                <button
                    onClick={() => setIsOpen(!isOpen)}
                    className={`flex items-center gap-2 px-3 py-2 rounded-full transition-all duration-200 focus:outline-none focus:ring-2 ${baseTheme}`}
                    aria-label="My Account"
                >
                    <FaUserCircle className="text-xl opacity-90" />
                    <span className="text-sm font-semibold leading-none">{accountName}</span>
                </button>

                {isOpen && (
                    <div
                        className="
                        absolute right-0 mt-2 w-52 rounded-xl shadow-lg
                        bg-white dark:bg-[#1f1f1f]
                        border border-gray-200 dark:border-gray-700
                        z-50
                    "
                    >
                        {/* THE ARROW INDICATOR */}
                        <div
                            className="
                                absolute -top-1.5 right-4 w-3 h-3
                                border-l border-t border-gray-200 dark:border-gray-700
                                bg-white dark:bg-[#1f1f1f]
                                transform rotate-45 z-0
                            "
                        />

                        {/* Content Wrapper (relative z-10 to sit above arrow) */}
                        <div className="relative z-10 bg-white dark:bg-[#1f1f1f] rounded-xl overflow-hidden">
                            <div className="px-4 py-3 border-b border-gray-200 dark:border-gray-700">
                                <p className="text-sm font-semibold text-gray-800 dark:text-gray-100">
                                    {accountName}
                                </p>
                                <p className="text-xs text-gray-500 dark:text-gray-400">
                                    {isAdmin ? 'Administrator' : 'User'}
                                </p>
                            </div>

                            <div className="py-1">
                                <button
                                    className="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-[#2a2a2a] transition"
                                    onClick={() => {}}
                                >
                                    Profile
                                </button>
                                <button
                                    className="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-[#2a2a2a] transition"
                                    onClick={() => {}}
                                >
                                    Account Settings
                                </button>
                                {isAdmin && (
                                    <button
                                        className="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-[#2a2a2a] transition"
                                        onClick={() => { navigate('/admin') }}
                                    >
                                        Panel Settings
                                    </button>
                                )}
                            </div>

                            <div className="border-t border-gray-200 dark:border-gray-700">
                                <button
                                    className="w-full text-left px-4 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-[#2a1f1f] transition"
                                    onClick={() => { logout() }}
                                >
                                    Log out
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default MyAccount;