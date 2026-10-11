import { beforeEach, describe, expect, it, vi } from "vitest";
import { z } from "zod";
import { createApiClient, retryAfterSeconds } from "./client";

const fetch = vi.fn<(url: string, options: RequestInit) => Promise<Response>>();
const refreshToken = vi.fn(async () => "refreshed-token");
const request = createApiClient({
  baseUrl: () => "https://api.hashtune.test",
  fetch,
  getToken: async () => "existing-token",
  refreshToken,
});

beforeEach(() => {
  vi.clearAllMocks();
});

describe("native API transport", () => {
  it("refreshes a rejected token once before returning validated data", async () => {
    fetch
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(Response.json({ status: "MATCHED" }));
    await expect(
      request("/recognitions", z.object({ status: z.string() })),
    ).resolves.toEqual({ status: "MATCHED" });
    expect(refreshToken).toHaveBeenCalledOnce();
    expect(
      new Headers(fetch.mock.calls[1]?.[1]?.headers).get("Authorization"),
    ).toBe("Bearer refreshed-token");
  });

  it("does not replay a POST when its outcome is unknown", async () => {
    fetch.mockRejectedValueOnce(new TypeError("Network request failed"));
    await expect(
      request("/recognitions", z.unknown(), { method: "POST" }),
    ).rejects.toMatchObject({ code: "NETWORK_ERROR" });
    expect(fetch).toHaveBeenCalledOnce();
  });

  it("exposes the server cooldown without retrying a rate-limited request", async () => {
    fetch.mockResolvedValueOnce(
      new Response(JSON.stringify({ code: "TOO_MANY_REQUESTS" }), {
        status: 429,
        headers: { "Retry-After": "30" },
      }),
    );
    await expect(request("/recognitions", z.unknown())).rejects.toMatchObject({
      status: 429,
      retryAfterSeconds: 30,
    });
    expect(fetch).toHaveBeenCalledOnce();
  });

  it("rejects malformed success responses and accepts an empty 204", async () => {
    fetch.mockResolvedValueOnce(Response.json({ status: 12 }));
    await expect(
      request("/history", z.object({ status: z.string() })),
    ).rejects.toMatchObject({ code: "INVALID_RESPONSE" });
    fetch.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await expect(
      request("/history", z.undefined(), { method: "DELETE" }),
    ).resolves.toBeUndefined();
  });

  it("does not send bearer tokens to public library requests", async () => {
    fetch.mockResolvedValueOnce(Response.json([]));
    await request("/library", z.array(z.unknown()), {}, false);
    expect(
      new Headers(fetch.mock.calls[0]?.[1]?.headers).has("Authorization"),
    ).toBe(false);
  });

  it("parses an HTTP-date retry header", () => {
    expect(
      retryAfterSeconds(
        "Sun, 11 Oct 2026 12:00:30 GMT",
        Date.parse("2026-10-11T12:00:00Z"),
      ),
    ).toBe(30);
    expect(retryAfterSeconds("unknown")).toBeNull();
  });

  it("reports a deadline separately from a canceled request", async () => {
    vi.useFakeTimers();
    fetch.mockImplementation(
      (_url, options) =>
        new Promise((_resolve, reject) => {
          options.signal?.addEventListener(
            "abort",
            () => reject(new DOMException("Aborted", "AbortError")),
            { once: true },
          );
        }),
    );
    try {
      const deadline = expect(
        request("/history", z.unknown()),
      ).rejects.toMatchObject({ code: "TIMEOUT" });
      await vi.advanceTimersByTimeAsync(60_000);
      await deadline;
      const controller = new AbortController();
      const canceled = expect(
        request("/history", z.unknown(), { signal: controller.signal }),
      ).rejects.toMatchObject({ code: "CANCELED" });
      await vi.advanceTimersByTimeAsync(0);
      controller.abort();
      await canceled;
    } finally {
      vi.useRealTimers();
    }
  });

  it("treats non-JSON successful responses as contract errors", async () => {
    fetch.mockResolvedValueOnce(new Response("<html>Proxy error</html>"));
    await expect(request("/history", z.unknown())).rejects.toMatchObject({
      code: "INVALID_RESPONSE",
    });
  });
});
