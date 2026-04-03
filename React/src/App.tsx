import React from 'react';
import { Routes, Route } from 'react-router-dom';
import '@styles/App.css'

import LoginPage from '@pages/auth/LoginPage';
import RegisterPage from '@pages/auth/RegisterPage';
import VerifyPage from '@pages/auth/VerifyPage';

import PunishmentsPage from '@pages/PunishmentsPage';
import HistoryPage from '@pages/HistoryPage';
import PunishmentDetailsPage from '@pages/PunishmentDetailsPage';
import AdminPanelPage from '@pages/AdminPanelPage';
import LogoutRoute from '@pages/auth/LogoutRoute';
import Error404 from '@pages/errors/Error404';

import ProtectedRoute from '@components/common/auth/ProtectedRoute';
import PublicRoute from '@components/common/auth/PublicRoute';
import { AuthProvider } from '@components/common/auth/AuthProvider';
import { ThemeProvider } from '@components/common/ThemeContext';

const App: React.FC = () => {
  
  return (
    <ThemeProvider>
      <AuthProvider>
          <Routes>
              <Route element={<PublicRoute />}>
                  <Route path="/login" element={<LoginPage />} />
                  <Route path="/register" element={<RegisterPage />} />
              </Route>

              <Route path="/logout" element={<LogoutRoute />} />

              <Route element={<ProtectedRoute />}>
                  <Route path="/verify" element={<VerifyPage />} />
              </Route>

              <Route element={<ProtectedRoute />}>
                  <Route path="/" element={<PunishmentsPage />} />
                  <Route path="/player/:playerName" element={<HistoryPage />} />
                  <Route path="/moderator/:moderatorName" element={<HistoryPage />} />
                  <Route path="/punishment/:id" element={<PunishmentDetailsPage />} />
                  <Route path="/admin" element={<AdminPanelPage />} />
              </Route>

              <Route path="*" element={<Error404 />} />
          </Routes>
      </AuthProvider>
    </ThemeProvider>
  );
};

export default App;