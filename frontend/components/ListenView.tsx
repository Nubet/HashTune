import { useEffect, useState } from "react";
import type { Recognition } from "@/lib/music";
import { MicIcon, UploadIcon } from "./icons";
import { MicrophoneRecordingPlayer } from "./MicrophoneRecordingPlayer";
import { RecognitionResult } from "./RecognitionResult";
import { Button, SectionLabel } from "./ui";

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

function useAudioLevel(stream: MediaStream | undefined) {
  const [level, setLevel] = useState(0);

  useEffect(() => {
    if (!stream) {
      setLevel(0);
      return;
    }

    const context = new AudioContext();
    const source = context.createMediaStreamSource(stream);
    const analyser = context.createAnalyser();
    const samples = new Uint8Array(256);
    let frame = 0;

    analyser.fftSize = 256;
    source.connect(analyser);
    void context.resume();

    const measure = () => {
      analyser.getByteTimeDomainData(samples);
      const rms = Math.sqrt(
        samples.reduce((sum, sample) => sum + (sample - 128) ** 2, 0) / samples.length,
      );
      setLevel(Math.min(1, rms / 32));
      frame = requestAnimationFrame(measure);
    };

    measure();
    return () => {
      cancelAnimationFrame(frame);
      source.disconnect();
      analyser.disconnect();
      void context.close();
    };
  }, [stream]);

  return level;
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
  const audioLevel = useAudioLevel(microphoneStream);
  const microphoneRecordingUrl = useObjectUrl(microphoneRecording);
  const waveformLevel = Math.min(1, Math.sqrt(audioLevel) * 1.5);

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
    waveformLevel,
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
    waveformLevel,
    recognizeFile,
    recordMicrophone,
  } = useListenController(onRecognize);

  return (
    <>
      <section className="bg-canvas overflow-hidden border-b border-line/40">
        <div className="mx-auto grid min-h-100 max-w-250 grid-cols-1 items-center gap-10 px-6 py-10 lg:grid-cols-[1fr_380px] lg:gap-12 lg:py-16 lg:px-8">
          <div className="flex flex-col items-center lg:items-start text-center lg:text-left z-10">
            <SectionLabel>Local recognition</SectionLabel>
            <h1 className="mt-2 text-4xl sm:text-5xl font-extrabold tracking-tight text-ink leading-[1.1]">
              {isListening
                ? "Listening…"
                : recognition?.status === "MATCHED"
                  ? "Song identified"
                  : "Identify music"}
            </h1>
            <p className="mt-4 max-w-sm text-[15px] leading-relaxed text-muted font-medium">
              {isListening
                ? microphoneStream
                  ? "Listening to your microphone. Checking the clip against the library."
                  : fileName
                    ? "Checking the clip against the library."
                    : "Capturing a short sample from your microphone."
                : recognition?.status === "MATCHED"
                  ? "This track matches something in your library."
                  : "Use your microphone or choose an audio clip to search your local library."}
            </p>
            <div className="mt-8 flex flex-col sm:flex-row gap-4 w-full sm:w-auto">
              <Button
                variant="primary"
                size="md"
                onClick={() => void recordMicrophone()}
                disabled={isListening}
                className={isListening ? "animate-pulse" : "shadow-md shadow-brand/20"}
              >
                <MicIcon className="size-4" />
                {isListening ? "Checking…" : "Use microphone"}
              </Button>
              <Button
                size="md"
                onClick={() => document.getElementById("sample-input")?.click()}
                disabled={isListening}
              >
                <UploadIcon className="size-4" />
                Upload file
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
            {fileName && (
              <div className="mt-4 px-3 py-1.5 rounded-full bg-subtle text-[11px] font-bold text-muted border border-line shadow-sm">
                {fileName}
              </div>
            )}
            {microphoneRecordingUrl && (
              <div className="mt-6 w-full flex lg:justify-start justify-center">
                <MicrophoneRecordingPlayer
                  key={microphoneRecordingUrl}
                  src={microphoneRecordingUrl}
                />
              </div>
            )}
          </div>

          <div className="relative mx-auto flex size-72 items-center justify-center mt-6 lg:mt-0">
            <div
              className={`absolute inset-16 rounded-full border ${isListening ? "border-brand/60" : "border-brand/20"} animate-ripple`}
            />
            <div
              className={`absolute inset-16 rounded-full border ${isListening ? "border-brand/50" : "border-brand/20"} animate-ripple`}
              style={{ animationDelay: "1.33s" }}
            />
            <div
              className={`absolute inset-16 rounded-full border ${isListening ? "border-brand/40" : "border-brand/20"} animate-ripple`}
              style={{ animationDelay: "2.66s" }}
            />

            <div
              className={`logo-disc relative z-20 flex size-28 items-center justify-center rounded-full transition-transform duration-75 ease-out bg-canvas shadow-[0_4px_20px_rgba(0,0,0,0.05)] border border-line/40 ${isListening ? "shadow-[0_0_30px_rgba(59,130,246,0.2)]" : ""}`}
              style={isListening ? { transform: `scale(${1 + waveformLevel * 0.15})` } : undefined}
            >
              <img
                src="/hashtune-logo-blue.svg"
                alt=""
                aria-hidden="true"
                className="logo-mark logo-mark-blue size-14"
              />
              <img
                src="/hashtune-logo-white.svg"
                alt=""
                aria-hidden="true"
                className="logo-mark logo-mark-white size-14"
              />
            </div>
          </div>
        </div>
      </section>
      {recognition && <RecognitionResult recognition={recognition} />}
    </>
  );
}
