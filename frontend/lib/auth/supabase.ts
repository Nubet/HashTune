import { createClient, type Session } from "@supabase/supabase-js";
import { authStore } from "./store";

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL;
const supabasePublishableKey = process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY;

export const supabase =
  supabaseUrl && supabasePublishableKey
    ? createClient(supabaseUrl, supabasePublishableKey, {
        auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: true },
      })
    : null;

function rolesFromAccessToken(accessToken: string) {
  try {
    const [, payload] = accessToken.split(".");
    if (!payload) return [];
    const claims = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/"))) as {
      roles?: unknown;
    };
    return Array.isArray(claims.roles)
      ? claims.roles.filter((role): role is string => typeof role === "string")
      : [];
  } catch {
    return [];
  }
}

function applySession(session: Session | null) {
  if (session) {
    authStore.setSession(session.access_token, rolesFromAccessToken(session.access_token));
  } else {
    authStore.clearSession();
  }
}

export async function initializeAuth() {
  if (!supabase) {
    authStore.clearSession();
    return;
  }

  const { data, error } = await supabase.auth.getSession();
  if (error) {
    authStore.clearSession();
    return;
  }

  if (data.session) {
    applySession(data.session);
  } else {
    const anonymous = await supabase.auth.signInAnonymously();
    applySession(anonymous.data.session);
  }

  supabase.auth.onAuthStateChange((_event, session) => applySession(session));
}

export async function signInAdmin(email: string, password: string) {
  if (!supabase) throw new Error("Authentication is not configured");
  const { error } = await supabase.auth.signInWithPassword({ email, password });
  if (error) throw error;
}

export async function signOutAdmin() {
  if (!supabase) return;
  const { error } = await supabase.auth.signOut();
  if (error) throw error;
  const anonymous = await supabase.auth.signInAnonymously();
  applySession(anonymous.data.session);
}

if (typeof window !== "undefined") {
  void initializeAuth();
}
