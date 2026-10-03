import { useEffect, useState } from "react";
import type { Recognition } from "@/lib/music";
import { MicIcon, UploadIcon } from "./icons";
import { MicrophoneRecordingPlayer } from "./MicrophoneRecordingPlayer";
import { RecognitionResult } from "./RecognitionResult";
import FluidFieldBackground from "./FluidFieldBackground";
import { AudioOrb } from "./AudioOrb";
import { useTheme } from "@/lib/theme";

const RECOGNITION_STAGES_MS = [5_000, 8_000, 12_000] as const;

function supportedMimeType() {
  return ["audio/webm;codecs=opus", "audio/webm"].find((type) =>
    MediaRecorder.isTypeSupported(type),
  );
}

function recordingFile(chunks: Blob[], mimeType: string) {
  return new File([...chunks], "microphone-sample.webm", { type: mimeType || "audio/webm" });
}

function finishRecording(recorder: MediaRecorder, chunks: Blob[]) {
  return new Promise<File>((resolve, reject) => {
    const onData = (event: BlobEvent) => {
      if (event.data.size > 0) chunks.push(event.data);
    };
    const onStop = () => {
      recorder.removeEventListener("dataavailable", onData);
      recorder.removeEventListener("stop", onStop);
      recorder.removeEventListener("error", onError);
      resolve(recordingFile(chunks, recorder.mimeType));
    };
    const onError = () => {
      recorder.removeEventListener("dataavailable", onData);
      recorder.removeEventListener("stop", onStop);
      recorder.removeEventListener("error", onError);
      reject(new Error("Microphone recording failed"));
    };

    recorder.addEventListener("dataavailable", onData);
    recorder.addEventListener("stop", onStop);
    recorder.addEventListener("error", onError);
    recorder.stop();
  });
}

function useObjectUrl(file: File | null) {
  const [url, setUrl] = useState<string>();

  useEffect(() => {
    if (!file) {
      setUrl(undefined);
      return;
    }

    const nextUrl = URL.createObjectURL(file);
    setUrl(nextUrl);
    return () => URL.revokeObjectURL(nextUrl);
  }, [file]);

  return url;
}

type ListenViewProps = {
  recognition: Recognition | null;
  onRecognize: (
    file: File,
    source: "MICROPHONE" | "AUDIO_FILE",
    probe?: boolean,
  ) => Promise<boolean>;
};

function useListenController(onRecognize: ListenViewProps["onRecognize"]) {
  const [isListening, setIsListening] = useState(false);
  const [fileName, setFileName] = useState("");
  const [microphoneStream, setMicrophoneStream] = useState<MediaStream>();
  const [microphoneRecording, setMicrophoneRecording] = useState<File | null>(null);
  const microphoneRecordingUrl = useObjectUrl(microphoneRecording);

  async function recognizeFile(file: File, source: "MICROPHONE" | "AUDIO_FILE") {
    setIsListening(true);
    setFileName(file.name);
    try {
      await onRecognize(file, source);
    } finally {
      setIsListening(false);
    }
  }

  async function recordMicrophone() {
    let stream: MediaStream | undefined;
    let lastSample: File | null = null;
    try {
      setMicrophoneRecording(null);
      stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      setMicrophoneStream(stream);
      const mimeType = supportedMimeType();
      const recorder = mimeType
        ? new MediaRecorder(stream, { mimeType })
        : new MediaRecorder(stream);
      const chunks: Blob[] = [];
      const startedAt = performance.now();
      setIsListening(true);
      recorder.start();

      for (let stage = 0; stage < RECOGNITION_STAGES_MS.length; stage += 1) {
        const remainingMs = RECOGNITION_STAGES_MS[stage] - (performance.now() - startedAt);
        if (remainingMs > 0) {
          await new Promise((resolve) => window.setTimeout(resolve, remainingMs));
        }
        const sample = await new Promise<File>((resolve, reject) => {
          const onData = (event: BlobEvent) => {
            if (event.data.size > 0) chunks.push(event.data);
            recorder.removeEventListener("dataavailable", onData);
            recorder.removeEventListener("error", onError);
            resolve(recordingFile(chunks, recorder.mimeType));
          };
          const onError = () => {
            recorder.removeEventListener("dataavailable", onData);
            recorder.removeEventListener("error", onError);
            reject(new Error("Microphone recording failed"));
          };
          recorder.addEventListener("dataavailable", onData);
          recorder.addEventListener("error", onError);
          try {
            recorder.requestData();
          } catch (error) {
            recorder.removeEventListener("dataavailable", onData);
            recorder.removeEventListener("error", onError);
            reject(error);
          }
        });
        lastSample = sample;
        const matched = await onRecognize(
          sample,
          "MICROPHONE",
          stage < RECOGNITION_STAGES_MS.length - 1,
        );
        if (matched) break;
      }

      if (recorder.state !== "inactive") {
        lastSample = await finishRecording(recorder, chunks);
      }
      setMicrophoneRecording(lastSample);
    } catch (error) {
      setIsListening(false);
      if (error instanceof Error) throw error;
    } finally {
      stream?.getTracks().forEach((track) => track.stop());
      setMicrophoneStream(undefined);
      setIsListening(false);
    }
  }

  return {
    isListening,
    fileName,
    microphoneStream,
    microphoneRecordingUrl,
    recognizeFile,
    recordMicrophone,
  };
}

