import React from 'react';
import ErrorPageLayout from '../../components/errors/ErrorPageLayout';

const Error403: React.FC = () => {
    return (
        <ErrorPageLayout
            statusCode={403}
            title="Forbidden"
            description="You don’t have permission to access this page."
            actionText="Return to Homepage"
            iconClass="fa-solid fa-home"
        />
    );
};

export default Error403;