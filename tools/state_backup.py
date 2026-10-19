"""Back up/restore private MedicalInfo state. Stop the web app before either command."""
from contextlib import closing
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import sqlite3
import tempfile
import zipfile


def checksum(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def verify_db(path):
    with closing(sqlite3.connect(path.as_uri() + '?mode=ro', uri=True)) as db:
        if db.execute('PRAGMA integrity_check').fetchone() != ('ok',):
            raise ValueError('SQLite integrity check failed')
        if db.execute('PRAGMA foreign_key_check').fetchone():
            raise ValueError('SQLite foreign key check failed')


def backup(state, output):
    state, output = state.resolve(), output.resolve()
    if output.exists() or output.is_relative_to(state):
        raise ValueError('Choose a new output outside the state directory')
    database = state / 'members.db'
    keys = sorted((state / 'keys').glob('*.xml'))
    if not database.is_file() or not keys:
        raise ValueError('members.db and authentication keys are required')
    if database.is_symlink() or (state / 'keys').is_symlink() or any(p.is_symlink() for p in keys):
        raise ValueError('State must not contain symbolic links')
    output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='medicalinfo-backup-', dir=output.parent) as temporary:
        root = Path(temporary)
        copy = root / 'members.db'
        with closing(sqlite3.connect(database.as_uri() + '?mode=ro', uri=True)) as source:
            with closing(sqlite3.connect(copy)) as target:
                source.backup(target)
        verify_db(copy)
        files = {'members.db': copy}
        for key in keys:
            destination = root / key.name
            shutil.copyfile(key, destination)
            files['keys/' + key.name] = destination
        manifest = {'version': 1, 'files': {name: checksum(path) for name, path in files.items()}}
        archive_path = root / 'backup.zip'
        with zipfile.ZipFile(archive_path, 'x', compression=zipfile.ZIP_DEFLATED) as archive:
            for name, path in files.items():
                archive.write(path, name)
            archive.writestr('manifest.json', json.dumps(manifest))
        # Exclusive creation prevents overwriting an existing backup, even after a race.
        with os.fdopen(os.open(output, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600), 'wb') as destination, archive_path.open('rb') as source:
            shutil.copyfileobj(source, destination)


def restore(archive_path, destination):
    destination = destination.resolve()
    if destination.exists():
        raise ValueError('Restore destination must not exist; never overwrite running state')
    destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='medicalinfo-restore-', dir=destination.parent) as temporary:
        root = Path(temporary) / 'state'
        root.mkdir(mode=0o700)
        with zipfile.ZipFile(archive_path) as archive:
            entries = archive.infolist()
            names = [entry.filename for entry in entries]
            if len(names) != len(set(names)) or sum(entry.file_size for entry in entries) > 4_000_000_000:
                raise ValueError('Duplicate entries or backup exceeds 4 GB restore limit')
            if 'manifest.json' not in names or archive.getinfo('manifest.json').file_size > 1_000_000:
                raise ValueError('Missing or oversized manifest')
            manifest = json.loads(archive.read('manifest.json'))
            files = manifest.get('files', {})
            if manifest.get('version') != 1 or not isinstance(files, dict) or 'members.db' not in files:
                raise ValueError('Unsupported backup manifest')
            if set(names) != set(files) | {'manifest.json'}:
                raise ValueError('Unexpected or missing archive entries')
            if not any(name.startswith('keys/') for name in files):
                raise ValueError('Missing authentication keys')
            for name, expected in files.items():
                # Strict whitelist works identically on Windows and Unix; no extractall.
                import re
                if name != 'members.db' and not re.fullmatch(r'keys/[a-zA-Z0-9_-]+\.xml', name):
                    raise ValueError('Invalid archive path')
                target = root / name
                target.parent.mkdir(exist_ok=True, mode=0o700)
                digest = hashlib.sha256()
                with archive.open(name) as source, target.open('xb') as output:
                    while block := source.read(1024 * 1024):
                        digest.update(block)
                        output.write(block)
                target.chmod(0o600)
                if digest.hexdigest() != expected:
                    raise ValueError('Backup checksum mismatch')
        verify_db(root / 'members.db')
        # Reserve destination exclusively. On failure, no pre-existing state is changed.
        destination.mkdir(mode=0o700)
        try:
            for item in root.iterdir():
                shutil.move(str(item), str(destination / item.name))
        except Exception:
            shutil.rmtree(destination)
            raise


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['backup', 'restore'])
    parser.add_argument('--state', type=Path, required=True, help='Source for backup; NEW directory for restore')
    parser.add_argument('--archive', type=Path, required=True)
    parser.add_argument('--app-stopped', action='store_true', required=True,
                        help='Confirm the web app is stopped so keys and DB remain consistent')
    args = parser.parse_args()
    if args.action == 'backup':
        backup(args.state, args.archive)
    else:
        restore(args.archive, args.state)
    print('Completed. This private archive contains member data and authentication keys; store it securely.')
