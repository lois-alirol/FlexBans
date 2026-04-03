import React from 'react';
import { FaCheckCircle, FaTimesCircle, FaHistory } from 'react-icons/fa';
import {useTranslation} from "react-i18next";

interface StatusBadgeProps {
    status: 'Active' | 'Expired' | 'Removed' | string | null;
    className?: string;
}

const StatusBadge: React.FC<StatusBadgeProps> = ({ status, className = '' }) => {
    const { t } = useTranslation();

    const statusStyles = () => {
        switch (status) {
            case 'Active':
                return {
                    color: 'text-status-active',
                    bg: 'bg-status-active/10',
                    icon: <FaCheckCircle className="mr-2" />,
                    label: t("statuses.active"),
                };
            case 'Expired':
                return {
                    color: 'text-status-expired',
                    bg: 'bg-status-expired/10',
                    icon: <FaHistory className="mr-2" />,
                    label: t("statuses.expired"),
                };
            case 'Removed':
                return {
                    color: 'text-status-removed',
                    bg: 'bg-status-removed/10',
                    icon: <FaTimesCircle className="mr-2" />,
                    label: t("statuses.removed"),
                };
            default:
                return {
                    color: 'text-status-unknown',
                    bg: 'bg-status-unknown/10',
                    icon: null,
                    label: status || 'UNKNOWN',
                };
        }
    };

    const { color, bg, icon, label } = statusStyles();

    return (
        <span className={`inline-flex items-center py-1 rounded-full 
                          tracking-wide ${color} ${bg} ${className}`}
        >
          {icon} {label}
        </span>
    );
};

export default StatusBadge;