import type { Session, SupabaseClient } from "@supabase/supabase-js";

type SessionAuth = Pick<
  SupabaseClient["auth"],
  "getSession" | "signInAnonymously" | "refreshSession"
>;

export function createAnonymousSession(auth: SessionAuth) {
  let pending: Promise<Session> | null = null;

  async function restoreSession() {
    const restored = await auth.getSession();
    if (restored.error) throw restored.error;
    if (restored.data.session) return restored.data.session;
    const created = await auth.signInAnonymously();
    if (created.error) throw created.error;
    if (!created.data.session)
      throw new Error("An anonymous session could not be created.");
    return created.data.session;
  }

  return {
    getSession() {
      pending ??= restoreSession().finally(() => {
        pending = null;
      });
      return pending;
    },
    async refreshToken() {
      const { data, error } = await auth.refreshSession();
      if (error) throw error;
      if (!data.session)
        throw new Error("Your session could not be refreshed. Try again.");
      return data.session.access_token;
    },
  };
}
