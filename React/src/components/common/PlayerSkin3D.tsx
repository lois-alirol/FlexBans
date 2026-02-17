import React, { useEffect, useRef } from 'react';
import { usePlayerSkin } from "../../hooks/usePlayerSkin.ts";
import { SkinViewer } from 'skinview3d';

type Theme = 'light' | 'dark';

interface PlayerSkin3DProps {
    username: string;
    width?: number;
    height?: number;
    theme?: Theme;
}

const PlayerSkin3D: React.FC<PlayerSkin3DProps> = ({
                                                       username,
                                                       width = 360,
                                                       height = 220,
                                                       theme = 'light',
                                                   }) => {
    const canvasRef = useRef<HTMLCanvasElement | null>(null);
    const viewerRef = useRef<SkinViewer | null>(null);
    const { skinUrl } = usePlayerSkin(username);

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;

        const viewer = new SkinViewer({
            canvas,
            width,
            height,
            background: theme === 'dark' ? '#242424' : '#ffffff',
        });
        viewerRef.current = viewer;

        if (skinUrl) {
            viewer.loadSkin(skinUrl).catch(() => {});
        }

        viewer.zoom = 0.9;

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
            viewer.zoom = Math.max(0.4, Math.min(2.0, viewer.zoom + delta * 0.05));
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

            viewer.dispose();
            viewerRef.current = null;
        };
    }, [skinUrl, width, height, theme]);

    return (
        <div className={`rounded-xl overflow-hidden border ${theme === 'dark' ? 'border-gray-700' : 'border-gray-200'}`}>
            <canvas
                ref={canvasRef}
                width={width}
                height={height}
                style={{ display: 'block', width, height }}
            />
            <div className="px-3 py-2 text-xs opacity-70 flex items-center justify-between">
                <span>Drag to rotate • Scroll to zoom</span>
            </div>
        </div>
    );
};

export default PlayerSkin3D;