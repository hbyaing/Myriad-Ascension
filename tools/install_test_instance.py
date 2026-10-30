"""Install a verified production JAR into the user's authorized AXO test instance.

Never stops a running game. Existing artifacts are backed up before replacement.
Only archives whose own [[mods]] entry is wuxingxiantu are selected.
"""
import argparse
from datetime import datetime
import hashlib
import json
from pathlib import Path
import re
import shutil
import uuid
import zipfile

ROOT = Path(__file__).resolve().parents[1]
GAME = Path('D:/MC/库/red.ghs.axolotl/profiles/测试 (1)')


def sha(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


def is_our_mod(path):
    with zipfile.ZipFile(path) as archive:
        if 'META-INF/mods.toml' not in archive.namelist():
            return False
        toml = archive.read('META-INF/mods.toml').decode('utf-8')
        for section in re.split(r'(?m)^\s*\[\[mods\]\]\s*$', toml)[1:]:
            section = re.split(r'(?m)^\s*\[', section, maxsplit=1)[0]
            if re.search(r'''(?m)^\s*modId\s*=\s*["']wuxingxiantu["']\s*$''', section):
                return True
    return False


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path, default=ROOT / 'build/libs/myriad-ascension-0.2.jar')
    parser.add_argument("--game-dir", type=Path, help="Explicitly authorized Minecraft instance directory")
    parser.add_argument("--backup-root", type=Path, default=Path("E:/垃圾"))
    args = parser.parse_args()
    source = args.jar.resolve(strict=True)
    game = (args.game_dir or GAME).resolve(strict=True)
    mods = (game / 'mods').resolve(strict=True)
    if mods.parent != game or (args.game_dir is None and not (game / 'axolotl_config.json').is_file()):
        raise RuntimeError('The authorized AXO instance is unavailable; rediscover it first')
    if not is_our_mod(source):
        raise RuntimeError('Source is not the production wuxingxiantu Mod')
    with zipfile.ZipFile(source) as archive:
        if archive.testzip() is not None:
            raise RuntimeError('Source JAR CRC verification failed')
        if any('/qa/myriad/' in name or name.endswith('GameTests.class') for name in archive.namelist()):
            raise RuntimeError('Source contains test fixtures')
    old = [p for p in mods.glob('*.jar') if is_our_mod(p)]
    expected = sha(source)
    target = mods / source.name
    if target.exists() and target not in old:
        raise RuntimeError('Destination filename belongs to a different Mod')
    if len(old) == 1 and old[0] == target and sha(target) == expected:
        record = {'source': str(source), 'target': str(target), 'sha256': expected,
                  'gameDir': str(game), 'status': 'already_installed_on_disk',
                  'runtime': 'Disk package verified; a running JVM may still need a restart.'}
        report = ROOT / 'reports/last-test-instance-installation.json'
        report.parent.mkdir(parents=True, exist_ok=True)
        report.write_text(json.dumps(record, ensure_ascii=False, indent=2), encoding='utf-8')
        print(json.dumps(record, ensure_ascii=False, indent=2))
        return
    stamp = datetime.now().strftime('%Y-%m-%d/万界仙途自动安装-%H%M%S-') + uuid.uuid4().hex[:8]
    backup = args.backup_root / stamp
    backup.mkdir(parents=True, exist_ok=False)
    record = {'source': str(source), 'target': str(target), 'sha256': expected,
              'gameDir': str(game), 'backup': str(backup), 'old': [], 'status': 'backed_up'}
    for path in old:
        saved = backup / path.name
        old_sha = sha(path)
        shutil.copy2(path, saved)
        if sha(saved) != old_sha:
            raise RuntimeError('Backup SHA-256 mismatch: ' + str(path))
        record['old'].append({'name': path.name, 'sha256': old_sha})
    manifest = backup / 'installation.json'
    manifest.write_text(json.dumps(record, ensure_ascii=False, indent=2), encoding='utf-8')
    staged = mods / ('.myriad-install-' + uuid.uuid4().hex + '.tmp')
    shutil.copy2(source, staged)
    if sha(staged) != expected:
        raise RuntimeError('Staged JAR SHA-256 mismatch')
    parked = []
    installed = False
    try:
        for path in old:
            parked_path = mods / ('.retired-' + uuid.uuid4().hex + '.tmp')
            path.rename(parked_path)
            parked.append((path, parked_path))
        staged.rename(target)
        installed = True
        if sha(target) != expected:
            raise RuntimeError('Installed JAR SHA-256 mismatch')
        active = [p for p in mods.glob('*.jar') if is_our_mod(p)]
        if active != [target]:
            raise RuntimeError('Expected exactly one active wuxingxiantu JAR')
    except Exception:
        if installed and target.exists():
            shutil.move(str(target), str(backup / 'failed-new.jar'))
        for original, temporary in reversed(parked):
            temporary.rename(original)
        if staged.exists():
            shutil.move(str(staged), str(backup / 'pending-new.jar'))
        record['status'] = 'failed_original_restored'
        manifest.write_text(json.dumps(record, ensure_ascii=False, indent=2), encoding='utf-8')
        raise
    for index, (_, temporary) in enumerate(parked):
        shutil.move(str(temporary), str(backup / ('retired-original-' + str(index) + '.jar')))
    record['status'] = 'installed_on_disk'
    record['runtime'] = 'Restart required if the game had already loaded the previous JAR; no process was stopped.'
    manifest.write_text(json.dumps(record, ensure_ascii=False, indent=2), encoding='utf-8')
    report = ROOT / 'reports/last-test-instance-installation.json'
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps(record, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(record, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
