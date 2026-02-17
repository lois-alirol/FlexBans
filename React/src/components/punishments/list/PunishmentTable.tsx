import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import TableRow from './TableRow';
import TableHeader from './TableHeader';
import PunishmentContextMenu from './PunishmentContextMenu';
import EmptyState from './EmptyState';
import type {Punishment, PunishmentType} from "../../../types/punishments.tsx";
import { useAuth } from "../../../hooks/useAuth.ts";
import { usePunishmentRevocation } from "../../../hooks/usePunishmentRevocation.ts";
import RevokeModal from "../../common/modals/RevokeModal.tsx";
import EditionModal from "../../common/modals/EditionModal.tsx";

interface PunishmentTableProps {
  punishments: Punishment[];
  activeType: PunishmentType | null;
  serverColor: string;
  currentTheme: 'dark' | 'light';
}

const PunishmentTable: React.FC<PunishmentTableProps> = ({
                                                           punishments,
                                                           activeType,
                                                           currentTheme,
                                                         }) => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { isRevoking } = usePunishmentRevocation();

  const [menuState, setMenuState] = useState<{
    visible: boolean;
    x: number;
    y: number;
    side: 'top' | 'bottom';
    punishment: Punishment | null;
  }>({ visible: false, x: 0, y: 0, side: 'top', punishment: null });

  const [revokeModalState, setRevokeModalState] = useState<{
    isOpen: boolean;
    punishment: Punishment | null;
  }>({ isOpen: false, punishment: null });

  const [editionModalState, setEditionModalState] = useState<{
    isOpen: boolean;
    punishment: Punishment | null;
  }>({ isOpen: false, punishment: null });

  const tableClasses = currentTheme === 'dark' ? 'bg-[#2c2c2c] border-gray-700' : 'bg-white border-gray-200';
  const dividerClasses = currentTheme === 'dark' ? 'divide-gray-700' : 'divide-gray-200';

  const punishmentTypeDisplay = activeType ? `${activeType}S` : 'Punishments';
  const showExtendedInfo = punishments.some(p => p.type.toUpperCase() !== 'KICK');

  const handleContextMenu = (e: React.MouseEvent, punishment: Punishment) => {
    e.preventDefault();

    const rect = e.currentTarget.getBoundingClientRect();
    const centerY = window.scrollY + rect.top + (rect.height / 2);
    const isBottomHalf = rect.top > window.innerHeight / 2;

    setMenuState({
      visible: true,
      x: e.pageX,
      y: centerY,
      side: isBottomHalf ? 'bottom' : 'top',
      punishment,
    });
  };

  const closeMenu = () => {
    setMenuState(prev => ({ ...prev, visible: false }));
  };

  const handleEditClick = () => {
    if (menuState.punishment) {
      setEditionModalState({ isOpen: true, punishment: menuState.punishment });
    }
  };

  const handleRevokeClick = () => {
    if (menuState.punishment) {
      setRevokeModalState({ isOpen: true, punishment: menuState.punishment });
    }
  };

  if (punishments.length === 0) {
    return (
        <EmptyState
            type="no-punishments"
            currentTheme={currentTheme}
            punishmentTypeDisplay={punishmentTypeDisplay}
        />
    );
  }

  return (
      <>
        <div className={`rounded-xl shadow-xl border ${tableClasses}`}>
          <table className={`w-full table-fixed divide-y ${dividerClasses}`}>
            <colgroup>
              <col className="w-[60px]" />
              <col className="w-[16%]" />
              <col className="hidden sm:table-column w-[17%]" />
              <col className="w-[20%]" />
              <col className="w-[18%]" />
              {showExtendedInfo ? (
                  <>
                    <col className="hidden md:table-column w-[13%]" />
                    <col className="hidden md:table-column w-[13%]" />
                  </>
              ) : (
                  <col className="hidden md:table-column w-0" />
              )}
            </colgroup>

            <TableHeader showExtendedInfo={showExtendedInfo} currentTheme={currentTheme} />

            <tbody className={`divide-y ${dividerClasses}`}>
            {punishments.map(p => (
                <TableRow
                    key={p.punishment_id}
                    punishment={p}
                    currentTheme={currentTheme}
                    showExtendedInfo={showExtendedInfo}
                    onRowClick={() => navigate(`/punishment/${p.punishment_id}`)}
                    onContextMenu={handleContextMenu}
                />
            ))}
            </tbody>
          </table>
        </div>

        {menuState.visible && menuState.punishment && (
            <PunishmentContextMenu
                position={{ x: menuState.x, y: menuState.y }}
                side={menuState.side}
                punishment={menuState.punishment}
                onClose={closeMenu}
                onEdit={handleEditClick}
                onRevoke={handleRevokeClick}
                currentTheme={currentTheme}
                isRevoking={isRevoking}
            />
        )}

        {menuState.punishment?.punishment_id &&
            <RevokeModal
                isOpen={revokeModalState.isOpen}
                onClose={() => setRevokeModalState({isOpen: false, punishment: null})}
                removerName={user?.username ?? 'Unknown'}
                currentTheme={currentTheme}
                punishmentId={menuState.punishment?.database_id}
                punishmentType={menuState.punishment.type}
            />
        }

        {menuState.punishment?.punishment_id &&
            <EditionModal
                isOpen={editionModalState.isOpen}
                onClose={() => setEditionModalState({isOpen: false, punishment: null})}
                currentTheme={currentTheme}
                punishmentId={menuState.punishment?.database_id}
                initialExpiry={menuState.punishment?.expiration_date}
                punishmentType={menuState.punishment?.type}
            />
        }
      </>
  );
};

export default PunishmentTable;