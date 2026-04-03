import React from 'react';
import MyAccount from '@components/common/MyAccount';
import {Trans, useTranslation} from "react-i18next";

interface PageHeaderProps {
    serverName: string;
    typeDisplay: string;
    isAuthenticated: boolean;
    isSecured: boolean;
    onPreferencesApply: () => void;
}

const PageHeader: React.FC<PageHeaderProps> = ({
                                                   serverName,
                                                   typeDisplay,
                                                   isAuthenticated,
                                                   isSecured,
                                                   onPreferencesApply
                                               }) => {
    const { t } = useTranslation();

    return (
        <>
            <div className="flex justify-between items-start">
                <div>
                    <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-text-primary">
                        {t("home.title", {server_name: serverName})}
                    </h1>
                </div>

                {isSecured && isAuthenticated && (
                    <div className="mt-1">
                        <MyAccount onPreferencesApply={onPreferencesApply}/>
                    </div>
                )}
            </div>

            <p className="text-sm sm:text-base mb-6 text-text-secondary">
                <Trans
                    i18nKey="home.description"
                    values={{ type: typeDisplay }}
                    components={[
                        <span className="font-semibold text-[13px] text-text-primary" />
                    ]}
                />
            </p>
        </>
    );
};

export default PageHeader;