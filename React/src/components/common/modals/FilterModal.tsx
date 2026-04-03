import React, { useEffect, useState } from 'react';
import { FaTimes, FaGavel, FaCheck, FaUndo } from 'react-icons/fa';
import {useTranslation} from "react-i18next";

interface FilterModalProps {
  isOpen: boolean;
  onClose: () => void;
  onApply: (filters: {
    executor: string;
    status: 'active' | 'expired' | 'removed' | '';
    dateFilterType: 'none' | 'exact' | 'before' | 'after';
    dateValue: string;
  }) => void;
}

const FilterModal: React.FC<FilterModalProps> = ({
                                                   isOpen,
                                                   onClose,
                                                   onApply,
                                                 }) => {
  const [executor, setExecutor] = useState('');
  const [status, setStatus] = useState<'active' | 'expired' | 'removed' | ''>('');
  const [dateFilterType, setDateFilterType] =
      useState<'none' | 'exact' | 'before' | 'after'>('none');
  const [dateValue, setDateValue] = useState('');

  const { t } = useTranslation();

  useEffect(() => {
    if (isOpen) document.body.classList.add('overflow-hidden');
    else document.body.classList.remove('overflow-hidden');

    return () => document.body.classList.remove('overflow-hidden');
  }, [isOpen]);

  if (!isOpen) return null;

  const modalClasses = 'bg-modal-background text-modal-text-primary border border-border-c'
  const inputClasses = 'bg-surface border border-surface-border text-white placeholder-modal-text-secondary focus:ring-2 focus:ring-green-500/70'

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

  const dateFilter: {key: 'exact' | 'before' | 'after', label: string}[] = [
    {key: 'exact', label: t("filter-modal.filters.date.exact")},
    {key: 'before', label: t("filter-modal.filters.date.before")},
    {key: 'after', label: t("filter-modal.filters.date.after")},
  ];

  return (
      <div className="fixed inset-0 backdrop-filter backdrop-blur-sm bg-modal-background/40 flex items-center justify-center z-200 p-4">
        <div
            className={`relative w-full max-w-md max-h-[90vh] rounded-2xl overflow-hidden flex flex-col ${modalClasses}`}
        >
          <button
              onClick={onClose}
              className="absolute top-3 right-3 h-10 w-10 grid place-items-center rounded-full bg-surface text-text-secondary hover:text-modal-text-error hover:bg-modal-error transition duration-200 backdrop-blur z-10"
              aria-label="Close modal"
          >
            <FaTimes />
          </button>

          <div className="p-8 pb-4 pr-16">
            <h2 className="text-3xl font-bold leading-tight">{t("filter-modal.title")}</h2>
              {hasActiveFilters && (
                  <p className="text-sm text-text-secondary mt-2">
                    {t("filter-modal.active-count",
                      { count:
                            Object.entries({ executor, status, dateFilterType }).filter(([k, v]) =>
                              k === 'dateFilterType' ? v !== 'none' : v
                            ).length  })}
                  </p>
              )}
          </div>

          <div className="px-8 pb-8 flex-1 overflow-y-auto space-y-6">
            <div className="space-y-2">
              <label className="block text-sm font-semibold uppercase tracking-wide text-text-secondary">
                {t("filter-modal.filters.moderator.label")}
              </label>
              <div className="relative">
                <FaGavel className="absolute left-3 top-1/2 z-200 -translate-y-1/2 text-text-secondary" />
                <input
                    type="text"
                    placeholder={t("filter-modal.filters.moderator.placeholder")}
                    value={executor}
                    onChange={(e) => setExecutor(e.target.value)}
                    className={`w-full rounded-lg px-3 py-2 pl-10 text-base transition-all duration-200 focus:outline-none focus:ring-2 focus:-translate-y-px focus:ring-server-color/30 ${inputClasses}`}
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="block text-sm font-semibold uppercase tracking-wide text-text-secondary">
                {t("filter-modal.filters.statuses.title")}
              </label>
              <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value as any)}
                  className={`w-full rounded-lg px-3 py-2 text-base transition ${inputClasses}`}
              >
                <option value="">{t("filter-modal.filters.statuses.all")}</option>
                <option value="active">{t("filter-modal.filters.statuses.active")}</option>
                <option value="expired">{t("filter-modal.filters.statuses.expired")}</option>
                <option value="removed">{t("filter-modal.filters.statuses.removed")}</option>
              </select>
            </div>

            <div className="space-y-3 rounded-xl border border-surface-border bg-surface px-4 py-4 backdrop-blur">
              <p className="text-sm font-semibold uppercase tracking-wide text-text-secondary">
                {t("filter-modal.filters.date.title")}
              </p>

              <div className="space-y-1">
                <label className="flex items-center gap-2 text-sm text-text-primary accent-server-color">
                  <input
                      type="radio"
                      name="dateFilter"
                      checked={dateFilterType === 'none'}
                      onChange={() => {
                        setDateFilterType('none');
                        setDateValue('');
                      }}
                      className="h-4 w-4"
                  />
                  {t("filter-modal.filters.date.none")}
                </label>
              </div>

              {dateFilter.map((type) => (
                  <div key={type.key} className="space-y-1">
                    <label className="flex items-center gap-2 text-sm text-text-primary accent-server-color">
                      <input
                          type="radio"
                          name="dateFilter"
                          checked={dateFilterType === type.key}
                          onChange={() => setDateFilterType(type.key)}
                          className="h-4 w-4"
                      />
                      {type.label}
                    </label>

                    <input
                        type="date"
                        value={dateFilterType === type.key ? dateValue : ''}
                        onChange={(e) => setDateValue(e.target.value)}
                        disabled={dateFilterType !== type.key}
                        className={`w-full rounded-lg px-3 py-2 text-base transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-server-color/30 ${inputClasses} ${
                            dateFilterType !== type.key ? 'opacity-50 cursor-not-allowed' : 'focus:-translate-y-px'
                        }`}
                    />
                  </div>
              ))}
            </div>

            <div className="flex gap-3">
              <button
                  onClick={handleReset}
                  disabled={!hasActiveFilters}
                  className={`flex-1 font-semibold py-3 rounded-xl transition duration-200 transform hover:-translate-y-px active:translate-y-0 ${
                      hasActiveFilters
                          ? 'bg-modal-surface text-modal-text-primary hover:bg-modal-surface/80'
                          : 'bg-modal-surface/50 text-modal-text-secondary cursor-not-allowed'
                  }`}
              >
                <FaUndo className="inline mr-2" />
                {t("filter-modal.buttons.reset")}
              </button>

              <button
                  onClick={handleApply}
                  className="bg-server-color flex-1 text-text-primary font-semibold py-3 rounded-xl transition duration-200 transform hover:-translate-y-px active:translate-y-0"
              >
                <FaCheck className="inline mr-2" />
                {t("filter-modal.buttons.apply")}
              </button>
            </div>
          </div>
        </div>
      </div>
  );
};

export default FilterModal;