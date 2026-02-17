import React from 'react';
import { FaImage, FaLink, FaAlignLeft } from 'react-icons/fa';
import type {BrandingForm, Theme} from "../../types/admin.tsx";

interface Props {
    branding: BrandingForm;
    theme: Theme;
    accent: string;
    onChange: (key: keyof BrandingForm, value: string) => void;
}

const BrandingMeta: React.FC<Props> = ({ branding, theme, onChange }) => (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="space-y-4">
            <div>
                <label className="text-sm font-semibold flex items-center gap-2">Server Name</label>
                <input
                    type="text"
                    value={branding.serverName}
                    onChange={(e) => onChange('serverName', e.target.value)}
                    placeholder="My Server"
                    className={`mt-2 w-full p-3 rounded-xl border ${theme === 'dark' ? 'bg-[#2c2c2c] border-gray-700 text-white' : 'bg-white border-gray-200 text-gray-800'}`}
                />
            </div>

            <div className="space-y-2">
                <label className="text-sm font-semibold flex items-center gap-2">Primary Color</label>
                <div className="flex items-center gap-3">
                    <label className="relative inline-block">
                        <div
                            className="w-12 h-12 rounded-lg border shadow-sm"
                            style={{
                                borderColor: theme === 'dark' ? '#333' : '#e5e7eb',
                                backgroundColor: branding.primaryColor,
                            }}
                        />
                        <input
                            type="color"
                            value={branding.primaryColor}
                            onChange={(e) => onChange('primaryColor', e.target.value)}
                            onInput={(e) => onChange('primaryColor', (e.target as HTMLInputElement).value)}
                            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                            aria-label="Pick primary color"
                        />
                    </label>
                    <input
                        type="text"
                        value={branding.primaryColor}
                        onChange={(e) => onChange('primaryColor', e.target.value)}
                        className={`w-full p-3 rounded-xl border ${theme === 'dark' ? 'bg-[#2c2c2c] border-gray-700 text-white' : 'bg-white border-gray-200 text-gray-800'}`}
                    />
                </div>
            </div>

            <div className="space-y-2">
                <label className="text-sm font-semibold flex items-center gap-2">Secondary Color</label>
                <div className="flex items-center gap-3">
                    <label className="relative inline-block">
                        <div
                            className="w-12 h-12 rounded-lg border shadow-sm"
                            style={{
                                borderColor: theme === 'dark' ? '#333' : '#e5e7eb',
                                backgroundColor: branding.secondaryColor,
                            }}
                        />
                        <input
                            type="color"
                            value={branding.secondaryColor}
                            onChange={(e) => onChange('secondaryColor', e.target.value)}
                            onInput={(e) => onChange('secondaryColor', (e.target as HTMLInputElement).value)}
                            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
                            aria-label="Pick secondary color"
                        />
                    </label>
                    <input
                        type="text"
                        value={branding.secondaryColor}
                        onChange={(e) => onChange('secondaryColor', e.target.value)}
                        className={`w-full p-3 rounded-xl border ${theme === 'dark' ? 'bg-[#2c2c2c] border-gray-700 text-white' : 'bg-white border-gray-200 text-gray-800'}`}
                    />
                </div>
            </div>
        </div>

        <div className="space-y-4">
            <div>
                <label className="text-sm font-semibold flex items-center gap-2">
                    <FaImage /> Logo URL
                </label>
                <input
                    type="text"
                    value={branding.logo}
                    onChange={(e) => onChange('logo', e.target.value)}
                    placeholder="https://example.com/logo.png"
                    className={`mt-2 w-full p-3 rounded-xl border ${theme === 'dark' ? 'bg-[#2c2c2c] border-gray-700 text-white' : 'bg-white border-gray-200 text-gray-800'}`}
                />
            </div>
            <div>
                <label className="text-sm font-semibold flex items-center gap-2">
                    <FaLink /> Favicon URL
                </label>
                <input
                    type="text"
                    value={branding.favicon}
                    onChange={(e) => onChange('favicon', e.target.value)}
                    placeholder="https://example.com/favicon.ico"
                    className={`mt-2 w-full p-3 rounded-xl border ${theme === 'dark' ? 'bg-[#2c2c2c] border-gray-700 text-white' : 'bg-white border-gray-200 text-gray-800'}`}
                />
            </div>
            <div>
                <label className="text-sm font-semibold flex items-center gap-2">
                    <FaAlignLeft /> Description
                </label>
                <textarea
                    value={branding.description}
                    onChange={(e) => onChange('description', e.target.value)}
                    placeholder="Short description for the panel..."
                    rows={3}
                    className={`mt-2 w-full p-3 rounded-xl border resize-none ${theme === 'dark' ? 'bg-[#2c2c2c] border-gray-700 text-white' : 'bg-white border-gray-200 text-gray-800'}`}
                />
            </div>
        </div>
    </div>
);

export default BrandingMeta;