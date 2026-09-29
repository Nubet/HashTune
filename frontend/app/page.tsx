"use client";

import { useState } from "react";
import { Header, type Page } from "@/components/Header";
import { HistoryView } from "@/components/HistoryView";
import { LibraryView } from "@/components/LibraryView";
import { ListenView } from "@/components/ListenView";
import { musicApi, type ApiHistoryItem } from "@/lib/api/musicApi";
import type { ApiTrack, RecognitionResponse } from "@/lib/api/contracts";
import type { Recognition, Track } from "@/lib/music";

const colors = ["#0866F5", "#6EA7F3", "#B2D1F7", "#172A42", "#0D1C2E", "#EEF0F3"];

function colorFor(id: string) {
  const value = [...id].reduce((sum, character) => sum + character.charCodeAt(0), 0);
  return colors[value % colors.length];
}

function formatDuration(durationMs?: number | null) {
  if (!durationMs) return "--:--";
  const seconds = Math.floor(durationMs / 1000);
  return `${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`;
}

function toTrack(track: ApiTrack): Track {
  return {
    id: track.id,
    title: track.title,
    artist: track.artist,
    album: track.album ?? undefined,
    duration: formatDuration(track.durationMs),
    color: colorFor(track.id),
    status: track.status,
  };
}

function toRecognition(response: RecognitionResponse, source: string): Recognition {
  const track = response.track;
  return {
    id: `${Date.now()}-${track?.id ?? "no-match"}`,
    title: track?.title ?? "No match",
    artist: track?.artist ?? "No matching track found",
    score: response.confidence == null ? "--" : `${Math.round(response.confidence * 100)}%`,
    time: "Just now",
    source,
    color: colorFor(track?.id ?? "no-match"),
    matchedAtMs: response.matchedAtMs ?? undefined,
    sampleDurationMs: response.sampleDurationMs ?? undefined,
    recognitionTimeMs: response.recognitionTimeMs ?? undefined,
    status: response.status,
  };
}

function toHistoryItem(item: ApiHistoryItem): Recognition {
  const track = item.track;
  return {
    id: item.id,
    title: track?.title ?? "No match",
    artist: track?.artist ?? "No matching track found",
    score: item.confidence == null ? "--" : `${Math.round(item.confidence * 100)}%`,
    time: new Date(item.createdAt).toLocaleString("pl-PL"),
    source: item.source,
    color: colorFor(track?.id ?? "no-match"),
    status: item.status,
  };
}

export default function Home() {
  const [page, setPage] = useState<Page>("listen");
  const [tracks, setTracks] = useState<Track[]>([]);
  const [history, setHistory] = useState<Recognition[]>([]);
  const [toast, setToast] = useState("");
  const [recognition, setRecognition] = useState<Recognition | null>(null);
  const [indexProgress, setIndexProgress] = useState<number | null>(null);
  const [error, setError] = useState("");

  function showToast(message: string) {
    setToast(message);
    window.setTimeout(() => setToast(""), 1800);
  }

  async function recognize(file: File, source: "MICROPHONE" | "AUDIO_FILE") {
    try {
      setError("");
      const result = toRecognition(await musicApi.recognize(file, source), source);
      setRecognition(result);
      if (result.status === "MATCHED") setHistory((current) => [result, ...current]);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Recognition failed");
    }
  }

  function changePage(nextPage: Page) {
    setPage(nextPage);
    setError("");

    if (nextPage === "library") {
      void musicApi
        .listTracks()
        .then((loadedTracks) => setTracks(loadedTracks.map(toTrack)))
        .catch((reason: unknown) =>
          setError(reason instanceof Error ? reason.message : "Could not load library"),
        );
    }

    if (nextPage === "history") {
      void musicApi
        .history()
        .then((loadedHistory) => setHistory(loadedHistory.map(toHistoryItem)))
        .catch((reason: unknown) =>
          setError(reason instanceof Error ? reason.message : "Could not load history"),
        );
    }
  }

  async function addFiles(files: FileList) {
    try {
      setError("");
      setIndexProgress(0);
      await musicApi.uploadAndIndex(files, setIndexProgress);
      setTracks((await musicApi.listTracks()).map(toTrack));
      showToast("Library updated");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Could not index files");
    } finally {
      setIndexProgress(null);
    }
  }

  async function removeTrack(id: string) {
    try {
      await musicApi.removeTrack(id);
      setTracks((current) => current.filter((track) => track.id !== id));
      showToast("Track removed");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Could not remove track");
    }
  }

  async function reindexTrack(id: string) {
    try {
      await musicApi.reindexTrack(id);
      showToast("Fingerprint regeneration queued");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Could not reindex track");
    }
  }

  async function clearHistory(): Promise<boolean> {
    try {
      await musicApi.clearHistory();
      setHistory([]);
      return true;
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Could not clear history");
      return false;
    }
  }

  return (
    <div className="min-h-screen bg-canvas text-ink">
      <Header page={page} onPageChange={changePage} />
      <main>
        {error && (
          <div className="mx-auto max-w-[1200px] px-6 pt-6 text-[14px] text-red-600 lg:px-8">
            {error}
          </div>
        )}
        {page === "listen" && (
          <ListenView recognition={recognition} onRecognize={recognize} error={error} />
        )}
        {page === "library" && (
          <LibraryView
            tracks={tracks}
            indexingProgress={indexProgress}
            onAddFiles={addFiles}
            onReindex={reindexTrack}
            onRemove={removeTrack}
          />
        )}
        {page === "history" && (
          <HistoryView history={history} onClear={clearHistory} onToast={showToast} />
        )}
      </main>
      {toast && (
        <div className="pointer-events-none fixed bottom-6 left-1/2 z-50 -translate-x-1/2 rounded-full bg-ink px-4 py-2.5 text-[11px] font-semibold text-white animate-enter">
          {toast}
        </div>
      )}
    </div>
  );
}
