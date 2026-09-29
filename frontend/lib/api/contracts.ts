import { z } from "zod";

export const trackSchema = z.object({
  id: z.string(),
  title: z.string(),
  artist: z.string(),
  album: z.string().nullable().optional(),
  albumArtist: z.string().nullable().optional(),
  composer: z.string().nullable().optional(),
  genre: z.string().nullable().optional(),
  releaseYear: z.string().nullable().optional(),
  trackNumber: z.number().nullable().optional(),
  discNumber: z.number().nullable().optional(),
  isrc: z.string().nullable().optional(),
  barcode: z.string().nullable().optional(),
  comment: z.string().nullable().optional(),
  coverArtUrl: z.string().url().nullable().optional(),
  durationMs: z.number().nullable().optional(),
  status: z.string(),
  createdAt: z.string(),
});

export const uploadResponseSchema = z.object({
  trackId: z.string(),
  indexingJobId: z.string(),
  status: z.string(),
  track: trackSchema,
});

export const indexingJobSchema = z.object({
  id: z.string(),
  trackId: z.string(),
  status: z.string(),
  progress: z.number(),
  errorCode: z.string().nullable().optional(),
  errorMessage: z.string().nullable().optional(),
});

export const recognitionSchema = z.object({
  status: z.enum(["MATCHED", "NO_MATCH", "INVALID_AUDIO"]),
  track: trackSchema.nullable().optional(),
  confidence: z.number().nullable().optional(),
  matchedAtMs: z.number().nullable().optional(),
  sampleDurationMs: z.number().nullable().optional(),
  recognitionTimeMs: z.number().nullable().optional(),
});

export const problemSchema = z.object({
  type: z.string().optional(),
  title: z.string().optional(),
  code: z.string().optional(),
  status: z.number().optional(),
  detail: z.string().optional(),
  instance: z.string().optional(),
});

export type ApiTrack = z.infer<typeof trackSchema>;
export type UploadResponse = z.infer<typeof uploadResponseSchema>;
export type IndexingJob = z.infer<typeof indexingJobSchema>;
export type RecognitionResponse = z.infer<typeof recognitionSchema>;
