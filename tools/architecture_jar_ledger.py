"""Read-only production artifact ledger. Byte duplicates are candidates, never deletion advice."""
import argparse
from collections import defaultdict
import hashlib
import json
from pathlib import Path
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def category(name):
    if name.endswith('.class'):
        return 'classes'
    if name.startswith('assets/'):
        parts = name.split('/')
        return 'assets/' + (parts[2] if len(parts) > 2 else 'other')
    if name.startswith('data/'):
        return 'data'
    if name.startswith('META-INF/'):
        return 'metadata-and-nested-dependencies'
    return 'other-runtime-resources'


def ledger(jar):
    groups = defaultdict(lambda: {'entries': 0, 'rawBytes': 0, 'compressedBytes': 0})
    duplicates = defaultdict(list)
    with zipfile.ZipFile(jar) as archive:
        entries = [e for e in archive.infolist() if not e.is_dir()]
        bad = archive.testzip()
        if bad:
            raise ValueError(f'Invalid ZIP CRC: {bad}')
        for e in entries:
            group = groups[category(e.filename)]
            group['entries'] += 1
            group['rawBytes'] += e.file_size
            group['compressedBytes'] += e.compress_size
            if e.filename.startswith(('assets/', 'data/')) and e.file_size > 0:
                digest = hashlib.sha256(archive.read(e)).hexdigest()
                duplicates[(digest, e.file_size)].append(e.filename)
        # Capture source identity, not a promise that every source/input was fully validated.
        head = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
        status = subprocess.check_output(['git', 'status', '--porcelain'], cwd=ROOT, text=True)
        fixture_names = set()
        for source_set in ('gameTest', 'admissionExample', 'architectureClientTest'):
            fixture_root = ROOT / 'build/classes/java' / source_set
            fixture_names.update(p.relative_to(fixture_root).as_posix() for p in fixture_root.rglob('*.class'))
        forbidden = [e.filename for e in entries if
                     e.filename.startswith(('source-assets/', 'reference_assets/', 'software/bernie/'))
                     or '/qa/myriad/' in e.filename or e.filename.endswith('GameTests.class')
                     or e.filename in fixture_names]
        return {
            'schema': 1, 'jar': str(jar), 'sha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
            'sourceHead': head, 'workingTreeDirty': bool(status.strip()),
            'compiledNonProductionClassNamesChecked': len(fixture_names),
            'jarBytes': jar.stat().st_size, 'entries': len(entries),
            'rawBytes': sum(e.file_size for e in entries),
            'compressedPayloadBytes': sum(e.compress_size for e in entries),
            'categories': dict(sorted(groups.items())),
            'largestCompressedEntries': [
                {'path': e.filename, 'rawBytes': e.file_size, 'compressedBytes': e.compress_size}
                for e in sorted(entries, key=lambda e: e.compress_size, reverse=True)[:25]],
            'exactResourceDuplicateGroups': [
                {'sha256': key[0], 'rawBytesEach': key[1], 'paths': paths}
                for key, paths in sorted(duplicates.items(), key=lambda pair: pair[0][1] * (len(pair[1])-1), reverse=True)
                if len(paths) > 1],
            'forbiddenSourceOrFixtureEntries': forbidden,
            'limits': 'ZIP bytes are not heap, VRAM, load-time, or frame-time measurements. '
                      'Equal resource bytes can serve distinct resource locations; do not delete automatically.'
        }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('jar', type=Path)
    parser.add_argument('--report', required=True, type=Path)
    args = parser.parse_args()
    report = ledger(args.jar.resolve(strict=True))
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({k: report[k] for k in ('sha256', 'jarBytes', 'entries', 'rawBytes', 'forbiddenSourceOrFixtureEntries')}, ensure_ascii=False))
    if report['forbiddenSourceOrFixtureEntries']:
        raise SystemExit('Unexpected source/stub/QA entries in production artifact')


if __name__ == '__main__':
    main()
