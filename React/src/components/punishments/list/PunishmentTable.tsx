import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import type { Punishment, PunishmentType } from '@/types/punishments';

import { useAuth } from '@hooks/useAuth';
import { usePunishmentRevocation } from '@hooks/usePunishmentRevocation';

import TableRow from '@components/punishments/list/TableRow';
import TableHeader from '@components/punishments/list/TableHeader';
import PunishmentContextMenu from '@components/punishments/list/PunishmentContextMenu';
import EmptyState from '@components/punishments/list/EmptyState';
import RevokeModal from '@components/common/modals/RevokeModal';
import EditionModal from '@components/common/modals/EditionModal';

interface PunishmentTableProps {
  punishments: Punishment[];
  activeType: PunishmentType | null;
  onRevokeSuccess: () => void;
  onEditSuccess: () => void;
}

const PunishmentTable: React.FC<PunishmentTableProps> = ({
                                                           punishments,
                                                           activeType,
                                                           onRevokeSuccess,
                                                           onEditSuccess,
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

  const containerClasses = 'bg-table-row-background border-surface-border';
  const dividerClasses = 'divide-surface-border';

  const punishmentTypeDisplay = activeType ? `${activeType}S` : '';
  const showExtendedInfo = activeType !== 'KICK';

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

  const closeMenu = () => setMenuState(prev => ({ ...prev, visible: false }));

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
            punishmentTypeDisplay={punishmentTypeDisplay}
        />
    );
  }

  return (
      <>
        <div className={`rounded-2xl border backdrop-blur-xl overflow-hidden transition-all duration-300 ${containerClasses}`}>
          <div className="overflow-x-auto">
            <table className={`w-full table-fixed divide-y ${dividerClasses} min-w-[1000px]`}>
              <colgroup>
                <col className="w-[60px]" />
                <col className="w-[15%]" />
                <col className="w-[15%]" />
                <col className="w-[25%]" />
                <col className="w-[120px]" />
                {showExtendedInfo && (
                    <>
                      <col className="w-[120px]" />
                      <col className="w-[130px]" />
                    </>
                )}
              </colgroup>

              <TableHeader showExtendedInfo={showExtendedInfo} />

              <tbody className={`divide-y ${dividerClasses}`}>
              {punishments.map(p => (
                  <TableRow
                      key={p.punishment_id}
                      punishment={p}
                      showExtendedInfo={showExtendedInfo}
                      onRowClick={() => navigate(`/punishment/${p.punishment_id}`)}
                      onContextMenu={handleContextMenu}
                  />
              ))}
              </tbody>
            </table>
          </div>
        </div>

        {menuState.visible && menuState.punishment && (
            <PunishmentContextMenu
                position={{x: menuState.x, y: menuState.y}}
                side={menuState.side}
                punishment={menuState.punishment}
                onClose={closeMenu}
                onEdit={handleEditClick}
                onRevoke={handleRevokeClick}
                isRevoking={isRevoking}
            />
        )}

        {revokeModalState.isOpen && menuState.punishment && (
            <RevokeModal
                isOpen={revokeModalState.isOpen}
                onClose={() => setRevokeModalState({ isOpen: false, punishment: null })}
                removerName={user?.username ?? 'Unknown'}
                punishmentId={menuState.punishment.database_id}
                punishmentType={menuState.punishment.type}
                onSuccess={() => {
                  setRevokeModalState({ isOpen: false, punishment: null });
                  onRevokeSuccess();
                }}
            />
        )}

        {editionModalState.isOpen && menuState.punishment && (
            <EditionModal
                isOpen={editionModalState.isOpen}
                onClose={() => setEditionModalState({ isOpen: false, punishment: null })}
                punishmentId={menuState.punishment.database_id}
                initialExpiry={menuState.punishment.expiration_date}
                punishmentType={menuState.punishment.type}
                onSuccess={() => {
                  setEditionModalState({ isOpen: false, punishment: null });
                  onEditSuccess();
                }}
            />
        )}
      </>
  );
};

export default PunishmentTable;