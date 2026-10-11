import { beforeEach, describe, expect, it, vi } from "vitest";
import { musicApi } from "./music-api";

const { fetch, getAccessToken } = vi.hoisted(() => ({
  fetch: vi.fn<(url: string, options: RequestInit) => Promise<Response>>(),
  getAccessToken: vi.fn(async () => "session-token"),
}));

vi.mock("expo/fetch", () => ({ fetch }));
vi.mock("../config", () => ({
  requireConfiguration: () => ({ apiUrl: "https://api.hashtune.test" }),
}));
vi.mock("../auth/supabase", () => ({
  getAccessToken,
  refreshAccessToken: async () => "refreshed-token",
}));

beforeEach(() => {
  vi.clearAllMocks();
});

describe("backend integration contract", () => {
  it.each(["audio/mp4", "audio/x-m4a"])(
    "uploads a finalized microphone file as %s with the existing multipart contract",
    async (type) => {
      fetch.mockResolvedValueOnce(
        Response.json({
          status: "NO_MATCH",
          track: null,
          sampleDurationMs: 12000,
          recognitionTimeMs: 30,
        }),
      );
      await musicApi.recognize(
        new Blob(["aac"], { type }),
        new AbortController().signal,
      );
      const request = fetch.mock.calls[0];
      expect(request?.[0]).toBe(
        "https://api.hashtune.test/api/v1/recognitions?source=MICROPHONE",
      );
      expect(request?.[1].method).toBe("POST");
      const body = request?.[1].body;
      expect(body).toBeInstanceOf(FormData);
      if (!(body instanceof FormData))
        throw new Error("Expected multipart form data");
      const file = body.get("file");
      expect(file).toBeInstanceOf(File);
      if (!(file instanceof File)) throw new Error("Expected audio file");
      expect(file.type).toBe(type);
      expect(file.name).toBe("microphone-sample.m4a");
      expect(new Headers(request?.[1].headers).get("Content-Type")).toBeNull();
    },
  );

  it("reads history as an authenticated offset-paginated array and clears with 204", async () => {
    fetch.mockResolvedValueOnce(Response.json([]));
    await expect(
      musicApi.history(25, new AbortController().signal),
    ).resolves.toEqual([]);
    expect(fetch.mock.calls[0]?.[0]).toContain("limit=25&offset=25");
    expect(
      new Headers(fetch.mock.calls[0]?.[1].headers).get("Authorization"),
    ).toBe("Bearer session-token");
    fetch.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await expect(musicApi.clearHistory()).resolves.toBeUndefined();
    expect(fetch.mock.calls[1]?.[1].method).toBe("DELETE");
  });

  it("encodes artist and album filters without leaking credentials to public reads", async () => {
    fetch.mockResolvedValueOnce(
      Response.json({
        content: [],
        page: 0,
        size: 25,
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
        hasPrevious: false,
      }),
    );
    await musicApi.tracks(
      {
        query: "",
        origin: "ALL",
        artist: "Earth, Wind & Fire",
        album: "That's the Way",
      },
      0,
      new AbortController().signal,
    );
    const url = new URL(fetch.mock.calls[0]?.[0] ?? "");
    expect(url.searchParams.get("artist")).toBe("Earth, Wind & Fire");
    expect(url.searchParams.get("album")).toBe("That's the Way");
    expect(url.searchParams.has("origin")).toBe(false);
    expect(getAccessToken).not.toHaveBeenCalled();
  });

  it("keeps authenticated sample playback on the configured API origin", () => {
    expect(musicApi.recordingUrl("sample-id")).toBe(
      "https://api.hashtune.test/api/v1/recognition-history/sample-id/recording",
    );
  });
});
