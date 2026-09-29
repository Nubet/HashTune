import { useEffect, useState } from "react";
import type { Recognition } from "../lib/music";
import { MicIcon, UploadIcon } from "./icons";
import { MicrophoneRecordingPlayer } from "./MicrophoneRecordingPlayer";
import { RecognitionResult } from "./RecognitionResult";
import { Button, SectionLabel } from "./ui";

const RECOGNITION_STAGES_MS = [5_000, 8_000, 12_000] as const;

function supportedMimeType() {
  return ["audio/webm;codecs=opus", "audio/webm"].find((type) => MediaRecorder.isTypeSupported(type));
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

export function ListenView({
  recognition,
  onRecognize,
}: {
  recognition: Recognition | null;
  onRecognize: (
    file: File,
    source: "MICROPHONE" | "AUDIO_FILE",
    probe?: boolean,
  ) => Promise<boolean>;
}) {
  const [isListening, setIsListening] = useState(false);
  const [fileName, setFileName] = useState("");
  const [microphoneStream, setMicrophoneStream] = useState<MediaStream>();
  const [microphoneRecording, setMicrophoneRecording] = useState<File | null>(null);
  const audioLevel = useAudioLevel(microphoneStream);
  const microphoneRecordingUrl = useObjectUrl(microphoneRecording);
  const waveformLevel = Math.min(1, Math.sqrt(audioLevel) * 1.25);

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
      const recorder = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream);
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
            resolve(
              recordingFile(chunks, recorder.mimeType),
            );
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
                ? microphoneStream
                  ? "Listening to your microphone. Checking the clip against the library."
                  : fileName
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
            {microphoneRecordingUrl && (
              <MicrophoneRecordingPlayer key={microphoneRecordingUrl} src={microphoneRecordingUrl} />
            )}
            {isListening && (
              <div
                className="mt-5 flex h-6 items-center gap-0.75"
                aria-label="Microphone activity"
              >
                {Array.from({ length: 28 }, (_, index) => (
                  <i
                    key={index}
                    className="block w-0.5 rounded-full bg-brand transition-[height,opacity] duration-100"
                    style={{
                      height: `${4 + Math.round(waveformLevel * (10 + (index % 8) * 3))}px`,
                      opacity: `${0.4 + waveformLevel * 0.6}`,
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
