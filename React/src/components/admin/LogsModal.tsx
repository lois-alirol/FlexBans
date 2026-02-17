import React from 'react';
import { FaTimes } from 'react-icons/fa';
import type { Theme as UiTheme } from '../../types/admin';

type LogsModalProps = {
    isOpen: boolean;
    onClose: () => void;
    accent: string;
    theme: UiTheme;
    logs: string[];
};

const LogsModal: React.FC<LogsModalProps> = ({ isOpen, onClose, accent, theme, logs }) => {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center px-4">
            <div className="absolute inset-0 bg-black/60" onClick={onClose} aria-label="Close logs modal" />
            <div
                className={`relative w-full max-w-3xl rounded-2xl shadow-2xl ${
                    theme === 'dark' ? 'bg-[#1f1f1f] text-gray-100' : 'bg-white text-gray-900'
                }`}
            >
                <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700">
                    <div>
                        <h2 className="text-lg font-semibold">System Logs</h2>
                        <p className="text-xs text-gray-500 dark:text-gray-400">Mock data for now</p>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800"
                        aria-label="Close"
                    >
                        <FaTimes />
                    </button>
                </div>
                <div className="p-6">
                    <div
                        className={`rounded-xl border overflow-auto max-h-[60vh] ${
                            theme === 'dark'
                                ? 'bg-black text-green-300 border-gray-800'
                                : 'bg-[#0b1021] text-[#32cd32] border-gray-200'
                        }`}
                    >
                        <pre className="p-4 text-sm leading-6 font-mono whitespace-pre-wrap">
                            {logs.join('\n')}
                        </pre>
                    </div>
                </div>
                <div className="flex justify-end gap-3 px-6 pb-6">
                    <button
                        className={`px-4 py-2 rounded-lg text-sm font-semibold ${
                            theme === 'dark' ? 'bg-[#2c2c2c] text-white' : 'bg-gray-200 text-gray-800'
                        }`}
                        onClick={onClose}
                    >
                        Close
                    </button>
                    <button
                        className="px-4 py-2 rounded-lg text-sm font-semibold text-white shadow"
                        style={{ backgroundColor: accent }}
                        onClick={() => alert('Trigger your log export here (mock).')}
                    >
                        Export Logs
                    </button>
                </div>
            </div>
        </div>
    );
};

export default LogsModal;