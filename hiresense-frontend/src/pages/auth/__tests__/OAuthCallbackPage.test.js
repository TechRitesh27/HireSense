import { render, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from '../../../context/AuthContext';
import OAuthCallbackPage from '../OAuthCallbackPage';

function buildToken(payload) {
  const encode = (value) => btoa(JSON.stringify(value));
  return `header.${encode(payload)}.signature`;
}

describe('OAuthCallbackPage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('stores the OAuth token and user information from the callback URL', async () => {
    const token = buildToken({ sub: 'oauth.user@example.com', role: 'SUPER_ADMIN' });

    render(
      <MemoryRouter initialEntries={[`/oauth-callback?token=${token}`]}>
        <AuthProvider>
          <Routes>
            <Route path="/oauth-callback" element={<OAuthCallbackPage />} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(localStorage.getItem('hiresense_token')).toBe(token);
    });

    const storedUser = JSON.parse(localStorage.getItem('hiresense_user'));
    expect(storedUser.email).toBe('oauth.user@example.com');
    expect(storedUser.role).toBe('SUPER_ADMIN');
  });
});
