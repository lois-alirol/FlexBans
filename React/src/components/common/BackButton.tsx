import React from 'react';
import { FaArrowLeft } from 'react-icons/fa';
import { Link } from 'react-router-dom';

interface BackButtonProps {
    to: string;
    currentTheme: 'light' | 'dark';
}

const BackButton: React.FC<BackButtonProps> = ({ to, currentTheme }) => {
  const buttonClasses = currentTheme === 'dark' 
    ? 'bg-[#2c2c2c] text-white hover:bg-[#383838]' 
    : 'bg-white text-gray-800 hover:bg-gray-100 border border-gray-300';

  return (
    <Link 
      to={to} 
      className={`inline-flex items-center px-4 py-2 rounded-xl shadow-md font-semibold transition duration-300 transform hover:scale-[1.02] ${buttonClasses}`}
      aria-label="Back to index"
    >
      <FaArrowLeft className="mr-2" />
      Back to Index
    </Link>
  );
};

export default BackButton;
