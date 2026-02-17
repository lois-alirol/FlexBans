import React from 'react';
import { FaShieldAlt } from 'react-icons/fa';
import type {Theme} from "../../types/admin.tsx";

interface CardSectionProps {
    title: string;
    children: React.ReactNode;
    accent: string;
    theme: Theme;
    description?: string;
}

const CardSection: React.FC<CardSectionProps> = ({ title, children, accent, theme, description }) => (
    <div className={`p-6 rounded-2xl shadow-lg border ${theme === 'dark' ? 'bg-[#1f1f1f] border-gray-700' : 'bg-white border-gray-200'}`}>
        <div className="flex items-start justify-between mb-4">
            <div>
                <h2 className="text-xl font-bold flex items-center gap-2" style={{ color: accent }}>
                    <FaShieldAlt className="text-base" /> {title}
                </h2>
                {description && <p className={`text-sm ${theme === 'dark' ? 'text-gray-400' : 'text-gray-600'}`}>{description}</p>}
            </div>
        </div>
        {children}
    </div>
);

export default CardSection;