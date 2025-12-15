import React, { useState, useCallback, useEffect } from 'react';
import { FaCheckCircle, FaTimesCircle, FaBan, FaUser, FaUserShield, FaCalendarAlt, FaClock, FaServer, FaListAlt, FaInfoCircle, FaFingerprint, FaHistory } from 'react-icons/fa'; 
import { useNavigate, useParams } from 'react-router-dom';
import BackButton from '../components/common/BackButton';
import RevokeModal from '../components/punishments/RevokeModal';
import { usePunishmentDetails, type PunishmentDetailData } from '../hooks/usePunishmentDetails'; 
import { useServerConfig } from '../hooks/useServerConfig';
import { useAuth } from '../hooks/useAuth';
import { usePunishmentRevocation } from '../hooks/usePunishmentRevocation';
import { useTheme } from '../hooks/useTheme';

type Theme = 'dark' | 'light';

const DetailItem: React.FC<{ icon: React.ReactNode, label: string, value: string, accentColor: string, currentTheme: Theme }> = ({ icon, label, value, accentColor, currentTheme }) => (
    <div className="flex items-center space-x-4 py-3 border-b border-gray-600/10 dark:border-gray-300/10 last:border-b-0">
        <div className="shrink-0 text-xl" style={{ color: accentColor }}>
            {icon}
        </div>
        <div className="grow">
            <p className={`text-sm font-medium ${currentTheme === 'dark' ? 'opacity-70' : 'text-gray-600'}`}>{label}</p>
            <p className="text-base font-semibold wrap-break-word">{value}</p>
        </div>
    </div>
);

const DetailCard: React.FC<{ title: string, children: React.ReactNode, accentColor: string, currentTheme: Theme }> = ({ title, children, accentColor, currentTheme }) => (
    <div className={`p-5 rounded-xl border ${currentTheme === 'dark' ? 'border-gray-700' : 'border-gray-300'} shadow-md`}>
        <h2 className={`text-lg font-bold mb-3 border-b pb-2 flex items-center ${currentTheme === 'dark' ? 'border-gray-700' : 'border-gray-300'}`} style={{ color: accentColor }}>
            <FaInfoCircle className='mr-2 text-base' /> {title}
        </h2>
        {children}
    </div>
);

interface PunishmentDetailContentProps extends PunishmentDetailData {
    server_color: string;
    server_color_hover: string;
    currentTheme: Theme;
}