export function ListenView({ recognition, onRecognize }: ListenViewProps) {
  const {
    isListening,
    fileName,
    microphoneStream,
    microphoneRecordingUrl,
    recognizeFile,
    recordMicrophone,
  } = useListenController(onRecognize);
  const theme = useTheme();
  const heading = isListening
    ? "Listening…"
    : recognition?.status === "MATCHED"
      ? "Song identified"
      : "Discover the music";
  const description = isListening
    ? microphoneStream
      ? "Listening to your microphone. Checking the clip against the library."
      : fileName
        ? "Checking the clip against the library."
        : "Capturing a short sample from your microphone."
    : recognition?.status === "MATCHED"
      ? "This track matches something in your library."
      : "Use your microphone or choose an audio clip to search your local library.";

  return (
    <>
      <section className="relative overflow-hidden bg-canvas text-ink dark:bg-[#0a0a0c] dark:text-white">
        {/* Fluid WebGL background */}
        <div className="absolute inset-0 z-0 opacity-70 mix-blend-normal dark:opacity-80 dark:mix-blend-screen">
          <FluidFieldBackground mode={theme} />
        </div>

        <div className="mx-auto flex min-h-[calc(100vh-4.5rem)] max-w-[62.5rem] flex-col items-center justify-center px-6 py-16 text-center lg:px-8">
          <div className="z-10 flex w-full flex-col items-center">
            <h1 className="mt-3 text-[clamp(2.75rem,6vw,4.5rem)] font-semibold leading-[1.05] tracking-tight text-ink drop-shadow-sm dark:text-white">
              {heading}
            </h1>
            <p className="mt-4 max-w-lg text-[0.9375rem] font-medium leading-relaxed text-muted sm:text-base">
              {description}
            </p>

            {/* Suno-like input bar */}
            <div className="mt-12 flex w-full max-w-[40rem] items-center justify-between rounded-full border border-line bg-subtle p-2 pr-2.5 shadow-[0_0.75rem_2.5rem_rgba(15,23,42,0.08)] backdrop-blur-md transition-[background-color,border-color,box-shadow] focus-within:border-brand focus-within:bg-canvas hover:border-brand/50 dark:border-white/10 dark:bg-white/5 dark:shadow-[0_0.75rem_2.5rem_rgba(0,0,0,0.4)] dark:focus-within:border-white/20 dark:focus-within:bg-white/10 dark:hover:border-white/20 dark:hover:bg-white/[0.07]">
              <button
                onClick={() => document.getElementById("sample-input")?.click()}
                disabled={isListening}
                className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-canvas text-muted transition-colors hover:bg-line hover:text-ink dark:bg-white/5 dark:text-white/70 dark:hover:bg-white/15 dark:hover:text-white"
                title="Upload audio file"
              >
                <UploadIcon className="size-[1.125rem]" />
              </button>

              <div className="flex-1 px-4 text-left text-sm font-medium text-muted sm:text-[0.9375rem]">
                {isListening
                  ? "Listening to your microphone..."
                  : "Upload a file or use your microphone..."}
              </div>

              <button
                onClick={() => void recordMicrophone()}
                disabled={isListening}
                className={`relative isolate flex h-11 shrink-0 items-center justify-center gap-2 overflow-hidden rounded-full border border-transparent bg-clip-padding px-5 text-sm font-semibold text-white outline-none transition-[background-color,box-shadow,transform] sm:text-[0.9375rem] ${
                  isListening
                    ? "bg-white/20 animate-pulse"
                    : "bg-gradient-to-r from-blue-600 to-cyan-500 shadow-[0_0_1.25rem_rgba(59,130,246,0.3)] hover:scale-105 hover:shadow-[0_0_1.5rem_rgba(59,130,246,0.5)]"
                }`}
              >
                <MicIcon className="size-[1.125rem]" />
                {isListening ? "Listening" : "Identify"}
              </button>

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

            {fileName && (
              <div className="mt-6 rounded-full border border-line bg-subtle px-4 py-1.5 text-xs font-bold text-muted shadow-sm backdrop-blur-md">
                {fileName}
              </div>
            )}

            {microphoneRecordingUrl && (
              <div className="mt-6 flex w-full justify-center">
                <MicrophoneRecordingPlayer
                  key={microphoneRecordingUrl}
                  src={microphoneRecordingUrl}
                />
              </div>
            )}
          </div>

          {isListening && microphoneStream && <AudioOrb audioStream={microphoneStream} />}
        </div>
      </section>
      {recognition && <RecognitionResult recognition={recognition} />}
    </>
  );
}
