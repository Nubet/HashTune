import type { Recognition } from "@/lib/music";
import { SectionLabel, Skeleton, Badge, Button } from "./ui";
import { TrackArtwork } from "./TrackArtwork";
import { MicrophoneRecordingPlayer } from "./MicrophoneRecordingPlayer";
import { InfoIcon } from "./icons";

export function HistoryView({
  history,
  isLoading = false,
  onClear,
  onToast,
}: {
  history: Recognition[];
  isLoading?: boolean;
  onClear: () => Promise<boolean>;
  onToast: (message: string) => void;
}) {
  return (
    <section>
      <div className="mx-4 my-6 max-w-[75rem] rounded-[1.75rem] border border-line bg-subtle px-4 py-8 shadow-[0_1.125rem_3.125rem_rgba(15,23,42,0.08)] sm:mx-auto lg:px-8 dark:border-white/10 dark:bg-[#17171b] dark:shadow-[0_1.125rem_3.125rem_rgba(0,0,0,0.25)]">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
          <div>
            <SectionLabel>Your activity</SectionLabel>
            <h1 className="mt-2 text-4xl font-extrabold tracking-tight">Search history</h1>
          </div>
          <Button
            variant="ghost"
            onClick={async () => {
              if (await onClear()) onToast("History cleared");
            }}
            disabled={isLoading || history.length === 0}
          >
            Clear history
          </Button>
        </div>

        <div className="mt-8">
          {isLoading ? (
            <div className="flex flex-col border-t border-line/50 pt-2">
              {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="flex items-center gap-4 border-b border-line/50 py-4">
                  <Skeleton className="size-12 rounded-md" />
                  <div className="flex-1 space-y-2">
                    <Skeleton className="h-4 w-1/3" />
                    <Skeleton className="h-3 w-1/4" />
                  </div>
                  <Skeleton className="h-4 w-24 hidden md:block" />
                  <Skeleton className="h-4 w-20 hidden md:block" />
                </div>
              ))}
            </div>
          ) : history.length > 0 ? (
            <div className="border-t border-line/50">
              {history.map((item) => (
                <div
                  key={item.id}
                  className="-mx-2 grid gap-3 rounded-lg border-b border-line/50 px-2 py-4 transition-colors hover:bg-subtle md:grid-cols-[3rem_1fr_7.5rem_7.5rem_6.25rem] md:items-center md:gap-4"
                >
                  <TrackArtwork
                    src={item.coverArtUrl}
                    alt={`${item.title} cover art`}
                    seed={`history:${item.id}`}
                    className="size-12 rounded-md shadow-sm"
                  />
                  <div className="min-w-0">
                    <b className="block truncate text-[0.9375rem] font-bold text-ink">
                      {item.title}
                    </b>
                    <div className="mt-0.5 truncate text-[0.8125rem] font-medium text-muted">
                      {item.artist}
                    </div>
                  </div>
                  <span className="text-xs font-semibold text-ink">
                    <Badge variant={item.score.includes("match") ? "success" : "default"}>
                      {item.score}
                    </Badge>
                  </span>
                  <span className="text-xs font-medium text-muted">
                    {item.source === "MICROPHONE" ? "Microphone" : "Audio file"}
                  </span>
                  <span className="text-left text-xs font-medium text-muted md:text-right">
                    {item.time}
                  </span>

                  {item.source === "MICROPHONE" && item.recordingUrl && (
                    <div className="md:col-span-5 md:pl-16 mt-2">
                      <MicrophoneRecordingPlayer
                        src={item.recordingUrl}
                        initialDurationMs={item.sampleDurationMs}
                      />
                      {item.downloadUrl && (
                        <a
                          href={item.downloadUrl}
                          className="mt-3 inline-block text-xs font-bold text-brand transition-colors hover:text-brand-hover"
                          download
                        >
                          Download recording ↓
                        </a>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <div className="py-24 text-center rounded-xl border border-line/50 bg-subtle/50 mt-4">
              <div className="mx-auto size-16 rounded-full bg-canvas shadow-sm flex items-center justify-center mb-4">
                <InfoIcon className="size-8 text-muted" />
              </div>
              <p className="text-base font-bold text-ink">No history yet</p>
              <p className="mt-1 text-sm text-muted">Your identified tracks will appear here.</p>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
