import { useState } from "react";
import type { Track } from "../lib/music";
import { MoreIcon } from "./icons";
import { Button, SectionLabel } from "./ui";

export function LibraryView({
  tracks,
  indexingProgress,
  onAddFiles,
  onReindex,
  onRemove,
}: {
  tracks: Track[];
  indexingProgress: number | null;
  onAddFiles: (files: FileList) => Promise<void>;
  onReindex: (id: string) => Promise<void>;
  onRemove: (id: string) => Promise<void>;
}) {
  const [query, setQuery] = useState("");
  const [activeMenu, setActiveMenu] = useState<string | null>(null);
  const visibleTracks = tracks.filter((track) =>
    `${track.title} ${track.artist}`.toLowerCase().includes(query.toLowerCase()),
  );

  return (
    <section>
      <div className="mx-auto max-w-[1040px] px-6 py-11">
        <div className="flex flex-col items-start justify-between gap-5 sm:flex-row sm:items-end">
          <div>
            <SectionLabel>Fingerprint database</SectionLabel>
            <h1 className="mt-1 text-[32px] font-bold tracking-[-.045em]">Music Library</h1>
            <p className="mt-2 text-[12px] text-muted">
              {tracks.length} tracks · mock fingerprints · stored in backend
            </p>
          </div>
          <div className="flex gap-2">
            <Button onClick={() => document.getElementById("library-files")?.click()}>
              Add files
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
            <div className="flex justify-between text-[11px] font-semibold">
              <span>Creating fingerprints…</span>
              <span>{indexingProgress}%</span>
            </div>
            <div className="mt-3 h-1 bg-[#dddfe3]">
              <div
                className="h-full bg-brand transition-[width]"
                style={{ width: `${indexingProgress}%` }}
              />
            </div>
            <div className="mt-3 flex justify-between text-[9px] text-[#888]">
              <span>Reading audio</span>
              <span>Spectrogram → peaks → hashes → database</span>
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
            className="w-full border-0 py-3 text-[12px] outline-none placeholder:text-[#aaa]"
            placeholder="Search title or artist"
          />
          <span className="hidden text-[10px] text-[#aaa] sm:block">TITLE / ARTIST / DURATION</span>
        </div>
        <div>
          {visibleTracks.map((track) => (
            <div
              key={track.id}
              className="grid grid-cols-[44px_1fr_76px_90px_36px] items-center gap-4 border-b border-line py-3 transition-colors hover:bg-[#fafafa]"
            >
              <div className="size-10" style={{ background: track.color }} />
              <div>
                <b className="text-[12px]">{track.title}</b>
                <div className="mt-0.5 text-[10px] text-muted">{track.artist}</div>
              </div>
              <span className="text-[10px] text-muted">{track.duration}</span>
              <span className="text-[9px] font-bold text-success">INDEXED</span>
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
                      className="block w-full px-3 py-2 text-left text-[11px] hover:bg-[#f5f5f5]"
                      onClick={() => {
                        setActiveMenu(null);
                        void onReindex(track.id);
                      }}
                    >
                      Regenerate fingerprint
                    </button>
                    <button
                      className="block w-full px-3 py-2 text-left text-[11px] text-red-600 hover:bg-[#f5f5f5]"
                      onClick={() => {
                        setActiveMenu(null);
                        void onRemove(track.id);
                      }}
                    >
                      Remove from library
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
