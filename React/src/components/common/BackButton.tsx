import React from 'react';
import { FaArrowLeft } from 'react-icons/fa';
import { useNavigate } from 'react-router-dom';

interface BackButtonProps {
    currentTheme: 'light' | 'dark';
}

const BackButton: React.FC<BackButtonProps> = ({ currentTheme }) => {
    const navigate = useNavigate();

    const buttonClasses = currentTheme === 'dark'
        ? 'bg-[#2c2c2c] text-white hover:bg-[#383838]'
        : 'bg-white text-gray-800 hover:bg-gray-100 border border-gray-300';

    const handleGoBack = () => {
        if (window.history.length > 1) {
            navigate(-1);
        } else {
            navigate('/');
        }
    };

    return (
        <button
            onClick={handleGoBack}
            className={`inline-flex items-center px-4 py-2 rounded-xl shadow-md font-semibold transition duration-300 transform hover:scale-[1.02] ${buttonClasses}`}
            aria-label="Go back"
        >
            <FaArrowLeft className="mr-2" />
            Back
        </button>
    );
};

export default BackButton;