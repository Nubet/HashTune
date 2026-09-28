export type Track = {
  id: string;
  title: string;
  artist: string;
  album?: string;
  duration: string;
  color: string;
  status: string;
};

export type Recognition = {
  id: string;
  title: string;
  artist: string;
  score: string;
  time: string;
  source: string;
  color: string;
  matchedAtMs?: number;
  sampleDurationMs?: number;
  recognitionTimeMs?: number;
  status: string;
};
