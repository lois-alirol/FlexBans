import React from 'react';
import ErrorPageLayout from '../../components/errors/ErrorPageLayout';

const Error404: React.FC = () => {
    return (
        <ErrorPageLayout
            statusCode={404}
            title="Not Found"
            description="Oops! The page you’re looking for doesn’t exist."
            actionText="Return to Homepage"
            iconClass="fa-solid fa-arrow-left"
        />
    );
};

export default Error404;