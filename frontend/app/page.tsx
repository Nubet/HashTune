"use client";

import { useRef, useState } from "react";
import { Header, type Page } from "@/components/Header";
import { HistoryView } from "@/components/HistoryView";
import { LibraryView, type LibraryQuery } from "@/components/LibraryView";
import { ListenView } from "@/components/ListenView";
import { MetadataReview } from "@/components/MetadataReview";
import { musicApi, type ApiHistoryItem } from "@/lib/api/musicApi";
import type { ApiTrack, RecognitionResponse } from "@/lib/api/contracts";
import type {
  AlbumSummary,
  ArtistSummary,
  LibraryPagination,
  MetadataDraft,
  Recognition,
  Track,
} from "@/lib/music";

const colors = ["#0866F5", "#6EA7F3", "#B2D1F7", "#172A42", "#0D1C2E", "#EEF0F3"];
const pendingRemovalsStorageKey = "hashtune.pending-removals";
let pendingRemovalIds = readPendingRemovalIds();
const emptyPagination: LibraryPagination = {
  page: 0,
  size: 50,
  totalElements: 0,
  totalPages: 0,
  hasNext: false,
  hasPrevious: false,
};
const initialLibraryQuery: LibraryQuery = {
  resource: "tracks",
  query: "",
  page: 0,
  size: 50,
};

function readPendingRemovalIds() {
  if (typeof window === "undefined") return new Set<string>();
  try {
    return new Set<string>(
      JSON.parse(window.sessionStorage.getItem(pendingRemovalsStorageKey) ?? "[]"),
    );
  } catch {
    return new Set<string>();
  }
}

function persistPendingRemovalIds(ids: Set<string>) {
  if (typeof window !== "undefined") {
    window.sessionStorage.setItem(pendingRemovalsStorageKey, JSON.stringify([...ids]));
  }
}

function colorFor(id: string) {
  const value = [...id].reduce((sum, character) => sum + character.charCodeAt(0), 0);
  return colors[value % colors.length];
}

function formatDuration(durationMs?: number | null) {
  if (!durationMs) return "--:--";
  const seconds = Math.floor(durationMs / 1000);
  return `${String(Math.floor(seconds / 60)).padStart(2, "0")}:${String(seconds % 60).padStart(2, "0")}`;
}

function matchLabel(status: string) {
  return status === "MATCHED" ? "Strong match" : "--";
}

function toTrack(track: ApiTrack): Track {
  return {
    id: track.id,
    title: track.title,
    artist: track.artist,
    album: track.album ?? undefined,
    origin: track.origin,
    albumArtist: track.albumArtist ?? undefined,
    composer: track.composer ?? undefined,
    genre: track.genre ?? undefined,
    releaseYear: track.releaseYear ?? undefined,
    trackNumber: track.trackNumber ?? undefined,
    discNumber: track.discNumber ?? undefined,
    isrc: track.isrc ?? undefined,
    barcode: track.barcode ?? undefined,
    comment: track.comment ?? undefined,
    coverArtUrl: track.coverArtUrl ?? undefined,
    duration: formatDuration(track.durationMs),
    color: colorFor(track.id),
    status: track.status,
  };
}

function toRecognition(response: RecognitionResponse, source: string): Recognition {
  const track = response.track;
  const isInvalidAudio = response.status === "INVALID_AUDIO";
  return {
    id: `${Date.now()}-${track?.id ?? "no-match"}`,
    title: track?.title ?? (isInvalidAudio ? "Audio could not be read" : "Nothing matched"),
    artist:
      track?.artist ??
      (isInvalidAudio
        ? "Try a different audio file."
        : "Try a clearer clip or add this track to your library."),
    album: track?.album ?? undefined,
    albumArtist: track?.albumArtist ?? undefined,
    composer: track?.composer ?? undefined,
    genre: track?.genre ?? undefined,
    releaseYear: track?.releaseYear ?? undefined,
    trackNumber: track?.trackNumber ?? undefined,
    discNumber: track?.discNumber ?? undefined,
    isrc: track?.isrc ?? undefined,
    barcode: track?.barcode ?? undefined,
    comment: track?.comment ?? undefined,
    coverArtUrl: track?.coverArtUrl ?? undefined,
    durationMs: track?.durationMs ?? undefined,
    score: matchLabel(response.status),
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
    title: track?.title ?? "Nothing matched",
    artist: track?.artist ?? "No track from your library matched this search.",
    album: track?.album ?? undefined,
    albumArtist: track?.albumArtist ?? undefined,
    composer: track?.composer ?? undefined,
    genre: track?.genre ?? undefined,
    releaseYear: track?.releaseYear ?? undefined,
    trackNumber: track?.trackNumber ?? undefined,
    discNumber: track?.discNumber ?? undefined,
    isrc: track?.isrc ?? undefined,
    barcode: track?.barcode ?? undefined,
    comment: track?.comment ?? undefined,
    coverArtUrl: track?.coverArtUrl ?? undefined,
    durationMs: track?.durationMs ?? undefined,
    score: matchLabel(item.status),
    time: new Date(item.createdAt).toLocaleString("pl-PL"),
    source: item.source,
    color: colorFor(track?.id ?? "no-match"),
    status: item.status,
  };
}

