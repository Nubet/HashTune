export type Track = {
  id: string;
  title: string;
  artist: string;
  album?: string;
  albumArtist?: string;
  composer?: string;
  genre?: string;
  releaseYear?: string;
  trackNumber?: number;
  discNumber?: number;
  isrc?: string;
  barcode?: string;
  comment?: string;
  coverArtUrl?: string;
  duration: string;
  color: string;
  status: string;
};

export type MetadataDraft = {
  trackId: string;
  fileName: string;
  title: string;
  artist: string;
  album: string;
  coverArtUrl?: string;
};

export type Recognition = {
  id: string;
  title: string;
  artist: string;
  coverArtUrl?: string;
  album?: string;
  albumArtist?: string;
  composer?: string;
  genre?: string;
  releaseYear?: string;
  trackNumber?: number;
  discNumber?: number;
  isrc?: string;
  barcode?: string;
  comment?: string;
  durationMs?: number;
  score: string;
  time: string;
  source: string;
  color: string;
  matchedAtMs?: number;
  sampleDurationMs?: number;
  recognitionTimeMs?: number;
  status: string;
};
