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
      <div className="mx-auto max-w-[1200px] px-4 py-10 lg:px-8">
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
                  className="grid gap-3 border-b border-line/50 py-4 md:grid-cols-[48px_1fr_120px_120px_100px] md:items-center md:gap-4 hover:bg-subtle transition-colors rounded-lg px-2 -mx-2"
                >
                  <TrackArtwork
                    src={item.coverArtUrl}
                    alt={`${item.title} cover art`}
                    color={item.color}
                    className="size-12 rounded-md shadow-sm"
                  />
                  <div className="min-w-0">
                    <b className="block truncate text-[15px] text-ink font-bold">{item.title}</b>
                    <div className="mt-0.5 truncate text-[13px] text-muted font-medium">
                      {item.artist}
                    </div>
                  </div>
                  <span className="text-[12px] font-semibold text-ink">
                    <Badge variant={item.score.includes("match") ? "success" : "default"}>
                      {item.score}
                    </Badge>
                  </span>
                  <span className="text-[12px] font-medium text-muted">
                    {item.source === "MICROPHONE" ? "Microphone" : "Audio file"}
                  </span>
                  <span className="text-left md:text-right text-[12px] font-medium text-muted">
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
                          className="mt-3 inline-block text-[12px] font-bold text-brand hover:text-brand-hover transition-colors"
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
              <p className="text-[16px] font-bold text-ink">No history yet</p>
              <p className="text-[14px] text-muted mt-1">
                Your identified tracks will appear here.
              </p>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
