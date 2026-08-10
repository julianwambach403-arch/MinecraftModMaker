#!/usr/bin/env python3
"""Führt schnelle Python-Definitionstests + optional Gradle-Tests aus."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent


def main() -> int:
    print("==> Python-Definitionstests …")
    r1 = subprocess.run(
        [sys.executable, "-m", "unittest", "discover", "-s", "tests", "-v"],
        cwd=str(ROOT),
    )
    if r1.returncode != 0:
        return r1.returncode

    # Gradle-Tests über run_test.py (kompiliert bei Bedarf)
    if "--gradle" in sys.argv or "--full" in sys.argv:
        print()
        r2 = subprocess.run([sys.executable, "run_test.py"], cwd=str(ROOT))
        return r2.returncode

    print()
    print("✓ Definitionstests OK.")
    print("  Vollständiger Testlauf:  python run_test.py")
    print("  Oder:                    python test_all.py --full")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
