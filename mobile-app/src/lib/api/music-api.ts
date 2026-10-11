import { fetch } from "expo/fetch";
import { z } from "zod";
import { requireConfiguration } from "../config";
import { getAccessToken, refreshAccessToken } from "../auth/supabase";
import { createApiClient } from "./client";
import {
  albumSchema,
  artistSchema,
  historyItemSchema,
  pageSchema,
  recognitionSchema,
  trackSchema,
} from "./contracts";

const request = createApiClient({
  baseUrl: () => requireConfiguration().apiUrl,
  fetch,
  getToken: getAccessToken,
  refreshToken: refreshAccessToken,
});

export type LibraryFilters = {
  query: string;
  origin: "ALL" | "HASH_TUNE" | "MTG_JAMENDO";
  artist?: string;
  album?: string;
};

function libraryParams(filters: LibraryFilters, page: number) {
  const params = new URLSearchParams({ page: String(page), size: "25" });
  if (filters.query) params.set("query", filters.query);
  if (filters.origin !== "ALL") params.set("origin", filters.origin);
  if (filters.artist) params.set("artist", filters.artist);
  if (filters.album) params.set("album", filters.album);
  return params;
}

export const musicApi = {
  recognize(sample: Blob, signal: AbortSignal) {
    const body = new FormData();
    body.append("file", sample, "microphone-sample.m4a");
    return request(
      "/api/v1/recognitions?source=MICROPHONE",
      recognitionSchema,
      { method: "POST", body, signal },
    );
  },
  history(offset: number, signal: AbortSignal) {
    return request(
      `/api/v1/recognition-history?limit=25&offset=${offset}`,
      z.array(historyItemSchema),
      { signal },
    );
  },
  clearHistory() {
    return request("/api/v1/recognition-history", z.undefined(), {
      method: "DELETE",
    });
  },
  tracks(filters: LibraryFilters, page: number, signal: AbortSignal) {
    return request(
      `/api/v1/library/tracks?${libraryParams(filters, page)}`,
      pageSchema(trackSchema),
      { signal },
      false,
    );
  },
  albums(filters: LibraryFilters, page: number, signal: AbortSignal) {
    return request(
      `/api/v1/library/tracks/albums?${libraryParams(filters, page)}`,
      pageSchema(albumSchema),
      { signal },
      false,
    );
  },
  artists(filters: LibraryFilters, page: number, signal: AbortSignal) {
    return request(
      `/api/v1/library/tracks/artists?${libraryParams(filters, page)}`,
      pageSchema(artistSchema),
      { signal },
      false,
    );
  },
  recordingUrl(id: string) {
    return `${requireConfiguration().apiUrl.replace(/\/$/, "")}/api/v1/recognition-history/${encodeURIComponent(id)}/recording`;
  },
};
