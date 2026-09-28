import { useState } from "react";
import type { Recognition } from "../lib/music";

export function RecognitionResult({ recognition }: { recognition: Recognition }) {
  const [detailsOpen, setDetailsOpen] = useState(false);

  return (
    <div className="border-b border-line bg-canvas">
      <div className="animate-enter mx-auto max-w-[1040px] px-6 py-7">
        <div className="flex items-center gap-5">
          <div className="grid size-20 shrink-0 place-items-center bg-[#e8d25f] text-[9px] font-bold tracking-[.15em]">
            AM
          </div>
          <div>
            <div className="text-[10px] font-bold uppercase tracking-[.1em] text-success">
              {recognition.status === "MATCHED" ? `Match found · ${recognition.score}` : "No match"}
            </div>
            <div className="mt-1 text-[23px] font-bold tracking-[-.035em]">{recognition.title}</div>
            <div className="text-[12px] text-muted">{recognition.artist}</div>
          </div>
          <div className="ml-auto hidden gap-9 sm:flex">
            <div>
              <small className="text-[9px] uppercase text-[#999]">Matched at</small>
              <div className="text-[12px] font-semibold">
                {recognition.matchedAtMs
                  ? `${Math.floor(recognition.matchedAtMs / 60000)}:${String(Math.floor(recognition.matchedAtMs / 1000) % 60).padStart(2, "0")}`
                  : "--:--"}
              </div>
            </div>
            <div>
              <small className="text-[9px] uppercase text-[#999]">Recognition</small>
              <div className="text-[12px] font-semibold">
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
              <span className="text-[#999]">Sample</span>
              <b className="ml-2">
                {recognition.sampleDurationMs
                  ? `${(recognition.sampleDurationMs / 1000).toFixed(1)} s`
                  : "--"}
              </b>
            </div>
            <div>
              <span className="text-[#999]">Hash matches</span>
              <b className="ml-2">--</b>
            </div>
            <div>
              <span className="text-[#999]">Offset cluster</span>
              <b className="ml-2">--</b>
            </div>
            <div>
              <span className="text-[#999]">Source</span>
              <b className="ml-2">{recognition.source}</b>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
