export type Track = {
  id: string;
  title: string;
  artist: string;
  album?: string;
  origin: "PERSONAL" | "MTG_JAMENDO";
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
  status: string;
};

export type LibraryPagination = {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
};

export type AlbumSummary = {
  title: string;
  artist: string;
  coverArtUrl?: string;
  trackCount: number;
};

export type ArtistSummary = {
  name: string;
  trackCount: number;
  albumCount: number;
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
  matchedAtMs?: number;
  sampleDurationMs?: number;
  recognitionTimeMs?: number;
  status: string;
  recordingUrl?: string;
  downloadUrl?: string;
};
