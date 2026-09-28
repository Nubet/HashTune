export type Track = {
  id: number;
  title: string;
  artist: string;
  duration: string;
  color: string;
};

export type Recognition = {
  title: string;
  artist: string;
  score: string;
  time: string;
  source: string;
  color: string;
};

export const initialTracks: Track[] = [
  {
    id: 1,
    title: "Do I Wanna Know?",
    artist: "Arctic Monkeys",
    duration: "04:32",
    color: "#e8d25f",
  },
  {
    id: 2,
    title: "Get Lucky",
    artist: "Daft Punk feat. Pharrell Williams",
    duration: "06:09",
    color: "#d7b15a",
  },
  { id: 3, title: "Midnight City", artist: "M83", duration: "04:03", color: "#18243a" },
  {
    id: 4,
    title: "Everything In Its Right Place",
    artist: "Radiohead",
    duration: "04:11",
    color: "#d7d7d3",
  },
  { id: 5, title: "Teardrop", artist: "Massive Attack", duration: "05:30", color: "#a88975" },
  { id: 6, title: "Archangel", artist: "Burial", duration: "04:00", color: "#404040" },
];

export const initialHistory: Recognition[] = [
  {
    title: "Midnight City",
    artist: "M83",
    score: "91%",
    time: "Today, 20:21",
    source: "Microphone",
    color: "#18243a",
  },
  {
    title: "Teardrop",
    artist: "Massive Attack",
    score: "97%",
    time: "Today, 18:04",
    source: "Audio file",
    color: "#a88975",
  },
];
