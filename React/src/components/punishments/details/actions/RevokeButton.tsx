import React from 'react';
import { FaBan } from 'react-icons/fa';

interface RevokeButtonProps {
    onClick: () => void;
    isRevoking: boolean;
}

const RevokeButton: React.FC<RevokeButtonProps> = ({ onClick, isRevoking }) => {
    const revokeButtonClass = `mt-8 w-full py-4 px-6 rounded-xl text-white font-extrabold tracking-wide transition duration-300 shadow-md focus:ring-4 focus:ring-opacity-50`;

    return (
        <button
            onClick={onClick}
            className={`${revokeButtonClass} hover:bg-server-color-hover bg-server-color shadow-server-color`}
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