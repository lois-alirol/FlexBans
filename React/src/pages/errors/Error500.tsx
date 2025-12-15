import React from 'react';
import ErrorPageLayout from '../../components/errors/ErrorPageLayout';

interface Error500Props {
    stackTrace: string;
}

const Error500: React.FC<Error500Props> = ({ stackTrace }) => {
    const stackTraceElement = (
        <div className="bg-[#1a1a1a] text-left p-4 rounded-md overflow-auto max-h-60 text-s">
            <pre className="text-[#d1d5db]">
                <code>{stackTrace}</code>
            </pre>
        </div>
    );

    return (
        <ErrorPageLayout
            statusCode={500}
            title="Internal Error"
            description="Something went wrong on our end. Please try again later."
            actionText="Return to Homepage"
            iconClass="fa-solid fa-home"
            details={stackTraceElement}
        />
    );
};

export default Error500;