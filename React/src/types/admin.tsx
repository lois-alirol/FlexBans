export type Theme = 'dark' | 'light';

export interface BrandingForm {
    serverName: string;
    primaryColor: string;
    secondaryColor: string;
    logo: string;
    favicon: string;
    description: string;
}

export interface UserRow {
    name: string;
    permissions: string[];
    status: string;
}