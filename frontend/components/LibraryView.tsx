import { useState } from "react";
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

function compareTracks(left: Track, right: Track, sortBy: "title" | "artist" | "album") {
  const leftValue =
    (sortBy === "title" ? left.title : sortBy === "artist" ? left.artist : left.album) ?? "";
  const rightValue =
    (sortBy === "title" ? right.title : sortBy === "artist" ? right.artist : right.album) ?? "";
  return leftValue.localeCompare(rightValue, undefined, { sensitivity: "base" });
}

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
  origin: "PERSONAL" | "MTG_JAMENDO" | "ALL";
  onOriginChange: (origin: "PERSONAL" | "MTG_JAMENDO" | "ALL") => void;
}) {
  const [query, setQuery] = useState("");
  const [selectedArtist, setSelectedArtist] = useState("ALL");
  const [selectedAlbum, setSelectedAlbum] = useState("ALL");
  const [sortBy, setSortBy] = useState<"title" | "artist" | "album">("title");
  const [groupBy, setGroupBy] = useState<"none" | "artist" | "album">("none");
  const [pageSize, setPageSize] = useState(50);
  const [page, setPage] = useState(0);
  const [activeMenu, setActiveMenu] = useState<string | null>(null);
  const indexedCount = tracks.filter((track) => track.status === "INDEXED").length;
  const failedCount = tracks.filter((track) => track.status === "FAILED").length;
  const pendingCount = tracks.length - indexedCount - failedCount;
  const artists = [...new Set(tracks.map((track) => track.artist).filter(Boolean))].sort(
    (left, right) => left.localeCompare(right, undefined, { sensitivity: "base" }),
  );
  const albums = [
    ...new Set(
      tracks
        .filter((track) => selectedArtist === "ALL" || track.artist === selectedArtist)
        .map((track) => track.album)
        .filter((album): album is string => Boolean(album)),
    ),
  ].sort((left, right) => left.localeCompare(right, undefined, { sensitivity: "base" }));
  const normalizedQuery = query.trim().toLowerCase();
  const filteredTracks = tracks
    .filter((track) => {
      const searchable = `${track.title} ${track.artist} ${track.album ?? ""}`.toLowerCase();
      return (
        (!normalizedQuery || searchable.includes(normalizedQuery)) &&
        (selectedArtist === "ALL" || track.artist === selectedArtist) &&
        (selectedAlbum === "ALL" || track.album === selectedAlbum)
      );
    })
    .sort((left, right) => compareTracks(left, right, sortBy));
  const pageCount = Math.max(1, Math.ceil(filteredTracks.length / pageSize));
  const currentPage = Math.min(page, pageCount - 1);
  const visibleTracks = filteredTracks.slice(currentPage * pageSize, (currentPage + 1) * pageSize);
  const groupedTracks = visibleTracks.reduce<Record<string, Track[]>>((groups, track) => {
    const group = groupBy === "artist" ? track.artist : (track.album ?? "Unknown album");
    (groups[group] ??= []).push(track);
    return groups;
  }, {});
  const resetPage = () => setPage(0);
  const clearFilters = () => {
    setQuery("");
    setSelectedArtist("ALL");
    setSelectedAlbum("ALL");
    setSortBy("title");
    setGroupBy("none");
    resetPage();
  };

  function changeOrigin(nextOrigin: "PERSONAL" | "MTG_JAMENDO" | "ALL") {
    clearFilters();
    onOriginChange(nextOrigin);
  }

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
          className={`hidden text-[10px] font-bold sm:block ${track.status === "FAILED" ? "text-red-600" : track.status === "INDEXED" ? "text-success" : "text-muted"}`}
        >
          {statusLabel(track.status)}
        </span>
        <div className="relative">
          <button
            className="grid size-7 place-items-center text-muted"
            aria-label={`Actions for ${track.title}`}
            onClick={() => setActiveMenu(activeMenu === track.id ? null : track.id)}
          >
            <MoreIcon />
          </button>
          {activeMenu === track.id && (
            <div className="absolute right-0 top-9 z-20 w-44 border border-line bg-canvas py-1 shadow-[0_12px_35px_rgba(0,0,0,.12)]">
              <button
                className="block w-full px-3 py-2 text-left text-[11px] hover:bg-subtle"
                onClick={() => {
                  setActiveMenu(null);
                  void onReindex(track.id);
                }}
              >
                Reprocess audio
              </button>
              <button
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
            <select
              value={origin}
              onChange={(event) =>
                changeOrigin(event.target.value as "PERSONAL" | "MTG_JAMENDO" | "ALL")
              }
              className="h-12 border border-line bg-canvas px-3 text-[12px] font-semibold"
              aria-label="Library source"
            >
              <option value="PERSONAL">Personal</option>
              <option value="MTG_JAMENDO">MTG-Jamendo</option>
              <option value="ALL">All sources</option>
            </select>
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
        <div className="mt-8 flex items-center border-b border-line">
          <label className="sr-only" htmlFor="library-search">
            Search title, artist or album
          </label>
          <input
            id="library-search"
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              resetPage();
            }}
            className="w-full border-0 bg-transparent py-4 text-[14px] outline-none placeholder:text-muted"
            placeholder="Search title, artist or album"
          />
          {query && (
            <button
              className="mr-4 text-[11px] font-semibold text-muted hover:text-ink"
              onClick={() => {
                setQuery("");
                resetPage();
              }}
            >
              Clear
            </button>
          )}
        </div>
        <div className="mt-5 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
          <select
            value={selectedArtist}
            onChange={(event) => {
              setSelectedArtist(event.target.value);
              setSelectedAlbum("ALL");
              resetPage();
            }}
            className="h-11 border border-line bg-canvas px-3 text-[12px] font-semibold"
            aria-label="Filter by artist"
          >
            <option value="ALL">All artists</option>
            {artists.map((artist) => (
              <option key={artist} value={artist}>
                {artist}
              </option>
            ))}
          </select>
          <select
            value={selectedAlbum}
            onChange={(event) => {
              setSelectedAlbum(event.target.value);
              resetPage();
            }}
            className="h-11 border border-line bg-canvas px-3 text-[12px] font-semibold"
            aria-label="Filter by album"
          >
            <option value="ALL">All albums</option>
            {albums.map((album) => (
              <option key={album} value={album}>
                {album}
              </option>
            ))}
          </select>
          <select
            value={sortBy}
            onChange={(event) => {
              setSortBy(event.target.value as "title" | "artist" | "album");
              resetPage();
            }}
            className="h-11 border border-line bg-canvas px-3 text-[12px] font-semibold"
            aria-label="Sort tracks"
          >
            <option value="title">Sort: title</option>
            <option value="artist">Sort: artist</option>
            <option value="album">Sort: album</option>
          </select>
          <select
            value={groupBy}
            onChange={(event) => {
              setGroupBy(event.target.value as "none" | "artist" | "album");
              resetPage();
            }}
            className="h-11 border border-line bg-canvas px-3 text-[12px] font-semibold"
            aria-label="Group tracks"
          >
            <option value="none">View: all tracks</option>
            <option value="artist">View: by artist</option>
            <option value="album">View: by album</option>
          </select>
        </div>
        <div className="mt-6 flex flex-wrap items-center justify-between gap-3 text-[11px] text-muted">
          <span>
            {filteredTracks.length} {filteredTracks.length === 1 ? "track" : "tracks"} found
            {filteredTracks.length !== tracks.length ? ` of ${tracks.length}` : ""}
          </span>
          {(query ||
            selectedArtist !== "ALL" ||
            selectedAlbum !== "ALL" ||
            sortBy !== "title" ||
            groupBy !== "none") && (
            <button className="font-semibold text-ink hover:text-brand" onClick={clearFilters}>
              Reset filters
            </button>
          )}
        </div>
        <div className="mt-2">
          {visibleTracks.length === 0 ? (
            <div className="border-y border-line py-16 text-center">
              <p className="text-[14px] font-semibold">No tracks match these filters</p>
              <button className="mt-3 text-[12px] font-semibold text-brand" onClick={clearFilters}>
                Reset filters
              </button>
            </div>
          ) : groupBy === "none" ? (
            visibleTracks.map(renderTrack)
          ) : (
            Object.entries(groupedTracks).map(([group, groupTracks]) => (
              <section key={group} className="border-b border-line py-4">
                <h2 className="mb-1 text-[12px] font-bold uppercase tracking-[.12em] text-muted">
                  {group}
                </h2>
                {groupTracks.map(renderTrack)}
              </section>
            ))
          )}
        </div>
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
                    resetPage();
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
                className="border border-line px-3 py-2 text-[11px] font-semibold disabled:cursor-not-allowed disabled:opacity-40"
                disabled={currentPage === 0}
                onClick={() => setPage(currentPage - 1)}
              >
                Previous
              </button>
              <button
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
    </section>
  );
}
