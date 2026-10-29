"""Run a second hidden QA JVM from the actual owned host's launch command on Windows.

Capture only the V5Host BootstrapLauncher process (not an unrelated user's client) into
a local JSON with ProcessId/CommandLine. This bypasses Gradle's single-project execution
lock while retaining the resolved Forge launch arguments. Never use a user's login command.
"""
import argparse
import ctypes
import json
import os
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--host-command-json', type=Path, required=True)
    parser.add_argument('--game-dir', type=Path, required=True)
    parser.add_argument('--log', type=Path, required=True)
    args = parser.parse_args()
    if os.name != 'nt':
        raise RuntimeError('This QA launch adapter is for Windows only')
    game = args.game_dir.resolve()
    if not game.is_relative_to(ROOT / 'build' / 'v5-final-acceptance'):
        raise RuntimeError('Peer must use its own disposable acceptance directory')
    data = json.loads(args.host_command_json.read_text(encoding='utf-8-sig'))
    shell = ctypes.windll.shell32
    shell.CommandLineToArgvW.argtypes = [ctypes.c_wchar_p, ctypes.POINTER(ctypes.c_int)]
    shell.CommandLineToArgvW.restype = ctypes.POINTER(ctypes.c_wchar_p)
    count = ctypes.c_int()
    argv = shell.CommandLineToArgvW(data['CommandLine'], ctypes.byref(count))
    if not argv:
        raise RuntimeError('Cannot parse QA launch command')
    try:
        command = [argv[i] for i in range(count.value)]
    finally:
        ctypes.windll.kernel32.LocalFree(argv)
    if ('cpw.mods.bootstraplauncher.BootstrapLauncher' not in command
            or 'forgeclientuserdev' not in command
            or '-Dwuxingxiantu.v5NetworkRole=host' not in command
            or '--username' not in command
            or command[command.index('--username') + 1] != 'V5Host'):
        raise RuntimeError('Refuses an unrelated or authenticated user client')
    command = [arg.replace('-Dwuxingxiantu.v5NetworkRole=host',
                           '-Dwuxingxiantu.v5NetworkRole=peer') for arg in command]
    command[command.index('--username') + 1] = 'V5Peer'
    environment = os.environ.copy()
    environment['MOD_CLASSES'] = ';'.join('wuxingxiantu%%' + str(ROOT / path) for path in (
        'build/resources/main', 'build/classes/java/main',
        'build/resources/v5ClientTest', 'build/classes/java/v5ClientTest'))
    game.mkdir(parents=True, exist_ok=True)
    args.log.parent.mkdir(parents=True, exist_ok=True)
    with args.log.open('w', encoding='utf-8') as log:
        process = subprocess.Popen(command, cwd=game, env=environment, stdout=log,
                                   stderr=subprocess.STDOUT,
                                   creationflags=subprocess.CREATE_NO_WINDOW)
        print(json.dumps({'peerPid': process.pid, 'hostPid': data['ProcessId']}), flush=True)
        code = process.wait()
    # A zero JVM exit is not acceptance: inspect both host-result and peer-result JSON.
    raise SystemExit(code)


if __name__ == '__main__':
    main()
