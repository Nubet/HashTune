import { useMemo, useState } from "react";
import type { Track } from "../lib/music";
import { MoreIcon } from "./icons";
import { TrackArtwork } from "./TrackArtwork";
import { Button, SectionLabel } from "./ui";

function statusLabel(status: string) {
  if (status === "INDEXED") return "Ready";
  if (status === "PROCESSING") return "Preparing";
  if (status === "FAILED") return "Needs attention";
  return "Waiting";
}

type LibraryViewMode = "tracks" | "albums" | "artists";
type LibraryOrigin = "PERSONAL" | "MTG_JAMENDO" | "ALL";
type SelectedAlbum = { title: string; artist: string };

export function LibraryView({
  tracks,
  indexingProgress,
  onAddFiles,
  onReindex,
  onRemove,
  origin,
  onOriginChange,
}: {
  tracks: Track[];
  indexingProgress: number | null;
  onAddFiles: (files: FileList) => Promise<void>;
  onReindex: (id: string) => Promise<void>;
  onRemove: (id: string) => Promise<void>;
  origin: LibraryOrigin;
  onOriginChange: (origin: LibraryOrigin) => void;
}) {
  const [view, setView] = useState<LibraryViewMode>("tracks");
  const [query, setQuery] = useState("");
  const [selectedArtist, setSelectedArtist] = useState<string | null>(null);
  const [selectedAlbum, setSelectedAlbum] = useState<SelectedAlbum | null>(null);
  const [pageSize, setPageSize] = useState(50);
  const [page, setPage] = useState(0);
  const [activeMenu, setActiveMenu] = useState<string | null>(null);

  const indexedCount = tracks.filter((track) => track.status === "INDEXED").length;
  const failedCount = tracks.filter((track) => track.status === "FAILED").length;
  const pendingCount = tracks.length - indexedCount - failedCount;

  const normalizedQuery = query.trim().toLowerCase();

  function changeOrigin(nextOrigin: LibraryOrigin) {
    setQuery("");
    setSelectedArtist(null);
    setSelectedAlbum(null);
    setView("tracks");
    setPage(0);
    onOriginChange(nextOrigin);
  }

  const artists = useMemo(() => {
    const artistSet = new Set(tracks.map((t) => t.artist).filter(Boolean));
    return [...artistSet]
      .filter((a) => !normalizedQuery || a.toLowerCase().includes(normalizedQuery))
      .sort((a, b) => a.localeCompare(b, undefined, { sensitivity: "base" }));
  }, [tracks, normalizedQuery]);

  const albums = useMemo(() => {
    const albumMap = new Map<
      string,
      {
        title: string;
        artist: string;
        coverArtUrl?: string;
        color?: string;
        trackCount: number;
      }
    >();
    tracks.forEach((track) => {
      if (track.album) {
        const albumKey = `${track.artist}\u0000${track.album}`;
        if (!albumMap.has(albumKey)) {
          albumMap.set(albumKey, {
            title: track.album,
            artist: track.artist,
            coverArtUrl: track.coverArtUrl,
            color: track.color,
            trackCount: 1,
          });
        } else {
          albumMap.get(albumKey)!.trackCount++;
        }
      }
    });
    return [...albumMap.values()]
      .filter(
        (a) =>
          !normalizedQuery ||
          a.title.toLowerCase().includes(normalizedQuery) ||
          a.artist.toLowerCase().includes(normalizedQuery),
      )
      .sort((a, b) => a.title.localeCompare(b.title, undefined, { sensitivity: "base" }));
  }, [tracks, normalizedQuery]);

  const filteredTracks = useMemo(() => {
    return tracks
      .filter((track) => {
        const searchable = `${track.title} ${track.artist} ${track.album ?? ""}`.toLowerCase();
        const matchQuery = !normalizedQuery || searchable.includes(normalizedQuery);
        const matchArtist = !selectedArtist || track.artist === selectedArtist;
        const matchAlbum =
          !selectedAlbum ||
          (track.album === selectedAlbum.title && track.artist === selectedAlbum.artist);
        return matchQuery && matchArtist && matchAlbum;
      })
      .sort((left, right) =>
        left.title.localeCompare(right.title, undefined, { sensitivity: "base" }),
      );
  }, [tracks, normalizedQuery, selectedArtist, selectedAlbum]);

  const pageCount = Math.max(1, Math.ceil(filteredTracks.length / pageSize));
  const currentPage = Math.min(page, pageCount - 1);
  const visibleTracks = filteredTracks.slice(currentPage * pageSize, (currentPage + 1) * pageSize);

  function renderTrack(track: Track) {
    return (
      <div
        key={track.id}
        className="grid grid-cols-[44px_1fr_auto_36px] items-center gap-3 border-b border-line py-3 transition-colors hover:bg-subtle sm:grid-cols-[44px_1fr_76px_90px_36px] sm:gap-4"
      >
        <TrackArtwork
          src={track.coverArtUrl}
          alt={`${track.title} cover art`}
          color={track.color}
          className="size-10"
        />
        <div className="min-w-0">
          <b className="block truncate text-[14px]">{track.title}</b>
          <div className="mt-1 truncate text-[12px] text-muted">
            {track.artist}
            {track.album ? ` · ${track.album}` : ""}
          </div>
        </div>
        <span className="hidden text-[12px] text-muted sm:block">{track.duration}</span>
        <span
          className={`hidden text-[10px] font-bold sm:block ${
            track.status === "FAILED"
              ? "text-red-600"
              : track.status === "INDEXED"
                ? "text-success"
                : "text-muted"
          }`}
        >
          {statusLabel(track.status)}
        </span>
        <div className="relative">
          <button
            type="button"
            className="grid size-7 place-items-center text-muted"
            aria-label={`Actions for ${track.title}`}
            onClick={() => setActiveMenu(activeMenu === track.id ? null : track.id)}
          >
            <MoreIcon />
          </button>
          {activeMenu === track.id && (
            <div
              className="absolute right-0 top-9 z-20 w-44 border border-line bg-canvas py-1 shadow-[0_12px_35px_rgba(0,0,0,.12)]"
              role="menu"
            >
              <button
                type="button"
                role="menuitem"
                className="block w-full px-3 py-2 text-left text-[11px] hover:bg-subtle"
                onClick={() => {
                  setActiveMenu(null);
                  void onReindex(track.id);
                }}
              >
                Reprocess audio
              </button>
              <button
                type="button"
                role="menuitem"
                className="block w-full px-3 py-2 text-left text-[11px] text-red-500 hover:bg-subtle"
                onClick={() => {
                  setActiveMenu(null);
                  void onRemove(track.id);
                }}
              >
                Remove track
              </button>
            </div>
          )}
        </div>
      </div>
    );
  }

  return (
    <section>
      <div className="mx-auto max-w-300 px-6 py-14 lg:px-8">
        <div className="flex flex-col items-start justify-between gap-5 sm:flex-row sm:items-end">
          <div>
            <SectionLabel>Your collection</SectionLabel>
            <h1 className="mt-1 text-[40px] font-bold tracking-[-.045em]">Library</h1>
            <p className="mt-3 text-[14px] text-muted">
              {tracks.length === 0
                ? "Add music to start identifying tracks."
                : `${tracks.length} ${tracks.length === 1 ? "track" : "tracks"} · ${indexedCount} ready`}
              {pendingCount > 0 ? ` · ${pendingCount} being prepared` : ""}
              {failedCount > 0 ? ` · ${failedCount} need attention` : ""}
            </p>
          </div>
          <div className="flex w-full flex-wrap gap-2 sm:w-auto">
            <div
              className="flex h-12 items-center rounded-full border border-line p-1"
              role="group"
              aria-label="Library source"
            >
              {[
                ["PERSONAL", "Personal"],
                ["MTG_JAMENDO", "MTG-Jamendo"],
                ["ALL", "All sources"],
              ].map(([value, label]) => (
                <button
                  key={value}
                  type="button"
                  aria-pressed={origin === value}
                  onClick={() => changeOrigin(value as LibraryOrigin)}
                  className={`h-10 rounded-full px-3 text-[11px] font-semibold transition-colors ${
                    origin === value ? "bg-ink text-canvas" : "text-muted hover:text-ink"
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
            <Button onClick={() => document.getElementById("library-files")?.click()}>
              Add audio
            </Button>
            <Button
              variant="primary"
              onClick={() => document.getElementById("library-folder")?.click()}
            >
              Add folder
            </Button>
            <input
              id="library-files"
              type="file"
              accept="audio/*,.mp3,.wav,.flac"
              multiple
              hidden
              onChange={(event) => event.target.files && void onAddFiles(event.target.files)}
            />
            <input
              id="library-folder"
              type="file"
              accept="audio/*"
              multiple
              hidden
              onChange={(event) => event.target.files && void onAddFiles(event.target.files)}
            />
          </div>
        </div>

        {indexingProgress !== null && (
          <div className="mt-7 bg-subtle px-5 py-4">
            <div className="flex justify-between text-[12px] font-semibold">
              <span>Preparing your library…</span>
              <span>{indexingProgress}%</span>
            </div>
            <div className="mt-3 h-1 bg-line">
              <div
                className="h-full bg-brand transition-[width]"
                style={{ width: `${indexingProgress}%` }}
              />
            </div>
            <div className="mt-3 flex justify-between text-[9px] text-muted">
              <span>Processing audio</span>
              <span>Your tracks will be ready to identify shortly</span>
            </div>
          </div>
        )}

        <div
          className="mt-8 flex gap-6 border-b border-line"
          role="tablist"
          aria-label="Library views"
        >
          {(["tracks", "albums", "artists"] as LibraryViewMode[]).map((v) => (
            <button
              key={v}
              type="button"
              role="tab"
              aria-selected={view === v}
              onClick={() => {
                setView(v);
                setSelectedArtist(null);
                setSelectedAlbum(null);
                setPage(0);
              }}
              className={`pb-3 text-[14px] font-bold capitalize transition-colors border-b-2 ${
                view === v
                  ? "border-brand text-brand"
                  : "border-transparent text-muted hover:text-ink"
              }`}
            >
              {v === "tracks" ? "Tracks" : v === "albums" ? "Albums" : "Artists"}
            </button>
          ))}
        </div>

        <div className="mt-6 flex items-center border-b border-line pb-4">
          <label className="sr-only" htmlFor="library-search">
            Search
          </label>
          <input
            id="library-search"
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setPage(0);
            }}
            className="w-full border-0 bg-transparent text-[14px] outline-none placeholder:text-muted"
            placeholder={selectedArtist || selectedAlbum ? "Search tracks..." : `Search ${view}...`}
          />
          {query && (
            <button
              className="mr-4 text-[11px] font-semibold text-muted hover:text-ink"
              type="button"
              onClick={() => {
                setQuery("");
                setPage(0);
              }}
            >
              Clear
            </button>
          )}
        </div>

        <div className="mt-6">
          {(selectedArtist || selectedAlbum) && (
            <div className="mb-6">
              <button
                type="button"
                onClick={() => {
                  setSelectedArtist(null);
                  setSelectedAlbum(null);
                  setPage(0);
                }}
                className="mb-2 flex items-center gap-1 text-[12px] font-semibold text-brand hover:underline"
              >
                ← Back to {selectedAlbum ? "albums" : "artists"}
              </button>
              <h2 className="text-[28px] font-bold">{selectedArtist || selectedAlbum?.title}</h2>
              {selectedAlbum && <p className="text-[13px] text-muted">{selectedAlbum.artist}</p>}
              <p className="text-[13px] text-muted">
                {filteredTracks.length} {filteredTracks.length === 1 ? "track" : "tracks"}
              </p>
            </div>
          )}

          {!selectedArtist && !selectedAlbum && view === "albums" && (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6">
              {albums.length === 0 ? (
                <div className="col-span-full py-16 text-center text-[14px] font-semibold">
                  No albums found
                </div>
              ) : (
                albums.map((album) => (
                  <button
                    key={`${album.artist}-${album.title}`}
                    type="button"
                    onClick={() => {
                      setSelectedAlbum({ title: album.title, artist: album.artist });
                      setPage(0);
                    }}
                    className="group block min-w-0 text-left focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand"
                  >
                    <div className="relative mb-3 aspect-square w-full overflow-hidden rounded-md border border-line bg-subtle">
                      <TrackArtwork
                        src={album.coverArtUrl}
                        color={album.color ?? "#ccc"}
                        alt={`${album.title} cover art`}
                        className="h-full w-full object-cover transition-transform group-hover:scale-105"
                      />
                    </div>
                    <div className="truncate text-[14px] font-bold" title={album.title}>
                      {album.title}
                    </div>
                    <div className="truncate text-[12px] text-muted" title={album.artist}>
                      {album.artist}
                    </div>
                    <div className="mt-1 text-[11px] text-muted">
                      {album.trackCount} {album.trackCount === 1 ? "track" : "tracks"}
                    </div>
                  </button>
                ))
              )}
            </div>
          )}

          {!selectedArtist && !selectedAlbum && view === "artists" && (
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-6">
              {artists.length === 0 ? (
                <div className="col-span-full py-16 text-center text-[14px] font-semibold">
                  No artists found
                </div>
              ) : (
                artists.map((artist) => (
                  <button
                    key={artist}
                    type="button"
                    onClick={() => {
                      setSelectedArtist(artist);
                      setPage(0);
                    }}
                    className="group block min-w-0 text-center focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand"
                  >
                    <div className="mb-3 flex aspect-square w-full items-center justify-center overflow-hidden rounded-full border border-line bg-subtle shadow-sm transition-transform group-hover:scale-105">
                      <span className="text-4xl font-light text-muted">
                        {artist.charAt(0).toUpperCase()}
                      </span>
                    </div>
                    <div className="truncate text-[14px] font-bold" title={artist}>
                      {artist}
                    </div>
                  </button>
                ))
              )}
            </div>
          )}

          {((view === "tracks" && !selectedArtist && !selectedAlbum) ||
            selectedArtist ||
            selectedAlbum) && (
            <div>
              {visibleTracks.length === 0 ? (
                <div className="border-y border-line py-16 text-center">
                  <p className="text-[14px] font-semibold">No tracks match your search</p>
                </div>
              ) : (
                visibleTracks.map(renderTrack)
              )}

              {filteredTracks.length > 0 && (
                <div className="mt-6 flex items-center justify-between border-t border-line pt-4">
                  <div className="flex items-center gap-3 text-[11px] text-muted">
                    <span>
                      Page {currentPage + 1} of {pageCount}
                    </span>
                    <label className="flex items-center gap-2">
                      <span>Show</span>
                      <select
                        value={pageSize}
                        onChange={(event) => {
                          setPageSize(Number(event.target.value));
                          setPage(0);
                        }}
                        className="border border-line bg-canvas px-2 py-1.5 text-[11px] font-semibold text-ink"
                        aria-label="Tracks per page"
                      >
                        <option value={25}>25</option>
                        <option value={50}>50</option>
                        <option value={100}>100</option>
                      </select>
                      <span>per page</span>
                    </label>
                  </div>
                  <div className="flex gap-2">
                    <button
                      type="button"
                      className="border border-line px-3 py-2 text-[11px] font-semibold disabled:cursor-not-allowed disabled:opacity-40"
                      disabled={currentPage === 0}
                      onClick={() => setPage(currentPage - 1)}
                    >
                      Previous
                    </button>
                    <button
                      type="button"
                      className="border border-line px-3 py-2 text-[11px] font-semibold disabled:cursor-not-allowed disabled:opacity-40"
                      disabled={currentPage === pageCount - 1}
                      onClick={() => setPage(currentPage + 1)}
                    >
                      Next
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
