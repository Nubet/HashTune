import { useState } from "react";
import type { AlbumSummary, ArtistSummary, LibraryPagination, Track } from "../lib/music";
import { MoreIcon } from "./icons";
import { TrackArtwork } from "./TrackArtwork";
import { Button, SectionLabel } from "./ui";

type LibraryViewMode = "tracks" | "albums" | "artists";
type LibraryOrigin = "PERSONAL" | "MTG_JAMENDO" | "ALL";
type SelectedAlbum = { title: string; artist: string };

export type LibraryQuery = {
  resource: LibraryViewMode;
  query: string;
  artist?: string;
  album?: string;
  page: number;
  size: number;
};

function statusLabel(status: string) {
  if (status === "INDEXED") return "Ready";
  if (status === "PROCESSING") return "Preparing";
  if (status === "FAILED") return "Needs attention";
  return "Waiting";
}

function TrackRow({
  track,
  activeMenu,
  onMenuChange,
  onReindex,
  onRemove,
}: {
  track: Track;
  activeMenu: string | null;
  onMenuChange: (id: string | null) => void;
  onReindex: (id: string) => Promise<void>;
  onRemove: (id: string) => Promise<void>;
}) {
  const menuOpen = activeMenu === track.id;

  return (
    <div className="grid grid-cols-[44px_1fr_auto_36px] items-center gap-3 border-b border-line py-3 transition-colors hover:bg-subtle sm:grid-cols-[44px_1fr_76px_90px_36px] sm:gap-4">
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
          aria-expanded={menuOpen}
          onClick={() => onMenuChange(menuOpen ? null : track.id)}
        >
          <MoreIcon />
        </button>
        {menuOpen && (
          <div
            className="absolute right-0 top-9 z-20 w-44 border border-line bg-canvas py-1 shadow-[0_12px_35px_rgba(0,0,0,.12)]"
            role="menu"
          >
            <button
              type="button"
              role="menuitem"
              className="block w-full px-3 py-2 text-left text-[11px] hover:bg-subtle"
              onClick={() => {
                onMenuChange(null);
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
                onMenuChange(null);
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

function LibraryPagination({
  pagination,
  pageSize,
  onPageChange,
  onPageSizeChange,
}: {
  pagination: LibraryPagination;
  pageSize: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (size: number) => void;
}) {
  if (pagination.totalElements === 0) return null;

  return (
    <div className="mt-6 flex items-center justify-between border-t border-line pt-4">
      <div className="flex items-center gap-3 text-[11px] text-muted">
        <span>
          Page {pagination.page + 1} of {pagination.totalPages}
        </span>
        <label className="flex items-center gap-2">
          <span>Show</span>
          <select
            value={pageSize}
            onChange={(event) => onPageSizeChange(Number(event.target.value))}
            className="border border-line bg-canvas px-2 py-1.5 text-[11px] font-semibold text-ink"
            aria-label="Items per page"
          >
            <option value={25}>25</option>
            <option value={50}>50</option>
            <option value={100}>100</option>
          </select>
        </label>
      </div>
      <div className="flex gap-2">
        <button
          type="button"
          className="border border-line px-3 py-2 text-[11px] font-semibold disabled:cursor-not-allowed disabled:opacity-40"
          disabled={!pagination.hasPrevious}
          onClick={() => onPageChange(pagination.page - 1)}
        >
          Previous
        </button>
        <button
          type="button"
          className="border border-line px-3 py-2 text-[11px] font-semibold disabled:cursor-not-allowed disabled:opacity-40"
          disabled={!pagination.hasNext}
          onClick={() => onPageChange(pagination.page + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
}

export function LibraryView({
  tracks,
  albums,
  artists,
  pagination,
  indexingProgress,
  onAddFiles,
  onReindex,
  onRemove,
  origin,
  onOriginChange,
  onQueryChange,
}: {
  tracks: Track[];
  albums: AlbumSummary[];
  artists: ArtistSummary[];
  pagination: LibraryPagination;
  indexingProgress: number | null;
  onAddFiles: (files: FileList) => Promise<void>;
  onReindex: (id: string) => Promise<void>;
  onRemove: (id: string) => Promise<void>;
  origin: LibraryOrigin;
  onOriginChange: (origin: LibraryOrigin) => void;
  onQueryChange: (query: LibraryQuery) => void;
}) {
  const [view, setView] = useState<LibraryViewMode>("tracks");
  const [query, setQuery] = useState("");
  const [selectedArtist, setSelectedArtist] = useState<string | null>(null);
  const [selectedAlbum, setSelectedAlbum] = useState<SelectedAlbum | null>(null);
  const [pageSize, setPageSize] = useState(50);
  const [activeMenu, setActiveMenu] = useState<string | null>(null);

  const selectedResource = selectedArtist || selectedAlbum ? "tracks" : view;

  function requestPage(page: number, resource = selectedResource, filters = {}, nextQuery = query) {
    onQueryChange({
      resource,
      query: nextQuery,
      page,
      size: pageSize,
      artist: selectedArtist ?? undefined,
      album: selectedAlbum?.title,
      ...filters,
    });
  }

  function changeOrigin(nextOrigin: LibraryOrigin) {
    setQuery("");
    setSelectedArtist(null);
    setSelectedAlbum(null);
    setView("tracks");
    onOriginChange(nextOrigin);
  }

  function selectView(nextView: LibraryViewMode) {
    setView(nextView);
    setSelectedArtist(null);
    setSelectedAlbum(null);
    setQuery("");
    onQueryChange({ resource: nextView, query: "", page: 0, size: pageSize });
  }

  function selectAlbum(album: AlbumSummary) {
    const selection = { title: album.title, artist: album.artist };
    setSelectedAlbum(selection);
    requestPage(0, "tracks", { artist: album.artist, album: album.title });
  }

  function selectArtist(artist: ArtistSummary) {
    setSelectedArtist(artist.name);
    requestPage(0, "tracks", { artist: artist.name });
  }

  function goBack() {
    setSelectedArtist(null);
    setSelectedAlbum(null);
    requestPage(0, view, { artist: undefined, album: undefined });
  }

  function updateQuery(nextQuery: string) {
    setQuery(nextQuery);
    requestPage(0, selectedResource, {
      artist: selectedArtist ?? undefined,
      album: selectedAlbum?.title,
    }, nextQuery);
  }

  return (
    <section>
      <div className="mx-auto max-w-300 px-6 py-14 lg:px-8">
        <div className="flex flex-col items-start justify-between gap-5 sm:flex-row sm:items-end">
          <div>
            <SectionLabel>Your collection</SectionLabel>
            <h1 className="mt-1 text-[40px] font-bold tracking-[-.045em]">Library</h1>
            <p className="mt-3 text-[14px] text-muted">
              {pagination.totalElements === 0
                ? "Add music to start identifying tracks."
                : `${pagination.totalElements} ${pagination.totalElements === 1 ? "item" : "items"}`}
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
              accept="audio/*,.mp3,.wav,.flac,.m4a,.ogg"
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
          </div>
        )}

        <div
          className="mt-8 flex gap-6 border-b border-line"
          role="tablist"
          aria-label="Library views"
        >
          {(["tracks", "albums", "artists"] as LibraryViewMode[]).map((value) => (
            <button
              key={value}
              type="button"
              role="tab"
              aria-selected={view === value}
              onClick={() => selectView(value)}
              className={`border-b-2 pb-3 text-[14px] font-bold capitalize transition-colors ${
                view === value
                  ? "border-brand text-brand"
                  : "border-transparent text-muted hover:text-ink"
              }`}
            >
              {value}
            </button>
          ))}
        </div>

        <div className="mt-6 flex items-center border-b border-line pb-4">
          <label className="sr-only" htmlFor="library-search">
            Search library
          </label>
          <input
            id="library-search"
            value={query}
            onChange={(event) => updateQuery(event.target.value)}
            className="w-full border-0 bg-transparent text-[14px] outline-none placeholder:text-muted"
            placeholder={selectedArtist || selectedAlbum ? "Search tracks..." : `Search ${view}...`}
          />
          {query && (
            <button
              type="button"
              className="mr-4 text-[11px] font-semibold text-muted hover:text-ink"
              onClick={() => updateQuery("")}
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
                onClick={goBack}
                className="mb-2 flex items-center gap-1 text-[12px] font-semibold text-brand hover:underline"
              >
                ← Back to {selectedAlbum ? "albums" : "artists"}
              </button>
              <h2 className="text-[28px] font-bold">{selectedArtist || selectedAlbum?.title}</h2>
              {selectedAlbum && <p className="text-[13px] text-muted">{selectedAlbum.artist}</p>}
              <p className="text-[13px] text-muted">
                {pagination.totalElements} {pagination.totalElements === 1 ? "track" : "tracks"}
              </p>
            </div>
          )}

          {!selectedArtist && !selectedAlbum && view === "albums" && (
            <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
              {albums.length === 0 ? (
                <div className="col-span-full py-16 text-center text-[14px] font-semibold">
                  No albums found
                </div>
              ) : (
                albums.map((album) => (
                  <button
                    key={`${album.artist}-${album.title}`}
                    type="button"
                    onClick={() => selectAlbum(album)}
                    className="group block min-w-0 text-left focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand"
                  >
                    <div className="relative mb-3 aspect-square w-full overflow-hidden rounded-md border border-line bg-subtle">
                      <TrackArtwork
                        src={album.coverArtUrl}
                        color="#64748b"
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
            <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
              {artists.length === 0 ? (
                <div className="col-span-full py-16 text-center text-[14px] font-semibold">
                  No artists found
                </div>
              ) : (
                artists.map((artist) => (
                  <button
                    key={artist.name}
                    type="button"
                    onClick={() => selectArtist(artist)}
                    className="group block min-w-0 text-center focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand"
                  >
                    <div className="mb-3 flex aspect-square w-full items-center justify-center overflow-hidden rounded-full border border-line bg-subtle shadow-sm transition-transform group-hover:scale-105">
                      <span className="text-4xl font-light text-muted">
                        {artist.name.charAt(0).toUpperCase()}
                      </span>
                    </div>
                    <div className="truncate text-[14px] font-bold" title={artist.name}>
                      {artist.name}
                    </div>
                    <div className="mt-1 text-[11px] text-muted">
                      {artist.trackCount} tracks · {artist.albumCount} albums
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
              {tracks.length === 0 ? (
                <div className="border-y border-line py-16 text-center">
                  <p className="text-[14px] font-semibold">No tracks match your search</p>
                </div>
              ) : (
                tracks.map((track) => (
                  <TrackRow
                    key={track.id}
                    track={track}
                    activeMenu={activeMenu}
                    onMenuChange={setActiveMenu}
                    onReindex={onReindex}
                    onRemove={onRemove}
                  />
                ))
              )}
            </div>
          )}

          <LibraryPagination
            pagination={pagination}
            pageSize={pageSize}
            onPageChange={(page) => requestPage(page)}
            onPageSizeChange={(size) => {
              setPageSize(size);
              onQueryChange({
                resource: selectedResource,
                query,
                artist: selectedArtist ?? undefined,
                album: selectedAlbum?.title,
                page: 0,
                size,
              });
            }}
          />
        </div>
      </div>
    </section>
  );
}
