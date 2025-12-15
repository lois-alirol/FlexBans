import React from 'react';
import { FaUserCircle } from 'react-icons/fa';
import { useAuth } from '../../hooks/useAuth';

interface MyAccountProps {
  currentTheme: 'dark' | 'light';
}

const MyAccount: React.FC<MyAccountProps> = ({ currentTheme }) => {
  const { user, isAuthenticated } = useAuth();
  const accountName = isAuthenticated && user ? user.username : '?';

  return (
    <div className="relative group">
      <button 
        className={`flex items-center space-x-2 p-2 rounded-full transition duration-200 focus:outline-none focus:ring-2 ${
            currentTheme === 'dark' 
                ? 'text-white hover:bg-[#383838]' 
                : 'text-gray-800 hover:bg-gray-100'
        }`}
        aria-label="My Account"
      >
        <FaUserCircle className="text-2xl" />
        <span className={`text-sm font-medium lg:inline`}>
            {accountName}
        </span>
      </button>
    </div>
  );
};

export default MyAccount;