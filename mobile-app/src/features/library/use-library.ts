import { useInfiniteQuery } from "@tanstack/react-query";
import type { Album, Artist, Track } from "@/lib/api/contracts";
import { musicApi } from "@/lib/api/music-api";
import type { LibraryFilters } from "@/lib/api/music-api";

export type LibraryResource = "tracks" | "albums" | "artists";
export type LibraryEntry =
  | { kind: "track"; track: Track }
  | { kind: "album"; album: Album }
  | { kind: "artist"; artist: Artist };

type LibraryPage = {
  content: LibraryEntry[];
  hasNext: boolean;
  page: number;
  totalElements: number;
};

export function useLibrary(resource: LibraryResource, filters: LibraryFilters) {
  return useInfiniteQuery({
    queryKey: ["library", resource, filters],
    initialPageParam: 0,
    queryFn: async ({ pageParam, signal }): Promise<LibraryPage> => {
      switch (resource) {
        case "tracks": {
          const page = await musicApi.tracks(filters, pageParam, signal);
          return {
            ...page,
            content: page.content.map((track) => ({ kind: "track", track })),
          };
        }
        case "albums": {
          const page = await musicApi.albums(filters, pageParam, signal);
          return {
            ...page,
            content: page.content.map((album) => ({ kind: "album", album })),
          };
        }
        case "artists": {
          const page = await musicApi.artists(filters, pageParam, signal);
          return {
            ...page,
            content: page.content.map((artist) => ({ kind: "artist", artist })),
          };
        }
      }
    },
    getNextPageParam: (page) => (page.hasNext ? page.page + 1 : undefined),
  });
}

export function libraryEntryKey(entry: LibraryEntry) {
  switch (entry.kind) {
    case "track":
      return entry.track.id;
    case "album":
      return JSON.stringify([entry.album.artist, entry.album.title]);
    case "artist":
      return entry.artist.name;
  }
}
