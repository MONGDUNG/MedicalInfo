"""Configure Docker data mounts relative to an extracted portable bundle."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
data = root.parent / 'data'
datasets = {'HIRA_DATA_DIR': ('hira-202606', 'facilities.jsonl'),
            'DRUG_DATA_DIR': ('mfds-drugs-20261015-v2', 'drugs.jsonl'),
            'SUPPLEMENT_DATA_DIR': ('mfds-supplements-20261015-v2', 'supplements.jsonl')}
for directory, filename in datasets.values():
    if not (data / directory / filename).is_file():
        raise SystemExit(f'Missing dataset: {directory}/{filename}')
settings = ['ELASTICSEARCH_URL=http://elasticsearch:9200', 'ELASTICSEARCH_INDEX=item-hira-202606',
            'HTTP_BIND_ADDRESS=127.0.0.1', 'HTTP_PORT=8080', 'ALLOWED_HOSTS=localhost;127.0.0.1']
for key, (directory, _) in datasets.items():
    settings.append(f"{key}='{(data / directory).as_posix()}'")
try:
    with (root / '.env').open('x', encoding='utf-8') as stream:
        stream.write('\n'.join(settings) + '\n')
except FileExistsError:
    raise SystemExit('Existing .env preserved; edit it manually if paths have changed.')
print('Created .env. Run the Docker Compose commands in README.md.')
