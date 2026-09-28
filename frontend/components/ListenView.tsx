import { useState } from "react";
import type { Recognition } from "../lib/music";
import { MicIcon, UploadIcon } from "./icons";
import { RecognitionResult } from "./RecognitionResult";
import { Button, SectionLabel } from "./ui";

export function ListenView({
  recognition,
  onRecognize,
  error,
}: {
  recognition: Recognition | null;
  onRecognize: (file: File, source: "MICROPHONE" | "AUDIO_FILE") => Promise<void>;
  error: string;
}) {
  const [isListening, setIsListening] = useState(false);
  const [fileName, setFileName] = useState("");

  async function recognizeFile(file: File, source: "MICROPHONE" | "AUDIO_FILE") {
    setIsListening(true);
    setFileName(file.name);
    await onRecognize(file, source);
    setIsListening(false);
  }

  async function recordMicrophone() {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream);
      const chunks: Blob[] = [];
      recorder.ondataavailable = (event) => chunks.push(event.data);
      recorder.onstop = () => {
        stream.getTracks().forEach((track) => track.stop());
        void recognizeFile(
          new File(chunks, "microphone-sample.webm", { type: "audio/webm" }),
          "MICROPHONE",
        );
      };
      setIsListening(true);
      recorder.start();
      window.setTimeout(() => recorder.stop(), 5000);
    } catch {
      setIsListening(false);
    }
  }

  return (
    <>
      <section className="bg-subtle">
        <div className="mx-auto grid min-h-[520px] max-w-[1200px] grid-cols-1 items-center gap-12 px-6 py-16 lg:grid-cols-[1fr_400px] lg:gap-20 lg:px-8">
          <div>
            <SectionLabel>Local recognition</SectionLabel>
            <h1 className="mt-3 max-w-[620px] text-[clamp(44px,5vw,60px)] font-bold leading-[.98] tracking-[-.055em]">
              {isListening
                ? "Listening…"
                : recognition?.status === "MATCHED"
                  ? "Song identified"
                  : "What song is this?"}
            </h1>
            <p className="mt-6 max-w-[560px] text-[16px] leading-7 text-muted">
              {isListening
                ? fileName
                  ? "Extracting a fingerprint from the selected clip."
                  : "Capturing a short sample from your microphone."
                : recognition?.status === "MATCHED"
                  ? "Strong fingerprint alignment found in your local library."
                  : "Listen through your microphone or identify an audio file against your indexed library."}
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Button
                variant="primary"
                onClick={() => void recordMicrophone()}
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
                  if (file) void recognizeFile(file, "AUDIO_FILE");
                }}
              />
            </div>
            {fileName && <div className="mt-3 text-[12px] text-muted">{fileName}</div>}
            {error && <div className="mt-3 text-[12px] text-red-600">{error}</div>}
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
          <div className="relative mx-auto size-[min(400px,75vw)]">
            <div className="animate-breathe absolute inset-0 rounded-full border border-pale-blue/20" />
            <div className="animate-orbit absolute inset-[18px] rounded-full border border-transparent border-t-light-blue">
              <i className="absolute -top-[3px] left-1/2 size-1.5 rounded-full bg-brand" />
            </div>
            <div className="absolute inset-[21%] grid place-items-center rounded-full bg-canvas shadow-[0_12px_45px_rgba(0,0,0,.08)] dark:shadow-[0_12px_45px_rgba(0,0,0,.5)]">
              <img
                src="/hashtune-logo-blue.svg"
                alt=""
                aria-hidden="true"
                className="size-24 dark:hidden"
              />
              <img
                src="/hashtune-logo-white.svg"
                alt=""
                aria-hidden="true"
                className="size-24 hidden dark:block"
              />
            </div>
          </div>
        </div>
      </section>
      {recognition && <RecognitionResult recognition={recognition} />}
    </>
  );
}
