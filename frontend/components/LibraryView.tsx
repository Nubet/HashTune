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
  const [activeMenu, setActiveMenu] = useState<string | null>(null);
  const indexedCount = tracks.filter((track) => track.status === "INDEXED").length;
  const failedCount = tracks.filter((track) => track.status === "FAILED").length;
  const pendingCount = tracks.length - indexedCount - failedCount;
  const visibleTracks = tracks.filter((track) =>
    `${track.title} ${track.artist}`.toLowerCase().includes(query.toLowerCase()),
  );

  return (
    <section>
      <div className="mx-auto max-w-[1200px] px-6 py-14 lg:px-8">
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
          <div className="flex gap-2">
            <select
              value={origin}
              onChange={(event) => onOriginChange(event.target.value as "PERSONAL" | "MTG_JAMENDO" | "ALL")}
              className="border border-line bg-canvas px-3 text-[12px] font-semibold"
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
            Search title or artist
          </label>
          <input
            id="library-search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            className="w-full border-0 bg-transparent py-4 text-[14px] outline-none placeholder:text-muted"
            placeholder="Search your library"
          />
              <span className="hidden text-[11px] text-muted sm:block">TITLE / ARTIST / ALBUM / DURATION</span>
        </div>
        <div>
          {visibleTracks.map((track) => (
            <div
              key={track.id}
              className="grid grid-cols-[44px_1fr_76px_90px_36px] items-center gap-4 border-b border-line py-3 transition-colors hover:bg-subtle"
            >
              <TrackArtwork
                src={track.coverArtUrl}
                alt={`${track.title} cover art`}
                color={track.color}
                className="size-10"
              />
              <div>
                <b className="text-[14px]">{track.title}</b>
                <div className="mt-1 text-[12px] text-muted">
                  {track.artist}
                  {track.album ? ` · ${track.album}` : ""}
                </div>
              </div>
              <span className="text-[12px] text-muted">{track.duration}</span>
              <span
                className={`text-[10px] font-bold ${track.status === "FAILED" ? "text-red-600" : track.status === "INDEXED" ? "text-success" : "text-muted"}`}
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
          ))}
        </div>
      </div>
    </section>
  );
}
