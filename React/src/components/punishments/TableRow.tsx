import React, { useState, useCallback } from 'react';
import { FaEye, FaTrash, FaEdit, FaSpinner } from 'react-icons/fa';
import type { Punishment } from '../../types/punishments';
import { Link, useNavigate } from 'react-router-dom';
import RevokeModal from './RevokeModal';
import { useServerConfig } from '../../hooks/useServerConfig';
import { useAuth } from '../../hooks/useAuth';
import { usePunishmentRevocation } from '../../hooks/usePunishmentRevocation';

interface TableRowProps {
  punishment: Punishment;
  currentTheme: 'dark' | 'light';
}

const TableRow: React.FC<TableRowProps> = ({ punishment, currentTheme }) => {
  const { serverConfig } = useServerConfig();
  const { user, isAuthenticated } = useAuth();

  const { revokePunishment, isRevoking } = usePunishmentRevocation();
  
  const navigate = useNavigate();

  const punishmentDetailPath = `/punishment/${punishment.id}`;
  const removerName = user?.username || 'Unknown'; 

  const [isRevokeModalOpen, setIsRevokeModalOpen] = useState(false);
  
  const handleRevoke = useCallback(async (revocationReason: string) => {
    if (!user) {
        alert('Authentication error: User information is missing.');
        return;
    }
    
    const payload = {
        punishmentType: punishment.type.toString(),
        punishmentId: punishment.id.toString(),
        identity: user.username,
        removalReason: revocationReason,
    };
    
    try {
        await revokePunishment(payload);
        
        alert('Punishment revoked successfully!');
        setIsRevokeModalOpen(false);
        navigate(0); 
    } catch (error) {
        alert(`Error revoking punishment: ${error instanceof Error ? error.message : 'An unknown error occurred.'}`);
    }
  }, [punishment.type, punishment.id, user, revokePunishment, navigate]);


  const handleRevokeClick = (e: React.MouseEvent) => {
      handleNestedClick(e);
      setIsRevokeModalOpen(true);
  };

  const getStatusClasses = (status: Punishment['status']) => {
    switch (status) {
      case 'Active':
        return 'bg-red-500 text-white';
      case 'Expired':
        return 'bg-green-500 text-white';
      case 'Removed':
        return 'bg-gray-500 text-white';
      default:
        return 'bg-gray-300 text-gray-800';
    }
  };

  const rowClasses = currentTheme === 'dark' 
    ? 'hover:bg-[#333333] transition duration-200 cursor-pointer' 
    : 'hover:bg-[#f0f0f0] transition duration-200 cursor-pointer';

  const actionButtonClasses = "p-1 rounded-full transition duration-300 hover:scale-110 flex-shrink-0 disabled:opacity-50 disabled:cursor-not-allowed"; // Added disabled classes
  const iconClasses = "text-sm sm:text-base";
  const cellClasses = "px-2 sm:px-3 py-3 sm:py-4 text-xs sm:text-sm whitespace-nowrap";
  const actionCellClasses = "px-2 sm:px-3 py-3 sm:py-4 text-xs sm:text-sm font-medium text-center";
  
  const handleRowClick = () => {
    navigate(punishmentDetailPath);
  };

  const handleNestedClick = (e: React.MouseEvent) => {
    e.stopPropagation();
  };

  return (
    <>
    <tr 
      className={rowClasses} 
      onClick={handleRowClick}
    >
      <td className={`${cellClasses} font-medium`}>
        {punishment.id}
      </td>
      
      <td 
        className="px-6 py-4 whitespace-nowrap text-sm font-semibold" 
        onClick={handleNestedClick}
      >
        <Link 
          to={`/player/${punishment.player}`}
          className={`cursor-pointer underline-offset-2 hover:underline`}
        >
          {punishment.player}
        </Link>
      </td>
      
      <td 
        className={`${cellClasses} text-gray-500 dark:text-gray-400 hidden sm:table-cell min-w-[90px]`}
        onClick={handleNestedClick}
      >
        <Link 
          to={`/moderator/${punishment.moderator}`}
          className={`cursor-pointer underline-offset-2 hover:underline`}
        >
          {punishment.moderator}
        </Link>
      </td>
      
      <td className={cellClasses}>
        <div className="truncate max-w-20 sm:max-w-[120px]" title={punishment.reason}>
          {punishment.reason}
        </div>
      </td>
      
      <td className={`${cellClasses} min-w-[85px]`}>
        {punishment.date}
      </td>
      
      <td className={`${cellClasses} hidden sm:table-cell min-w-[70px]`}>
        {punishment.duration}
      </td>
      
      <td className={`${cellClasses} text-center min-w-[90px]`}>
        <span className={`px-2 py-0.5 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusClasses(punishment.status)}`}>
          {punishment.status}
        </span>
      </td>

      <td className={`${actionCellClasses} min-w-[90px]`}>
        <div className="flex justify-center gap-1 sm:gap-2">
          <button 
            title="View Details"
            className={`${actionButtonClasses} text-gray-500 dark:text-gray-400 hover:text-blue-500`}
            onClick={(e) => {
              handleNestedClick(e);
              navigate(punishmentDetailPath);
            }}
          >
            <FaEye className={iconClasses} />
          </button>

          {serverConfig.isSecured && punishment.status === 'Active' &&
            <button 
              title="Edit Punishment"
              className={`${actionButtonClasses} text-gray-500 dark:text-gray-400 hover:text-yellow-500`}
              onClick={handleNestedClick}
              disabled={isRevoking}
            >
              <FaEdit className={iconClasses} />
            </button>
          }

          {serverConfig.isSecured && punishment.status === 'Active' &&
            <button 
              title="Remove Punishment"
              className={`${actionButtonClasses} text-gray-500 dark:text-gray-400 hover:text-red-500`}
              onClick={handleRevokeClick}
              disabled={isRevoking}
            >
              {isRevoking ? <FaSpinner className={`${iconClasses} animate-spin`} /> : <FaTrash className={iconClasses} />}
            </button>
          }
        </div>
      </td>
    </tr>
    
    {serverConfig.isSecured && isAuthenticated &&
      <RevokeModal 
        isOpen={isRevokeModalOpen}
        onClose={() => setIsRevokeModalOpen(false)}
        onRevoke={handleRevoke}
        removerName={removerName}
        currentTheme={currentTheme}
        isSubmitting={isRevoking}
      />
    }
    </>
  );
};

export default TableRow;