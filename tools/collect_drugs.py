"""Collect the approved MFDS e약은요 API without putting credentials in logs."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import time
import urllib.error
import urllib.parse
import urllib.request

ENDPOINT = "https://apis.data.go.kr/1471000/DrbEasyDrugInfoService/getDrbEasyDrugList"
SOURCE = "https://www.data.go.kr/data/15075057/openapi.do"


def collect(key, destination, supplements=False):
    endpoint = "https://apis.data.go.kr/1471000/HtfsInfoService03/getHtfsItem01" if supplements else ENDPOINT
    filename = "supplements.jsonl" if supplements else "drugs.jsonl"
    destination.mkdir(parents=True, exist_ok=True)
    output = destination / filename
    if output.exists():
        raise RuntimeError("Output already exists; choose a new output directory.")
    temporary = destination / (filename + ".partial")
    seen = set()
    record_count = 0
    duplicate_count = 0
    expected = None
    page = 1
    def fetch_page(number):
        query = urllib.parse.urlencode({
            "serviceKey": urllib.parse.unquote(key.strip()),
            "type": "json", "pageNo": number, "numOfRows": 100,
        })
        with urllib.request.urlopen(endpoint + "?" + query, timeout=60) as response:
            return json.load(response)
    # Four bounded read requests at most; rows are committed in page order.
    with ThreadPoolExecutor(max_workers=4) as executor, temporary.open("w", encoding="utf-8", newline="\n") as stream:
        pending = {1: executor.submit(fetch_page, 1)}
        while True:
            try:
                payload = pending.pop(page).result()
            except (urllib.error.URLError, ValueError, TimeoutError):
                raise RuntimeError(f"Request failed on page {page}; check approval, connectivity and key. Partial output retained.") from None
            envelope = payload.get("response", payload)
            header = envelope.get("header", {})
            if str(header.get("resultCode")) not in ("00", "0"):
                raise RuntimeError(f"API rejected page {page}; response details suppressed to protect credentials.")
            body = envelope.get("body", {})
            total = int(body["totalCount"])
            if expected is None:
                expected = total
            elif expected != total:
                raise RuntimeError("Source count changed during collection; retry in a new directory.")
            for upcoming in range(page + 1, min(page + 4, (expected + 99) // 100) + 1):
                if upcoming not in pending:
                    pending[upcoming] = executor.submit(fetch_page, upcoming)
            items = body.get("items") or []
            if isinstance(items, dict):
                items = items.get("item", [])
            if isinstance(items, dict):
                items = [items]
            if not items and record_count < expected:
                raise RuntimeError("Unexpected empty page; partial output is not a complete dataset.")
            for item in items:
                if supplements:
                    item = item.get("item", item)
                identifier = str(item.get("STTEMNT_NO" if supplements else "itemSeq") or "").strip()
                if not identifier:
                    raise RuntimeError("Missing product identifier; collection stopped.")
                if identifier in seen:
                    duplicate_count += 1
                seen.add(identifier)
                record_count += 1
                stream.write(json.dumps(item, ensure_ascii=False) + "\n")
            print(f"Page {page}: {record_count}/{expected} records", flush=True)
            if record_count >= expected:
                if record_count != expected:
                    raise RuntimeError("Record count exceeds reported total.")
                break
            page += 1
            time.sleep(0.25)
    temporary.replace(output)
    manifest = {
        "source": endpoint if supplements else SOURCE, "provider": "식품의약품안전처", "dataset": "건강기능식품정보" if supplements else "의약품개요정보(e약은요)",
        "license": "이용허락범위 제한 없음", "collectedAt": dt.datetime.now(dt.timezone.utc).isoformat(),
        "recordCount": record_count, "uniqueProductCount": len(seen),
        "duplicateProductRows": duplicate_count, "pages": page, "status": "complete",
        "sha256": hashlib.sha256(output.read_bytes()).hexdigest(),
        "note": "Official fields preserved. Collection time is not the source revision date. REGIST_DT is a registration date, not a revision date.",
    }
    (destination / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    return record_count


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--key-file", type=Path, help="Private file outside the repository; alternatively use DATA_GO_KR_SERVICE_KEY.")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--supplements", action="store_true", help="Collect approved MFDS health supplement details instead.")
    arguments = parser.parse_args()
    try:
        credential = arguments.key_file.read_text(encoding="utf-8-sig").strip() if arguments.key_file else os.environ.get("DATA_GO_KR_SERVICE_KEY", "").strip()
        if not credential:
            raise RuntimeError("Supply --key-file or DATA_GO_KR_SERVICE_KEY; do not put keys in source control.")
        collect(credential, arguments.output, arguments.supplements)
    except (RuntimeError, OSError, KeyError, TypeError) as error:
        # Do not print external exception text: it may include request URLs or secrets.
        print(str(error) if isinstance(error, RuntimeError) else "Collection failed: check input file, response schema and output permissions.")
        raise SystemExit(1)
