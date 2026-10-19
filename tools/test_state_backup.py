from contextlib import closing
import hashlib
import json
from pathlib import Path
import sqlite3
import tempfile
import unittest
import zipfile
from state_backup import backup, restore


class StateBackupTests(unittest.TestCase):
    def test_round_trip_with_wal_and_private_attachment(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            state = root / 'original'
            (state / 'keys').mkdir(parents=True)
            (state / 'keys/key-test.xml').write_text('<key>fixture</key>')
            with closing(sqlite3.connect(state / 'members.db')) as db:
                db.execute('PRAGMA journal_mode=WAL')
                db.execute('CREATE TABLE attachments(id INTEGER PRIMARY KEY, bytes BLOB)')
                db.execute('INSERT INTO attachments VALUES(1, ?)', (b'private-image',))
                db.commit()
                backup(state, root / 'backup.zip')
            restore(root / 'backup.zip', root / 'restored')
            with closing(sqlite3.connect(root / 'restored/members.db')) as db:
                self.assertEqual(db.execute('SELECT bytes FROM attachments').fetchone()[0], b'private-image')
            self.assertEqual((root / 'restored/keys/key-test.xml').read_text(), '<key>fixture</key>')
            with self.assertRaises(ValueError):
                restore(root / 'backup.zip', state)
            with self.assertRaises(ValueError):
                backup(state, root / 'backup.zip')

    def test_corrupt_and_traversal_archives_leave_no_state(self):
        for name in ['../escape.xml', 'keys/../../escape.xml', 'keys/C:evil.xml', 'keys/key.xml']:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as temporary:
                root = Path(temporary)
                with zipfile.ZipFile(root / 'bad.zip', 'w') as archive:
                    archive.writestr('members.db', b'not-a-db')
                    archive.writestr(name, b'x')
                    archive.writestr('manifest.json', json.dumps({'version': 1, 'files': {
                        'members.db': hashlib.sha256(b'not-a-db').hexdigest(), name: 'incorrect-hash'}}))
                with self.assertRaises(ValueError):
                    restore(root / 'bad.zip', root / 'restored')
                self.assertFalse((root / 'restored').exists())
                self.assertFalse((root / 'escape.xml').exists())


if __name__ == '__main__':
    unittest.main()
