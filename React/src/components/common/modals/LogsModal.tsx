import React from 'react';
import { FaTimes } from 'react-icons/fa';
import {useTranslation} from "react-i18next";

type LogsModalProps = {
    isOpen: boolean;
    onClose: () => void;
    logs: string[];
};

const LogsModal: React.FC<LogsModalProps> = ({ isOpen, onClose, logs }) => {
    const { t } =useTranslation();

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-200 flex items-center justify-center px-4 backdrop-blur">
            <div className="absolute inset-0 bg-modal-background/40" onClick={onClose} aria-label="Close logs modal" />
            <div
                className={`relative w-full max-w-3xl rounded-2xl border
                            bg-modal-background border-border-c text-text-primary`}
            >
                <div className="flex items-center justify-between px-6 py-4 border-b border-border-c">
                    <div>
                        <h2 className="text-lg font-semibold">{t("admin.logs.modal.title")}</h2>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-3 rounded-full bg-modal-surface text-modal-text-secondary
                                   hover:bg-modal-error hover:text-modal-text-error transition-all
                                   duration-200"
                        aria-label="Close"
                    >
                        <FaTimes />
                    </button>
                </div>
                <div className="p-6">
                    <div
                        className={`rounded-2xl border overflow-auto max-h-[60vh]
                                    bg-modal-surface text-modal-text-secondary border-surface-border
                                  `}
                    >
                        <pre className="p-4 text-sm leading-6 font-mono whitespace-pre-wrap">
                            {logs.join('\n')}
                        </pre>
                    </div>
                </div>
                <div className="flex justify-end gap-3 px-6 pb-6">
                    <button
                        className={`px-4 py-2 rounded-xl
                                    bg-modal-surface text-modal-text-primary
                                    hover:bg-modal-surface-elevated transition-all duration-200
                                  `}
                        onClick={onClose}
                    >
                        {t("admin.logs.modal.buttons.close")}
                    </button>
                    <button
                        className="bg-server-color px-4 py-2 rounded-xl font-semibold text-modal-text-primary"
                        onClick={() => alert('Trigger your log export here (mock).')}
                    >
                        {t("admin.logs.modal.buttons.export")}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default LogsModal;