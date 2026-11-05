"""Compile the verified admission result boundary with Java 17 and no game classpath."""
import argparse
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
RESULT = ROOT / 'src/main/java/cn/blockforge/generated/generatedmod/alchemy/batch/BatchStartResult.java'


def check(source, javac):
    # Copy into an otherwise empty directory: javac cannot silently discover other repository sources.
    with tempfile.TemporaryDirectory(prefix='myriad-admission-boundary-') as temp:
        temp = Path(temp)
        copied = temp / 'BatchStartResult.java'
        shutil.copyfile(source, copied)
        result = subprocess.run([str(javac), '--release', '17', '-encoding', 'UTF-8',
                                 '-classpath', str(temp), '-sourcepath', str(temp),
                                 '-d', str(temp / 'classes'), str(copied)],
                                cwd=temp, text=True, capture_output=True)
        if result.returncode:
            raise RuntimeError('Admission result requires unavailable dependencies or fails Java 17 compilation:\n'
                               + result.stdout + result.stderr)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--javac', default='javac')
    args = parser.parse_args()
    check(RESULT, args.javac)
    print('Architecture boundary: BatchStartResult compiles in isolation on Java 17 (no Forge/Minecraft/consumer dependency).')


if __name__ == '__main__':
    main()
