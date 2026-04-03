import { useEffect } from "react";
import { useServerConfig } from "@hooks/useServerConfig";

export function useTitle(title: string, suffix: boolean = true) {
    const { serverConfig, isLoading } = useServerConfig();

    useEffect(() => {
        document.title = title + (suffix && !isLoading && serverConfig ? ' • ' + serverConfig.serverName : '')
    }, [title, serverConfig && serverConfig.serverName]);
}