import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Field';
import { BrandMark } from '../components/ui/BrandMark';
import { useSettings } from '../lib/settings';
import type { Role } from '../types';

export function Login() {
  const { user, login, registerClient } = useAuth();
  const navigate = useNavigate();
  const { show } = useToast();
  const settings = useSettings();

  const [role, setRole] = useState<Role>('admin');
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [businessName, setBusinessName] = useState('');
  const [phone, setPhone] = useState('');
  const [error, setError] = useState<string | null>(null);

  if (user) return <Navigate to={user.role === 'admin' ? '/admin' : '/client'} replace />;

  const fillDemo = (demoRole: Role) => {
    setRole(demoRole);
    setMode('login');
    if (demoRole === 'admin') {
      setEmail('admin@taxtitan.com');
      setPassword('admin123');
    } else {
      setEmail('arjun.kumar@example.com');
      setPassword('client123');
    }
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (mode === 'login') {
      const result = login(email, password, role);
      if (!result.ok) {
        setError(result.error ?? 'Login failed.');
        return;
      }
      show('Welcome back!', 'success');
      navigate(role === 'admin' ? '/admin' : '/client');
    } else {
      const result = registerClient({ name, businessName, email, phone, password });
      if (!result.ok) {
        setError(result.error ?? 'Registration failed.');
        return;
      }
      show('Account created. Welcome to TaxTitan!', 'success');
      navigate('/client');
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-4">
      <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
        <div className="mb-6 flex flex-col items-center text-center">
          <div className="mb-3">
            <BrandMark logoUrl={settings.logoUrl} size={64} />
          </div>
          <h1 className="text-lg font-semibold text-slate-900">{settings.firmName}</h1>
          <p className="text-sm text-slate-500">Practice management for your firm and clients</p>
        </div>

        <div className="mb-4 grid grid-cols-2 gap-1 rounded-lg bg-slate-100 p-1 text-sm font-medium">
          <button
            type="button"
            onClick={() => setRole('admin')}
            className={`rounded-md py-1.5 ${role === 'admin' ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500'}`}
          >
            Admin Portal
          </button>
          <button
            type="button"
            onClick={() => setRole('client')}
            className={`rounded-md py-1.5 ${role === 'client' ? 'bg-white shadow-sm text-slate-900' : 'text-slate-500'}`}
          >
            Client Portal
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-3">
          {mode === 'register' && (
            <>
              <Input label="Full name" required value={name} onChange={(e) => setName(e.target.value)} />
              <Input
                label="Business name"
                required
                value={businessName}
                onChange={(e) => setBusinessName(e.target.value)}
              />
              <Input label="Phone" required value={phone} onChange={(e) => setPhone(e.target.value)} />
            </>
          )}
          <Input
            label="Email"
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="username"
          />
          <Input
            label="Password"
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />

          {error && <p className="text-xs font-medium text-rose-600">{error}</p>}

          <Button type="submit" className="w-full">
            {mode === 'login' ? 'Sign in' : 'Create account'}
          </Button>
        </form>

        {role === 'client' && (
          <p className="mt-3 text-center text-xs text-slate-500">
            {mode === 'login' ? (
              <>
                New client?{' '}
                <button className="font-medium text-indigo-600 hover:underline" onClick={() => setMode('register')}>
                  Register here
                </button>
              </>
            ) : (
              <>
                Already have an account?{' '}
                <button className="font-medium text-indigo-600 hover:underline" onClick={() => setMode('login')}>
                  Sign in
                </button>
              </>
            )}
          </p>
        )}

        <div className="mt-6 rounded-lg bg-slate-50 p-3 text-center text-xs text-slate-500">
          <p className="mb-1.5 font-medium text-slate-600">Try a demo account</p>
          <div className="flex justify-center gap-2">
            <button onClick={() => fillDemo('admin')} className="rounded-md border border-slate-200 bg-white px-2.5 py-1 hover:bg-slate-100">
              Demo Admin
            </button>
            <button onClick={() => fillDemo('client')} className="rounded-md border border-slate-200 bg-white px-2.5 py-1 hover:bg-slate-100">
              Demo Client
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
