#!/usr/bin/env python3
"""Generate deterministic audio fixtures from the MTG-Jamendo mini dataset."""

import argparse
import csv
import subprocess
from pathlib import Path


DEFAULT_SOURCE_DIR = Path("data/dev/mtg_jamendo_mini")
DEFAULT_OUTPUT_DIR = Path("data/dev/mtg_jamendo_samples")
DEFAULT_SAMPLE_DURATION = 8.0
DEFAULT_SAMPLES_PER_TRACK = 5


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate structured audio samples for every track in metadata.csv."
    )
    parser.add_argument("--source-dir", type=Path, default=DEFAULT_SOURCE_DIR)
    parser.add_argument("--output-dir", type=Path, default=DEFAULT_OUTPUT_DIR)
    parser.add_argument("--sample-duration", type=float, default=DEFAULT_SAMPLE_DURATION)
    parser.add_argument("--samples-per-track", type=int, default=DEFAULT_SAMPLES_PER_TRACK)
    parser.add_argument("--ffmpeg", default="ffmpeg")
    return parser.parse_args()


def sample_starts(duration: float, sample_duration: float, count: int) -> list[float]:
    max_start = max(0.0, duration - sample_duration)
    if count == 1:
        return [round(max_start / 2, 3)]

    starts = [round(max_start * index / (count - 1), 3) for index in range(count)]
    return list(dict.fromkeys(starts))


def run_ffmpeg(
    ffmpeg: str,
    source: Path,
    destination: Path,
    start: float,
    duration: float,
) -> None:
    command = [
        ffmpeg,
        "-hide_banner",
        "-loglevel",
        "error",
        "-ss",
        f"{start:.3f}",
        "-i",
        str(source),
        "-t",
        f"{duration:.3f}",
        "-vn",
        "-ac",
        "1",
        "-ar",
        "22050",
        "-map_metadata",
        "-1",
        "-c:a",
        "libmp3lame",
        "-b:a",
        "96k",
        "-y",
        str(destination),
    ]
    subprocess.run(command, check=True)


def main() -> None:
    args = parse_args()
    if args.sample_duration <= 0:
        raise SystemExit("--sample-duration must be greater than zero")
    if args.samples_per_track <= 0:
        raise SystemExit("--samples-per-track must be greater than zero")

    source_dir = args.source_dir.resolve()
    output_dir = args.output_dir.resolve()
    manifest_path = source_dir / "metadata.csv"
    if not manifest_path.is_file():
        raise SystemExit(f"Manifest not found: {manifest_path}")

    with manifest_path.open(newline="", encoding="utf-8-sig") as manifest_file:
        tracks = sorted(csv.DictReader(manifest_file), key=lambda row: row["track_id"])

    output_dir.mkdir(parents=True, exist_ok=True)
    manifest_rows: list[dict[str, str]] = []

    for track in tracks:
        track_id = track["track_id"]
        source = (source_dir / track["audio_path"]).resolve()
        if not source.is_file() or source.parent != (source_dir / "audio").resolve():
            raise SystemExit(f"Source audio not found or outside audio directory: {source}")

        track_output_dir = output_dir / "tracks" / track_id
        track_output_dir.mkdir(parents=True, exist_ok=True)
        duration = float(track["duration_seconds"])
        starts = sample_starts(duration, args.sample_duration, args.samples_per_track)

        for index, start in enumerate(starts, start=1):
            start_ms = round(start * 1000)
            sample_name = (
                f"{track_id}__sample-{index:02d}__start-{start_ms:09d}ms"
                f"__duration-{args.sample_duration:g}s.mp3"
            )
            destination = track_output_dir / sample_name
            run_ffmpeg(args.ffmpeg, source, destination, start, args.sample_duration)
            manifest_rows.append(
                {
                    "track_id": track_id,
                    "title": track["title"],
                    "artist": track["artist"],
                    "sample_index": str(index),
                    "start_ms": str(start_ms),
                    "duration_seconds": f"{args.sample_duration:g}",
                    "source_audio": track["audio_path"],
                    "sample_audio": destination.relative_to(output_dir).as_posix(),
                }
            )

    output_manifest = output_dir / "manifest.csv"
    with output_manifest.open("w", newline="", encoding="utf-8") as manifest_file:
        writer = csv.DictWriter(manifest_file, fieldnames=list(manifest_rows[0]))
        writer.writeheader()
        writer.writerows(manifest_rows)

    readme = output_dir / "README.md"
    readme.write_text(
        "# MTG-Jamendo Audio Samples\n\n"
        f"Generated from `{manifest_path.as_posix()}`.\n\n"
        f"Each track has {len(manifest_rows) // len(tracks)} deterministic samples of "
        f"{args.sample_duration:g} seconds. See `manifest.csv` for source offsets.\n",
        encoding="utf-8",
    )
    print(f"Generated {len(manifest_rows)} samples for {len(tracks)} tracks in {output_dir}")


if __name__ == "__main__":
    main()
