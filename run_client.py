#!/usr/bin/env python3
"""Startet den Minecraft Dev-Client mit der Mod (echter Testlauf).

Voraussetzungen:
  - JDK 25 (wird bei Bedarf nach .jdk/ heruntergeladen)
  - Genug RAM
  - Grafik-/Display (lokal in Cursor/VS Code oder Desktop)

In headless Cloud-Umgebungen schlägt runClient oft fehl – nutze dann run_test.py.
"""

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
    print("==> Aktualisiere generiertes Projekt aus mod.py …")
    mod.compile()

    print()
    print(f"==> Starte Minecraft Dev-Client mit '{mod.name}' …")
    print("    (Fenster öffnet sich; Beenden mit Esc → Quit oder Ctrl+C hier)")
    print()
    try:
        mod.run()
    except KeyboardInterrupt:
        print("\nAbgebrochen.")
        return 130
    except FileNotFoundError as exc:
        print("FEHLER:", exc)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
