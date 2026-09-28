import { useState } from "react";
import { MicIcon, UploadIcon } from "./icons";
import { Button, SectionLabel } from "./ui";

type Match = { source: string; fileName?: string };

export function ListenView({ onRecognized }: { onRecognized: (source: string) => void }) {
  const [isListening, setIsListening] = useState(false);
  const [match, setMatch] = useState<Match | null>(null);
  const [fileName, setFileName] = useState("");
  const [detailsOpen, setDetailsOpen] = useState(false);

  function recognize(source: string, name?: string) {
    setIsListening(true);
    setFileName(name ?? "");
    setMatch(null);
    window.setTimeout(() => {
      setIsListening(false);
      setMatch({ source, fileName: name });
      onRecognized(source);
    }, 1500);
  }

  return (
    <>
      <section className="bg-subtle">
        <div className="mx-auto grid min-h-[440px] max-w-[1040px] grid-cols-1 items-center gap-12 px-6 py-14 lg:grid-cols-[1fr_360px] lg:gap-16">
          <div>
            <SectionLabel>Local recognition</SectionLabel>
            <h1 className="mt-3 max-w-[520px] text-[clamp(40px,5vw,49px)] font-bold leading-[.98] tracking-[-.055em]">
              {isListening ? "Listening…" : match ? "Song identified" : "What song is this?"}
            </h1>
            <p className="mt-5 max-w-[480px] text-[14px] leading-6 text-muted">
              {isListening
                ? fileName
                  ? "Extracting a fingerprint from the selected clip."
                  : "Capturing a short sample from your microphone."
                : match
                  ? "Strong fingerprint alignment found in your local library."
                  : "Listen through your microphone or identify an audio file against your indexed library."}
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Button
                variant="primary"
                onClick={() => recognize("Microphone")}
                disabled={isListening}
              >
                <MicIcon />
                {isListening ? "Listening" : "Listen"}
              </Button>
              <Button
                onClick={() => document.getElementById("sample-input")?.click()}
                disabled={isListening}
              >
                <UploadIcon />
                Upload audio clip
              </Button>
              <input
                id="sample-input"
                type="file"
                accept="audio/*,.mp3,.wav,.flac,.m4a,.ogg"
                hidden
                onChange={(event) => {
                  const file = event.target.files?.[0];
                  if (file) recognize("Audio file", file.name);
                }}
              />
            </div>
            {fileName && <div className="mt-3 text-[10px] text-[#888]">{fileName}</div>}
            {isListening && (
              <div className="mt-5 flex h-6 items-center gap-[3px]" aria-label="Recognizing audio">
                {Array.from({ length: 28 }, (_, index) => (
                  <i
                    key={index}
                    className="animate-wave block w-[2px] rounded-full bg-brand"
                    style={{
                      height: `${6 + (index % 8) * 2}px`,
                      animationDelay: `${-index * 0.04}s`,
                    }}
                  />
                ))}
              </div>
            )}
          </div>
          <div className="relative mx-auto size-[min(320px,75vw)]">
            <div className="animate-breathe absolute inset-0 rounded-full border border-[#d5d7dc]" />
            <div className="animate-orbit absolute inset-[18px] rounded-full border border-transparent border-t-[#9cc9ff]">
              <i className="absolute -top-[3px] left-1/2 size-1.5 rounded-full bg-brand" />
            </div>
            <div className="absolute inset-[21%] grid place-items-center rounded-full bg-canvas shadow-[0_12px_45px_rgba(0,0,0,.08)]">
              <svg className="size-20 text-brand" viewBox="0 0 120 120" fill="none">
                <path
                  d="M17 65c12-36 23 31 41-4 18-35 27-37 45 0"
                  stroke="currentColor"
                  strokeWidth="8"
                  strokeLinecap="round"
                />
              </svg>
            </div>
          </div>
        </div>
      </section>
      {match && (
        <div className="border-b border-line bg-canvas">
          <div className="animate-enter mx-auto max-w-[1040px] px-6 py-7">
            <div className="flex items-center gap-5">
              <div className="grid size-20 shrink-0 place-items-center bg-[#e8d25f] text-[9px] font-bold tracking-[.15em]">
                AM
              </div>
              <div>
                <div className="text-[10px] font-bold uppercase tracking-[.1em] text-success">
                  Match found · 96%
                </div>
                <div className="mt-1 text-[23px] font-bold tracking-[-.035em]">
                  Do I Wanna Know?
                </div>
                <div className="text-[12px] text-muted">Arctic Monkeys · AM</div>
              </div>
              <div className="ml-auto hidden gap-9 sm:flex">
                <div>
                  <small className="text-[9px] uppercase text-[#999]">Matched at</small>
                  <div className="text-[12px] font-semibold">02:17</div>
                </div>
                <div>
                  <small className="text-[9px] uppercase text-[#999]">Recognition</small>
                  <div className="text-[12px] font-semibold">0.28 s</div>
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
                  <b className="ml-2">5.0 s</b>
                </div>
                <div>
                  <span className="text-[#999]">Hash matches</span>
                  <b className="ml-2">302</b>
                </div>
                <div>
                  <span className="text-[#999]">Offset cluster</span>
                  <b className="ml-2">287</b>
                </div>
                <div>
                  <span className="text-[#999]">Source</span>
                  <b className="ml-2">{match.source}</b>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}
