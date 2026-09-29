export type Track = {
  id: string;
  title: string;
  artist: string;
  album?: string;
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
};

export type Recognition = {
  id: string;
  title: string;
  artist: string;
  coverArtUrl?: string;
  album?: string;
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
