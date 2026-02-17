import React from 'react';
import { FaBan } from 'react-icons/fa';

interface RevokeButtonProps {
    onClick: () => void;
    isRevoking: boolean;
    serverColor: string;
    serverColorHover: string;
}

const RevokeButton: React.FC<RevokeButtonProps> = ({ onClick, isRevoking, serverColor, serverColorHover }) => {
    const revokeButtonClass = `mt-8 w-full py-4 px-6 rounded-xl text-white font-extrabold tracking-wide transition duration-300 shadow-md focus:ring-4 focus:ring-opacity-50`;
    const revokeButtonStyles = {
        backgroundColor: serverColor,
        boxShadow: `0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1), 0 0 0 3px ${serverColor}40`,
    } as React.CSSProperties;

    return (
        <button
            onClick={onClick}
            className={`${revokeButtonClass} hover:bg-[${serverColorHover}]`}
            style={revokeButtonStyles}
            disabled={isRevoking}
        >
            {isRevoking ? (
                <>
                    <div className="inline-block animate-spin rounded-full h-5 w-5 border-b-2 border-white mr-3"></div>
                    Revoking...
                </>
            ) : (
                <>
                    <FaBan className="inline mr-3 text-lg" /> Revoke Punishment
                </>
            )}
        </button>
    );
};

export default RevokeButton;