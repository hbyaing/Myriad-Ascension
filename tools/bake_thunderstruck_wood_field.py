"""Bake the approved workbench's surface distance, travel distance and strength."""

from pathlib import Path
import struct
import subprocess

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
WORKBENCH = ROOT / "docs/previews/thunderstruck-wood/preview.js"
FIELD = ROOT / "src/main/resources/assets/wuxingxiantu/textures/effect/thunderstruck_wood_field.png"


def main():
    raw = subprocess.check_output([
        "node", "-e",
        """
        const {w, h, bytes} = require(process.argv[1]).buildField();
        const size = Buffer.alloc(8);
        size.writeUInt32BE(w, 0);
        size.writeUInt32BE(h, 4);
        process.stdout.write(Buffer.concat([size, Buffer.from(bytes)]));
        """,
        str(WORKBENCH),
    ])
    size = struct.unpack_from(">II", raw)
    FIELD.parent.mkdir(parents=True, exist_ok=True)
    Image.frombytes("RGBA", size, raw[8:]).convert("RGB").save(FIELD, optimize=True)


if __name__ == "__main__":
    main()
