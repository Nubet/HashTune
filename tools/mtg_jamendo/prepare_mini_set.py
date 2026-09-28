#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import hashlib
import html
import random
import re
import shutil
import ssl
import sys
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path
from urllib.request import Request, urlopen


DEFAULT_DATASET_DIR = Path("external/mtg-jamendo-dataset")
DEFAULT_OUTPUT_DIR = Path("data/dev/mtg_jamendo_mini")
DEFAULT_AUDIO_BASE_URL = "https://cdn.freesound.org/mtg-jamendo/raw_30s/audio"
TRACK_ID_PATTERN = re.compile(r"(?:track_)?(\d+)$")

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")


@dataclass(frozen=True)
class MtgJamendoTrack:
    track_id: str
    dataset_path: str
    duration: float
    artist_id: str
    genres: tuple[str, ...]
    primary_genre: str
    sha256: str
    title: str
    artist: str
    album: str
    release_date: str
    source_url: str
    license_name: str
    license_url: str


def parse_track_number(track_id: str) -> str:
    match = TRACK_ID_PATTERN.fullmatch(track_id)
    if match is None:
        raise ValueError(f"Unsupported track id: {track_id}")
    return match.group(1)


def load_raw_track_metadata(dataset_dir: Path) -> dict[str, dict[str, str]]:
    metadata_path = dataset_dir / "data" / "raw.meta.tsv"
    with metadata_path.open(encoding="utf-8", newline="") as file:
        return {
            parse_track_number(row["TRACK_ID"]): row
            for row in csv.DictReader(file, delimiter="\t")
        }


def load_audio_licenses(dataset_dir: Path) -> dict[str, tuple[str, str]]:
    license_path = dataset_dir / "audio_licenses.txt"
    if not license_path.exists():
        return {}

    licenses: dict[str, tuple[str, str]] = {}
    current_path: str | None = None
    lines = license_path.read_text(encoding="utf-8").splitlines()
    for line in lines:
        if re.fullmatch(r"\d+/\d+\.mp3", line):
            current_path = line
            continue
        if current_path is None or "Available under a" not in line:
            continue
        license_url = line.rsplit(": ", maxsplit=1)[-1]
        license_name = line.removeprefix("Available under a ").removesuffix(f": {license_url}")
        licenses[current_path] = (license_name, license_url)
    return licenses


def load_track_checksums(dataset_dir: Path) -> dict[str, str]:
    checksum_path = dataset_dir / "data" / "download" / "raw_30s_audio_sha256_tracks.txt"
    with checksum_path.open(encoding="utf-8") as file:
        return {
            path: checksum
            for line in file
            for checksum, path in [line.strip().split(maxsplit=1)]
        }


def load_genre_tracks(dataset_dir: Path) -> list[MtgJamendoTrack]:
    metadata = load_raw_track_metadata(dataset_dir)
    licenses = load_audio_licenses(dataset_dir)
    checksums = load_track_checksums(dataset_dir)
    genre_path = dataset_dir / "data" / "autotagging_genre.tsv"

    tracks: list[MtgJamendoTrack] = []
    with genre_path.open(encoding="utf-8", newline="") as file:
        reader = csv.reader(file, delimiter="\t")
        header = next(reader)
        columns = {name: index for index, name in enumerate(header)}
        for values in reader:
            row = dict(zip(header, values))
            track_number = parse_track_number(row["TRACK_ID"])
            source = metadata.get(track_number)
            if source is None:
                continue

            genres = tuple(
                value.removeprefix("genre---")
                for value in values[columns["TAGS"] :]
                if value.startswith("genre---")
            )
            if not genres:
                continue

            checksum = checksums.get(row["PATH"])
            if checksum is None:
                raise ValueError(f"Missing checksum for dataset path: {row['PATH']}")

            license_name, license_url = licenses.get(row["PATH"], ("", ""))
            tracks.append(
                MtgJamendoTrack(
                    track_id=track_number,
                    dataset_path=row["PATH"],
                    duration=float(row["DURATION"]),
                    artist_id=row["ARTIST_ID"],
                    genres=genres,
                    primary_genre=genres[0],
                    sha256=checksum,
                    title=html.unescape(source["TRACK_NAME"]),
                    artist=html.unescape(source["ARTIST_NAME"]),
                    album=html.unescape(source["ALBUM_NAME"]),
                    release_date=source["RELEASEDATE"],
                    source_url=source["URL"],
                    license_name=license_name,
                    license_url=license_url,
                )
            )
    return tracks


