import React from 'react';
import Header from './Header';
import Footer from './Footer';

export interface MainLayoutProps {
  children: React.ReactNode;
  className?: string;
}

/**
 * Main layout component for authenticated pages
 * Provides consistent structure with Header, main content area, and Footer
 * Ensures Montra branding appears on all pages with proper spacing
 */
const MainLayout: React.FC<MainLayoutProps> = ({ children, className = '' }) => {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Header />
      
      <main className={`flex-1 container mx-auto px-4 py-8 pb-0 ${className}`}>
        <div className="min-h-full">
          {children}
        </div>
      </main>
      
      <Footer />
    </div>
  );
};

export default MainLayout;
