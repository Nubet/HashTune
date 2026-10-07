import { useSyncExternalStore } from "react";

export type AuthState = {
  status: "signed-out" | "authenticated";
  accessToken: string | null;
  roles: string[];
};

const signedOutState: AuthState = {
  status: "signed-out",
  accessToken: null,
  roles: [],
};

let state = signedOutState;
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
};

export function useAuth() {
  const auth = useSyncExternalStore(authStore.subscribe, authStore.getSnapshot, () => signedOutState);
  return {
    ...auth,
    isAdmin: auth.roles.includes("admin"),
  };
}