export default function Home() {
  const [page, setPage] = useState<Page>("listen");
  const [libraryOrigin, setLibraryOrigin] = useState<"PERSONAL" | "MTG_JAMENDO" | "ALL">(
    "PERSONAL",
  );
  const [tracks, setTracks] = useState<Track[]>([]);
  const [albums, setAlbums] = useState<AlbumSummary[]>([]);
  const [artists, setArtists] = useState<ArtistSummary[]>([]);
  const [libraryPagination, setLibraryPagination] = useState(emptyPagination);
  const libraryQuery = useRef<LibraryQuery>(initialLibraryQuery);
  const libraryRequestId = useRef(0);
  const [history, setHistory] = useState<Recognition[]>([]);
  const [toast, setToast] = useState("");
  const [recognition, setRecognition] = useState<Recognition | null>(null);
  const [indexProgress, setIndexProgress] = useState<number | null>(null);

  const [error, setError] = useState("");
  const [metadataReview, setMetadataReview] = useState<MetadataDraft | null>(null);
  const reviewResolver = useRef<((approved: boolean) => void) | null>(null);

  function getPendingRemovalIds() {
    return pendingRemovalIds;
  }

  function showToast(message: string) {
    setToast(message);
    window.setTimeout(() => setToast(""), 1800);
  }

  function replacePendingRemovalIds(ids: Set<string>) {
    pendingRemovalIds = ids;
    persistPendingRemovalIds(ids);
  }

  async function recognize(file: File, source: "MICROPHONE" | "AUDIO_FILE", probe = false) {
    try {
      setError("");
      const response = await musicApi.recognize(file, source, probe);
      const matched = response.status === "MATCHED";
      if (!probe || matched) {
        const result = toRecognition(response, source);
        setRecognition(result);
        if (matched) setHistory((current) => [result, ...current]);
      }
      return matched;
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "We couldn't identify that audio.");
      throw reason;
    }
  }

  async function loadLibrary(query: LibraryQuery, nextOrigin = libraryOrigin) {
    const requestId = ++libraryRequestId.current;
    libraryQuery.current = query;
    setError("");

    try {
      const request = { ...query, origin: nextOrigin };

      if (query.resource === "tracks") {
        const page = await musicApi.listTracks(request);
        if (requestId !== libraryRequestId.current) return;
        setLibraryPagination({
          page: page.page,
          size: page.size,
          totalElements: page.totalElements,
          totalPages: page.totalPages,
          hasNext: page.hasNext,
          hasPrevious: page.hasPrevious,
        });
        const mappedTracks = page.content.map(toTrack);
        const loadedIds = new Set(mappedTracks.map((track) => track.id));
        const activePendingIds = new Set(
          [...getPendingRemovalIds()].filter((id) => loadedIds.has(id)),
        );
        replacePendingRemovalIds(activePendingIds);
        setTracks(mappedTracks.filter((track) => !activePendingIds.has(track.id)));
      } else if (query.resource === "albums") {
        const page = await musicApi.listAlbums(request);
        if (requestId !== libraryRequestId.current) return;
        setLibraryPagination({
          page: page.page,
          size: page.size,
          totalElements: page.totalElements,
          totalPages: page.totalPages,
          hasNext: page.hasNext,
          hasPrevious: page.hasPrevious,
        });
        setAlbums(
          page.content.map((album) => ({ ...album, coverArtUrl: album.coverArtUrl ?? undefined })),
        );
      } else {
        const page = await musicApi.listArtists(request);
        if (requestId !== libraryRequestId.current) return;
        setLibraryPagination({
          page: page.page,
          size: page.size,
          totalElements: page.totalElements,
          totalPages: page.totalPages,
          hasNext: page.hasNext,
          hasPrevious: page.hasPrevious,
        });
        setArtists(page.content);
      }
    } catch (reason: unknown) {
      if (requestId !== libraryRequestId.current) return;
      setError(reason instanceof Error ? reason.message : "We couldn't load your library.");
    }
  }

  function changePage(nextPage: Page) {
    setPage(nextPage);
    setError("");

    if (nextPage === "library") {
      loadLibrary(libraryQuery.current);
    }

    if (nextPage === "history") {
      void musicApi
        .history()
        .then((loadedHistory) => setHistory(loadedHistory.map(toHistoryItem)))
        .catch((reason: unknown) =>
          setError(
            reason instanceof Error ? reason.message : "We couldn't load your search history.",
          ),
        );
    }
  }

  async function addFiles(files: FileList) {
    try {
      setError("");
      setIndexProgress(0);
      for (let index = 0; index < files.length; index += 1) {
        const file = files[index];
        const upload = await musicApi.uploadTrack(file);
        const approved = await new Promise<boolean>((resolve) => {
          reviewResolver.current = resolve;
          setMetadataReview({
            trackId: upload.trackId,
            fileName: file.name,
            title: upload.track.title,
            artist: upload.track.artist,
            album: upload.track.album ?? "",
            coverArtUrl: upload.track.coverArtUrl ?? undefined,
          });
        });
        if (approved) {
          await musicApi.waitForIndexing(upload.indexingJobId);
        }
        setIndexProgress(Math.round(((index + 1) / files.length) * 100));
      }
      loadLibrary({ ...initialLibraryQuery });
      showToast("Library is ready");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "We couldn't add those files.");
    } finally {
      setIndexProgress(null);
    }
  }

  async function confirmMetadata(metadata: Omit<MetadataDraft, "trackId" | "fileName">) {
    if (!metadataReview) return;
    await musicApi.updateTrackMetadata(metadataReview.trackId, {
      title: metadata.title,
      artist: metadata.artist,
      album: metadata.album,
    });
    setMetadataReview(null);
    reviewResolver.current?.(true);
    reviewResolver.current = null;
  }

  async function skipMetadataReview() {
    if (!metadataReview) return;
    await musicApi.removeTrack(metadataReview.trackId);
    setMetadataReview(null);
    reviewResolver.current?.(false);
    reviewResolver.current = null;
  }

  async function removeTrack(id: string) {
    const removedTrack = tracks.find((track) => track.id === id);
    if (!removedTrack) return;

    const pendingAfterStart = new Set(getPendingRemovalIds()).add(id);
    replacePendingRemovalIds(pendingAfterStart);
    setTracks((current) => current.filter((track) => track.id !== id));
    showToast("Removing track…");

    try {
      await musicApi.removeTrack(id);
      const pendingAfterSuccess = new Set(getPendingRemovalIds());
      pendingAfterSuccess.delete(id);
      replacePendingRemovalIds(pendingAfterSuccess);
      showToast("Track removed from your library");
    } catch (reason) {
      const pendingAfterFailure = new Set(getPendingRemovalIds());
      pendingAfterFailure.delete(id);
      replacePendingRemovalIds(pendingAfterFailure);
      setTracks((current) =>
        current.some((track) => track.id === id) ? current : [removedTrack, ...current],
      );
      setError(reason instanceof Error ? reason.message : "We couldn't remove that track.");
    }
  }

  async function reindexTrack(id: string) {
    try {
      const job = await musicApi.reindexTrack(id);
      await musicApi.waitForIndexing(job.id);
      loadLibrary(libraryQuery.current);
      showToast("Track is ready to identify");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "We couldn't refresh that track.");
    }
  }

  async function clearHistory(): Promise<boolean> {
    try {
      await musicApi.clearHistory();
      setHistory([]);
      return true;
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "We couldn't clear your search history.");
      return false;
    }
  }

  return (
    <div className="min-h-screen bg-canvas text-ink">
      <Header page={page} onPageChange={changePage} trackCount={libraryPagination.totalElements} />
      <main>
        {error && (
          <div className="mx-auto max-w-[1200px] px-6 pt-6 text-[14px] text-red-600 lg:px-8">
            {error}
          </div>
        )}
        {page === "listen" && <ListenView recognition={recognition} onRecognize={recognize} />}
        {page === "library" && (
          <LibraryView
            tracks={tracks}
            albums={albums}
            artists={artists}
            pagination={libraryPagination}
            indexingProgress={indexProgress}
            onAddFiles={addFiles}
            onReindex={reindexTrack}
            onRemove={removeTrack}
            origin={libraryOrigin}
            onOriginChange={(origin) => {
              setLibraryOrigin(origin);
              loadLibrary(initialLibraryQuery, origin);
            }}
            onQueryChange={loadLibrary}
          />
        )}
        {page === "history" && (
          <HistoryView history={history} onClear={clearHistory} onToast={showToast} />
        )}
      </main>
      {metadataReview && (
        <MetadataReview
          draft={metadataReview}
          onConfirm={confirmMetadata}
          onSkip={skipMetadataReview}
        />
      )}
      {toast && (
        <div className="pointer-events-none fixed bottom-6 left-1/2 z-50 -translate-x-1/2 rounded-full bg-ink px-4 py-2.5 text-[11px] font-semibold text-white animate-enter">
          {toast}
        </div>
      )}
    </div>
  );
}
