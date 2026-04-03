import React from 'react';
import { FaShieldAlt } from 'react-icons/fa';

interface CardSectionProps {
    title: string;
    children: React.ReactNode;
    description?: string;
}

const CardSection: React.FC<CardSectionProps> = ({ title, children, description }) => (
    <div className={`p-6 rounded-2xl shadow-lg border bg-surface border-surface-border`}>
        <div className="flex items-start justify-between mb-4">
            <div>
                <h2 className="text-server-color text-xl font-bold flex items-center gap-2">
                    <FaShieldAlt className="text-base" /> {title}
                </h2>
                {description && <p className={`text-sm text-text-secondary`}>{description}</p>}
            </div>
        </div>
        {children}
    </div>
);

export default CardSection;