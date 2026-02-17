import React from 'react';
import { FaBolt, FaUndo } from 'react-icons/fa';
import Toggle from './Toggle';
import Slider from './Slider';
import type {ServerConfig} from "../../types/config.tsx";
import type {Theme} from "../../types/admin.tsx";

type PunishmentKey = keyof ServerConfig['punishments'];

interface Props {
    punishments: ServerConfig['punishments'];
    features: { allowExecution: boolean; allowRevocation: boolean };
    theme: Theme;
    accent: string;
    onUpdatePunishment: (key: PunishmentKey, patch: Partial<ServerConfig['punishments'][PunishmentKey]>) => void;
    onUpdateFeature: (key: 'allowExecution' | 'allowRevocation', value: boolean) => void;
}

const PunishmentSettings: React.FC<Props> = ({ punishments, features, theme, accent, onUpdatePunishment, onUpdateFeature }) => {
    const renderToggleRow = (label: string, checked: boolean, onChange: (v: boolean) => void, helper?: string) => (
        <div className="flex items-center justify-between py-3">
            <div>
                <p className="font-semibold">{label}</p>
                {helper && <p className="text-sm text-gray-500 dark:text-gray-400">{helper}</p>}
            </div>
            <Toggle checked={checked} onChange={onChange} accent={accent} />
        </div>
    );

    const renderMaxPerPage = (label: string, value: number, onChange: (v: number) => void) => (
        <div className="space-y-2">
            <div className="flex items-center justify-between">
                <p className="font-semibold">{label}</p>
                <span className="text-xs text-gray-500 dark:text-gray-400">5 - 100</span>
            </div>
            <Slider value={value} min={5} max={100} step={5} accent={accent} onChange={onChange} />
        </div>
    );

    return (
        <>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {(['bans', 'mutes', 'warnings', 'kicks'] as const).map((key) => {
                    const label = key.charAt(0).toUpperCase() + key.slice(1);
                    const entry = punishments[key];
                    return (
                        <div key={key} className={`p-4 rounded-xl border ${theme === 'dark' ? 'border-gray-700 bg-[#222222]' : 'border-gray-200 bg-gray-50'}`}>
                            {renderToggleRow(`${label} Enabled`, entry.enabled, (v) => onUpdatePunishment(key, { enabled: v }), `Toggle ${label.toLowerCase()} visibility and actions`)}
                            {renderMaxPerPage('Max per page', entry.maxPerPage, (v) => onUpdatePunishment(key, { maxPerPage: v }))}
                        </div>
                    );
                })}
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-4">
                <div className={`p-4 rounded-xl border ${theme === 'dark' ? 'border-gray-700 bg-[#222222]' : 'border-gray-200 bg-gray-50'}`}>
                    <div className="space-y-3">
                        <div className="flex items-start gap-3">
                            <div className="flex h-10 w-10 items-center justify-center rounded-lg text-white shadow-sm" style={{ backgroundColor: accent }}>
                                <FaBolt />
                            </div>
                            <div className="flex-1">
                                <div className="flex items-center justify-between gap-3">
                                    <p className="font-semibold">Execute new punishments</p>
                                    <Toggle checked={features.allowExecution} onChange={(v) => onUpdateFeature('allowExecution', v)} accent={accent} />
                                </div>
                                <p className="text-sm text-gray-500 dark:text-gray-400">
                                    Allow moderators to apply new punishments directly from the dashboard.
                                </p>
                            </div>
                        </div>

                        <div className="flex items-start gap-3 border-t border-gray-200 dark:border-gray-700 pt-3">
                            <div className="flex h-10 w-10 items-center justify-center rounded-lg text-white shadow-sm" style={{ backgroundColor: accent }}>
                                <FaUndo />
                            </div>
                            <div className="flex-1">
                                <div className="flex items-center justify-between gap-3">
                                    <p className="font-semibold">Revoke active punishments</p>
                                    <Toggle checked={features.allowRevocation} onChange={(v) => onUpdateFeature('allowRevocation', v)} accent={accent} />
                                </div>
                                <p className="text-sm text-gray-500 dark:text-gray-400">
                                    Allow revocation of ongoing punishments from the dashboard.
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </>
    );
};

export default PunishmentSettings;