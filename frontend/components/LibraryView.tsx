import { useState } from "react";
import type { AlbumSummary, ArtistSummary, LibraryPagination, Track } from "@/lib/music";
import { MoreIcon, SearchIcon, UploadIcon, InfoIcon } from "./icons";
import { TrackArtwork } from "./TrackArtwork";
import { Button, SectionLabel, Skeleton, Badge } from "./ui";

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
  if (status === "FAILED") return "Failed";
  return "Waiting";
}

function statusVariant(status: string) {
  if (status === "INDEXED") return "success";
  if (status === "FAILED") return "danger";
  if (status === "PROCESSING") return "warning";
  return "default";
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
    <div className="group grid grid-cols-[2.75rem_1fr_auto_2.25rem] items-center gap-3 border-b border-line py-3 transition-colors hover:bg-subtle sm:grid-cols-[2.75rem_1fr_4.75rem_5.625rem_2.25rem] sm:gap-4">
      <TrackArtwork
        src={track.coverArtUrl}
        alt={`${track.title} cover art`}
        color={track.color}
        className="size-11 rounded-md shadow-sm"
      />
      <div className="min-w-0">
        <b className="block truncate text-sm font-semibold text-ink group-hover:text-brand transition-colors">
          {track.title}
        </b>
        <div className="mt-0.5 truncate text-xs text-muted">
          {track.artist}
          {track.album ? ` · ${track.album}` : ""}
        </div>
      </div>
      <span className="hidden text-xs text-muted font-medium sm:block">{track.duration}</span>
      <span className="hidden sm:flex justify-end">
        {track.status !== "INDEXED" && (
          <Badge variant={statusVariant(track.status)}>{statusLabel(track.status)}</Badge>
        )}
      </span>
      <div className="relative">
        <button
          type="button"
          className="grid size-8 place-items-center text-muted hover:bg-line rounded-full transition-colors"
          aria-label={`Actions for ${track.title}`}
          aria-expanded={menuOpen}
          onClick={() => onMenuChange(menuOpen ? null : track.id)}
        >
          <MoreIcon />
        </button>
        {menuOpen && (
          <>
            <button
              type="button"
              className="fixed inset-0 z-10 cursor-default"
              aria-label="Close track actions"
              onClick={() => onMenuChange(null)}
            />
            <div
              className="absolute right-0 top-10 z-20 w-44 rounded-md border border-line bg-canvas py-1.5 shadow-xl animate-toast-enter"
              role="menu"
            >
              <button
                type="button"
                role="menuitem"
                className="block w-full px-4 py-2 text-left text-xs font-medium text-ink hover:bg-subtle"
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
                className="block w-full px-4 py-2 text-left text-xs font-medium text-red-600 hover:bg-red-50 dark:hover:bg-red-950/30"
                onClick={() => {
                  onMenuChange(null);
                  void onRemove(track.id);
                }}
              >
                Remove track
              </button>
            </div>
          </>
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
    <div className="mt-8 flex flex-col sm:flex-row items-center justify-between border-t border-line pt-6 gap-4">
      <div className="flex items-center gap-4 text-xs text-muted font-medium">
        <span>
          Page {pagination.page + 1} of {pagination.totalPages}
        </span>
        <label className="flex items-center gap-2">
          <span>Show</span>
          <select
            value={pageSize}
            onChange={(event) => onPageSizeChange(Number(event.target.value))}
            className="rounded-md border border-line bg-canvas px-2 py-1 text-xs font-semibold text-ink outline-none focus:border-brand"
            aria-label="Items per page"
          >
            <option value={25}>25</option>
            <option value={50}>50</option>
            <option value={100}>100</option>
          </select>
        </label>
      </div>
      <div className="flex gap-2 w-full sm:w-auto">
        <Button
          variant="secondary"
          size="sm"
          className="flex-1 sm:flex-none"
          disabled={!pagination.hasPrevious}
          onClick={() => onPageChange(pagination.page - 1)}
        >
          Previous
        </Button>
        <Button
          variant="secondary"
          size="sm"
          className="flex-1 sm:flex-none"
          disabled={!pagination.hasNext}
          onClick={() => onPageChange(pagination.page + 1)}
        >
          Next
        </Button>
      </div>
    </div>
  );
}

function Skeletons({ view }: { view: LibraryViewMode }) {
  if (view === "tracks") {
    return (
      <div className="flex flex-col">
        {Array.from({ length: 10 }).map((_, i) => (
          <div key={i} className="flex items-center gap-4 border-b border-line py-3">
            <Skeleton className="size-11 rounded-md" />
            <div className="flex-1 space-y-2">
              <Skeleton className="h-4 w-1/3" />
              <Skeleton className="h-3 w-1/4" />
            </div>
            <Skeleton className="h-4 w-12 hidden sm:block" />
            <Skeleton className="h-5 w-16 rounded-full hidden sm:block" />
            <Skeleton className="size-8 rounded-full" />
          </div>
        ))}
      </div>
    );
  }

  if (view === "albums") {
    return (
      <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
        {Array.from({ length: 10 }).map((_, i) => (
          <div key={i} className="space-y-3">
            <Skeleton className="aspect-square w-full rounded-md" />
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-3 w-1/2" />
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="grid grid-cols-2 gap-6 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
      {Array.from({ length: 10 }).map((_, i) => (
        <div key={i} className="flex flex-col items-center space-y-3">
          <Skeleton className="aspect-square w-full rounded-full" />
          <Skeleton className="h-4 w-2/3" />
          <Skeleton className="h-3 w-1/2" />
        </div>
      ))}
    </div>
  );
}

function EmptyState({ title, message }: { title: string; message?: string }) {
  return (
    <div className="col-span-full mt-4 rounded-xl border-y border-line/50 py-20 text-center">
      <div className="mx-auto mb-4 flex size-16 items-center justify-center rounded-full bg-subtle">
        <InfoIcon className="size-8 text-muted" />
      </div>
      <p className="text-base font-bold">{title}</p>
      {message && <p className="mt-1 text-sm text-muted">{message}</p>}
    </div>
  );
}

function AlbumGrid({
  albums,
  onSelect,
}: {
  albums: AlbumSummary[];
  onSelect: (album: AlbumSummary) => void;
}) {
  if (albums.length === 0)
    return <EmptyState title="No albums found" message="Try adjusting your search or source." />;

  return (
    <div className="grid grid-cols-2 gap-x-6 gap-y-10 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
      {albums.map((album) => (
        <button
          key={`${album.artist}-${album.title}`}
          type="button"
          onClick={() => onSelect(album)}
          className="group block min-w-0 text-left focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand"
        >
          <div className="relative mb-3 aspect-square w-full overflow-hidden rounded-xl border border-line bg-subtle shadow-sm transition-[box-shadow] group-hover:shadow-md">
            <TrackArtwork
              src={album.coverArtUrl}
              color="#64748b"
              alt={`${album.title} cover art`}
              className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
            />
          </div>
          <div
            className="truncate text-[0.9375rem] font-bold text-ink transition-colors group-hover:text-brand"
            title={album.title}
          >
            {album.title}
          </div>
          <div className="mt-0.5 truncate text-[0.8125rem] text-muted" title={album.artist}>
            {album.artist}
          </div>
        </button>
      ))}
    </div>
  );
}

function ArtistGrid({
  artists,
  onSelect,
}: {
  artists: ArtistSummary[];
  onSelect: (artist: ArtistSummary) => void;
}) {
  if (artists.length === 0) return <EmptyState title="No artists found" />;

  return (
    <div className="grid grid-cols-2 gap-x-6 gap-y-10 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
      {artists.map((artist) => (
        <button
          key={artist.name}
          type="button"
          onClick={() => onSelect(artist)}
          className="group block min-w-0 text-center focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand"
        >
          <div className="mx-auto mb-4 flex aspect-square w-full max-w-[10rem] items-center justify-center overflow-hidden rounded-full border border-line bg-subtle shadow-sm transition-transform duration-500 group-hover:scale-105 group-hover:shadow-md">
            <span className="text-5xl font-light text-muted">
              {artist.name.charAt(0).toUpperCase()}
            </span>
          </div>
          <div
            className="truncate text-[0.9375rem] font-bold text-ink transition-colors group-hover:text-brand"
            title={artist.name}
          >
            {artist.name}
          </div>
          <div className="mt-1 text-xs font-medium text-muted">{artist.trackCount} tracks</div>
        </button>
      ))}
    </div>
  );
}

function TrackList({
  tracks,
  activeMenu,
  onMenuChange,
  onReindex,
  onRemove,
}: {
  tracks: Track[];
  activeMenu: string | null;
  onMenuChange: (id: string | null) => void;
  onReindex: (id: string) => Promise<void>;
  onRemove: (id: string) => Promise<void>;
}) {
  if (tracks.length === 0)
    return <EmptyState title="No tracks match your search" message="Try adding more music." />;

  return (
    <div className="flex flex-col">
      {tracks.map((track) => (
        <TrackRow
          key={track.id}
          track={track}
          activeMenu={activeMenu}
          onMenuChange={onMenuChange}
          onReindex={onReindex}
          onRemove={onRemove}
        />
      ))}
    </div>
  );
}

function LibraryResults({
  view,
  selectedArtist,
  selectedAlbum,
  isLoading,
  tracks,
  albums,
  artists,
  activeMenu,
  onSelectAlbum,
  onSelectArtist,
  onMenuChange,
  onReindex,
  onRemove,
}: {
  view: LibraryViewMode;
  selectedArtist: string | null;
  selectedAlbum: SelectedAlbum | null;
  isLoading: boolean;
  tracks: Track[];
  albums: AlbumSummary[];
  artists: ArtistSummary[];
  activeMenu: string | null;
  onSelectAlbum: (album: AlbumSummary) => void;
  onSelectArtist: (artist: ArtistSummary) => void;
  onMenuChange: (id: string | null) => void;
  onReindex: (id: string) => Promise<void>;
  onRemove: (id: string) => Promise<void>;
}) {
  if (isLoading) return <Skeletons view={view} />;
  if (!selectedArtist && !selectedAlbum && view === "albums")
    return <AlbumGrid albums={albums} onSelect={onSelectAlbum} />;
  if (!selectedArtist && !selectedAlbum && view === "artists")
    return <ArtistGrid artists={artists} onSelect={onSelectArtist} />;
  if (view === "tracks" || selectedArtist || selectedAlbum) {
    return (
      <TrackList
        tracks={tracks}
        activeMenu={activeMenu}
        onMenuChange={onMenuChange}
        onReindex={onReindex}
        onRemove={onRemove}
      />
    );
  }
  return null;
}

export function LibraryView({
  tracks,
  albums,
  artists,
  pagination,
  indexingProgress,
  isLoading = false,
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
  isLoading?: boolean;
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
    requestPage(0, "tracks", { artist: undefined, album: album.title });
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
    requestPage(
      0,
      selectedResource,
      {
        artist: selectedArtist ?? undefined,
        album: selectedAlbum?.title,
      },
      nextQuery,
    );
  }

  return (
    <section>
      <div className="mx-4 my-6 max-w-[75rem] rounded-[1.75rem] border border-line bg-subtle px-4 py-8 shadow-[0_1.125rem_3.125rem_rgba(15,23,42,0.08)] sm:mx-auto lg:px-8 dark:border-white/10 dark:bg-[#17171b] dark:shadow-[0_1.125rem_3.125rem_rgba(0,0,0,0.25)]">
        <div className="flex flex-col items-start justify-between gap-6 sm:flex-row sm:items-end">
          <div>
            <SectionLabel>Your collection</SectionLabel>
            <h1 className="mt-2 text-4xl font-extrabold tracking-tight">Library</h1>
            <p className="mt-2 text-sm text-muted">
              {pagination.totalElements === 0 && !isLoading
                ? "Add music to start identifying tracks."
                : `${pagination.totalElements} ${pagination.totalElements === 1 ? "item" : "items"}`}
            </p>
          </div>
          <div className="flex w-full flex-wrap items-center gap-3 sm:w-auto">
            <div
              className="flex h-11 items-center rounded-full bg-subtle p-1 mr-2"
              role="group"
              aria-label="Library source"
            >
              {[
                ["PERSONAL", "Personal"],
                ["MTG_JAMENDO", "MTG-Jamendo"],
                ["ALL", "All"],
              ].map(([value, label]) => (
                <button
                  key={value}
                  type="button"
                  aria-pressed={origin === value}
                  onClick={() => changeOrigin(value as LibraryOrigin)}
                  className={`h-9 rounded-full px-4 text-xs font-semibold transition-[color,background-color,box-shadow] ${
                    origin === value
                      ? "bg-white dark:bg-ink text-ink dark:text-canvas shadow-sm"
                      : "text-muted hover:text-ink"
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>

            <Button
              variant="secondary"
              onClick={() => document.getElementById("library-files")?.click()}
            >
              <UploadIcon className="size-4" />
              Files
            </Button>
            <Button
              variant="primary"
              onClick={() => document.getElementById("library-folder")?.click()}
            >
              <UploadIcon className="size-4" />
              Folder
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
              /* @ts-expect-error directory upload */
              webkitdirectory=""
              directory=""
              onChange={(event) => event.target.files && void onAddFiles(event.target.files)}
            />
          </div>
        </div>

        {indexingProgress !== null && (
          <div className="mt-8 rounded-xl bg-subtle px-6 py-5 border border-line">
            <div className="mb-3 flex justify-between text-[0.8125rem] font-bold text-ink">
              <span className="flex items-center gap-2">
                <InfoIcon className="size-4 text-brand" /> Preparing your library…
              </span>
              <span className="text-brand">{indexingProgress}%</span>
            </div>
            <div className="h-2 rounded-full bg-line overflow-hidden">
              <div
                className="h-full bg-brand transition-[width] duration-300 ease-out"
                style={{ width: `${indexingProgress}%` }}
              />
            </div>
          </div>
        )}

        <div
          className="mt-10 flex gap-2 border-b border-line/50 overflow-x-auto no-scrollbar"
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
              className={`border-b-2 px-5 py-3 text-sm font-bold capitalize transition-colors ${
                view === value
                  ? "border-brand text-brand"
                  : "border-transparent text-muted hover:text-ink hover:border-line"
              }`}
            >
              {value}
            </button>
          ))}
        </div>

        <div className="mt-6 flex items-center rounded-lg border border-line bg-canvas px-4 py-1 focus-within:border-brand focus-within:ring-1 focus-within:ring-brand transition-[border-color,box-shadow] shadow-sm">
          <SearchIcon className="size-5 text-muted mr-2" />
          <label className="sr-only" htmlFor="library-search">
            Search library
          </label>
          <input
            id="library-search"
            value={query}
            onChange={(event) => updateQuery(event.target.value)}
            className="w-full bg-transparent py-3 text-sm outline-none placeholder:text-muted"
            placeholder={selectedArtist || selectedAlbum ? "Search tracks..." : `Search ${view}...`}
          />
          {query && (
            <button
              type="button"
              className="rounded px-2 py-1 text-xs font-semibold text-muted hover:bg-subtle hover:text-ink"
              onClick={() => updateQuery("")}
            >
              Clear
            </button>
          )}
        </div>

        <div className="mt-8">
          {(selectedArtist || selectedAlbum) && (
            <div className="mb-8">
              <button
                type="button"
                onClick={goBack}
                className="mb-4 inline-flex items-center gap-1.5 rounded-full bg-subtle px-3 py-1.5 text-xs font-semibold text-ink transition-colors hover:bg-line"
              >
                ← Back to {selectedAlbum ? "albums" : "artists"}
              </button>
              <h2 className="text-3xl font-extrabold">{selectedArtist || selectedAlbum?.title}</h2>
              {selectedAlbum && (
                <p className="mt-1 text-[0.9375rem] font-medium text-muted">
                  {selectedAlbum.artist}
                </p>
              )}
              <p className="mt-2 text-[0.8125rem] font-medium text-muted">
                {pagination.totalElements} {pagination.totalElements === 1 ? "track" : "tracks"}
              </p>
            </div>
          )}

          <LibraryResults
            view={view}
            selectedArtist={selectedArtist}
            selectedAlbum={selectedAlbum}
            isLoading={isLoading}
            tracks={tracks}
            albums={albums}
            artists={artists}
            activeMenu={activeMenu}
            onSelectAlbum={selectAlbum}
            onSelectArtist={selectArtist}
            onMenuChange={setActiveMenu}
            onReindex={onReindex}
            onRemove={onRemove}
          />

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
