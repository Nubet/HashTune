"use client";

import { useState } from "react";
import { Header, type Page } from "../components/Header";
import { HistoryView } from "../components/HistoryView";
import { LibraryView } from "../components/LibraryView";
import { ListenView } from "../components/ListenView";
import { initialHistory, initialTracks, type Recognition } from "../lib/music";

export default function Home() {
  const [page, setPage] = useState<Page>("listen");
  const [tracks, setTracks] = useState(initialTracks);
  const [history, setHistory] = useState(initialHistory);
  const [toast, setToast] = useState("");

  function showToast(message: string) {
    setToast(message);
    window.setTimeout(() => setToast(""), 1800);
  }

  function addRecognition(source: string) {
    const recognition: Recognition = {
      title: "Do I Wanna Know?",
      artist: "Arctic Monkeys",
      score: "96%",
      time: "Just now",
      source,
      color: "#e8d25f",
    };
    setHistory((current) => [recognition, ...current]);
  }

  return (
    <div className="min-h-screen bg-canvas text-ink">
      <Header page={page} onPageChange={setPage} />
      <main>
        {page === "listen" && <ListenView onRecognized={addRecognition} />}
        {page === "library" && (
          <LibraryView
            tracks={tracks}
            onRemove={(id) => setTracks((current) => current.filter((track) => track.id !== id))}
            onToast={showToast}
          />
        )}
        {page === "history" && (
          <HistoryView history={history} onClear={() => setHistory([])} onToast={showToast} />
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
