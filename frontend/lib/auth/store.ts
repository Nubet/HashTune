import { useSyncExternalStore } from "react";

export type AuthState = {
  status: "loading" | "signed-out" | "authenticated";
  accessToken: string | null;
  roles: string[];
};

const signedOutState: AuthState = {
  status: "signed-out",
  accessToken: null,
  roles: [],
};

const loadingState: AuthState = {
  status: "loading",
  accessToken: null,
  roles: [],
};

let state = loadingState;
const listeners = new Set<() => void>();

export const authStore = {
  getSnapshot() {
    return state;
  },

  subscribe(listener: () => void) {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },

  setSession(accessToken: string, roles: string[] = []) {
    state = { status: "authenticated", accessToken, roles };
    listeners.forEach((listener) => listener());
  },

  clearSession() {
    state = signedOutState;
    listeners.forEach((listener) => listener());
  },

  setLoading() {
    state = loadingState;
    listeners.forEach((listener) => listener());
  },
};

export function useAuth() {
  const auth = useSyncExternalStore(authStore.subscribe, authStore.getSnapshot, () => loadingState);
  return {
    ...auth,
    isAdmin: auth.roles.includes("admin"),
  };
}
