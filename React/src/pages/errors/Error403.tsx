import React from 'react';

import ErrorPageLayout from '@components/errors/ErrorPageLayout';
import {useTranslation} from "react-i18next";

const Error403: React.FC = () => {
    const { t } = useTranslation();

    return (
        <ErrorPageLayout
            statusCode={403}
            title={t("errors.403.title")}
            description={t("errors.403.description")}
        />
    );
};

export default Error403;