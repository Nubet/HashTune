import type { Recognition } from "../lib/music";
import { SectionLabel } from "./ui";
import { TrackArtwork } from "./TrackArtwork";

export function HistoryView({
  history,
  onClear,
  onToast,
}: {
  history: Recognition[];
  onClear: () => Promise<boolean>;
  onToast: (message: string) => void;
}) {
  return (
    <section>
      <div className="mx-auto max-w-[1200px] px-6 py-14 lg:px-8">
        <div className="flex items-end justify-between">
          <div>
            <SectionLabel>Your activity</SectionLabel>
            <h1 className="mt-1 text-[40px] font-bold tracking-[-.045em]">Search history</h1>
          </div>
          <button
            className="text-[12px] font-bold text-brand"
            onClick={async () => {
              if (await onClear()) onToast("History cleared");
            }}
          >
             Clear history
          </button>
        </div>
        <div className="mt-8 border-t border-line">
          {history.length ? (
            history.map((item) => (
              <div
                key={`${item.title}-${item.time}-${item.source}`}
                className="grid grid-cols-[44px_1fr_100px_110px_80px] items-center gap-4 border-b border-line py-4"
              >
                <TrackArtwork
                  src={item.coverArtUrl}
                  alt={`${item.title} cover art`}
                  color={item.color}
                  className="size-10"
                />
                <div>
                  <b className="text-[14px]">{item.title}</b>
                  <div className="text-[12px] text-muted">{item.artist}</div>
                </div>
                <span className="text-[12px]">{item.score}</span>
                <span className="text-[12px] text-muted">
                  {item.source === "MICROPHONE" ? "Microphone" : "Audio file"}
                </span>
                <span className="text-right text-[12px] text-muted">{item.time}</span>
              </div>
            ))
          ) : (
            <div className="py-24 text-center text-[14px] text-muted">
              Your identified tracks will appear here.
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
