import React from 'react';

import ErrorPageLayout from '@components/errors/ErrorPageLayout';
import {useTranslation} from "react-i18next";

const Error404: React.FC = () => {
    const { t } = useTranslation();

    return (
        <ErrorPageLayout
            statusCode={404}
            title={t("errors.404.title")}
            description={t("errors.404.description")}
        />
    );
};

export default Error404;