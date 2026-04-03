import React, { useEffect, useRef, useState } from 'react';
import { SkinViewer } from 'skinview3d';

import { usePlayerSkin } from '@hooks/usePlayerSkin';
import {useTranslation} from "react-i18next";

interface PlayerSkin3DProps {
    username: string;
    width?: number;
    height?: number;
}

const PlayerSkin3D: React.FC<PlayerSkin3DProps> = ({
                                                       username,
                                                       width = 360,
                                                       height = 220,
                                                   }) => {
    const canvasRef = useRef<HTMLCanvasElement | null>(null);
    const viewerRef = useRef<SkinViewer | null>(null);
    const { skinUrl } = usePlayerSkin(username);
    const [contextLost, setContextLost] = useState(false);

    const { t } = useTranslation();

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;

        const handleContextLost = (e: Event) => {
            e.preventDefault();
            console.log('WebGL context lost');
            setContextLost(true);

            if (viewerRef.current) {
                try {
                    viewerRef.current.dispose();
                } catch (err) {
                }
                viewerRef.current = null;
            }
        };

        const handleContextRestored = () => {
            console.log('WebGL context restored');
            setContextLost(false);
        };

        canvas.addEventListener('webglcontextlost', handleContextLost);
        canvas.addEventListener('webglcontextrestored', handleContextRestored);

        let viewer: SkinViewer | null = null;
        try {
            viewer = new SkinViewer({
                canvas,
                width,
                height,
                background: getComputedStyle(document.documentElement)
                    .getPropertyValue('--surface-elevated')
                    .trim(),
            });
            viewerRef.current = viewer;

            if (skinUrl) {
                viewer.loadSkin(skinUrl).catch(() => {});
            }

            viewer.zoom = 0.9;
        } catch (error) {
            console.error('Failed to create SkinViewer:', error);
            setContextLost(true);

            canvas.removeEventListener('webglcontextlost', handleContextLost);
            canvas.removeEventListener('webglcontextrestored', handleContextRestored);
            return;
        }

        const v: any = viewer as any;
        const playerObject: any = v.playerObject ?? v.player;
        const camera: any = v.camera;

        let dragging = false;
        let lastX = 0;
        let lastY = 0;

        const onMouseDown = (e: MouseEvent) => {
            dragging = true;
            lastX = e.clientX;
            lastY = e.clientY;
            canvas.style.cursor = 'grabbing';
        };
        const onMouseUp = () => {
            dragging = false;
            canvas.style.cursor = 'grab';
        };
        const onMouseLeave = () => {
            dragging = false;
            canvas.style.cursor = 'grab';
        };
        const onMouseMove = (e: MouseEvent) => {
            if (!dragging) return;
            const dx = e.clientX - lastX;
            const dy = e.clientY - lastY;
            lastX = e.clientX;
            lastY = e.clientY;

            if (playerObject?.rotation) {
                playerObject.rotation.y += dx * 0.01;
            }
            if (camera?.rotation) {
                camera.rotation.x = Math.max(-1.2, Math.min(1.2, camera.rotation.x + dy * 0.005));
            }
        };

        const onWheel = (e: WheelEvent) => {
            const delta = Math.sign(e.deltaY);
            if (viewer) {
                viewer.zoom = Math.max(0.4, Math.min(2.0, viewer.zoom + delta * 0.05));
            }
        };

        let touchDragging = false;
        let lastTouchX = 0;
        let lastTouchY = 0;

        const onTouchStart = (e: TouchEvent) => {
            if (e.touches.length === 1) {
                touchDragging = true;
                lastTouchX = e.touches[0].clientX;
                lastTouchY = e.touches[0].clientY;
            }
        };
        const onTouchEnd = () => {
            touchDragging = false;
        };
        const onTouchMove = (e: TouchEvent) => {
            if (!touchDragging || e.touches.length !== 1) return;
            const t = e.touches[0];
            const dx = t.clientX - lastTouchX;
            const dy = t.clientY - lastTouchY;
            lastTouchX = t.clientX;
            lastTouchY = t.clientY;

            if (playerObject?.rotation) {
                playerObject.rotation.y += dx * 0.01;
            }
            if (camera?.rotation) {
                camera.rotation.x = Math.max(-1.2, Math.min(1.2, camera.rotation.x + dy * 0.005));
            }
        };

        canvas.addEventListener('mousedown', onMouseDown);
        window.addEventListener('mouseup', onMouseUp);
        canvas.addEventListener('mouseleave', onMouseLeave);
        canvas.addEventListener('mousemove', onMouseMove);
        canvas.addEventListener('wheel', onWheel, { passive: true });
        canvas.addEventListener('touchstart', onTouchStart, { passive: true });
        canvas.addEventListener('touchend', onTouchEnd);
        canvas.addEventListener('touchmove', onTouchMove, { passive: true });

        canvas.style.cursor = 'grab';

        return () => {
            canvas.removeEventListener('mousedown', onMouseDown);
            window.removeEventListener('mouseup', onMouseUp);
            canvas.removeEventListener('mouseleave', onMouseLeave);
            canvas.removeEventListener('mousemove', onMouseMove);
            canvas.removeEventListener('wheel', onWheel);
            canvas.removeEventListener('touchstart', onTouchStart);
            canvas.removeEventListener('touchend', onTouchEnd);
            canvas.removeEventListener('touchmove', onTouchMove);
            canvas.removeEventListener('webglcontextlost', handleContextLost);
            canvas.removeEventListener('webglcontextrestored', handleContextRestored);

            if (viewer) {
                try {
                    viewer.dispose();
                } catch (err) {
                }
            }
            viewerRef.current = null;
        };
    }, [skinUrl, width, height]);

    return (
        <div className={`rounded-xl overflow-hidden border border-surface-border`}>
            <canvas
                ref={canvasRef}
                width={width}
                height={height}
                style={{ display: 'block', width, height }}
            />
            <div className="px-3 py-2 text-xs opacity-70 flex items-center justify-between">
                <span>
                    {contextLost
                        ? t("history.skin-preview.error")
                        : t("history.skin-preview.controls")
                    }
                </span>
            </div>
        </div>
    );
};

export default PlayerSkin3D;