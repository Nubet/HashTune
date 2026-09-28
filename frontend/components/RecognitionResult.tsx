import { useState } from "react";
import type { Recognition } from "../lib/music";

export function RecognitionResult({ recognition }: { recognition: Recognition }) {
  const [detailsOpen, setDetailsOpen] = useState(false);

  return (
    <div className="border-b border-line bg-canvas">
      <div className="animate-enter mx-auto max-w-[1200px] px-6 py-9 lg:px-8">
        <div className="flex items-center gap-5">
          <div
            className="grid size-24 shrink-0 place-items-center text-[10px] font-bold tracking-[.15em] text-navy"
            style={{ background: recognition.color }}
          >
            AM
          </div>
          <div>
            <div className="text-[11px] font-bold uppercase tracking-[.1em] text-success">
              {recognition.status === "MATCHED" ? `Match found · ${recognition.score}` : "No match"}
            </div>
            <div className="mt-1 text-[28px] font-bold tracking-[-.035em]">{recognition.title}</div>
            <div className="text-[14px] text-muted">{recognition.artist}</div>
          </div>
          <div className="ml-auto hidden gap-9 sm:flex">
            <div>
              <small className="text-[10px] uppercase text-muted">Matched at</small>
              <div className="text-[14px] font-semibold">
                {recognition.matchedAtMs
                  ? `${Math.floor(recognition.matchedAtMs / 60000)}:${String(Math.floor(recognition.matchedAtMs / 1000) % 60).padStart(2, "0")}`
                  : "--:--"}
              </div>
            </div>
            <div>
              <small className="text-[10px] uppercase text-muted">Recognition</small>
              <div className="text-[14px] font-semibold">
                {recognition.recognitionTimeMs
                  ? `${(recognition.recognitionTimeMs / 1000).toFixed(2)} s`
                  : "--"}
              </div>
            </div>
          </div>
        </div>
        <button
          className="mt-5 text-[10px] font-bold text-brand"
          onClick={() => setDetailsOpen(!detailsOpen)}
        >
          {detailsOpen ? "Hide match details" : "Show match details"}
        </button>
        {detailsOpen && (
          <div className="mt-4 grid grid-cols-2 gap-y-3 border-t border-line pt-4 text-[10px] sm:grid-cols-4">
            <div>
              <span className="text-muted">Sample</span>
              <b className="ml-2">
                {recognition.sampleDurationMs
                  ? `${(recognition.sampleDurationMs / 1000).toFixed(1)} s`
                  : "--"}
              </b>
            </div>
            <div>
              <span className="text-muted">Hash matches</span>
              <b className="ml-2">--</b>
            </div>
            <div>
              <span className="text-muted">Offset cluster</span>
              <b className="ml-2">--</b>
            </div>
            <div>
              <span className="text-muted">Source</span>
              <b className="ml-2">{recognition.source}</b>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
