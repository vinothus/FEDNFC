import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import ProtectedRoute from './components/auth/ProtectedRoute';
import MainLayout from './components/MainLayout';
import Dashboard from './pages/Dashboard';
import Invoices from './pages/Invoices';
import Upload from './pages/Upload';
import PatternManagement from './pages/PatternManagement';
import UserManagement from './pages/UserManagement';
import Settings from './pages/Settings';
import Login from './pages/Login';
import './App.css';

function App() {
  return (
    <AuthProvider>
      <Router basename="/invoice-automation">
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<Login />} />
          
          {/* Protected routes */}
          <Route path="/" element={
            <ProtectedRoute>
              <MainLayout>
                <Dashboard />
              </MainLayout>
            </ProtectedRoute>
          } />
          
          <Route path="/dashboard" element={
            <ProtectedRoute>
              <MainLayout>
                <Dashboard />
              </MainLayout>
            </ProtectedRoute>
          } />
          
          <Route path="/invoices" element={
            <ProtectedRoute>
              <MainLayout>
                <Invoices />
              </MainLayout>
            </ProtectedRoute>
          } />
          
          <Route path="/upload" element={
            <ProtectedRoute>
              <MainLayout>
                <Upload />
              </MainLayout>
            </ProtectedRoute>
          } />
          
          <Route path="/patterns" element={
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <MainLayout>
                <PatternManagement />
              </MainLayout>
            </ProtectedRoute>
          } />
          
          <Route path="/users" element={
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <MainLayout>
                <UserManagement />
              </MainLayout>
            </ProtectedRoute>
          } />
          
          <Route path="/settings" element={
            <ProtectedRoute requiredRoles={['ADMIN']}>
              <MainLayout>
                <Settings />
              </MainLayout>
            </ProtectedRoute>
          } />
          
          {/* Fallback route - redirect any unmatched paths to dashboard */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
