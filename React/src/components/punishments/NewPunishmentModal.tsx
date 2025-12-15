import React, { useState, useEffect, type ChangeEvent, type FormEvent } from 'react';
import { FaTimes, FaCheck, FaSpinner } from 'react-icons/fa';
import { useServerConfig } from '../../hooks/useServerConfig';
import { useNewPunishment } from '../../hooks/useNewPunishment'; // <--- NEW HOOK IMPORT

interface NewPunishmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  executorName: string;
  currentTheme: 'dark' | 'light';
}

interface PunishmentFormData {
  target: string;
  identity: string;
  punishmentType: 'ban' | 'mute' | 'kick' | 'warning' | '';
  silent: boolean;
  reason: string;
  duration: string;
  permanent: boolean;
}

const NewPunishmentModal: React.FC<NewPunishmentModalProps> = ({ 
  isOpen, 
  onClose, 
  executorName, 
  currentTheme
}) => {
  const { serverConfig } = useServerConfig();
  const { createPunishment, isCreating, error, success, resetState } = useNewPunishment();
  
  const [formData, setFormData] = useState<PunishmentFormData>({
    target: '',
    identity: executorName,
    punishmentType: '',
    silent: false,
    reason: '',
    duration: '',
    permanent: false,
  });
  
  useEffect(() => {
    async function syncExecutorIdentity() {
      setFormData(prev => ({
        ...prev,
        identity: executorName,
      }));
    }

    syncExecutorIdentity();
  }, [executorName]);

  useEffect(() => {
    async function handleModalState() {
      if (isOpen) {
        document.body.classList.add("overflow-hidden");
        resetState();
      } else {
        document.body.classList.remove("overflow-hidden");
        setFormData({
          target: '',
          identity: executorName,
          punishmentType: '',
          silent: false,
          reason: '',
          duration: '',
          permanent: false,
        });
      }
    }

    handleModalState();

    return () => {
      document.body.classList.remove("overflow-hidden");
    };
  }, [isOpen, executorName, resetState]);

  if (!isOpen) return null;

  const isDark = currentTheme === 'dark';
  const modalClasses = isDark ? 'bg-[#242424] text-white' : 'bg-white text-gray-800';
  const inputClasses = isDark 
    ? 'bg-[#3b3b3bcc] border-none text-white focus:ring-1 focus:ring-green-500' 
    : 'bg-gray-100 border border-gray-300 text-gray-800 focus:ring-1 focus:ring-green-500';
  const readOnlyInputClasses = 'bg-gray-300 dark:bg-[#4a4a4acc] cursor-not-allowed text-[#333333] dark:text-[#e0e0e0]';

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const target = e.target as HTMLInputElement;
    const { name, value, type, checked } = target;

    setFormData(prev => {
      const newState = {
        ...prev,
        [name]: type === 'checkbox' ? checked : value,
      };

      if (name === 'punishmentType') {
        if (value === 'kick' || value === 'warning') {
          newState.duration = '';
          newState.permanent = false;
        }
      }

      if (name === 'permanent' && checked) {
        newState.duration = '';
      }
      
      return newState;
    });
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    resetState(); 

    const dataToSend = {
      target: formData.target,
      identity: formData.identity,
      punishmentType: formData.punishmentType,
      silent: formData.silent,
      reason: formData.reason,
      duration: (formData.punishmentType === 'kick' || formData.punishmentType === 'warning' || formData.permanent) ? null : formData.duration,
      permanent: (formData.punishmentType === 'kick' || formData.punishmentType === 'warning') ? false : formData.permanent,
    };

    try {
        await createPunishment(dataToSend);
    } catch (submitError) {
        console.error("Submission failed:", submitError);
    }
  };

  const showDurationFields = formData.punishmentType !== 'kick' && formData.punishmentType !== 'warning';
  const durationDisabled = formData.permanent || !showDurationFields;

  const PunishmentTypeOptions = ['ban', 'mute', 'kick', 'warning'];

  return (
    <div className="fixed inset-0 backdrop-filter backdrop-blur-sm bg-opacity-30 flex items-center justify-center z-200 p-4 h-auto">
      <div className={`p-8 rounded-xl shadow-2xl relative w-full max-w-lg overflow-y-auto max-h-[90vh] ${modalClasses}`}>
        <button 
          onClick={onClose} 
          className="absolute top-4 right-4 text-2xl text-gray-500 hover:text-red-500 transition duration-300 z-10"
          disabled={isCreating}
        >
          <FaTimes />
        </button>

        <h2 className="text-center text-3xl font-bold mb-8">New Punishment</h2>
        
        {error && (
            <div className="p-3 mb-4 rounded-lg bg-red-800 text-white font-medium">
                Error: {error}
            </div>
        )}
        {success && (
            <div className="p-3 mb-4 rounded-lg bg-green-700 text-white font-medium flex items-center">
                <FaCheck className="mr-2" /> Punishment successfully created!
            </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
            
            <div>
                <label htmlFor="target" className="block text-lg font-medium mb-1">Player to Punish</label>
                <input 
                  type="text" 
                  id="target" 
                  name="target" 
                  value={formData.target}
                  onChange={handleChange}
                  className={`w-full p-2 border rounded-md ${inputClasses}`} 
                  required 
                  disabled={isCreating || success}
                />
            </div>

            <div>
                <label htmlFor="executor" className="block text-lg font-medium mb-1">Executor Name</label>
                <input 
                  type="text" 
                  id="executor" 
                  name="identity" 
                  value={formData.identity}
                  readOnly 
                  className={`w-full p-2 border rounded-md ${readOnlyInputClasses}`} 
                  required
                />
            </div>

            <div>
                <label className="block text-lg font-medium mb-2">Punishment Type</label>
                <div className="flex flex-wrap gap-4">
                    {PunishmentTypeOptions.map((type) => (
                      <label key={type} className="flex items-center text-base">
                          <input 
                            type="radio" 
                            name="punishmentType" 
                            value={type} 
                            checked={formData.punishmentType === type}
                            onChange={handleChange}
                            className="mr-2 h-4 w-4"
                            style={{ accentColor: serverConfig.serverColor }}
                            required
                            disabled={isCreating || success}
                          /> 
                          {type.charAt(0).toUpperCase() + type.slice(1)}
                      </label>
                    ))}
                </div>
            </div>

            <div className="pt-2">
                <label className="flex items-center text-base font-medium">
                    <input 
                      type="checkbox" 
                      name="silent"
                      checked={formData.silent}
                      onChange={handleChange}
                      className="mr-2 h-4 w-4"
                      style={{ accentColor: serverConfig.serverColor }}
                      disabled={isCreating || success}
                    /> 
                    Silent Mode
                </label>
            </div>

            <div>
                <label htmlFor="reason" className="block text-lg font-medium mb-1">Reason</label>
                <input 
                  type="text" 
                  id="reason" 
                  name="reason" 
                  value={formData.reason}
                  onChange={handleChange}
                  className={`w-full p-2 border rounded-md ${inputClasses}`} 
                  required
                  disabled={isCreating || success}
                />
            </div>

            <div className={`space-y-4 transition-all duration-300 ${!showDurationFields ? 'hidden opacity-0' : 'block opacity-100'}`}>
                <label htmlFor="duration" className="block text-lg font-medium mb-1">Duration (e.g., 1h, 1d, 3w)</label>
                <input 
                    type="text" 
                    id="duration" 
                    name="duration" 
                    value={formData.duration}
                    onChange={handleChange}
                    className={`w-full p-2 border rounded-md ${inputClasses} ${durationDisabled ? 'opacity-50 cursor-not-allowed' : ''}`} 
                    placeholder="e.g., 1h, 1d"
                    disabled={durationDisabled || isCreating || success}
                    required={showDurationFields && !durationDisabled && !success}
                />
                <label className="flex items-center text-base font-medium pt-1">
                    <input 
                        type="checkbox" 
                        id="permanent" 
                        name="permanent"
                        checked={formData.permanent}
                        onChange={handleChange}
                        className="mr-2 h-4 w-4"
                        style={{ accentColor: serverConfig.serverColor }}
                        disabled={isCreating || success}
                    /> 
                    Permanent
                </label>
            </div>

            <button 
              type="submit" 
              className="w-full text-white font-bold py-3 rounded-xl transition duration-300 transform hover:scale-[1.01] active:scale-[0.99] disabled:opacity-60 disabled:cursor-not-allowed"
              style={{
                backgroundColor: serverConfig.serverColor,
                boxShadow: `0 4px 6px -1px ${serverConfig.serverColor}70, 0 2px 4px -2px ${serverConfig.serverColor}70`,
              }}
              disabled={isCreating || success}
            >
                {isCreating ? (
                    <>
                        <FaSpinner className="inline mr-2 animate-spin" /> Submitting...
                    </>
                ) : (
                    <>
                        <FaCheck className="inline mr-2" /> Submit Punishment
                    </>
                )}
            </button>
        </form>
      </div>
    </div>
  );
};

export default NewPunishmentModal;