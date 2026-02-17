import React, { useEffect, useState } from 'react';
import { FaTimes, FaGavel, FaCheck, FaUndo } from 'react-icons/fa';

interface FilterModalProps {
  isOpen: boolean;
  onClose: () => void;
  onApply: (filters: {
    executor: string;
    status: 'active' | 'expired' | 'removed' | '';
    dateFilterType: 'none' | 'exact' | 'before' | 'after';
    dateValue: string;
  }) => void;
  serverColor: string;
  currentTheme: 'dark' | 'light';
}

const FilterModal: React.FC<FilterModalProps> = ({
                                                   isOpen,
                                                   onClose,
                                                   onApply,
                                                   serverColor,
                                                   currentTheme,
                                                 }) => {
  const [executor, setExecutor] = useState('');
  const [status, setStatus] = useState<'active' | 'expired' | 'removed' | ''>('');
  const [dateFilterType, setDateFilterType] =
      useState<'none' | 'exact' | 'before' | 'after'>('none');
  const [dateValue, setDateValue] = useState('');

  useEffect(() => {
    if (isOpen) document.body.classList.add('overflow-hidden');
    else document.body.classList.remove('overflow-hidden');

    return () => document.body.classList.remove('overflow-hidden');
  }, [isOpen]);

  if (!isOpen) return null;

  const isDark = currentTheme === 'dark';

  const modalClasses = isDark
      ? 'bg-[#151515e6] text-white border border-white/10'
      : 'bg-white/90 text-gray-900 border border-black/5';

  const inputClasses = isDark
      ? 'bg-white/5 border border-white/10 text-white placeholder-white/60 focus:ring-2 focus:ring-green-500/70'
      : 'bg-gray-50 border border-gray-200 text-gray-900 placeholder-gray-500 focus:ring-2 focus:ring-green-500/70';

  const handleApply = () => {
    onApply({ executor, status, dateFilterType, dateValue });
  };

  const handleReset = () => {
    setExecutor('');
    setStatus('');
    setDateFilterType('none');
    setDateValue('');
    onApply({ executor: '', status: '', dateFilterType: 'none', dateValue: '' });
  };

  const hasActiveFilters = executor || status || dateFilterType !== 'none';

  return (
      <div className="fixed inset-0 backdrop-filter backdrop-blur-sm bg-black/40 flex items-center justify-center z-200 p-4">
        <div
            className={`relative w-full max-w-md max-h-[90vh] rounded-2xl shadow-[0_18px_55px_rgba(0,0,0,0.45)] overflow-hidden flex flex-col ${modalClasses}`}
            style={{
              backgroundImage: isDark
                  ? 'linear-gradient(160deg, rgba(28,28,28,0.95), rgba(18,18,18,0.9))'
                  : 'linear-gradient(160deg, rgba(255,255,255,0.95), rgba(245,245,245,0.9))',
            }}
        >
          {/* Close button */}
          <button
              onClick={onClose}
              className="absolute top-3 right-3 h-10 w-10 grid place-items-center rounded-full bg-white/6 text-gray-400 hover:text-red-400 hover:bg-red-400/10 transition duration-200 backdrop-blur z-10"
              aria-label="Close modal"
          >
            <FaTimes />
          </button>

          {/* Header */}
          <div className="p-8 pb-4 pr-16">
            <h2 className="text-3xl font-bold leading-tight">Filter Punishments</h2>
            {hasActiveFilters && (
                <p className="text-sm text-gray-400 mt-2">
                  {Object.entries({ executor, status, dateFilterType }).filter(([k, v]) =>
                      k === 'dateFilterType' ? v !== 'none' : v
                  ).length} filter(s) active
                </p>
            )}
          </div>

          {/* Body */}
          <div className="px-8 pb-8 flex-1 overflow-y-auto space-y-6">
            {/* Executor */}
            <div className="space-y-2">
              <label className="block text-sm font-semibold uppercase tracking-wide text-gray-400">
                Executed by
              </label>
              <div className="relative">
                <FaGavel className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                <input
                    type="text"
                    placeholder="Moderator name"
                    value={executor}
                    onChange={(e) => setExecutor(e.target.value)}
                    className={`w-full rounded-lg px-3 py-2 pl-10 text-base transition-all duration-200 focus:outline-none focus:ring-2 focus:-translate-y-px ${inputClasses}`}
                    style={{
                      '--tw-ring-color': `${serverColor}66`
                    } as React.CSSProperties}
                />
              </div>
            </div>

            {/* Status */}
            <div className="space-y-2">
              <label className="block text-sm font-semibold uppercase tracking-wide text-gray-400">
                Status
              </label>
              <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value as any)}
                  className={`w-full rounded-lg px-3 py-2 text-base transition ${inputClasses}`}
              >
                <option value="">All statuses</option>
                <option value="active">Active</option>
                <option value="expired">Expired</option>
                <option value="removed">Removed</option>
              </select>
            </div>

            {/* Date Filter */}
            <div className="space-y-3 rounded-xl border border-white/10 bg-white/6 px-4 py-4 backdrop-blur">
              <p className="text-sm font-semibold uppercase tracking-wide text-gray-400">
                Date Filter
              </p>

              <div className="space-y-1">
                <label className="flex items-center gap-2 text-sm text-gray-200">
                  <input
                      type="radio"
                      name="dateFilter"
                      checked={dateFilterType === 'none'}
                      onChange={() => {
                        setDateFilterType('none');
                        setDateValue('');
                      }}
                      className="h-4 w-4"
                      style={{ accentColor: serverColor }}
                  />
                  No date filter
                </label>
              </div>

              {(['exact', 'before', 'after'] as const).map((type) => (
                  <div key={type} className="space-y-1">
                    <label className="flex items-center gap-2 text-sm text-gray-200">
                      <input
                          type="radio"
                          name="dateFilter"
                          checked={dateFilterType === type}
                          onChange={() => setDateFilterType(type)}
                          className="h-4 w-4"
                          style={{ accentColor: serverColor }}
                      />
                      {type.charAt(0).toUpperCase() + type.slice(1)} date
                    </label>

                    <input
                        type="date"
                        value={dateFilterType === type ? dateValue : ''}
                        onChange={(e) => setDateValue(e.target.value)}
                        disabled={dateFilterType !== type}
                        className={`w-full rounded-lg px-3 py-2 text-base transition-all duration-200 focus:outline-none focus:ring-2 ${inputClasses} ${
                            dateFilterType !== type ? 'opacity-50 cursor-not-allowed' : 'focus:-translate-y-px'
                        }`}
                        style={{
                          '--tw-ring-color': `${serverColor}66`
                        } as React.CSSProperties}
                    />
                  </div>
              ))}
            </div>

            {/* Buttons */}
            <div className="flex gap-3">
              <button
                  onClick={handleReset}
                  disabled={!hasActiveFilters}
                  className={`flex-1 font-semibold py-3 rounded-xl transition duration-200 transform hover:-translate-y-px active:translate-y-0 ${
                      hasActiveFilters
                          ? 'bg-gray-600 text-white hover:bg-gray-700'
                          : 'bg-gray-600/50 text-gray-400 cursor-not-allowed'
                  }`}
              >
                <FaUndo className="inline mr-2" />
                Reset
              </button>

              <button
                  onClick={handleApply}
                  className="flex-1 text-white font-semibold py-3 rounded-xl transition duration-200 transform hover:-translate-y-px active:translate-y-0"
                  style={{
                    backgroundColor: serverColor,
                    boxShadow: `0 10px 25px ${serverColor}55`,
                  }}
              >
                <FaCheck className="inline mr-2" />
                Apply
              </button>
            </div>
          </div>
        </div>
      </div>
  );
};

export default FilterModal;