import React from 'react';
import { FaShieldAlt, FaUserShield } from 'react-icons/fa';

import {useTitle} from "@hooks/useTitle";

import BackButton from '@components/common/BackButton';

interface EmptyProfileStateProps {
    type: 'console' | 'no-punishments';
}

const EmptyProfileState: React.FC<EmptyProfileStateProps> = ({ type }) => {
    useTitle("Unaccessible Profile");

    const isConsole = type === 'console';
    const icon = isConsole ? <FaShieldAlt size={42} /> : <FaUserShield size={42} />;
    const iconColor = isConsole ? 'bg-text-secondary/10 text-text-secondary' : 'bg-error/10 text-error';
    const accentColor = isConsole ? 'bg-text-secondary' : 'bg-error';
    const title = isConsole ? 'System Profile' : 'Staff Profile Inaccessible';
    const message = isConsole
        ? 'The Console is a system entity and does not have a player profile. You can view punishments issued by the Console in the staff section, but it cannot receive punishments.'
        : 'This moderator has not executed any punishments yet. Moderator profiles are only generated for active staff members with recorded history.';

    return (
        <div className={`min-h-screen bg-background text-text-primary flex items-center justify-center p-6`}>
            <div className={`p-12 rounded-3xl text-center bg-surface border border-surface-border max-w-md w-full relative overflow-hidden`}>
                <div className={`absolute top-0 left-0 w-full h-1.5 ${accentColor} opacity-50`} />
                <div className="flex justify-center mb-6">
                    <div className={`p-5 rounded-2xl ${iconColor}`}>
                        {icon}
                    </div>
                </div>
                <h2 className="text-2xl font-black mb-3">{title}</h2>
                <p className="opacity-60 text-sm leading-relaxed mb-8">
                    {isConsole ? (
                        <>
                            The <strong>Console</strong> is a system entity and does not have a player profile.
                            You can view punishments issued by the Console in the staff section, but it cannot receive punishments.
                        </>
                    ) : (
                        message
                    )}
                </p>
                <BackButton />
            </div>
        </div>
    );
};

export default EmptyProfileState;