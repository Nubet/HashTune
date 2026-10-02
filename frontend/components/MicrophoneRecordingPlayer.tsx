import { useRef, useState } from "react";

function formatTime(seconds: number) {
  if (!Number.isFinite(seconds)) return "0:00";
  const minutes = Math.floor(seconds / 60);
  const remainder = Math.floor(seconds % 60);
  return `${minutes}:${String(remainder).padStart(2, "0")}`;
}

export function MicrophoneRecordingPlayer({
  src,
  initialDurationMs,
}: {
  src: string;
  initialDurationMs?: number;
}) {
  const audioRef = useRef<HTMLAudioElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(
    initialDurationMs && initialDurationMs > 0 ? initialDurationMs / 1000 : 0,
  );
  const progress = duration > 0 ? (currentTime / duration) * 100 : 0;

  function updateDuration(audio: HTMLAudioElement) {
    if (Number.isFinite(audio.duration) && audio.duration > 0) {
      setDuration(audio.duration);
      return;
    }

    const restoreTime = audio.currentTime;
    const onTimeUpdate = () => {
      if (Number.isFinite(audio.duration) && audio.duration > 0) {
        setDuration(audio.duration);
        audio.currentTime = restoreTime;
        audio.removeEventListener("timeupdate", onTimeUpdate);
      }
    };
    audio.addEventListener("timeupdate", onTimeUpdate);
    audio.currentTime = 1e101;
  }

  async function togglePlayback() {
    const audio = audioRef.current;
    if (!audio) return;

    if (audio.paused) {
      await audio.play();
    } else {
      audio.pause();
    }
  }

  function seek(value: string) {
    const audio = audioRef.current;
    const nextTime = Number(value);
    if (!audio || !Number.isFinite(nextTime)) return;
    audio.currentTime = nextTime;
    setCurrentTime(nextTime);
  }

  return (
    <div className="mt-5 max-w-120 overflow-hidden rounded-2xl border border-line bg-canvas shadow-[0_14px_30px_rgb(7_17_29/0.07)]">
      <audio
        ref={audioRef}
        src={src}
        preload="metadata"
        className="hidden"
        onLoadedMetadata={(event) => updateDuration(event.currentTarget)}
        onDurationChange={(event) => {
          if (Number.isFinite(event.currentTarget.duration) && event.currentTarget.duration > 0) {
            setDuration(event.currentTarget.duration);
          }
        }}
        onTimeUpdate={(event) => setCurrentTime(event.currentTarget.currentTime)}
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
        onEnded={(event) => {
          setIsPlaying(false);
          event.currentTarget.currentTime = 0;
          setCurrentTime(0);
        }}
      />
      <div className="flex items-center gap-3 px-4 pb-3 pt-4">
        <button
          type="button"
          onClick={() => void togglePlayback()}
          className="grid size-11 shrink-0 place-items-center rounded-full bg-brand text-white shadow-[0_8px_18px_rgb(8_102_245/0.28)] transition-transform hover:scale-105 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand active:scale-95"
          aria-label={isPlaying ? "Pause microphone recording" : "Play microphone recording"}
        >
          <span className="text-[15px] leading-none">{isPlaying ? "Ⅱ" : "▶"}</span>
        </button>
        <div className="min-w-0 flex-1">
          <div className="flex items-center justify-between gap-3">
            <div>
              <div className="text-[11px] font-semibold uppercase tracking-[0.14em] text-muted">
                Microphone recording
              </div>
              <div className="mt-1 text-[13px] font-medium text-ink">
                {isPlaying ? "Playing captured sample" : "Review captured sample"}
              </div>
            </div>
            <span className="rounded-full bg-subtle px-2 py-1 text-[11px] font-medium tabular-nums text-muted">
              {formatTime(duration)}
            </span>
          </div>
        </div>
      </div>
      <div className="px-4 pb-4">
        <div className="mb-2 flex h-5 items-center gap-0.75" aria-hidden="true">
          {Array.from({ length: 32 }, (_, index) => (
            <span
              key={index}
              className="w-0.5 rounded-full bg-brand/30"
              style={{ height: `${5 + ((index * 7) % 12)}px` }}
            />
          ))}
        </div>
        <input
          type="range"
          min="0"
          max={duration || 0}
          step="0.01"
          value={Math.min(currentTime, duration || 0)}
          onChange={(event) => seek(event.target.value)}
          className="h-1.5 w-full cursor-pointer accent-brand"
          style={{
            background: `linear-gradient(to right, var(--brand-primary-blue) ${progress}%, var(--border-line) ${progress}%)`,
          }}
          aria-label="Seek microphone recording"
          disabled={!duration}
        />
        <div className="mt-1 flex justify-between text-[11px] tabular-nums text-muted">
          <span>{formatTime(currentTime)}</span>
          <span>{formatTime(duration)}</span>
        </div>
      </div>
    </div>
  );
}
