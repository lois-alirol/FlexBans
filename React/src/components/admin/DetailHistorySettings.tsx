import React from 'react';
import Toggle from './Toggle';
import Slider from './Slider';
import type {Theme} from "../../types/admin.tsx";

interface DetailSectionProps {
    theme: Theme;
    accent: string;
    details: {
        moderator: { enabled: boolean; maxPerPage: number };
        player: { enabled: boolean; maxPerPage: number };
        punishment: { enabled: boolean; revokeButton: boolean };
    };
    onUpdateDetails: (key: 'moderator' | 'player' | 'punishment', patch: Record<string, any>) => void;
}

const DetailHistorySettings: React.FC<DetailSectionProps> = ({ theme, accent, details, onUpdateDetails }) => {
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
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className={`p-4 rounded-xl border ${theme === 'dark' ? 'border-gray-700 bg-[#222222]' : 'border-gray-200 bg-gray-50'}`}>
                {renderToggleRow('Moderator History', details.moderator.enabled, (v) => onUpdateDetails('moderator', { enabled: v }))}
                {renderMaxPerPage('Max per page', details.moderator.maxPerPage, (v) => onUpdateDetails('moderator', { maxPerPage: v }))}
            </div>
            <div className={`p-4 rounded-xl border ${theme === 'dark' ? 'border-gray-700 bg-[#222222]' : 'border-gray-200 bg-gray-50'}`}>
                {renderToggleRow('Player History', details.player.enabled, (v) => onUpdateDetails('player', { enabled: v }))}
                {renderMaxPerPage('Max per page', details.player.maxPerPage, (v) => onUpdateDetails('player', { maxPerPage: v }))}
            </div>
            <div className={`p-4 rounded-xl border ${theme === 'dark' ? 'border-gray-700 bg-[#222222]' : 'border-gray-200 bg-gray-50'}`}>
                {renderToggleRow('Punishment details', details.punishment.enabled, (v) => onUpdateDetails('punishment', { enabled: v }))}
                {renderToggleRow(
                    'Revoke button',
                    details.punishment.revokeButton,
                    (v) => onUpdateDetails('punishment', { revokeButton: v }),
                    'Show revoke button in detail view'
                )}
            </div>
        </div>
    );
};

export default DetailHistorySettings;