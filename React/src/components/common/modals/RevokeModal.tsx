import React, { useEffect, useState } from "react";
import { FaTimes } from "react-icons/fa";
import { usePunishmentRevocation } from "../../../hooks/usePunishmentRevocation.ts";
import { useServerConfig } from "../../../hooks/useServerConfig.ts";

type Theme = "dark" | "light";

interface RevokeModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
  removerName: string;
  currentTheme: Theme;
  punishmentId: number;
  punishmentType: string;
}

const RevokeModal: React.FC<RevokeModalProps> = ({
                                                   isOpen,
                                                   onClose,
                                                   onSuccess,
                                                   removerName,
                                                   currentTheme,
                                                   punishmentId,
                                                   punishmentType,
                                                 }) => {
  const [removalReason, setRemovalReason] = useState("");
  const [silent, setSilent] = useState(false);
  const { serverConfig } = useServerConfig();
  const { revokePunishment, isRevoking, error } = usePunishmentRevocation();

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

  const isDark = currentTheme === "dark";
  const modalClasses = isDark
      ? "bg-[#151515e6] text-white border border-white/10"
      : "bg-white/90 text-gray-900 border border-black/5";
  const inputClasses = isDark
      ? "bg-white/5 border border-white/10 text-white placeholder-white/60 focus:ring-2 focus:ring-red-500/70"
      : "bg-gray-50 border border-gray-200 text-gray-900 placeholder-gray-500 focus:ring-2 focus:ring-red-500/70";
  const readOnlyClasses =
      "bg-white/10 dark:bg-white/5 border border-transparent cursor-not-allowed text-white/80 dark:text-white/80";

  return (
      <div
          className="fixed inset-0 backdrop-filter backdrop-blur-sm bg-black/40 flex items-center justify-center z-[200] p-4"
          onClick={onClose}
      >
        <div
            className={`relative w-full max-w-md max-h-[90vh] rounded-2xl shadow-[0_18px_55px_rgba(0,0,0,0.45)] overflow-hidden flex flex-col ${modalClasses}`}
            style={{
              backgroundImage: isDark
                  ? "linear-gradient(160deg, rgba(28,28,28,0.95), rgba(18,18,18,0.9))"
                  : "linear-gradient(160deg, rgba(255,255,255,0.95), rgba(245,245,245,0.9))",
            }}
            onClick={(e) => e.stopPropagation()}
        >
          <button
              onClick={onClose}
              className="absolute top-3 right-3 h-10 w-10 grid place-items-center rounded-full bg-white/6 text-gray-400 hover:text-red-400 hover:bg-red-400/10 transition duration-200 backdrop-blur"
              disabled={isRevoking}
              aria-label="Close modal"
          >
            <FaTimes />
          </button>

          <div className="p-6 pb-3 flex items-center justify-between gap-4 pr-14">
            <h2 className="text-2xl font-bold leading-tight">Revoke Punishment</h2>
            <span className="px-3 py-1 text-xs font-semibold rounded-full tracking-wide uppercase bg-red-500/15 border border-red-500/30 text-red-300">
            REVOKE
          </span>
          </div>

          <div className="px-6 pb-6 flex-1 overflow-y-auto">
            <form onSubmit={handleSubmit} className="space-y-5">
              {error && (
                  <div className="p-3 text-sm bg-red-500/10 border border-red-500/20 text-red-400 rounded-lg">
                    {error}
                  </div>
              )}

              <div className="space-y-2">
                <label htmlFor="identity" className="block text-sm font-semibold uppercase tracking-wide text-gray-400">
                  Who are you?
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
                    className="block text-sm font-semibold uppercase tracking-wide text-gray-400"
                >
                  Reason for Removal
                </label>
                <textarea
                    id="removal-reason"
                    className={`w-full rounded-lg px-3 py-2 text-base transition ${inputClasses}`}
                    rows={4}
                    value={removalReason}
                    onChange={(e) => setRemovalReason(e.target.value)}
                    required
                    disabled={isRevoking}
                    placeholder="Provide a concise reason…"
                />
              </div>

              <div className="flex items-center justify-between rounded-xl border border-white/10 bg-white/6 px-3 py-3 backdrop-blur">
                <div>
                  <p className="text-sm font-semibold text-gray-200">Silent Mode</p>
                  <p className="text-xs text-gray-400">Don't notify everyone of revocation</p>
                </div>
                <label className="inline-flex items-center gap-2 text-sm font-medium">
                  <input
                      type="checkbox"
                      name="silent"
                      checked={silent}
                      onChange={(e) => setSilent(e.target.checked)}
                      className="h-4 w-4"
                      style={{ accentColor: serverConfig.serverColor }}
                      disabled={isRevoking}
                  />
                </label>
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                    type="button"
                    onClick={onClose}
                    className="px-4 py-2 rounded-xl bg-white/10 text-gray-200 hover:bg-white/15 transition duration-200 disabled:opacity-60"
                    disabled={isRevoking}
                >
                  Cancel
                </button>
                <button
                    type="submit"
                    className="px-4 py-2 rounded-xl text-white font-semibold transition duration-200 disabled:opacity-60 disabled:cursor-not-allowed"
                    style={{
                      backgroundColor: "#ef4444",
                      boxShadow: "0 10px 25px rgba(239,68,68,0.35)",
                    }}
                    disabled={isRevoking || !removalReason.trim()}
                >
                  {isRevoking ? "Submitting..." : "Submit"}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
  );
};

export default RevokeModal;