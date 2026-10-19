"""Combine verified normalized datasets, preserving every stable source identifier."""
import argparse
import collections
import hashlib
import json
from pathlib import Path


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def merge(sources, output):
    output.mkdir(parents=True, exist_ok=False)
    inputs = []
    for directory in sources:
        manifest = json.loads((directory / 'manifest.json').read_text(encoding='utf-8'))
        if digest(directory / 'facilities.jsonl') != manifest['sha256']:
            raise ValueError('Facility source checksum mismatch')
        inputs.append({'directory': directory.name, 'sha256': manifest['sha256'], 'metadata': manifest})
    ids = set()
    counts = collections.Counter()
    with (output / 'facilities.jsonl.partial').open('w', encoding='utf-8', newline='\n') as target:
        for directory in sources:
            with (directory / 'facilities.jsonl').open(encoding='utf-8') as source:
                for line in source:
                    row = json.loads(line)
                    identifier = row.get('ID')
                    if not isinstance(identifier, str) or not identifier or identifier in ids:
                        raise ValueError('Missing or duplicate facility identifier')
                    ids.add(identifier)
                    counts[row['CATEGORY_NAME']] += 1
                    target.write(json.dumps(row, ensure_ascii=False) + '\n')
    if not ids:
        raise ValueError('No facilities')
    result = output / 'facilities.jsonl'
    result.with_suffix('.jsonl.partial').rename(result)
    manifest = {'status': 'complete', 'recordCount': len(ids), 'counts': dict(counts),
                'sources': inputs, 'sha256': digest(result),
                'note': 'Source identifiers remain unchanged. Separate source datasets are not linked by similar names or addresses.'}
    (output / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
    return manifest


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, action='append', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(merge(args.source, args.output)['counts'], ensure_ascii=False))
