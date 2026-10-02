import { useState } from "react";
import type { MetadataDraft } from "@/lib/music";
import { Button } from "./ui";
import { TrackArtwork } from "./TrackArtwork";

export function MetadataReview({
  draft,
  onConfirm,
  onSkip,
}: {
  draft: MetadataDraft;
  onConfirm: (metadata: Omit<MetadataDraft, "trackId" | "fileName">) => Promise<void>;
  onSkip: () => Promise<void>;
}) {
  const [metadata, setMetadata] = useState({
    title: draft.title,
    artist: draft.artist,
    album: draft.album,
  });
  const [saving, setSaving] = useState(false);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    try {
      await onConfirm(metadata);
    } finally {
      setSaving(false);
    }
  }

  async function skip() {
    setSaving(true);
    try {
      await onSkip();
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 z-40 grid place-items-center bg-navy/45 px-5">
      <form
        onSubmit={submit}
        className="w-full max-w-130 bg-canvas p-6 shadow-[0_20px_60px_rgba(0,0,0,.2)]"
      >
        <div className="text-[11px] font-bold uppercase tracking-widest text-brand">
          Review metadata
        </div>
        <h2 className="mt-2 text-[26px] font-bold tracking-[-.035em]">Confirm this track</h2>
        <p className="mt-2 truncate text-[12px] text-muted" title={draft.fileName}>
          {draft.fileName}
        </p>
        <p className="mt-4 text-[13px] text-muted">
          We found these tags in the file. Correct anything before adding it to your library.
        </p>
        {draft.coverArtUrl && (
          <div className="mt-5 flex items-center gap-3">
            <TrackArtwork
              src={draft.coverArtUrl}
              alt={`${draft.title} cover art`}
              color="#17212b"
              className="size-16 shrink-0"
            />
            <span className="text-[12px] text-muted">Embedded cover found in the file</span>
          </div>
        )}
        <div className="mt-6 grid gap-4">
          {(["title", "artist", "album"] as const).map((field) => (
            <label
              key={field}
              className="grid gap-1 text-[11px] font-semibold uppercase tracking-[.08em]"
            >
              {field}
              <input
                required={field !== "album"}
                value={metadata[field]}
                onChange={(event) => setMetadata({ ...metadata, [field]: event.target.value })}
                className="border bg-transparent px-3 py-2 text-[14px] font-normal normal-case tracking-normal outline-none focus:border-brand"
              />
            </label>
          ))}
        </div>
        <div className="mt-7 flex justify-end gap-2">
          <Button type="button" onClick={() => void skip()} disabled={saving}>
            Skip file
          </Button>
          <Button type="submit" variant="primary" disabled={saving}>
            {saving ? "Saving…" : "Add to library"}
          </Button>
        </div>
      </form>
    </div>
  );
}
