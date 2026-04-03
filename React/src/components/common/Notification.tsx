import React, { useEffect } from "react";
import { FaExclamationTriangle, FaTimes, FaCheckCircle, FaInfoCircle } from "react-icons/fa";

interface NotificationProps {
    message: React.ReactNode;
    type?: 'error' | 'success' | 'info';
    onClose: () => void;
    visible: boolean;
    duration?: number;
}

const Notification: React.FC<NotificationProps> = ({
                                                       message,
                                                       type = 'error',
                                                       onClose,
                                                       visible,
                                                       duration = 5000
                                                   }) => {

    useEffect(() => {
        if (visible && duration > 0) {
            const timer = setTimeout(() => {
                onClose();
            }, duration);

            return () => clearTimeout(timer);
        }
    }, [visible, duration, onClose]);

    const Icon = () => {
        switch (type) {
            case 'success': return <FaCheckCircle className="text-xl" />;
            case 'info': return <FaInfoCircle className="text-xl" />;
            default: return <FaExclamationTriangle className="text-xl" />;
        }
    };

    return (
        <div
            className={`fixed z-200 top-5 left-1/2 transform -translate-x-1/2 min-w-[300px] max-w-[96vw] px-4 py-3 rounded-xl shadow-2xl flex items-center gap-3 transition-all duration-500
                ${
                type === 'error'
                    ? 'bg-rose-600 text-white border border-rose-700'
                    : type === 'success'
                        ? 'bg-emerald-500 text-white border border-emerald-600'
                        : 'bg-blue-500 text-white border border-blue-600'
            }
                ${visible
                ? 'opacity-100 translate-y-0 pointer-events-auto'
                : 'opacity-0 -translate-y-4 pointer-events-none'
            }
            `}
        >
            <Icon />
            <span className="flex-1 font-medium">{message}</span>
            <button
                className="ml-2 text-lg p-1 rounded-lg hover:bg-surface-elevated/30 transition-colors"
                onClick={onClose}
                aria-label="Dismiss"
            >
                <FaTimes />
            </button>
        </div>
    );
};

export default Notification;