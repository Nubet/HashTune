"""Download the curated landing-page cover set from the local music library."""

from __future__ import annotations

import json
import os
import re
import shutil
import urllib.request
from pathlib import Path

API_URL = os.environ.get("HASHTUNE_API_URL", "http://127.0.0.1:8080")
PROJECT_ROOT = Path(__file__).resolve().parents[1]
COVERS_DIR = PROJECT_ROOT / "frontend" / "public" / "landing-covers"
DATA_PATH = PROJECT_ROOT / "frontend" / "lib" / "landing-covers.json"
PAGE_SIZE = 100
CURATED_TITLES = [
    "Feels Like heaven",
    "Died in Your Arms",
    "A World Without You",
    "Bad Romance",
    "Brother Louie",
    'Gonna Fly Now (Theme From "Rocky")',
    "I'm Gonna Miss You",
    "All By Myself",
    "Chiquitita",
    "Blue Monday",
    "castles",
    "Caravansary",
    "Better Off Alone",
]


def fetch_tracks() -> list[dict]:
    tracks: list[dict] = []
    page = 0

    while True:
        query = f"{API_URL}/api/v1/library/tracks?origin=PERSONAL&page={page}&size={PAGE_SIZE}"
        with urllib.request.urlopen(query, timeout=20) as response:
            payload = json.load(response)

        tracks.extend(payload.get("content", []))
        if not payload.get("hasNext"):
            return tracks
        page += 1


def title_key(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", "", value.casefold())


def select_tracks(tracks: list[dict]) -> list[dict]:
    selected: list[dict] = []

    for requested_title in CURATED_TITLES:
        requested_key = title_key(requested_title)
        match = next(
            (
                track
                for track in tracks
                if track.get("coverArtUrl") and requested_key in title_key(track.get("title", ""))
            ),
            None,
        )
        if match is None:
            raise RuntimeError(f'Curated title not found in library: "{requested_title}"')
        selected.append(match)

    return selected


def extension_for(content_type: str) -> str:
    return {
        "image/jpeg": ".jpg",
        "image/png": ".png",
        "image/webp": ".webp",
        "image/avif": ".avif",
    }.get(content_type.split(";", 1)[0].lower(), ".jpg")


def sync_covers(tracks: list[dict]) -> list[dict]:
    temporary_dir = COVERS_DIR.with_name(f"{COVERS_DIR.name}.tmp")
    if temporary_dir.exists():
        shutil.rmtree(temporary_dir)
    temporary_dir.mkdir(parents=True)

    manifest: list[dict] = []
    try:
        for index, track in enumerate(tracks, start=1):
            with urllib.request.urlopen(track["coverArtUrl"], timeout=20) as response:
                content = response.read()
                extension = extension_for(response.headers.get("Content-Type", ""))

            filename = f"{index:02d}{extension}"
            (temporary_dir / filename).write_bytes(content)
            manifest.append(
                {
                    "id": track["id"],
                    "title": track["title"],
                    "artist": track["artist"],
                    "album": track.get("album"),
                    "cover": f"/landing-covers/{filename}",
                }
            )

        if COVERS_DIR.exists():
            shutil.rmtree(COVERS_DIR)
        temporary_dir.rename(COVERS_DIR)
        DATA_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        return manifest
    except Exception:
        shutil.rmtree(temporary_dir, ignore_errors=True)
        raise


def main() -> None:
    manifest = sync_covers(select_tracks(fetch_tracks()))
    print(f"Synced {len(manifest)} landing covers from {API_URL}")
    for track in manifest:
        print(f"- {track['artist']} - {track['title']}")


if __name__ == "__main__":
    main()
