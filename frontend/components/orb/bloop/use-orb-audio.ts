"use client";

import { useEffect, useRef } from "react";
import { AudioAnalyzer } from "./audio-analyzer";
import type { AudioMode } from "./types";

export function useOrbAudio(
  audioMode: AudioMode | undefined,
  audioElement: React.RefObject<HTMLAudioElement | null> | undefined,
  audioSrc?: string,
  audioStream?: MediaStream,
) {
  const analyzerRef = useRef<AudioAnalyzer | null>(null);

  useEffect(() => {
    const mode = audioMode || (audioStream ? "mic" : "ambient");
    let cancelled = false;
    let timer = 0;

    const release = () => {
      analyzerRef.current?.dispose();
      analyzerRef.current = null;
    };

    async function attachFile() {
      const el = audioElement?.current;
      if (cancelled) return;
      if (!el || (!el.src && !audioSrc)) {
        timer = window.setTimeout(attachFile, 120);
        return;
      }
      if (audioSrc && !el.src) el.src = audioSrc;
      const analyzer = new AudioAnalyzer();
      const ok = await analyzer.initElement(el);
      if (cancelled) {
        analyzer.dispose();
        return;
      }
      if (ok) analyzerRef.current = analyzer;
    }

    async function attachMic() {
      const analyzer = new AudioAnalyzer();
      const ok = await analyzer.initMic(audioStream);
      if (cancelled) {
        analyzer.dispose();
        return;
      }
      if (ok) analyzerRef.current = analyzer;
    }

    release();
    if (mode === "mic") attachMic();
    else if (mode === "file") attachFile();

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
      release();
    };
  }, [audioMode, audioElement, audioSrc, audioStream]);

  return analyzerRef;
}
