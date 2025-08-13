import React, { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Menu, Transition } from '@headlessui/react';
import { 
  UserCircleIcon, 
  Cog6ToothIcon, 
  ArrowRightOnRectangleIcon,
  Bars3Icon,
  XMarkIcon 
} from '@heroicons/react/24/outline';
import { useAuth } from '../contexts/AuthContext';
import { Button } from './ui';
import { cn } from '../utils/cn';
import { ASSETS } from '../utils/paths';

const Header: React.FC = () => {
  const location = useLocation();
  const { user, logout, hasRole } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);



  const isActive = (path: string): boolean => {
    return location.pathname === path;
  };

  const navLinkClass = (path: string): string => {
    const baseClass = "px-4 py-2 rounded-lg text-sm font-semibold transition-all duration-200 border border-transparent";
    return isActive(path)
      ? `${baseClass} bg-red-600 text-white shadow-md border-red-500`
      : `${baseClass} text-gray-200 hover:bg-red-600 hover:text-white hover:shadow-md hover:border-red-500`;
  };

  const handleLogout = async () => {
    try {
      await logout();
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  const navigationItems = [
    { name: 'Dashboard', href: '/dashboard', roles: ['USER', 'APPROVER', 'ADMIN'] },
    { name: 'Invoices', href: '/invoices', roles: ['USER', 'APPROVER', 'ADMIN'] },
    { name: 'Upload', href: '/upload', roles: ['USER', 'ADMIN'] },
  ];

  const adminNavigationItems = [
    { name: 'Users', href: '/users', roles: ['ADMIN'], icon: '👥' },
    { name: 'Patterns', href: '/patterns', roles: ['ADMIN'], icon: '🎯' },
    { name: 'Settings', href: '/settings', roles: ['ADMIN'], icon: '⚙️' },
  ];

  const visibleNavItems = navigationItems.filter(item => 
    item.roles.some(role => hasRole(role))
  );

  const visibleAdminItems = adminNavigationItems.filter(item => 
    item.roles.some(role => hasRole(role))
  );



  return (
    <header className="bg-gradient-to-r from-gray-800 to-gray-900 shadow-lg border-b-2 border-red-600">
      <div className="container mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <div className="flex items-center space-x-3">
            <Link to="/dashboard" className="flex items-center space-x-3">
              <img 
                src={ASSETS.MONTRA_LOGO} 
                alt="Montra Logo" 
                className="h-16 w-auto drop-shadow-lg logo-glow transition-all duration-300 hover:scale-105"
              />
              <span className="text-xl font-bold text-white hover:text-red-400 transition-colors duration-200">
                Invoice Automation
              </span>
            </Link>
          </div>
          
          {/* Desktop Navigation */}
          <nav className="hidden md:flex space-x-4" role="navigation" aria-label="Main navigation">
            {visibleNavItems.map((item) => (
              <Link
                key={item.name}
                to={item.href}
                className={navLinkClass(item.href)}
                aria-current={isActive(item.href) ? 'page' : undefined}
              >
                {item.name}
              </Link>
            ))}
            
            {/* Admin Section Separator */}
            {visibleAdminItems.length > 0 && (
              <>
                <div className="h-6 w-px bg-red-600 mx-3"></div>
                {visibleAdminItems.map((item) => (
                  <Link
                    key={item.name}
                    to={item.href}
                    className={cn(
                      navLinkClass(item.href),
                      'flex items-center space-x-1'
                    )}
                    aria-current={isActive(item.href) ? 'page' : undefined}
                    title={`Admin: ${item.name}`}
                  >
                    <span className="text-sm">{item.icon}</span>
                    <span>{item.name}</span>
                  </Link>
                ))}
              </>
            )}
          </nav>

          {/* User Menu */}
          <div className="flex items-center space-x-4">
            {/* User Dropdown */}
            <Menu as="div" className="relative">
              <Menu.Button className="flex items-center space-x-2 text-white hover:text-red-400 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-gray-800 focus:ring-red-500 rounded-lg p-3 hover:bg-gray-700 transition-all duration-200">
                <UserCircleIcon className="h-6 w-6" aria-hidden="true" />
                <span className="hidden md:block text-sm font-medium">
                  {user?.firstName || user?.username}
                </span>
              </Menu.Button>
              
              <Transition
                enter="transition ease-out duration-100"
                enterFrom="transform opacity-0 scale-95"
                enterTo="transform opacity-100 scale-100"
                leave="transition ease-in duration-75"
                leaveFrom="transform opacity-100 scale-100"
                leaveTo="transform opacity-0 scale-95"
              >
                <Menu.Items className="absolute right-0 mt-2 w-64 rounded-md shadow-lg bg-white ring-1 ring-black ring-opacity-5 focus:outline-none z-50">
                  <div className="py-1">
                    <div className="px-4 py-3 text-sm text-gray-700 border-b border-gray-200">
                      <p className="font-medium truncate">{user?.firstName || user?.username}</p>
                      <p className="text-gray-500 text-xs truncate mt-1" title={user?.email}>{user?.email}</p>
                    </div>
                    
                    {hasRole('ADMIN') && (
                      <Menu.Item>
                        {({ active }) => (
                          <Link
                            to="/settings"
                            className={cn(
                              active ? 'bg-gray-100' : '',
                              'group flex items-center px-4 py-2 text-sm text-gray-700'
                            )}
                          >
                            <Cog6ToothIcon className="mr-3 h-5 w-5" aria-hidden="true" />
                            Settings
                          </Link>
                        )}
                      </Menu.Item>
                    )}
                    
                    <Menu.Item>
                      {({ active }) => (
                        <button
                          onClick={handleLogout}
                          className={cn(
                            active ? 'bg-gray-100' : '',
                            'group flex w-full items-center px-4 py-2 text-sm text-gray-700'
                          )}
                        >
                          <ArrowRightOnRectangleIcon className="mr-3 h-5 w-5" aria-hidden="true" />
                          Sign out
                        </button>
                      )}
                    </Menu.Item>
                  </div>
                </Menu.Items>
              </Transition>
            </Menu>

            {/* Mobile menu button */}
            <div className="md:hidden">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                aria-label="Toggle mobile menu"
                className="text-white hover:text-red-400 hover:bg-gray-700 border border-transparent hover:border-red-500"
              >
                {mobileMenuOpen ? (
                  <XMarkIcon className="h-6 w-6" aria-hidden="true" />
                ) : (
                  <Bars3Icon className="h-6 w-6" aria-hidden="true" />
                )}
              </Button>
            </div>
          </div>
        </div>

        {/* Mobile Navigation */}
        {mobileMenuOpen && (
          <div className="md:hidden">
            <div className="px-2 pt-2 pb-3 space-y-1 sm:px-3 border-t border-red-600 bg-gray-800">
              {visibleNavItems.map((item) => (
                <Link
                  key={item.name}
                  to={item.href}
                  className={cn(
                    isActive(item.href)
                      ? 'bg-red-600 text-white border border-red-500'
                      : 'text-gray-200 hover:bg-red-600 hover:text-white border border-transparent hover:border-red-500',
                    'block px-3 py-2 rounded-lg text-base font-semibold transition-all duration-200'
                  )}
                  onClick={() => setMobileMenuOpen(false)}
                  aria-current={isActive(item.href) ? 'page' : undefined}
                >
                  {item.name}
                </Link>
              ))}
              
              {/* Mobile Admin Section */}
              {visibleAdminItems.length > 0 && (
                <>
                  <div className="border-t border-red-600 my-2"></div>
                  <div className="px-3 py-2 text-xs font-bold text-red-400 uppercase tracking-wider">
                    Admin
                  </div>
                  {visibleAdminItems.map((item) => (
                    <Link
                      key={item.name}
                      to={item.href}
                      className={cn(
                        isActive(item.href)
                          ? 'bg-red-600 text-white border border-red-500'
                          : 'text-gray-200 hover:bg-red-600 hover:text-white border border-transparent hover:border-red-500',
                        'flex items-center space-x-2 px-3 py-2 rounded-lg text-base font-semibold transition-all duration-200'
                      )}
                      onClick={() => setMobileMenuOpen(false)}
                      aria-current={isActive(item.href) ? 'page' : undefined}
                    >
                      <span className="text-sm">{item.icon}</span>
                      <span>{item.name}</span>
                    </Link>
                  ))}
                </>
              )}
            </div>
          </div>
        )}
      </div>
    </header>
  );
};

export default Header;