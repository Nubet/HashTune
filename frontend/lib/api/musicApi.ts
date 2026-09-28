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
} from "./contracts";

const trackListSchema = z.array(
  z.object({
    id: z.string(),
    title: z.string(),
    artist: z.string(),
    album: z.string().nullable().optional(),
    durationMs: z.number().nullable().optional(),
    status: z.string(),
    createdAt: z.string(),
  }),
);

const historyItemSchema = z.object({
  id: z.string(),
  track: trackListSchema.element.nullable().optional(),
  status: z.string(),
  confidence: z.number().nullable().optional(),
  source: z.string(),
  createdAt: z.string(),
});

const historySchema = z.array(historyItemSchema);

export type ApiHistoryItem = z.infer<typeof historyItemSchema>;

export const musicApi = {
  listTracks(query = "") {
    const params = new URLSearchParams({ limit: "100", offset: "0" });
    if (query) params.set("query", query);
    return request<ApiTrack[]>(`/api/v1/library/tracks?${params}`, {}, trackListSchema);
  },

  uploadTrack(file: File) {
    return request<UploadResponse>(
      "/api/v1/library/tracks",
      { method: "POST", body: multipart(file) },
      uploadResponseSchema,
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

  async uploadAndIndex(files: FileList, onProgress: (progress: number) => void) {
    for (let index = 0; index < files.length; index += 1) {
      const upload = await this.uploadTrack(files[index]);
      await this.waitForIndexing(upload.indexingJobId);
      onProgress(Math.round(((index + 1) / files.length) * 100));
    }
  },

  recognize(file: File, source: "MICROPHONE" | "AUDIO_FILE") {
    return request<RecognitionResponse>(
      `/api/v1/recognitions?source=${source}`,
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
