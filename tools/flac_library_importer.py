#!/usr/bin/env python3
"""Import a local FLAC collection without modifying the source files."""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import mimetypes
import sys
import time
from pathlib import Path
from typing import Any

try:
    import requests
    from mutagen.flac import FLAC
    DEPENDENCY_ERROR: ModuleNotFoundError | None = None
except ModuleNotFoundError as error:
    DEPENDENCY_ERROR = error


MAX_FILE_BYTES = 512 * 1024 * 1024
RETRY_COUNT = 3
AUDIO_EXTENSIONS = {".flac", ".m4a", ".mp4", ".aac"}


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    args = parse_args()
    if DEPENDENCY_ERROR is not None:
        print(
            "Missing importer dependencies. Run: python -m pip install -r tools/requirements-flac-importer.txt",
            file=sys.stderr,
        )
        return 2
    root = args.root.expanduser().resolve()
    report = args.report.expanduser().resolve()
    if not root.is_dir():
        print(f"Source directory does not exist: {root}", file=sys.stderr)
        return 2

    report.parent.mkdir(parents=True, exist_ok=True)
    previous = load_report(report)
    files = sorted(
        (path for path in root.rglob("*") if path.is_file() and path.suffix.lower() in AUDIO_EXTENSIONS),
        key=lambda path: path.as_posix().lower(),
    )
    local_checksums: set[str] = set()
    counts: dict[str, int] = {}

    with report.open("a", encoding="utf-8") as output:
        for path in files:
            relative_path = path.relative_to(root).as_posix()
            if relative_path in previous and previous[relative_path].get("status") == "INDEXED":
                increment(counts, previous[relative_path]["status"])
                continue

            result = inspect_file(path, relative_path)
            result["origin"] = args.origin
            if any(warning.startswith("METADATA_READ_FAILED") for warning in result["warnings"]):
                result.update(status="FAILED_METADATA", errors=["FLAC_METADATA_UNREADABLE"])
            elif result["checksum"] in local_checksums:
                result.update(status="DUPLICATE", warnings=result["warnings"] + ["DUPLICATE_IN_SOURCE"])
            elif result["sizeBytes"] > args.max_size:
                result.update(status="SKIPPED_UNSUPPORTED", errors=["FILE_TOO_LARGE"])
            elif args.dry_run:
                result["status"] = "DRY_RUN"
            else:
                print(f"IMPORTING          {relative_path}", flush=True)
                result = import_file(args.backend, args.origin, path, relative_path, result)
                append_report(output, result)
                if result.get("indexingJobId"):
                    print(f"WAITING_INDEXING   {relative_path}", flush=True)
                    result = wait_for_indexing(
                        args.backend,
                        result["indexingJobId"],
                        args.indexing_timeout,
                        args.poll_interval,
                        result,
                    )
                elif result.get("status") == "DUPLICATE":
                    result.update(status="FAILED_INDEXING", errors=["DUPLICATE_WITHOUT_INDEXING_JOB"])

            local_checksums.add(result["checksum"])
            append_report(output, result)
            increment(counts, result["status"])
            print(f"{result['status']:18} {relative_path}", flush=True)

    write_summary(report, counts, args.dry_run)
    return 0 if not any(key.startswith("FAILED") for key in counts) else 1


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--backend", default="http://localhost:8080")
    parser.add_argument("--origin", choices=("PERSONAL", "MTG_JAMENDO"), default="PERSONAL")
    parser.add_argument("--report", type=Path, default=Path("reports/aac256-import.jsonl"))
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--max-size", type=int, default=MAX_FILE_BYTES)
    parser.add_argument("--poll-interval", type=float, default=5.0, help="Seconds between indexing status checks")
    parser.add_argument(
        "--indexing-timeout",
        type=float,
        default=3600.0,
        help="Maximum seconds to wait for one fingerprinting job",
    )
    return parser.parse_args()


def inspect_file(path: Path, relative_path: str) -> dict[str, Any]:
    warnings: list[str] = []
    try:
        if path.suffix.lower() == ".flac":
            audio = FLAC(path)
            tags = audio.tags or {}
            has_cover = bool(audio.pictures)
        else:
            from mutagen.mp4 import MP4

            audio = MP4(path)
            tags = audio.tags or {}
            has_cover = bool(tags.get("covr"))
        if path.suffix.lower() == ".flac":
            title = tag(tags, "title")
            artist = tag(tags, "artist")
            album = tag(tags, "album")
            album_artist = tag(tags, "albumartist")
        else:
            title = tag(tags, "\xa9nam")
            artist = tag(tags, "\xa9ART")
            album = tag(tags, "\xa9alb")
            album_artist = tag(tags, "aART")
        if not title:
            warnings.append("MISSING_TITLE")
        if not artist:
            warnings.append("MISSING_ARTIST")
        if not album:
            warnings.append("MISSING_ALBUM")
        if not has_cover:
            warnings.append("MISSING_EMBEDDED_COVER")
    except Exception as error:
        warnings.append(f"METADATA_READ_FAILED: {error}")
        title = artist = album = album_artist = None

    if not title or not artist or not album:
        warnings.append("PATH_METADATA_FALLBACK")

    return {
        "path": relative_path,
        "checksum": sha256(path),
        "sizeBytes": path.stat().st_size,
        "title": title,
        "artist": artist,
        "album": album,
        "albumArtist": album_artist,
        "warnings": sorted(set(warnings)),
        "errors": [],
    }