const PunishmentDetailContent: React.FC<PunishmentDetailContentProps> = ({
  punishment_id,
  punishment_type,
  player_name,
  executor,
  reason,
  execution_date,
  expiration_date,
  duration,
  origin_server,
  scope_server,
  remover_name,
  removal_reason,
  status,
  server_color,
  server_color_hover,
  currentTheme,
}) => {
  const { serverConfig } = useServerConfig();
  const { user } = useAuth();
  const { revokePunishment, isRevoking } = usePunishmentRevocation(); // Use the dedicated hook

  const [isModalOpen, setIsModalOpen] = useState(false);
  const navigate = useNavigate();

  const handleRevoke = useCallback(async (revocationReason: string) => {
    if (!user) {
        return;
    }
    
    const payload = {
        punishmentType: punishment_type,
        punishmentId: punishment_id,
        identity: user.username,
        removalReason: revocationReason,
    };
    
    try {
        await revokePunishment(payload);
        
        alert('Punishment revoked successfully!');
        setIsModalOpen(false);
        navigate(0); 
    } catch (error) {
        alert(`Error revoking punishment: ${error instanceof Error ? error.message : 'An unknown error occurred.'}`);
    }
  }, [punishment_id, punishment_type, user, revokePunishment, navigate]);

  const textColor = currentTheme === 'dark' ? 'text-gray-100' : 'text-gray-800';
  const bodyBg = currentTheme === 'dark' ? 'bg-[#1c1c1c]' : 'bg-gray-50';
  const cardBg = currentTheme === 'dark' ? 'bg-[#2c2c2c]' : 'bg-white';
  const shadow = 'shadow-lg';

  const revokeButtonClass = `mt-8 w-full py-4 px-6 rounded-xl text-white font-extrabold tracking-wide transition duration-300 shadow-md focus:ring-4 focus:ring-opacity-50`;
  const revokeButtonStyles = {
    backgroundColor: server_color,
    boxShadow: `0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1), 0 0 0 3px ${server_color}40`, // Custom ring/shadow
  } as React.CSSProperties;
  
  let statusText: string;
  let statusColor: string;
  let statusBg: string;
  let statusIcon: React.ReactNode;
  
  const isRemoved = status === 'Removed';
  const isExpired = status === 'Expired';
  const isActive = status === 'Active';

  switch (status) {
    case 'Removed':
        statusText = 'REMOVED';
        statusColor = 'text-red-500';
        statusBg = 'bg-red-500/10';
        statusIcon = <FaTimesCircle />;
        break;
    case 'Expired':
        statusText = 'EXPIRED';
        statusColor = 'text-yellow-500';
        statusBg = 'bg-yellow-500/10';
        statusIcon = <FaHistory />;
        break;
    case 'Active':
    default:
        statusText = 'ACTIVE';
        statusColor = 'text-green-500';
        statusBg = 'bg-green-500/10';
        statusIcon = <FaCheckCircle />;
        break;
  }

  const RevokeButton = (
    <button
      onClick={() => setIsModalOpen(true)}
      className={`${revokeButtonClass} hover:bg-[${server_color_hover}]`}
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

  const StatusMessage = (
    <div className={`p-4 rounded-xl ${isRemoved ? 'bg-red-500/20 text-red-400 border-red-500/40' : 'bg-yellow-500/20 text-yellow-400 border-yellow-500/40'} font-bold text-center border shadow-md`}>
        {isRemoved ? 'This punishment has already been removed.' : 'This punishment has expired.'}
    </div>
  );

  return (
    <div className={`min-h-screen ${bodyBg} ${textColor} font-sans`}>
      <div className="container mx-auto p-4 sm:p-8 max-w-5xl">
        
        <div className="mb-6">
            <BackButton to={'/'} currentTheme={currentTheme} />
        </div>

        <div className={`p-6 sm:p-8 rounded-2xl ${cardBg} ${shadow}`}>
            
            <h1 className={`text-center text-3xl font-extrabold mb-4 ${currentTheme === 'dark' ? 'text-white' : 'text-gray-800'}`}>
                {punishment_type} on {player_name}
            </h1>
            
            <div className="flex flex-wrap justify-center items-center space-x-4 mb-8">
                
                <div className={`text-sm font-bold py-2 px-4 inline-flex items-center rounded-full ${statusBg} ${statusColor}`}>
                    {statusIcon}
                    <span className='ml-2 tracking-wider'>{statusText}</span>
                </div>

                <div className={`text-sm font-bold py-2 px-4 inline-flex items-center rounded-full border ${currentTheme === 'dark' ? 'border-gray-600/30' : 'border-gray-300'}`} 
                     style={{ color: server_color, backgroundColor: currentTheme === 'dark' ? 'rgba(79, 70, 229, 0.1)' : 'rgba(79, 70, 229, 0.05)' }}>
                    <FaFingerprint className='mr-2 text-base' />
                    <span className='tracking-wider'>ID: {punishment_id}</span>
                </div>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
                
                <DetailCard title="Core Information" accentColor={server_color} currentTheme={currentTheme}>
                    <DetailItem icon={<FaUser />} label="Player" value={player_name} accentColor={server_color} currentTheme={currentTheme} />
                    <DetailItem icon={<FaUserShield />} label="Executor" value={executor} accentColor={server_color} currentTheme={currentTheme} />
                    <DetailItem icon={<FaListAlt />} label="Type" value={punishment_type} accentColor={server_color} currentTheme={currentTheme} />
                </DetailCard>

                <DetailCard title="Time & Duration" accentColor={server_color} currentTheme={currentTheme}>
                    <DetailItem icon={<FaCalendarAlt />} label="Execution Date" value={execution_date} accentColor={server_color} currentTheme={currentTheme} />
                    <DetailItem icon={<FaClock />} label="Duration" value={duration} accentColor={server_color} currentTheme={currentTheme} />
                    <DetailItem icon={<FaCalendarAlt />} label="Expiration Date" value={expiration_date} accentColor={server_color} currentTheme={currentTheme} />
                </DetailCard>

                <DetailCard title="Server & Context" accentColor={server_color} currentTheme={currentTheme}>
                    <DetailItem icon={<FaServer />} label="Origin Server" value={origin_server} accentColor={server_color} currentTheme={currentTheme} />
                    <DetailItem icon={<FaServer />} label="Scope Server" value={scope_server} accentColor={server_color} currentTheme={currentTheme} />
                </DetailCard>
            </div>

            <div className={`mb-8 p-6 rounded-xl ${cardBg} border ${currentTheme === 'dark' ? 'border-gray-700' : 'border-gray-300'} shadow-md`}>
                <h2 className={`text-lg font-bold mb-3 border-b pb-2 flex items-center ${currentTheme === 'dark' ? 'border-gray-700' : 'border-gray-300'}`} style={{ color: server_color }}>
                    <FaInfoCircle className='mr-2' /> Reason for Punishment
                </h2>
                <p className="whitespace-pre-wrap text-base opacity-90">{reason}</p>
            </div>
            
            {isRemoved && (
                <div className="mb-8 p-6 rounded-xl bg-red-500/15 border border-red-500/40 shadow-md">
                    <h2 className="text-lg font-bold mb-3 text-red-400 border-b border-red-500/40 pb-2 flex items-center space-x-2">
                        <FaTimesCircle className="text-xl"/> <span>Removal Details</span>
                    </h2>
                    <p className="text-red-300 text-base mb-2"><strong>Removed By:</strong> {remover_name}</p>
                    <p className="text-red-300 text-base"><strong>Reason:</strong> {removal_reason}</p>
                </div>
            )}
            
            <div className='mt-8'>
                {serverConfig.isSecured && user && isActive && RevokeButton}
                {(serverConfig.isSecured && isRemoved || isExpired) && StatusMessage}
            </div>
        </div>
      </div>
      
      {user &&
        <RevokeModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onRevoke={handleRevoke}
          removerName={user.username}
          currentTheme={currentTheme}
          isSubmitting={isRevoking}
        />
      }
    </div>
  );
};

const PunishmentDetailPage: React.FC = () => {
    const { serverConfig } = useServerConfig();
    const { currentTheme } = useTheme();

    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();

    const { punishmentDetails, isLoading, error } = usePunishmentDetails(id);

    useEffect(() => {
        if (!isLoading && (!id || !punishmentDetails || error)) {
            navigate("/unknown"); 
        }
    }, [id, punishmentDetails, isLoading, error, navigate]);

    if (isLoading) {
        return (
            <div className={`flex items-center justify-center min-h-screen ${currentTheme === 'dark' ? 'bg-[#1c1c1c] text-gray-100' : 'bg-gray-50 text-gray-800'}`}>
                <div className={`flex items-center space-x-3 p-8 rounded-xl ${currentTheme === 'dark' ? 'bg-[#2c2c2c]' : 'bg-white'} shadow-xl`}>
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2" style={{ borderColor: serverConfig.serverColor }}></div>
                    <p className="text-xl font-semibold">Loading Punishment Details...</p>
                </div>
            </div>
        );
    }
    
    if (!punishmentDetails) {
        return null;
    }

    return (
        <PunishmentDetailContent 
            {...punishmentDetails}
            server_color={serverConfig.serverColor}
            server_color_hover={serverConfig.serverColorHover}
            currentTheme={currentTheme}
        />
    );
};

export default PunishmentDetailPage;