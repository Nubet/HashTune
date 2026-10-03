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
    <div className="w-full max-w-sm rounded-xl border border-line bg-canvas p-4 shadow-sm">
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
      <div className="flex items-center gap-4">
        <button
          type="button"
          onClick={() => void togglePlayback()}
          className="grid size-10 shrink-0 place-items-center rounded-full bg-brand text-white transition-[background-color,transform] hover:bg-brand-hover hover:scale-105 active:scale-95"
          aria-label={isPlaying ? "Pause microphone recording" : "Play microphone recording"}
        >
          <span className="text-sm leading-none">{isPlaying ? "Ⅱ" : "▶"}</span>
        </button>
        <div className="min-w-0 flex-1 flex flex-col justify-center">
          <div className="flex items-center justify-between mb-1">
            <div className="mr-2 truncate text-xs font-bold text-ink">Microphone recording</div>
            <span className="shrink-0 text-[0.6875rem] font-medium tabular-nums text-muted">
              {formatTime(currentTime)} / {formatTime(duration)}
            </span>
          </div>

          <input
            type="range"
            min="0"
            max={duration || 0}
            step="0.01"
            value={Math.min(currentTime, duration || 0)}
            onChange={(event) => seek(event.target.value)}
            className="w-full h-1.5 cursor-pointer accent-brand rounded-full bg-line appearance-none"
            aria-label="Seek microphone recording"
            disabled={!duration}
          />
        </div>
      </div>
    </div>
  );
}
