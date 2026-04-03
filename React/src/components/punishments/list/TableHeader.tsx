import React from 'react';
import {useTranslation} from "react-i18next";

interface TableHeaderProps {
    showExtendedInfo: boolean;
}

const TableHeader: React.FC<TableHeaderProps> = ({ showExtendedInfo }) => {
    const { t } = useTranslation();

    const headerClasses = 'bg-surface text-text-secondary';
    const thClasses = "px-4 py-5 text-center text-[10px] font-bold uppercase tracking-[0.15em] whitespace-nowrap";

    return (
        <thead className={headerClasses}>
        <tr>
            <th className={thClasses}>{t("home.table-header.id")}</th>
            <th className={thClasses}>{t("home.table-header.player")}</th>
            <th className={thClasses}>{t("home.table-header.moderator")}</th>
            <th className={thClasses}>{t("home.table-header.reason")}</th>
            <th className={thClasses}>{t("home.table-header.date")}</th>
            {showExtendedInfo && (
                <>
                    <th className={thClasses}>{t("home.table-header.duration")}</th>
                    <th className={`${thClasses} pr-6`}>{t("home.table-header.status")}</th>
                </>
            )}
        </tr>
        </thead>
    );
};

export default TableHeader;