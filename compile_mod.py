#!/usr/bin/env python3
"""Generiert aus mod.py ein vollständiges Fabric-Mod-Projekt und baut es."""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))

from project_fix import patch_fabricpy

patch_fabricpy()

from mod import get_mod  # noqa: E402


def main() -> int:
    mod = get_mod()
    print(f"==> Kompiliere Mod '{mod.name}' ({mod.mod_id}) …")
    print(f"    Zielverzeichnis: {mod.project_dir}")
    mod.compile()
    print("==> Baue mit Gradle …")
    mod.build()
    print()
    print("Fertig. Fabric-Projekt liegt unter:", mod.project_dir)
    print("Nächste Schritte:")
    print("  python run_test.py      # Unit-Tests")
    print("  python run_client.py    # Minecraft Dev-Client (Testlauf)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
