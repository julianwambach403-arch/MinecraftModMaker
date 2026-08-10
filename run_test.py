#!/usr/bin/env python3
"""Testlauf ohne Minecraft-Fenster.

1. Python-Definitionstests (Items/Blöcke/Texturen)
2. Mod aus mod.py generieren
3. Gradle-Compile + JAR-Build

Für den echten Ingame-Testlauf: python run_client.py
"""

from __future__ import annotations

import os
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))

from project_fix import patch_fabricpy

patch_fabricpy()

from mod import get_mod  # noqa: E402


def run_python_tests() -> int:
    print("==> Python-Definitionstests …")
    return subprocess.run(
        [sys.executable, "-m", "unittest", "discover", "-s", "tests", "-v"],
        cwd=str(ROOT),
    ).returncode


def ensure_compiled(mod) -> None:
    print("==> Aktualisiere generiertes Projekt aus mod.py …")
    mod.compile()
    # Alte, inkompatible fabricpy-JUnit-Tests entfernen
    test_java = Path(mod.project_dir) / "src" / "test" / "java"
    if test_java.exists():
        shutil.rmtree(test_java)
        print("  ✔ veraltete src/test/java entfernt")


def run_gradle_build(project_dir: Path) -> int:
    gradlew = project_dir / "gradlew"
    if not gradlew.exists():
        print(
            f"FEHLER: {gradlew} nicht gefunden. "
            "Bitte zuerst python compile_mod.py ausführen."
        )
        return 1

    gradlew.chmod(gradlew.stat().st_mode | 0o111)
    env = os.environ.copy()
    env.setdefault("JAVA_TOOL_OPTIONS", "-Djava.awt.headless=true")

    print("==> Gradle-Compile + JAR-Build (./gradlew build -x test) …")
    return subprocess.run(
        [str(gradlew), "build", "-x", "test", "--console=plain"],
        cwd=str(project_dir),
        env=env,
    ).returncode


def main() -> int:
    code = run_python_tests()
    if code != 0:
        print("\n✗ Python-Tests fehlgeschlagen")
        return code

    print()
    mod = get_mod()
    ensure_compiled(mod)
    code = run_gradle_build(Path(mod.project_dir))
    if code == 0:
        jars = list((Path(mod.project_dir) / "build" / "libs").glob("*.jar"))
        print()
        print("✓ Testlauf erfolgreich.")
        if jars:
            print("  JAR:", ", ".join(str(j) for j in jars))
        print("  Minecraft-Client starten:  python run_client.py")
        print("  Oder in Cursor/VS Code:    F5 → „Minecraft Testlauf (Dev-Client)“")
    else:
        print()
        print("✗ Gradle-Build fehlgeschlagen (Exit-Code", code, ")")
    return code


if __name__ == "__main__":
    raise SystemExit(main())
