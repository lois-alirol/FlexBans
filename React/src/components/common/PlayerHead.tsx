import React, { useState } from 'react';
import {usePlayerHead} from "../../hooks/usePlayerHead.ts";

interface PlayerHeadProps {
    username: string;
    size?: number;
}

const PlayerHead: React.FC<PlayerHeadProps> = ({ username, size = 28 }) => {
    const { headUrl } = usePlayerHead(username);
    const [hasError, setHasError] = useState(false);

    const fallbackUrl = `https://crafatar.com/avatars/steve?size=${size}`;

    return (
        <div
            className="inline-flex items-center justify-center overflow-hidden rounded shadow-sm bg-black/20"
            style={{ width: size, height: size }}
        >
            <img
                src={hasError ? fallbackUrl : headUrl}
                alt={`${username}'s head`}
                className="w-full h-full object-cover"
                onError={() => setHasError(true)}
            />
        </div>
    );
};

export default PlayerHead;