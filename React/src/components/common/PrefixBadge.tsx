import React, { useMemo } from 'react';

import { parsePrefixColors, parsePrefixLabel } from '@utils/prefixUtils';
import { useTranslation } from "react-i18next";

interface PrefixBadgeProps {
    prefix?: string;
    username: string;
    isPlayerContext: boolean;
    size?: 'sm' | 'md' | 'lg';
    className?: string;
}

const PrefixBadge: React.FC<PrefixBadgeProps> = ({
                                                     prefix = '',
                                                     username,
                                                     isPlayerContext,
                                                     size = 'md',
                                                     className = ''
                                                 }) => {
    const { t } = useTranslation();

    const isConsole = username.toLowerCase() === 'console';
    const prefixColors = parsePrefixColors(prefix);
    const prefixLabel = parsePrefixLabel(prefix);

    let badgeText = isConsole ? t("history.badges.short.console").toUpperCase() : prefixLabel.toUpperCase();
    if (badgeText === "") {
        badgeText = t("history.badges.short.player").toUpperCase();
    }

    const hasPrefixColors = prefixColors && prefixColors.length > 0;

    const textGradientStyle = useMemo(() => {
        if (hasPrefixColors) {
            if (prefixColors.length === 1) {
                return {
                    color: prefixColors[0],
                    display: 'inline-block'
                };
            }
            const gradient = `linear-gradient(135deg, ${prefixColors.join(', ')})`;
            return {
                backgroundImage: gradient,
                WebkitBackgroundClip: 'text',
                backgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                display: 'inline-block'
            };
        }
        return {};
    }, [prefixColors, hasPrefixColors]);

    const containerStyle = useMemo(() => {
        if (hasPrefixColors) {
            const transparentColors = prefixColors.map(color =>
                color.startsWith('#') ? `${color}40` : color
            );

            const bgGradient = transparentColors.length === 1
                ? transparentColors[0]
                : `linear-gradient(135deg, ${transparentColors.join(', ')})`;

            return {
                background: bgGradient,
                border: `1px solid ${prefixColors[0]}66`,
            };
        }
        return {};
    }, [prefixColors, hasPrefixColors]);

    const fallbackBadgeClasses = isConsole
        ? 'bg-gray-500/10 text-gray-500 border border-gray-500'
        : isPlayerContext
            ? 'bg-blue-500/20 text-blue-500 border border-blue-500/40'
            : 'bg-purple-500/20 text-purple-500 border border-purple-500/40';

    const sizeClasses = {
        sm: 'px-2 py-0.5 text-[8px] tracking-[0.15em]',
        md: 'px-3 py-1 text-[10px] tracking-[0.2em]',
        lg: 'px-4 py-1.5 text-xs tracking-[0.25em]'
    };

    return (
        <div
            className={`inline-block rounded-md font-black ${sizeClasses[size]} ${!hasPrefixColors ? fallbackBadgeClasses : ''} ${className}`}
            style={hasPrefixColors ? containerStyle : {}}
        >
            <span style={hasPrefixColors ? textGradientStyle : {}}>
                {badgeText}
            </span>
        </div>
    );
};

export default PrefixBadge;