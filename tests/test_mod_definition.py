#!/usr/bin/env python3
"""Leichte Python-Tests für die Mod-Definition (ohne Gradle)."""

from __future__ import annotations

import unittest
from pathlib import Path

from mod import get_mod


class TestModDefinition(unittest.TestCase):
    def setUp(self) -> None:
        self.mod = get_mod()

    def test_mod_metadata(self) -> None:
        self.assertEqual(self.mod.mod_id, "rubymod")
        self.assertTrue(self.mod.name)
        self.assertTrue(self.mod.version)
        self.assertTrue(self.mod.authors)

    def test_has_items_and_blocks(self) -> None:
        self.assertGreaterEqual(len(self.mod.registered_items), 1)
        self.assertGreaterEqual(len(self.mod.registered_blocks), 1)

    def test_item_ids_use_mod_namespace(self) -> None:
        for item in self.mod.registered_items:
            self.assertTrue(
                item.id.startswith("rubymod:"),
                f"Item-ID ohne Namespace: {item.id}",
            )

    def test_block_ids_use_mod_namespace(self) -> None:
        for block in self.mod.registered_blocks:
            self.assertTrue(
                block.id.startswith("rubymod:"),
                f"Block-ID ohne Namespace: {block.id}",
            )

    def test_textures_exist(self) -> None:
        root = Path(__file__).resolve().parent.parent
        for item in self.mod.registered_items:
            path = getattr(item, "texture_path", None)
            if path:
                self.assertTrue(Path(path).is_file(), f"Textur fehlt: {path}")
        for block in self.mod.registered_blocks:
            path = getattr(block, "block_texture_path", None)
            if path:
                self.assertTrue(Path(path).is_file(), f"Textur fehlt: {path}")


if __name__ == "__main__":
    unittest.main()
