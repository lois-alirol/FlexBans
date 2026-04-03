import React, { useEffect, useState } from 'react';
import { FaTimes } from 'react-icons/fa';
import { useTranslation } from "react-i18next";

import { usePunishmentRevocation } from '@hooks/usePunishmentRevocation';

interface RevokeModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
  removerName: string;
  punishmentId: number;
  punishmentType: string;
}

const RevokeModal: React.FC<RevokeModalProps> = ({
                                                   isOpen,
                                                   onClose,
                                                   onSuccess,
                                                   removerName,
                                                   punishmentId,
                                                   punishmentType,
                                                 }) => {
  const [removalReason, setRemovalReason] = useState("");
  const [silent, setSilent] = useState(false);

  const { revokePunishment, isRevoking, error } = usePunishmentRevocation();
  const { t } = useTranslation();

  useEffect(() => {
    if (isOpen) {
      setRemovalReason("");
      setSilent(false);
      document.body.classList.add("overflow-hidden");
    } else {
      document.body.classList.remove("overflow-hidden");
    }
    return () => document.body.classList.remove("overflow-hidden");
  }, [isOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!removalReason.trim() || isRevoking) return;

    try {
      await revokePunishment({
        punishmentId,
        punishmentType,
        silent: silent,
        reason: removalReason.trim()
      });

      if (onSuccess) onSuccess();
      onClose();
    } catch (err) {
      console.error("Revocation failed:", err);
    }
  };

  if (!isOpen) return null;

  const modalClasses = "bg-modal-background text-modal-text-primary border border-border-c";
  const inputClasses = "bg-modal-surface border border-surface-border text-modal-text-primary placeholder-modal-text-disabled focus:outline-none focus:ring-2 focus:ring-status-removed/70"
  const readOnlyClasses = "bg-modal-surface border border-transparent cursor-not-allowed text-text-disabled";

  return (
      <div
          className="fixed inset-0 backdrop-filter backdrop-blur-sm bg-modal-background/40 flex items-center justify-center z-200 p-4"
          onClick={onClose}
      >
        <div
            className={`relative w-full max-w-md max-h-[90vh] rounded-2xl overflow-hidden flex flex-col ${modalClasses}`}
            onClick={(e) => e.stopPropagation()}
        >
          <button
              onClick={onClose}
              className="absolute top-3 right-3 h-10 w-10 grid place-items-center rounded-full bg-modal-surface text-modal-text-secondary hover:text-red-400 hover:bg-red-400/10 transition duration-200 backdrop-blur"
              disabled={isRevoking}
              aria-label="Close modal"
          >
            <FaTimes />
          </button>

          <div className="p-6 pb-3 flex items-center justify-between gap-4 pr-14">
            <h2 className="text-2xl font-bold leading-tight">
              {t("revoke-modal.title")}
            </h2>
            <span className="px-3 py-1 text-xs font-semibold rounded-full tracking-wide uppercase bg-status-removed/15 border border-status-removed/30 text-status-removed/90">
            {t("revoke-modal.badge")}
          </span>
          </div>

          <div className="px-6 pb-6 flex-1 overflow-y-auto">
            <form onSubmit={handleSubmit} className="space-y-5">

              {error && (
                  <div className="p-3 text-sm bg-modal-error border border-modal-error-border text-modal-text-error rounded-lg">
                    {error}
                  </div>
              )}

              <div className="space-y-2">
                <label htmlFor="identity" className="block text-sm font-semibold uppercase tracking-wide text-modal-text-secondary">
                  {t("revoke-modal.identity.label")}
                </label>
                <input
                    type="text"
                    id="identity"
                    className={`w-full rounded-lg px-3 py-2 text-base ${readOnlyClasses}`}
                    value={removerName}
                    readOnly
                    required
                />
              </div>

              <div className="space-y-2">
                <label
                    htmlFor="removal-reason"
                    className="block text-sm font-semibold uppercase tracking-wide text-modal-text-secondary"
                >
                  {t("revoke-modal.reason.label")}
                </label>
                <textarea
                    id="removal-reason"
                    className={`w-full rounded-lg px-3 py-2 text-base transition ${inputClasses}`}
                    rows={4}
                    value={removalReason}
                    onChange={(e) => setRemovalReason(e.target.value)}
                    required
                    disabled={isRevoking}
                    placeholder={t("revoke-modal.reason.placeholder")}
                />
              </div>

              <div className="flex items-center justify-between rounded-xl border border-surface-border bg-modal-surface px-3 py-3 backdrop-blur">
                <div>
                  <p className="text-sm font-semibold text-modal-text-primary">
                    {t("revoke-modal.silent.title")}
                  </p>
                  <p className="text-xs text-modal-text-secondary">
                    {t("revoke-modal.silent.description")}
                  </p>
                </div>
                <label className="inline-flex items-center gap-2 text-sm font-medium">
                  <input
                      type="checkbox"
                      name="silent"
                      checked={silent}
                      onChange={(e) => setSilent(e.target.checked)}
                      className="accent-server-color h-4 w-4"
                      disabled={isRevoking}
                  />
                </label>
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                    type="button"
                    onClick={onClose}
                    className="px-4 py-2 rounded-xl bg-modal-surface text-text-primary hover:bg-modal-surface-elevated transition duration-200 disabled:opacity-60"
                    disabled={isRevoking}
                >
                  {t("revoke-modal.buttons.cancel")}
                </button>

                <button
                    type="submit"
                    className="px-4 py-2 rounded-xl text-modal-text-primary font-semibold transition-colors duration-200 bg-status-removed hover:bg-status-removed/80 disabled:opacity-60 disabled:cursor-not-allowed"
                    disabled={isRevoking || !removalReason.trim()}
                >
                  {isRevoking
                      ? t("revoke-modal.buttons.submit.submitting")
                      : t("revoke-modal.buttons.submit.submit")}
                </button>
              </div>

            </form>
          </div>
        </div>
      </div>
  );
};

export default RevokeModal;