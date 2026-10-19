"""Build a portable source/data bundle without API keys or local configuration."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import zipfile
import re


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def package(root, data_root, output, facilities_dir='hira-202606', facilities_index='item-hira-202606', permits_dir=None):
    if output.exists():
        raise ValueError('Output already exists; choose a new filename')
    if not re.fullmatch(r'[a-z0-9][a-z0-9_-]{0,99}',facilities_dir) or not re.fullmatch(r'[a-z0-9][a-z0-9_-]{0,127}',facilities_index):
        raise ValueError('Invalid facilities directory/index')
    datasets = {
        facilities_dir: 'facilities.jsonl',
        'mfds-drugs-20261015-v2': 'drugs.jsonl',
        'mfds-supplements-20261015-v2': 'supplements.jsonl',
    }
    metadata = {'facilitiesDirectory': facilities_dir, 'elasticsearchIndex': facilities_index}
    if permits_dir is not None:
        if not re.fullmatch(r'[a-z0-9][a-z0-9_-]{0,99}', permits_dir) or permits_dir in datasets:
            raise ValueError('Invalid or duplicate permit directory')
        datasets[permits_dir] = 'permits.jsonl'
        metadata['permitsDirectory'] = permits_dir
    inputs = []
    for directory, filename in datasets.items():
        source = data_root / directory
        manifest = json.loads((source / 'manifest.json').read_text(encoding='utf-8'))
        if digest(source / filename) != manifest['sha256']:
            raise ValueError(f'Dataset checksum mismatch: {directory}')
        inputs.extend((source / name, f'data/{directory}/{name}') for name in (filename, 'manifest.json'))
        if filename == 'permits.jsonl' and 'detailsSha256' in manifest:
            if digest(source/'permit-details.jsonl') != manifest['detailsSha256']:
                raise ValueError('Permit detail checksum mismatch')
            inputs.append((source/'permit-details.jsonl', f'data/{directory}/permit-details.jsonl'))
    tracked = subprocess.check_output(['git', 'ls-files', '-z'], cwd=root).decode().split('\0')
    for relative in filter(None, tracked):
        path = Path(relative)
        if path.name == '.env' or path.suffix.lower() in ('.key', '.pfx', '.pem'):
            raise ValueError(f'Private configuration must not be tracked: {relative}')
        inputs.append((root / path, 'MedicalInfo/' + path.as_posix()))
    if subprocess.check_output(['git', 'status', '--porcelain'], cwd=root).strip():
        raise ValueError('Commit or reconcile workspace changes before packaging')
    commit = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root).decode().strip()
    temporary = output.with_suffix(output.suffix + '.partial')
    with zipfile.ZipFile(temporary, 'x', compression=zipfile.ZIP_DEFLATED) as archive:
        for source, destination in inputs:
            archive.write(source, destination)
        archive.writestr('MedicalInfo/portable-data.json',json.dumps(metadata))
    with zipfile.ZipFile(temporary) as archive:
        if archive.testzip():
            raise ValueError('Bundle checksum verification failed')
    temporary.rename(output)
    report = {'file': output.name, 'sourceCommit': commit, 'bytes': output.stat().st_size,
              'sha256': digest(output), 'datasets': datasets, 'elasticsearchIndex': facilities_index}
    output.with_suffix('.sha256.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps(report))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--data-root', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--facilities-dir',default='hira-202606')
    parser.add_argument('--facilities-index',default='item-hira-202606')
    parser.add_argument('--permits-dir', help='Optional approved OTC permit dataset directory under data-root')
    args = parser.parse_args()
    package(Path(__file__).resolve().parents[1], args.data_root.resolve(), args.output.resolve(),args.facilities_dir,args.facilities_index,args.permits_dir)
