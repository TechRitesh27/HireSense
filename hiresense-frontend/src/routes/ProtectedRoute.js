import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Wraps a route to require authentication.
 * Optionally accepts `allowedRoles` to restrict by role.
 *
 * Role is read from context first, then falls back to localStorage
 * to avoid the race condition between navigate() and React state updates.
 */
function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, role } = useAuth();
  const location = useLocation();

  // Fallback: read role directly from localStorage (synchronously written on login)
  // This prevents a one-render race where context state hasn't updated yet
  const effectiveRole = role || (() => {
    try {
      const saved = localStorage.getItem('hiresense_user');
      return saved ? JSON.parse(saved).role : null;
    } catch {
      return null;
    }
  })();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && !allowedRoles.includes(effectiveRole)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return children;
}

export default ProtectedRoute;
