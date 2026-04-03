import React from 'react';

import ErrorPageLayout from '@components/errors/ErrorPageLayout';
import {useTranslation} from "react-i18next";

interface Error500Props {
    stackTrace: string;
}

const Error500: React.FC<Error500Props> = ({ stackTrace }) => {
    const { t } = useTranslation();

    const stackTraceElement = (
        <div className="bg-modal-surface-elevated text-left p-4 rounded-md overflow-auto max-h-60 text-s">
            <pre className="text-text-primary">
                <code>{ stackTrace }</code>
            </pre>
        </div>
    );

    return (
        <ErrorPageLayout
            statusCode={500}
            title={t("errors.500.title")}
            description={t("errors.500.description")}
            details={stackTraceElement}
        />
    );
};

export default Error500;