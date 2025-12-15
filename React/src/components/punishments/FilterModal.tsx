import React, { useEffect, useState } from 'react';
import { FaTimes, FaGavel } from 'react-icons/fa';

interface FilterModalProps {
  isOpen: boolean;
  onClose: () => void;
  onApply: (filters: unknown) => void;
  serverColor: string;
  currentTheme: 'dark' | 'light';
}

const FilterModal: React.FC<FilterModalProps> = ({ isOpen, onClose, onApply, serverColor, currentTheme }) => {
  const [executor, setExecutor] = useState('');
  const [status, setStatus] = useState<'active' | 'expired' | 'removed'>('active');
  const [dateFilterType, setDateFilterType] = useState<'none' | 'exact' | 'before' | 'after'>('none');
  const [dateValue, setDateValue] = useState('');

  useEffect(() => {
    if (isOpen) {
      document.body.classList.add("overflow-hidden");
    } else {
      document.body.classList.remove("overflow-hidden");
    }

    return () => {
      document.body.classList.remove("overflow-hidden");
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const modalClasses = currentTheme === 'dark' ? 'bg-[#242424] text-white' : 'bg-white text-gray-800';
  const inputClasses = currentTheme === 'dark' ? 'bg-[#3b3b3bcc] border-none text-white focus:ring-1 focus:ring-indigo-500' : 'bg-gray-100 border border-gray-300 text-gray-800 focus:ring-1 focus:ring-indigo-500';

  const handleApply = () => {
    onApply({ executor, status, dateFilterType, dateValue });
  };

  return (
    <div className="fixed inset-0 backdrop-filter backdrop-blur-sm bg-opacity-30 flex items-center justify-center z-200 p-4 h-auto">
      <div className={`px-6 py-5 rounded-xl shadow-2xl relative w-full max-w-sm ${modalClasses}`}>
        <button onClick={onClose} className="absolute top-4 right-4 text-2xl text-gray-500 hover:text-red-500 transition duration-300">
          <FaTimes />
        </button>
        
        <h2 className="text-2xl font-bold mb-6">Sort Punishments</h2>

        <div className="mb-4">
          <label htmlFor="executorInput" className="block mb-2 font-semibold text-sm">Executed by</label>
          <div className="relative">
            <input 
              type="text" 
              id="executorInput" 
              placeholder="Search Moderator..."
              value={executor}
              onChange={(e) => setExecutor(e.target.value)}
              className={`w-full p-2 pl-10 rounded-lg shadow-sm focus:outline-none ${inputClasses}`}
            />
            <FaGavel className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
          </div>
        </div>

        <div className="mb-4">
          <label htmlFor="status" className="block mb-2 font-semibold text-sm">Status</label>
          <select 
            id="status" 
            value={status} 
            onChange={(e) => setStatus(e.target.value as never)}
            className={`w-full p-2 rounded-lg shadow-sm focus:outline-none ${inputClasses} appearance-none cursor-pointer`}
          >
            <option value="active">Active</option>
            <option value="expired">Expired</option>
            <option value="removed">Removed</option>
          </select>
        </div>

        <div className="mb-6 border p-4 rounded-lg space-y-3" style={{borderColor: currentTheme === 'dark' ? '#333333' : '#e0e0e0'}}>
          <label className="block font-semibold text-sm">Date Filter</label>
          
          {['exact', 'before', 'after'].map(type => (
            <div key={type}>
              <label className="flex items-center mb-1 text-sm">
                <input 
                  type="radio" 
                  name="dateFilter" 
                  value={type} 
                  checked={dateFilterType === type}
                  onChange={() => setDateFilterType(type as never)}
                  className="mr-2 h-4 w-4"
                  style={{accentColor: serverColor}}
                /> 
                {type.charAt(0).toUpperCase() + type.slice(1)} Date
              </label>
              <input 
                type="date" 
                id={`${type}Date`} 
                value={dateFilterType === type ? dateValue : ''}
                onChange={(e) => setDateValue(e.target.value)}
                className={`w-full p-2 rounded-lg shadow-sm focus:outline-none ${inputClasses}`}
                disabled={dateFilterType !== type}
              />
            </div>
          ))}
        </div>

        <button 
          onClick={handleApply}
          className="w-full text-white font-bold py-3 rounded-xl transition duration-300 transform hover:scale-[1.02] active:scale-[0.98] custom-button-glow"
          style={{
            backgroundColor: serverColor,
          }}
        >
          Apply Filter
        </button>
      </div>
    </div>
  );
};

export default FilterModal;