import { z } from "zod";
import { multipart, request, requestVoid } from "./client";
import {
  indexingJobSchema,
  recognitionSchema,
  type ApiTrack,
  type IndexingJob,
  type RecognitionResponse,
  type UploadResponse,
  uploadResponseSchema,
  trackSchema,
} from "./contracts";

const historyItemSchema = z.object({
  id: z.string(),
  track: trackSchema.nullable().optional(),
  status: z.string(),
  confidence: z.number().nullable().optional(),
  source: z.string(),
  recordingUrl: z.string().url().nullable().optional(),
  downloadUrl: z.string().url().nullable().optional(),
  sampleDurationMs: z.number().nullable().optional(),
  createdAt: z.string(),
});

const historySchema = z.array(historyItemSchema);
const albumSchema = z.object({
  title: z.string(),
  artist: z.string(),
  coverArtUrl: z.string().url().nullable().optional(),
  trackCount: z.number(),
});
const artistSchema = z.object({
  name: z.string(),
  trackCount: z.number(),
  albumCount: z.number(),
  imageUrl: z.string().url().nullable().optional(),
});
const pageSchema = <T extends z.ZodTypeAny>(item: T) =>
  z.object({
    content: z.array(item),
    page: z.number(),
    size: z.number(),
    totalElements: z.number(),
    totalPages: z.number(),
    hasNext: z.boolean(),
    hasPrevious: z.boolean(),
  });

const trackPageSchema = pageSchema(trackSchema);
const albumPageSchema = pageSchema(albumSchema);
const artistPageSchema = pageSchema(artistSchema);

export type ApiHistoryItem = z.infer<typeof historyItemSchema>;
type ApiTrackPage = z.infer<typeof trackPageSchema>;
type ApiAlbumPage = z.infer<typeof albumPageSchema>;
type ApiArtistPage = z.infer<typeof artistPageSchema>;

export const musicApi = {
  listTracks({
    query = "",
    origin = "ALL",
    artist,
    album,
    page = 0,
    size = 50,
  }: {
    query?: string;
    origin?: "PERSONAL" | "MTG_JAMENDO" | "ALL";
    artist?: string;
    album?: string;
    page?: number;
    size?: number;
  } = {}) {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (query) params.set("query", query);
    if (artist) params.set("artist", artist);
    if (album) params.set("album", album);
    if (origin !== "ALL") params.set("origin", origin);

    return request<ApiTrackPage>(`/api/v1/library/tracks?${params}`, {}, trackPageSchema);
  },

  listAlbums({
    query = "",
    origin = "ALL",
    page = 0,
    size = 50,
  }: {
    query?: string;
    origin?: "PERSONAL" | "MTG_JAMENDO" | "ALL";
    page?: number;
    size?: number;
  } = {}) {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (query) params.set("query", query);
    if (origin !== "ALL") params.set("origin", origin);

    return request<ApiAlbumPage>(`/api/v1/library/tracks/albums?${params}`, {}, albumPageSchema);
  },

  listArtists({
    query = "",
    origin = "ALL",
    page = 0,
    size = 50,
  }: {
    query?: string;
    origin?: "PERSONAL" | "MTG_JAMENDO" | "ALL";
    page?: number;
    size?: number;
  } = {}) {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (query) params.set("query", query);
    if (origin !== "ALL") params.set("origin", origin);

    return request<ApiArtistPage>(`/api/v1/library/tracks/artists?${params}`, {}, artistPageSchema);
  },

  uploadTrack(file: File) {
    return request<UploadResponse>(
      "/api/v1/library/tracks",
      { method: "POST", body: multipart(file) },
      uploadResponseSchema,
    );
  },

  updateTrackMetadata(id: string, metadata: { title: string; artist: string; album: string }) {
    return request<ApiTrack>(
      `/api/v1/library/tracks/${id}/metadata`,
      {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(metadata),
      },
      trackSchema,
    );
  },

  getIndexingJob(id: string) {
    return request<IndexingJob>(`/api/v1/indexing-jobs/${id}`, {}, indexingJobSchema);
  },

  reindexTrack(id: string) {
    return request<IndexingJob>(
      `/api/v1/library/tracks/${id}/reindex`,
      { method: "POST" },
      indexingJobSchema,
    );
  },

  async waitForIndexing(id: string) {
    for (let attempt = 0; attempt < 120; attempt += 1) {
      const job = await this.getIndexingJob(id);
      if (job.status === "COMPLETED") return job;
      if (job.status === "FAILED") throw new Error(job.errorMessage ?? "Indexing failed");
      await new Promise((resolve) => window.setTimeout(resolve, 250));
    }
    throw new Error("Indexing timed out");
  },

  recognize(file: File, source: "MICROPHONE" | "AUDIO_FILE", probe = false) {
    const query = new URLSearchParams({ source });
    if (probe) query.set("probe", "true");
    return request<RecognitionResponse>(
      `/api/v1/recognitions?${query}`,
      { method: "POST", body: multipart(file) },
      recognitionSchema,
    );
  },

  history() {
    return request<ApiHistoryItem[]>(
      "/api/v1/recognition-history?limit=100&offset=0",
      {},
      historySchema,
    );
  },

  removeTrack(id: string) {
    return requestVoid(`/api/v1/library/tracks/${id}`, { method: "DELETE" });
  },

  clearHistory() {
    return requestVoid("/api/v1/recognition-history", { method: "DELETE" });
  },
};
