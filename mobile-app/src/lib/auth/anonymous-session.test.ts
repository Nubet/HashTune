import type { Session, SupabaseClient } from "@supabase/supabase-js";
import { describe, expect, it, vi } from "vitest";
import { createAnonymousSession } from "./anonymous-session";

const session = {
  access_token: "token",
  refresh_token: "refresh",
  expires_in: 3600,
  token_type: "bearer",
  user: {
    id: "user-id",
    app_metadata: {},
    user_metadata: {},
    aud: "authenticated",
    created_at: "2026-10-11T12:00:00Z",
  },
} satisfies Session;

function auth() {
  return {
    getSession: vi.fn<SupabaseClient["auth"]["getSession"]>(async () => ({
      data: { session: null },
      error: null,
    })),
    signInAnonymously: vi.fn(async () => ({
      data: { session, user: session.user },
      error: null,
    })),
    refreshSession: vi.fn(async () => ({
      data: { session, user: session.user },
      error: null,
    })),
  };
}

describe("anonymous session", () => {
  it("preserves an existing identity rather than creating a new one", async () => {
    const provider = auth();
    provider.getSession.mockResolvedValue({ data: { session }, error: null });
    await expect(createAnonymousSession(provider).getSession()).resolves.toBe(
      session,
    );
    expect(provider.signInAnonymously).not.toHaveBeenCalled();
  });

  it("creates only one identity for simultaneous initialization requests", async () => {
    const provider = auth();
    const sessions = createAnonymousSession(provider);
    await Promise.all([sessions.getSession(), sessions.getSession()]);
    expect(provider.signInAnonymously).toHaveBeenCalledOnce();
  });

  it("does not replace an identity when restoration fails", async () => {
    const provider = auth();
    provider.getSession.mockRejectedValueOnce(new Error("Network unavailable"));
    const sessions = createAnonymousSession(provider);
    await expect(sessions.getSession()).rejects.toThrow("Network unavailable");
    expect(provider.signInAnonymously).not.toHaveBeenCalled();
    await expect(sessions.getSession()).resolves.toBe(session);
  });
});