def import_file(
    backend: str,
    origin: str,
    path: Path,
    relative_path: str,
    result: dict[str, Any],
) -> dict[str, Any]:
    url = backend.rstrip("/") + "/api/v1/library/tracks/imports"
    for attempt in range(RETRY_COUNT):
        try:
            with path.open("rb") as source:
                response = requests.post(
                    url,
                    files={"file": (path.name, source, mimetypes.guess_type(path.name)[0] or "application/octet-stream")},
                    data={"origin": origin, "relativePath": relative_path},
                    timeout=(30, 900),
                )
            if response.status_code >= 500 and attempt + 1 < RETRY_COUNT:
                time.sleep(2**attempt)
                continue
            response.raise_for_status()
            payload = response.json()
            result.update(
                status=payload["status"],
                trackId=payload.get("trackId"),
                indexingJobId=payload.get("indexingJobId"),
            )
            return result
        except (OSError, requests.RequestException, ValueError, KeyError) as error:
            if attempt + 1 == RETRY_COUNT:
                result.update(status="FAILED_UPLOAD", errors=[str(error)])
                return result
            time.sleep(2**attempt)
    return result


def wait_for_indexing(
    backend: str,
    job_id: str,
    timeout: float,
    poll_interval: float,
    result: dict[str, Any],
) -> dict[str, Any]:
    url = backend.rstrip("/") + "/api/v1/indexing-jobs/" + job_id
    deadline = time.monotonic() + timeout
    while True:
        try:
            response = requests.get(url, timeout=(30, 60))
            response.raise_for_status()
            job = response.json()
            status = job.get("status")
            result["indexingStatus"] = status
            result["indexingProgress"] = job.get("progress")
            if status == "COMPLETED":
                result["status"] = "INDEXED"
                result["fingerprinted"] = True
                return result
            if status == "FAILED":
                result.update(
                    status="FAILED_INDEXING",
                    fingerprinted=False,
                    errors=[job.get("errorMessage") or job.get("errorCode") or "INDEXING_FAILED"],
                )
                return result
            if time.monotonic() >= deadline:
                result.update(status="INDEXING_PENDING", fingerprinted=False)
                return result
            time.sleep(poll_interval)
        except (requests.RequestException, ValueError) as error:
            if time.monotonic() >= deadline:
                result.update(status="INDEXING_PENDING", fingerprinted=False, errors=[str(error)])
                return result
            time.sleep(poll_interval)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        while chunk := source.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def tag(tags: Any, name: str) -> str | None:
    values = tags.get(name) or tags.get(name.upper()) or tags.get(name.lower())
    return str(values[0]).strip() if values and str(values[0]).strip() else None


def load_report(report: Path) -> dict[str, dict[str, Any]]:
    if not report.exists():
        return {}
    records: dict[str, dict[str, Any]] = {}
    with report.open(encoding="utf-8") as source:
        for line in source:
            if line.strip():
                try:
                    record = json.loads(line)
                except json.JSONDecodeError:
                    continue
                records[record["path"]] = record
    return records


def write_summary(report: Path, counts: dict[str, int], dry_run: bool) -> None:
    records = load_report(report)
    summary = report.with_name(report.stem + "-summary.json")
    latest_counts: dict[str, int] = {}
    for record in records.values():
        increment(latest_counts, record["status"])
    summary.write_text(json.dumps({"dryRun": dry_run, "counts": latest_counts}, indent=2), encoding="utf-8")

    exceptions = report.with_name(report.stem + "-exceptions.csv")
    with exceptions.open("w", newline="", encoding="utf-8") as output:
        writer = csv.DictWriter(output, fieldnames=["path", "status", "warnings", "errors"])
        writer.writeheader()
        for record in records.values():
            if record.get("status") not in {"INDEXED", "DRY_RUN"} or record.get("warnings"):
                writer.writerow(
                    {
                        "path": record.get("path"),
                        "status": record.get("status"),
                        "warnings": ";".join(record.get("warnings", [])),
                        "errors": ";".join(record.get("errors", [])),
                    }
                )


def increment(counts: dict[str, int], status: str) -> None:
    counts[status] = counts.get(status, 0) + 1


def append_report(output: Any, result: dict[str, Any]) -> None:
    output.write(json.dumps(result, ensure_ascii=False) + "\n")
    output.flush()


if __name__ == "__main__":
    raise SystemExit(main())
