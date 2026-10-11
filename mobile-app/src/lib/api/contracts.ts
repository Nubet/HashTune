import { z } from "zod";

export const trackSchema = z.object({
  id: z.uuid(),
  title: z.string(),
  artist: z.string(),
  album: z.string().nullish(),
  origin: z.enum(["HASH_TUNE", "MTG_JAMENDO"]),
  albumArtist: z.string().nullish(),
  composer: z.string().nullish(),
  genre: z.string().nullish(),
  releaseYear: z.string().nullish(),
  trackNumber: z.number().nullish(),
  discNumber: z.number().nullish(),
  isrc: z.string().nullish(),
  barcode: z.string().nullish(),
  comment: z.string().nullish(),
  coverArtUrl: z.url().nullish(),
  durationMs: z.number().nullish(),
  status: z.string(),
  createdAt: z.string(),
});

export const recognitionSchema = z.object({
  status: z.enum(["MATCHED", "NO_MATCH", "INVALID_AUDIO"]),
  track: trackSchema.nullish(),
  confidence: z.number().nullish(),
  matchedAtMs: z.number().nullish(),
  sampleDurationMs: z.number().nullish(),
  recognitionTimeMs: z.number().nullish(),
});

export const historyItemSchema = z.object({
  id: z.uuid(),
  track: trackSchema.nullish(),
  status: z.enum(["MATCHED", "NO_MATCH", "INVALID_AUDIO"]),
  confidence: z.number().nullish(),
  source: z.enum(["MICROPHONE", "AUDIO_FILE"]),
  recordingUrl: z.url().nullish(),
  downloadUrl: z.url().nullish(),
  sampleDurationMs: z.number().nullish(),
  createdAt: z.string(),
});

export const albumSchema = z.object({
  title: z.string(),
  artist: z.string(),
  coverArtUrl: z.url().nullish(),
  trackCount: z.number(),
});
export const artistSchema = z.object({
  name: z.string(),
  trackCount: z.number(),
  albumCount: z.number(),
  imageUrl: z.url().nullish(),
});

export function pageSchema<T extends z.ZodType>(item: T) {
  return z.object({
    content: z.array(item),
    page: z.number().int().nonnegative(),
    size: z.number().int().positive(),
    totalElements: z.number().int().nonnegative(),
    totalPages: z.number().int().nonnegative(),
    hasNext: z.boolean(),
    hasPrevious: z.boolean(),
  });
}

export const problemSchema = z.object({
  code: z.string().optional(),
  title: z.string().optional(),
  detail: z.string().optional(),
});

export type Track = z.infer<typeof trackSchema>;
export type Recognition = z.infer<typeof recognitionSchema>;
export type HistoryItem = z.infer<typeof historyItemSchema>;
export type Album = z.infer<typeof albumSchema>;
export type Artist = z.infer<typeof artistSchema>;
