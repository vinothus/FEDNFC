import React from 'react';

/**
 * Footer component with Montra branding
 * Enterprise-grade compact footer for all main application pages
 */
const Footer: React.FC = () => {
  return (
    <footer className="bg-gray-800 border-t border-gray-700 mt-8">
      <div className="container mx-auto px-4 py-3">
        <div className="flex flex-col sm:flex-row justify-between items-center text-xs text-gray-400 space-y-1 sm:space-y-0">
          {/* Copyright */}
          <span>
            © {new Date().getFullYear()} Invoice Automation System. All rights reserved.
          </span>
          
          {/* Montra Branding */}
          <span className="flex items-center space-x-1">
            <span>Powered by</span>
            <span className="font-semibold text-red-400">MONTRA</span>
          </span>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
