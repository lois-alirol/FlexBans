import { FaDiscord, FaGithub, FaXTwitter } from 'react-icons/fa6';
import { FcGoogle } from "react-icons/fc";

import type { ReactNode } from 'react';

export interface StatusMessage {
  text: string;
  type: 'success' | 'error' | 'hidden';
}

export interface Provider {
  service: 'Discord' | 'Google' | 'GitHub' | 'X';
  icon: ReactNode;
  color: string;
  hoverColor: string;
  enabled: boolean;
}

export const allProviders: Provider[] = [
  { service: 'Discord', icon: <FaDiscord/>, color: '#5865F2', hoverColor: '#4752C4', enabled: true },
  { service: 'Google', icon: <FcGoogle />, color: '#ffffff', hoverColor: '#EEEEEE', enabled: true },
  { service: 'GitHub', icon: <FaGithub />, color: '#333333', hoverColor: '#1c1c1c', enabled: true },
  { service: 'X', icon: <FaXTwitter />, color: '#000000', hoverColor: '#111111', enabled: true },
];