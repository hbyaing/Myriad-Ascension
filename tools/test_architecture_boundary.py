"""Prove the guard rejects a real forbidden type, including without an import statement."""
import os
from pathlib import Path
import shutil
import tempfile
import unittest
from check_architecture_boundary import check, RESULT


class BoundaryGuard(unittest.TestCase):
    def setUp(self):
        self.javac = os.environ.get('ARCHITECTURE_TEST_JAVAC') or shutil.which('javac')
        if not self.javac:
            self.fail('A Java compiler is required for the boundary guard tests')

    def test_actual_result_is_independent(self):
        check(RESULT, self.javac)

    def test_fully_qualified_game_dependency_is_rejected(self):
        with tempfile.TemporaryDirectory(prefix='myriad-boundary-counterexample-') as temp:
            source = Path(temp) / 'BatchStartResult.java'
            source.write_text('public record BatchStartResult(net.minecraft.world.level.Level level) {}', encoding='utf-8')
            with self.assertRaisesRegex(RuntimeError, 'unavailable dependencies'):
                check(source, self.javac)


if __name__ == '__main__':
    unittest.main(verbosity=2)
