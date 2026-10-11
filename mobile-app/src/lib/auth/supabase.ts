import { createClient } from "@supabase/supabase-js";
import * as SecureStore from "expo-secure-store";
import { AppState } from "react-native";
import { configuration, requireConfiguration } from "../config";
import { createAnonymousSession } from "./anonymous-session";

export const supabase = configuration.success
  ? createClient(
      configuration.data.supabaseUrl,
      configuration.data.supabasePublishableKey,
      {
        auth: {
          storage: {
            getItem: (key: string) => SecureStore.getItemAsync(key),
            setItem: (key: string, value: string) =>
              SecureStore.setItemAsync(key, value),
            removeItem: (key: string) => SecureStore.deleteItemAsync(key),
          },
          persistSession: true,
          autoRefreshToken: true,
          detectSessionInUrl: false,
        },
      },
    )
  : null;

const sessions = supabase ? createAnonymousSession(supabase.auth) : null;

if (supabase) {
  if (AppState.currentState === "active") supabase.auth.startAutoRefresh();
  AppState.addEventListener("change", (state) => {
    if (state === "active") supabase.auth.startAutoRefresh();
    else supabase.auth.stopAutoRefresh();
  });
}

export async function getSession() {
  requireConfiguration();
  if (!sessions) throw new Error("Authentication is not configured.");
  return sessions.getSession();
}

export async function getAccessToken() {
  return (await getSession()).access_token;
}

export async function refreshAccessToken() {
  if (!sessions) throw new Error("Authentication is not configured.");
  return sessions.refreshToken();
}
