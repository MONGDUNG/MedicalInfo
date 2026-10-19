"""Configure verified Docker data mounts relative to an extracted portable bundle."""
import hashlib
import json
from pathlib import Path
import re


def configure(root):
    data = root.parent / 'data'
    metadata = root / 'portable-data.json'
    config = json.loads(metadata.read_text(encoding='utf-8')) if metadata.exists() else {}
    facilities = config.get('facilitiesDirectory','hira-202606')
    index = config.get('elasticsearchIndex','item-hira-202606')
    if not isinstance(facilities,str) or not re.fullmatch(r'[a-z0-9][a-z0-9_-]{0,99}',facilities) or not isinstance(index,str) or not re.fullmatch(r'[a-z0-9][a-z0-9_-]{0,127}',index):
        raise ValueError('Invalid portable data metadata')
    datasets = {'HIRA_DATA_DIR': (facilities, 'facilities.jsonl'),
                'DRUG_DATA_DIR': ('mfds-drugs-20261015-v2', 'drugs.jsonl'),
                'SUPPLEMENT_DATA_DIR': ('mfds-supplements-20261015-v2', 'supplements.jsonl')}
    if 'permitsDirectory' in config:
        permits = config['permitsDirectory']
        if not isinstance(permits,str) or not re.fullmatch(r'[a-z0-9][a-z0-9_-]{0,99}',permits) or permits in [entry[0] for entry in datasets.values()]:
            raise ValueError('Invalid permit data metadata')
        datasets['DRUG_PERMIT_DATA_DIR'] = (permits, 'permits.jsonl')
    for directory, filename in datasets.values():
        path = data / directory / filename
        if not path.is_file():
            raise ValueError(f'Missing dataset: {directory}/{filename}')
        manifest = json.loads((path.parent / 'manifest.json').read_text(encoding='utf-8'))
        with path.open('rb') as stream:
            if hashlib.file_digest(stream,'sha256').hexdigest()!=manifest['sha256']:
                raise ValueError(f'Dataset checksum mismatch: {directory}')
        if filename == 'permits.jsonl' and 'detailsSha256' in manifest:
            with (path.parent/'permit-details.jsonl').open('rb') as stream:
                if hashlib.file_digest(stream,'sha256').hexdigest()!=manifest['detailsSha256']:
                    raise ValueError('Permit detail checksum mismatch')
    settings = ['ELASTICSEARCH_URL=http://elasticsearch:9200', 'ELASTICSEARCH_INDEX='+index,
                'HTTP_BIND_ADDRESS=127.0.0.1', 'HTTP_PORT=8080', 'ALLOWED_HOSTS=localhost;127.0.0.1']
    for key, (directory, _) in datasets.items():
        settings.append(f"{key}='{(data / directory).as_posix()}'")
    with (root / '.env').open('x', encoding='utf-8') as stream:
        stream.write('\n'.join(settings) + '\n')


if __name__=='__main__':
    try:
        configure(Path(__file__).resolve().parents[1])
    except FileExistsError:
        raise SystemExit('Existing .env preserved; edit it manually if paths have changed.')
    except (ValueError,OSError,KeyError):
        raise SystemExit('Configuration failed; check bundle metadata, files and checksums. Existing .env is preserved.')
    print('Created .env. Run the Docker Compose commands in README.md.')
