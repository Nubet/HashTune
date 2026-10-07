import { useState } from "react";
import { signInAdmin, signOutAdmin } from "@/lib/auth/supabase";
import { useAuth } from "@/lib/auth/store";
import { Button } from "./ui";

export function AuthControl() {
  const { status, isAdmin } = useAuth();
  const [open, setOpen] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);
    try {
      await signInAdmin(email, password);
      setPassword("");
      setOpen(false);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Could not sign in");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (status === "loading") return null;

  if (isAdmin) {
    return (
      <Button variant="secondary" size="sm" onClick={() => void signOutAdmin()}>
        Sign out admin
      </Button>
    );
  }

  return (
    <div className="relative">
      <Button variant="secondary" size="sm" onClick={() => setOpen((current) => !current)}>
        Admin login
      </Button>
      {open && (
        <form
          onSubmit={submit}
          className="absolute right-0 top-12 z-50 w-72 rounded-2xl border border-line bg-canvas p-4 shadow-xl"
        >
          <p className="mb-3 text-sm font-bold text-ink">Admin access</p>
          <label className="mb-2 block text-xs font-semibold text-muted" htmlFor="admin-email">
            Email
          </label>
          <input
            id="admin-email"
            type="email"
            autoComplete="username"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="mb-3 w-full rounded-lg border border-line bg-canvas px-3 py-2 text-sm outline-none focus:border-brand"
            required
          />
          <label className="mb-2 block text-xs font-semibold text-muted" htmlFor="admin-password">
            Password
          </label>
          <input
            id="admin-password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="mb-3 w-full rounded-lg border border-line bg-canvas px-3 py-2 text-sm outline-none focus:border-brand"
            required
          />
          {error && <p className="mb-3 text-xs font-semibold text-red-600">{error}</p>}
          <Button type="submit" variant="primary" size="sm" className="w-full" disabled={isSubmitting}>
            {isSubmitting ? "Signing in…" : "Sign in"}
          </Button>
        </form>
      )}
    </div>
  );
}
