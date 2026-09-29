import { useState } from "react";
import type { Recognition } from "../lib/music";
import { MicIcon, UploadIcon } from "./icons";
import { RecognitionResult } from "./RecognitionResult";
import { Button, SectionLabel } from "./ui";

export function ListenView({
  recognition,
  onRecognize,
}: {
  recognition: Recognition | null;
  onRecognize: (file: File, source: "MICROPHONE" | "AUDIO_FILE") => Promise<void>;
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
        <div className="mx-auto grid min-h-130 max-w-300 grid-cols-1 items-center gap-12 px-6 py-16 lg:grid-cols-[1fr_400px] lg:gap-20 lg:px-8">
          <div>
            <SectionLabel>Local recognition</SectionLabel>
            <h1 className="mt-3 max-w-155 text-[clamp(44px,5vw,60px)] font-bold leading-[.98] tracking-[-.055em]">
              {isListening
                ? "Listening…"
                : recognition?.status === "MATCHED"
                  ? "Song identified"
                  : "What are you listening to?"}
            </h1>
            <p className="mt-6 max-w-140 text-[16px] leading-7 text-muted">
              {isListening
                ? fileName
                  ? "Checking the clip against the library."
                  : "Capturing a short sample from your microphone."
                : recognition?.status === "MATCHED"
                  ? "This track matches something in library."
                  : "Use your microphone or choose an audio clip. HashTune only searches tracks included in Library."}
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Button
                variant="primary"
                onClick={() => void recordMicrophone()}
                disabled={isListening}
              >
                <MicIcon />
                {isListening ? "Checking…" : "Use microphone"}
              </Button>
              <Button
                onClick={() => document.getElementById("sample-input")?.click()}
                disabled={isListening}
              >
                <UploadIcon />
                Choose audio file
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
            {isListening && (
              <div className="mt-5 flex h-6 items-center gap-0.75" aria-label="Recognizing audio">
                {Array.from({ length: 28 }, (_, index) => (
                  <i
                    key={index}
                    className="animate-wave block w-0.5 rounded-full bg-brand"
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
            <div className="animate-orbit absolute inset-4.5 rounded-full border border-transparent border-t-light-blue">
              <i className="absolute -top-0.75 left-1/2 size-1.5 rounded-full bg-brand" />
            </div>
            <div className="logo-disc absolute inset-[21%] grid place-items-center rounded-full">
              <img
                src="/hashtune-logo-blue.svg"
                alt=""
                aria-hidden="true"
                className="logo-mark logo-mark-blue size-24"
              />
              <img
                src="/hashtune-logo-white.svg"
                alt=""
                aria-hidden="true"
                className="logo-mark logo-mark-white size-24"
              />
            </div>
          </div>
        </div>
      </section>
      {recognition && <RecognitionResult recognition={recognition} />}
    </>
  );
}
