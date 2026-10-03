import { useState } from "react";
import type { Recognition } from "@/lib/music";
import { TrackArtwork } from "./TrackArtwork";

export function RecognitionResult({ recognition }: { recognition: Recognition }) {
  const [detailsOpen, setDetailsOpen] = useState(false);
  const isMatch = recognition.status === "MATCHED";
  const isInvalidAudio = recognition.status === "INVALID_AUDIO";

  return (
    <div className="bg-canvas px-4 py-8">
      <div className="animate-enter mx-auto max-w-300 rounded-[1.75rem] border border-line px-6 py-8 shadow-[0_1.125rem_3.125rem_rgba(15,23,42,0.08)] lg:px-8 dark:bg-[#17171b]">
        <div className="flex items-center gap-5">
          <TrackArtwork
            src={recognition.coverArtUrl}
            alt={`${recognition.title} cover art`}
            color={recognition.color}
            className="grid size-24 shrink-0 place-items-center text-[0.625rem] font-bold tracking-[.15em] text-navy"
          />
          <div>
            <div
              className={`text-[0.6875rem] font-bold uppercase tracking-widest ${isMatch ? "text-success" : "text-muted"}`}
            >
              {isMatch
                ? `Match found · ${recognition.score}`
                : isInvalidAudio
                  ? "Audio not readable"
                  : "No match"}
            </div>
            <div className="mt-1 text-[1.75rem] font-bold tracking-[-.035em]">
              {recognition.title}
            </div>
            <div className="text-sm text-muted">
              {recognition.artist}
              {recognition.album ? ` · ${recognition.album}` : ""}
            </div>
          </div>
          <div className="ml-auto hidden gap-9 sm:flex">
            <div>
              <small className="text-[0.625rem] uppercase text-muted">Matched at</small>
              <div className="text-sm font-semibold">
                {recognition.matchedAtMs
                  ? `${Math.floor(recognition.matchedAtMs / 60000)}:${String(Math.floor(recognition.matchedAtMs / 1000) % 60).padStart(2, "0")}`
                  : "--:--"}
              </div>
            </div>
            <div>
              <small className="text-[0.625rem] uppercase text-muted">Search time</small>
              <div className="text-sm font-semibold">
                {recognition.recognitionTimeMs
                  ? `${(recognition.recognitionTimeMs / 1000).toFixed(2)} s`
                  : "--"}
              </div>
            </div>
          </div>
        </div>
        <button
          className="mt-5 text-[0.625rem] font-bold text-brand"
          onClick={() => setDetailsOpen(!detailsOpen)}
        >
          {detailsOpen ? "Hide details" : "View details"}
        </button>
        {detailsOpen && (
          <div className="mt-4 grid grid-cols-2 gap-y-3 border-t border-line pt-4 text-[0.625rem] sm:grid-cols-4">
            <div>
              <span className="text-muted">Sample length</span>
              <b className="ml-2">
                {recognition.sampleDurationMs
                  ? `${(recognition.sampleDurationMs / 1000).toFixed(1)} s`
                  : "--"}
              </b>
            </div>
            <div>
              <span className="text-muted">Match quality</span>
              <b className="ml-2">{recognition.score}</b>
            </div>
            <div>
              <span className="text-muted">Track length</span>
              <b className="ml-2">
                {recognition.durationMs ? `${(recognition.durationMs / 1000).toFixed(1)} s` : "--"}
              </b>
            </div>
            <div>
              <span className="text-muted">Track position</span>
              <b className="ml-2">
                {recognition.matchedAtMs
                  ? `${Math.floor(recognition.matchedAtMs / 60000)}:${String(Math.floor(recognition.matchedAtMs / 1000) % 60).padStart(2, "0")}`
                  : "--:--"}
              </b>
            </div>
            <div>
              <span className="text-muted">Source</span>
              <b className="ml-2">
                {recognition.source === "MICROPHONE" ? "Microphone" : "Audio file"}
              </b>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
