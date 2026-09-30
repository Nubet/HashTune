#!/usr/bin/env python3
"""Copy a FLAC directory tree while converting audio to M4A/AAC 256 kb/s."""

from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
from pathlib import Path


def main() -> int:
    args = parse_args()
    source = args.source.expanduser().resolve()
    destination = (args.destination or source.with_name(source.name + "-AAC256")).expanduser().resolve()

    if not source.is_dir():
        print(f"Source directory does not exist: {source}", file=sys.stderr)
        return 2
    if destination == source or destination.is_relative_to(source):
        print("Destination must not be the source directory or one of its children.", file=sys.stderr)
        return 2

    files = sorted(path for path in source.rglob("*") if path.is_file())
    flac_files = [path for path in files if path.suffix.lower() == ".flac"]
    other_files = [path for path in files if path.suffix.lower() != ".flac"]
    print(f"Source:      {source}")
    print(f"Destination: {destination}")
    print(f"FLAC files:  {len(flac_files)}")
    print(f"Other files: {len(other_files) if args.copy_other else 0}")

    if args.dry_run:
        for path in flac_files:
            print(f"WOULD CONVERT {path.relative_to(source)}")
        return 0

    destination.mkdir(parents=True, exist_ok=True)
    converted = skipped = failed = copied = 0

    for index, path in enumerate(flac_files, start=1):
        target = destination / path.relative_to(source).with_suffix(".m4a")
        try:
            if target.exists() and not args.overwrite:
                skipped += 1
                print(f"[{index}/{len(flac_files)}] SKIP     {target.relative_to(destination)}")
                continue
            target.parent.mkdir(parents=True, exist_ok=True)
            convert(path, target, args.ffmpeg)
            converted += 1
            print(f"[{index}/{len(flac_files)}] CONVERT  {target.relative_to(destination)}")
        except (OSError, subprocess.CalledProcessError) as error:
            failed += 1
            print(f"[{index}/{len(flac_files)}] FAILED   {path.relative_to(source)}: {error}", file=sys.stderr)

    if args.copy_other:
        for path in other_files:
            target = destination / path.relative_to(source)
            if target.exists() and not args.overwrite:
                continue
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, target)
            copied += 1

    print(f"Converted: {converted}; skipped: {skipped}; copied: {copied}; failed: {failed}")
    return 1 if failed else 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path, help="Source directory containing FLAC files")
    parser.add_argument("destination", type=Path, nargs="?", help="Output directory; defaults to <source>-AAC256")
    parser.add_argument("--ffmpeg", default="ffmpeg", help="FFmpeg executable")
    parser.add_argument("--overwrite", action="store_true", help="Replace existing M4A and copied files")
    parser.add_argument("--dry-run", action="store_true", help="List conversions without writing files")
    parser.add_argument(
        "--no-copy-other",
        dest="copy_other",
        action="store_false",
        help="Do not copy non-FLAC files such as JPG and M3U8 files",
    )
    parser.set_defaults(copy_other=True)
    return parser.parse_args()


def convert(source: Path, destination: Path, ffmpeg: str) -> None:
    temporary = destination.with_name(destination.stem + ".part" + destination.suffix)
    temporary.unlink(missing_ok=True)
    command = [
        ffmpeg,
        "-hide_banner",
        "-loglevel",
        "error",
        "-y",
        "-i",
        str(source),
        "-map",
        "0:a:0",
        "-map",
        "0:v?",
        "-map_metadata",
        "0",
        "-c:a",
        "aac",
        "-b:a",
        "256k",
        "-c:v",
        "copy",
        "-disposition:v",
        "attached_pic",
        "-movflags",
        "+faststart",
        str(temporary),
    ]
    try:
        subprocess.run(command, check=True)
        temporary.replace(destination)
    except Exception:
        temporary.unlink(missing_ok=True)
        raise


if __name__ == "__main__":
    raise SystemExit(main())
