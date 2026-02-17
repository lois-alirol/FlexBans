import React from 'react';
import { FaShieldAlt, FaUserShield } from 'react-icons/fa';
import BackButton from '../common/BackButton';

interface EmptyProfileStateProps {
    type: 'console' | 'no-punishments';
    currentTheme: 'light' | 'dark';
    bgColor: string;
    textColor: string;
    cardBg: string;
    borderColor: string;
}

const EmptyProfileState: React.FC<EmptyProfileStateProps> = ({
                                                                 type,
                                                                 currentTheme,
                                                                 bgColor,
                                                                 textColor,
                                                                 cardBg,
                                                                 borderColor
                                                             }) => {
    const isConsole = type === 'console';
    const icon = isConsole ? <FaShieldAlt size={42} /> : <FaUserShield size={42} />;
    const iconColor = isConsole ? 'bg-gray-500/10 text-gray-500' : 'bg-red-500/10 text-red-500';
    const accentColor = isConsole ? 'bg-gray-500' : 'bg-red-500';
    const title = isConsole ? 'System Profile' : 'Staff Profile Inaccessible';
    const message = isConsole
        ? 'The Console is a system entity and does not have a player profile. You can view punishments issued by the Console in the staff section, but it cannot receive punishments.'
        : 'This moderator has not executed any punishments yet. Moderator profiles are only generated for active staff members with recorded history.';

    return (
        <div className={`min-h-screen ${bgColor} ${textColor} flex items-center justify-center p-6`}>
            <div className={`p-12 rounded-3xl text-center shadow-2xl ${cardBg} border ${borderColor} max-w-md w-full relative overflow-hidden`}>
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
                <BackButton currentTheme={currentTheme} />
            </div>
        </div>
    );
};

export default EmptyProfileState;