def select_balanced(tracks: list[MtgJamendoTrack], count: int, seed: int) -> list[MtgJamendoTrack]:
    groups: dict[str, list[MtgJamendoTrack]] = defaultdict(list)
    for track in tracks:
        groups[track.primary_genre].append(track)

    randomizer = random.Random(seed)
    categories = sorted(groups, key=lambda genre: (-len(groups[genre]), genre))[:10]
    for category in categories:
        randomizer.shuffle(groups[category])

    selected: list[MtgJamendoTrack] = []
    used_tracks: set[str] = set()
    used_artists: set[str] = set()
    while len(selected) < count:
        progress = False
        for category in categories:
            if len(selected) >= count:
                break
            options = groups[category]
            candidate = next(
                (
                    track
                    for track in options
                    if track.track_id not in used_tracks and track.artist_id not in used_artists
                ),
                next((track for track in options if track.track_id not in used_tracks), None),
            )
            if candidate is None:
                continue
            selected.append(candidate)
            used_tracks.add(candidate.track_id)
            used_artists.add(candidate.artist_id)
            progress = True
        if not progress:
            break

    if len(selected) < count:
        raise ValueError(f"Only {len(selected)} balanced tracks are available, requested {count}")
    return selected


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def download_track_audio(
    url: str, destination: Path, expected_sha256: str, context: ssl.SSLContext
) -> None:
    if destination.exists() and destination.stat().st_size > 0:
        if sha256_file(destination) == expected_sha256:
            return
        destination.unlink()

    temporary = destination.with_suffix(destination.suffix + ".part")
    request = Request(url, headers={"User-Agent": "HashTune MVP dataset downloader"})
    try:
        with urlopen(request, timeout=60, context=context) as response, temporary.open("wb") as output:
            shutil.copyfileobj(response, output)
        if sha256_file(temporary) != expected_sha256:
            raise ValueError(f"Checksum mismatch for {destination.name}")
        temporary.replace(destination)
    finally:
        temporary.unlink(missing_ok=True)


def write_manifest(output_dir: Path, tracks: list[MtgJamendoTrack]) -> None:
    manifest_path = output_dir / "metadata.csv"
    fields = [
        "track_id",
        "title",
        "artist",
        "album",
        "release_date",
        "duration_seconds",
        "primary_genre",
        "audio_path",
        "source_url",
        "license",
        "license_url",
    ]
    with manifest_path.open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fields)
        writer.writeheader()
        for track in tracks:
            writer.writerow(
                {
                    "track_id": track.track_id,
                    "title": track.title,
                    "artist": track.artist,
                    "album": track.album,
                    "release_date": track.release_date,
                    "duration_seconds": track.duration,
                    "primary_genre": track.primary_genre,
                    "audio_path": f"audio/{track.track_id}.mp3",
                    "source_url": track.source_url,
                    "license": track.license_name,
                    "license_url": track.license_url,
                }
            )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dataset-dir", type=Path, default=DEFAULT_DATASET_DIR)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
    parser.add_argument("--count", type=int, default=100)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--audio-base-url", default=DEFAULT_AUDIO_BASE_URL)
    parser.add_argument(
        "--insecure",
        action="store_true",
        help="disable TLS certificate verification for environments with broken certificate stores",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.count < 1:
        raise ValueError("--count must be greater than zero")
    if not args.dataset_dir.exists():
        raise FileNotFoundError(
            f"Dataset not found: {args.dataset_dir}. Clone MTG-Jamendo there first."
        )

    selected = select_balanced(load_genre_tracks(args.dataset_dir), args.count, args.seed)
    context = ssl._create_unverified_context() if args.insecure else ssl.create_default_context()
    audio_dir = args.output_dir / "audio"
    audio_dir.mkdir(parents=True, exist_ok=True)

    failed: list[str] = []
    for index, track in enumerate(selected, start=1):
        url = f"{args.audio_base_url.rstrip('/')}/{track.dataset_path}"
        destination = audio_dir / f"{track.track_id}.mp3"
        print(f"[{index}/{len(selected)}] {track.artist} - {track.title}")
        try:
            download_track_audio(url, destination, track.sha256, context)
        except Exception as exception:  # noqa: BLE001 - report all individual downloads and continue
            print(f"  failed: {exception}", file=sys.stderr)
            failed.append(track.track_id)

    successful = [track for track in selected if (audio_dir / f"{track.track_id}.mp3").exists()]
    write_manifest(args.output_dir, successful)
    if failed:
        print(f"Downloaded {len(successful)}/{len(selected)} tracks. Failed: {', '.join(failed)}")
        return 1
    print(f"Downloaded {len(successful)} tracks to {args.output_dir}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (FileNotFoundError, ValueError) as exception:
        print(f"error: {exception}", file=sys.stderr)
        raise SystemExit(2) from exception
