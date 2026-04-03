import React from 'react';
import { useTranslation } from 'react-i18next';

interface Props {
    type:
        | 'checking-access'
        | 'loading'
        | 'error'
        | 'config-error'
        | 'no-punishments';
    errorMessage?: string;
    punishmentTypeDisplay?: string;
}

const EmptyState: React.FC<Props> = ({
                                         type,
                                         errorMessage,
                                         punishmentTypeDisplay
                                     }) => {
    const { t } = useTranslation();

    const titleKey = `empty-state.${type}.title`;
    const descriptionKey = `empty-state.${type}.description`;

    const baseClasses =
        "flex flex-col items-center justify-center text-center p-8 rounded-2xl bg-surface border border-surface-border";

    const titleClasses =
        "text-lg sm:text-xl font-semibold text-text-primary mb-2";

    const bodyClasses =
        "text-sm sm:text-base text-text-secondary max-w-md";

    const renderSpinner = () => (
        <div
            className="animate-spin rounded-full h-8 w-8 border-b-2 mt-4"
            style={{ borderColor: "var(--accent)" }}
        />
    );

    const description =
        type === "error"
            ? errorMessage || t(descriptionKey)
            : t(descriptionKey, {
                type: punishmentTypeDisplay
                    ? punishmentTypeDisplay.toLowerCase()
                    : "punishments"
            });

    const showSpinner = type === "checking-access" || type === "loading";

    const titleClass =
        type === "error"
            ? `${titleClasses} text-red-500 opacity-100`
            : titleClasses;

    const Content = (
        <>
            <h3 className={titleClass}>{t(titleKey)}</h3>
            <p className={bodyClasses}>{description}</p>
            {showSpinner && renderSpinner()}
        </>
    );

    if (type === "checking-access") {
        return (
            <div
                className="fixed inset-0 flex items-center justify-center
                           backdrop-blur-md z-200 animate-in fade-in duration-500"
            >
                <div className={`${baseClasses} scale-110`}>{Content}</div>
            </div>
        );
    }

    return (
        <div className="py-20 flex justify-center items-center w-full animate-in fade-in zoom-in-95 duration-700">
            <div className={baseClasses}>{Content}</div>
        </div>
    );
};

export default EmptyState;