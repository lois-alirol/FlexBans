import React, { type ReactNode } from 'react';
import { BiSolidErrorAlt } from "react-icons/bi";

import {useTitle} from "@hooks/useTitle.ts";

import BackButton from '@components/common/BackButton';

interface ErrorPageLayoutProps {
    statusCode: number;
    title: string;
    description: string;
    details?: ReactNode;
}

const ErrorPageLayout: React.FC<ErrorPageLayoutProps> = ({
                                                             statusCode,
                                                             title,
                                                             description,
                                                             details
                                                         }) => {
    useTitle(title);

    return (
        <div className={`min-h-screen bg-background text-text-primary flex items-center justify-center p-6 relative overflow-hidden`}>
            <div className="bg-surface p-12 rounded-3xl text-center border border-surface-border max-w-md w-full relative overflow-hidden">
                <div
                    className="bg-server-color absolute top-0 left-0 w-full h-1.5 opacity-50"
                />

                <div className="flex justify-center mb-6">
                    <div
                        className="bg-server-color/30 text-server-color p-5 rounded-2xl flex items-center justify-center transition-transform hover:scale-110 duration-500"
                    >
                        <BiSolidErrorAlt size={42}/>
                    </div>
                </div>

                <div className="mb-2">
                    <span className="text-xs font-bold uppercase tracking-[0.2em] opacity-30">
                        Error {statusCode}
                    </span>
                    <h2 className="text-2xl font-black text-text-primary mt-1">{title}</h2>
                </div>

                <p className="opacity-60 text-sm leading-relaxed mb-8">
                    {description}
                </p>

                {details && (
                    <div className="mb-8 p-4 bg-surface-elevated rounded-xl text-left text-[11px] font-mono border border-surface-border text-text-secondary break-all leading-normal">
                        <span className="text-text-secondary/40 block mb-1 uppercase tracking-tighter">More Information:</span>
                        {details}
                    </div>
                )}

                <div className="flex justify-center pt-2">
                    <BackButton />
                </div>
            </div>

            <div className="bg-server-color absolute w-[500px] h-[500px] rounded-full blur-[120px] opacity-[0.03] -z-10"/>
        </div>
    );
};

export default ErrorPageLayout;