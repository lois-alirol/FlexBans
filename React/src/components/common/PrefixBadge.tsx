import React, { useMemo } from 'react';
import { parsePrefixColors, parsePrefixLabel } from '../../utils/prefixUtils';

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
    const isConsole = username.toLowerCase() === 'console';
    const prefixColors = parsePrefixColors(prefix);
    const prefixLabel = parsePrefixLabel(prefix);

    let badgeText = isConsole ? 'SYSTEM' : prefixLabel.toUpperCase();
    if (badgeText === "") {
        badgeText = "PLAYER";
    }

    const hasPrefixColors = prefixColors && prefixColors.length > 0;

    // Logic for the clipped text gradient
    const textGradientStyle = useMemo(() => {
        if (hasPrefixColors) {
            const gradient = prefixColors.length === 1
                ? prefixColors[0]
                : `linear-gradient(135deg, ${prefixColors.join(', ')})`;

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

    // Logic for the semi-transparent background container
    const containerStyle = useMemo(() => {
        if (hasPrefixColors) {
            // Using '40' for 25% opacity - much clearer than the previous 10%
            const transparentColors = prefixColors.map(color =>
                color.startsWith('#') ? `${color}40` : color
            );

            const bgGradient = transparentColors.length === 1
                ? transparentColors[0]
                : `linear-gradient(135deg, ${transparentColors.join(', ')})`;

            return {
                background: bgGradient,
                // Using '66' for 40% opacity on the border to define the shape
                border: `1px solid ${prefixColors[0]}66`,
            };
        }
        return {};
    }, [prefixColors, hasPrefixColors]);

    const fallbackBadgeClasses = isPlayerContext
        ? 'bg-blue-500/20 text-blue-500 border border-blue-500/40'
        : (isConsole ? 'bg-gray-500/20 text-gray-500 border border-gray-500/40' : 'bg-purple-500/20 text-purple-500 border border-purple-500/40');

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