export interface ServerConfig {
  serverName: string;
  serverDescription: string;
  serverFavicon: string;
  serverLogo: string;
  serverColor: string;
  serverColorHover: string;
  isSecured: boolean,
  punishments: {
    bans: {
        enabled: boolean;
        maxPerPage: number;
    };
    mutes: {
        enabled: boolean;
        maxPerPage: number;
    };
    warnings: {
        enabled: boolean;
        maxPerPage: number;
    };
    kicks: {
        enabled: boolean;
        maxPerPage: number;
    };
  }
  histories: {
    playerMaxPerPage: number,
    moderatorMaxPerPage: number,
  }
  oauth: {
    discord: boolean;
    google: boolean;
    github: boolean;
    x: boolean;
  }
}