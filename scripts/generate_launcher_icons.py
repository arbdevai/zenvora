#!/usr/bin/env python3
"""
Android Adaptive & Themed Vector Icon Generator (Pure Python - No External Dependencies)
Menghasilkan Adaptive Vector Icons modern:
- res/drawable/ic_launcher_background.xml (Vector gradient/solid)
- res/drawable/ic_launcher_foreground.xml (Vector simbol/monogram di safe zone 66dp)
- res/drawable/ic_launcher_monochrome.xml (Dukungan penuh Android 13+ Material You Themed Icons)
- res/mipmap-anydpi-v26/ic_launcher.xml & ic_launcher_round.xml
Bebas 'AI Slop' & bebas gambar wifi petir jadul.
"""

import os
import sys
import re
import argparse
import zlib
import struct
import math

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# Vektor simbol geometris presisi di dalam safe zone (Viewport 108x108, center 54, 54)
PRESET_PATHS = {
    "keyboard": """
        M24,34 h60 a8,8 0 0,1 8,8 v24 a8,8 0 0,1 -8,8 h-60 a8,8 0 0,1 -8,-8 v-24 a8,8 0 0,1 8,-8 z
        M32,43 h7 v6 h-7 z M45,43 h7 v6 h-7 z M58,43 h7 v6 h-7 z M71,43 h7 v6 h-7 z
        M32,55 h7 v6 h-7 z M44,55 h22 v6 h-22 z M71,55 h7 v6 h-7 z
    """,
    "code": """
        M40,38 L26,54 L40,70
        M68,38 L82,54 L68,70
        M58,34 L50,74
    """,
    "chat": """
        M32,32 h44 a10,10 0 0,1 10,10 v18 a10,10 0 0,1 -10,10 h-24 l-14,9 v-9 h-6 a10,10 0 0,1 -10,-10 v-18 a10,10 0 0,1 10,-10 z
        M44,50 a3,3 0 1,1 0,-0.01 z
        M54,50 a3,3 0 1,1 0,-0.01 z
        M64,50 a3,3 0 1,1 0,-0.01 z
    """,
    "tool": """
        M54,28 A26,26 0 1,0 54,80 A26,26 0 1,0 54,28 Z
        M54,42 A12,12 0 1,1 54,66 A12,12 0 1,1 54,42 Z
        M51,22 h6 v10 h-6 z M51,76 h6 v10 h-6 z
        M22,51 h10 v6 h-10 z M76,51 h10 v6 h-10 z
    """,
    "app": """
        M54,26 L80,52 L54,78 L28,52 Z
        M54,36 L70,52 L54,68 L38,52 Z
    """
}

# Monogram geometris tebal A-Z untuk safe-zone 108x108
LETTER_PATHS = {
    "A": "M36,74 L54,32 L72,74 h-8 L54,50 L44,74 Z M47,56 h14 v6 h-14 Z",
    "B": "M38,32 h18 c6,0 10,3 10,8 c0,4 -3,7 -7,8 c5,1 9,4 9,9 c0,6 -5,9 -12,9 h-18 Z M46,40 v8 h10 c3,0 5,-1 5,-4 c0,-3 -2,-4 -5,-4 Z M46,54 v12 h11 c3,0 6,-2 6,-6 c0,-4 -3,-6 -6,-6 Z",
    "C": "M68,40 c-3,-4 -8,-6 -14,-6 c-12,0 -18,9 -18,20 c0,11 6,20 18,20 c6,0 11,-2 14,-6 v9 c-4,3 -9,4 -14,4 c-17,0 -26,-12 -26,-27 c0,-15 9,-27 26,-27 c5,0 10,1 14,4 Z",
    "D": "M38,32 h16 c14,0 22,9 22,22 c0,13 -8,22 -22,22 h-16 Z M46,40 v28 h8 c9,0 14,-6 14,-14 c0,-8 -5,-14 -14,-14 Z",
    "E": "M38,32 h28 v8 h-20 v10 h16 v8 h-16 v10 h20 v8 h-28 Z",
    "F": "M38,32 h28 v8 h-20 v12 h16 v8 h-16 v16 h-8 Z",
    "G": "M68,40 c-3,-4 -8,-6 -14,-6 c-12,0 -18,9 -18,20 c0,11 6,20 18,20 c5,0 9,-2 12,-5 v-11 h-12 v-8 h20 v23 c-5,5 -12,8 -20,8 c-17,0 -26,-12 -26,-27 c0,-15 9,-27 26,-27 c6,0 11,2 14,6 Z",
    "H": "M38,32 h8 v18 h18 v-18 h8 v44 h-8 v-18 h-18 v18 h-8 Z",
    "I": "M44,32 h20 v8 h-6 v28 h6 v8 h-20 v-8 h6 v-28 h-6 Z",
    "J": "M60,32 h8 v32 c0,9 -5,14 -14,14 c-8,0 -14,-4 -16,-10 l7,-4 c2,4 5,6 9,6 c4,0 6,-2 6,-6 Z",
    "K": "M38,32 h8 v18 l18,-18 h11 l-20,20 l21,24 h-11 l-16,-19 v19 h-8 Z",
    "L": "M38,32 h8 v36 h22 v8 h-30 Z",
    "M": "M34,74 v-42 h8 l12,22 l12,-22 h8 v42 h-8 v-26 l-12,22 h-1 l-12,-22 v26 Z",
    "N": "M38,74 v-42 h8 l18,26 v-26 h8 v42 h-8 l-18,-26 v26 Z",
    "O": "M54,30 c15,0 24,11 24,24 c0,13 -9,24 -24,24 c-15,0 -24,-11 -24,-24 c0,-13 9,-24 24,-24 Z M54,38 c-10,0 -16,7 -16,16 c0,9 6,16 16,16 c10,0 16,-7 16,-16 c0,-9 -6,-16 -16,-16 Z",
    "P": "M38,32 h18 c8,0 14,4 14,12 c0,8 -6,12 -14,12 h-10 v20 h-8 Z M46,40 v16 h10 c4,0 6,-2 6,-5 c0,-3 -2,-5 -6,-5 Z",
    "Q": "M54,30 c15,0 24,11 24,24 c0,6 -2,12 -6,16 l6,6 l-6,6 l-6,-6 c-3,1 -6,2 -8,2 c-15,0 -24,-11 -24,-24 c0,-13 9,-24 24,-24 Z M54,38 c-10,0 -16,7 -16,16 c0,9 6,16 16,16 c10,0 16,-7 16,-16 c0,-9 -6,-16 -16,-16 Z",
    "R": "M38,32 h18 c8,0 13,4 13,11 c0,5 -3,9 -8,10 l10,23 h-9 l-9,-22 h-7 v22 h-8 Z M46,40 v14 h10 c3,0 5,-2 5,-4 c0,-2 -2,-4 -5,-4 Z",
    "S": "M44,42 c2,-3 6,-5 10,-5 c7,0 11,3 11,8 c0,6 -5,8 -12,10 c-9,3 -15,6 -15,14 c0,9 7,14 16,14 c5,0 10,-2 13,-6 l-5,-5 c-2,3 -5,4 -8,4 c-5,0 -8,-3 -8,-6 c0,-5 5,-7 11,-9 c10,-3 16,-6 16,-15 c0,-8 -7,-14 -17,-14 c-6,0 -11,2 -14,6 Z",
    "T": "M36,32 h36 v8 h-14 v36 h-8 v-36 h-14 Z",
    "U": "M38,32 h8 v26 c0,9 5,14 13,14 c8,0 13,-5 13,-14 v-26 h8 v26 c0,14 -9,22 -21,22 c-12,0 -21,-8 -21,-22 Z",
    "V": "M36,32 h9 l12,33 l12,-33 h9 l-16,44 h-10 Z",
    "W": "M32,32 h8 l9,28 l8,-20 h6 l8,20 l9,-28 h8 l-12,44 h-9 l-8,-22 l-8,22 h-9 Z",
    "X": "M38,32 h10 l10,14 l10,-14 h10 l-15,20 l16,24 h-10 l-11,-16 l-11,16 h-10 l16,-24 Z",
    "Y": "M36,32 h10 l11,18 l11,-18 h10 l-17,25 v21 h-8 v-21 Z",
    "Z": "M38,32 h30 v8 l-20,28 h22 v8 h-32 v-8 l20,-28 h-20 Z"
}

def clean_svg_path(path_str):
    """Membersihkan whitespace pada string path SVG."""
    return " ".join(path_str.strip().split())

def extract_path_from_svg_file(svg_path):
    """Mengekstrak atribut 'd' dari tag path di dalam file SVG eksternal."""
    try:
        with open(svg_path, "r", encoding="utf-8") as f:
            content = f.read()
        matches = re.findall(r'<path[^>]*\bd=["\']([^"\']+)["\']', content)
        if matches:
            return " ".join(matches)
    except Exception as e:
        print(f"Peringatan: Gagal membaca file SVG {svg_path}: {e}")
    return None

def create_png_square(width, height, fill_rgb):
    """Membuat PNG solid sederhana untuk fallback legacy mipmap."""
    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0)
        for x in range(width):
            raw_data.extend([fill_rgb[0], fill_rgb[1], fill_rgb[2], 255])
    compressed = zlib.compress(bytes(raw_data), level=9)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xffffffff)

    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)) + chunk(b"IDAT", compressed) + chunk(b"IEND", b"")

def hex_to_rgb(hex_str, default=(46, 52, 64)):
    clean = hex_str.strip().lstrip("#")
    if len(clean) == 6:
        try:
            return tuple(int(clean[i:i+2], 16) for i in (0, 2, 4))
        except ValueError:
            pass
    return default

def generate_icons(
    res_dir,
    app_name="Sboard",
    letter=None,
    preset_type="letter",
    primary_hex="#2E3440",
    secondary_hex="#3B4252",
    accent_hex="#ECEFF4",
    svg_file=None,
    generate_png=False
):
    """Menghasilkan seluruh icon XML dan dukungan Themed Icons di folder res/."""
    drawable_dir = os.path.join(res_dir, "drawable")
    anydpi_dir = os.path.join(res_dir, "mipmap-anydpi-v26")
    os.makedirs(drawable_dir, exist_ok=True)
    os.makedirs(anydpi_dir, exist_ok=True)

    # 1. Tentukan path foreground
    chosen_path = None
    if svg_file and os.path.exists(svg_file):
        extracted = extract_path_from_svg_file(svg_file)
        if extracted:
            chosen_path = extracted
            print(f"Menggunakan custom SVG path dari: {svg_file}")

    if not chosen_path:
        if preset_type in PRESET_PATHS:
            chosen_path = PRESET_PATHS[preset_type]
            print(f"Menggunakan preset simbol: '{preset_type}'")
        else:
            # Gunakan inisial huruf nama aplikasi
            char = (letter or (app_name[0] if app_name else "S")).upper()
            chosen_path = LETTER_PATHS.get(char, LETTER_PATHS["S"])
            print(f"Menggunakan Smart Monogram inisial: '{char}'")

    clean_path = clean_svg_path(chosen_path)

    # 2. Generate ic_launcher_background.xml (Vector)
    bg_xml = f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Solid Primary Base -->
    <path
        android:fillColor="{primary_hex}"
        android:pathData="M0,0h108v108h-108z" />
    <!-- Subtle Modern Radial Depth -->
    <path
        android:fillColor="{secondary_hex}"
        android:fillAlpha="0.45"
        android:pathData="M54,18 A36,36 0 1,0 54,90 A36,36 0 1,0 54,18 Z" />
</vector>
"""
    with open(os.path.join(drawable_dir, "ic_launcher_background.xml"), "w", encoding="utf-8") as fp:
        fp.write(bg_xml)

    # 3. Generate ic_launcher_foreground.xml (Vector di Safe Zone 66dp)
    fg_xml = f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="{accent_hex}"
        android:strokeColor="{accent_hex}"
        android:strokeWidth="1.5"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:pathData="{clean_path}" />
</vector>
"""
    with open(os.path.join(drawable_dir, "ic_launcher_foreground.xml"), "w", encoding="utf-8") as fp:
        fp.write(fg_xml)

    # 4. Generate ic_launcher_monochrome.xml (Android 13+ Themed Icons)
    mono_xml = f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Pure White Monochrome Silhouette for Material You -->
    <path
        android:fillColor="#FFFFFFFF"
        android:strokeColor="#FFFFFFFF"
        android:strokeWidth="1.5"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:pathData="{clean_path}" />
</vector>
"""
    with open(os.path.join(drawable_dir, "ic_launcher_monochrome.xml"), "w", encoding="utf-8") as fp:
        fp.write(mono_xml)

    # 5. Tulis Adaptive Icon XML di mipmap-anydpi-v26
    adaptive_xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""
    with open(os.path.join(anydpi_dir, "ic_launcher.xml"), "w", encoding="utf-8") as fp:
        fp.write(adaptive_xml)
    with open(os.path.join(anydpi_dir, "ic_launcher_round.xml"), "w", encoding="utf-8") as fp:
        fp.write(adaptive_xml)

    # 6. Optional: Generate lightweight legacy PNGs jika user minta flag --png
    if generate_png:
        p_rgb = hex_to_rgb(primary_hex)
        for folder_name, size in DENSITIES.items():
            folder_path = os.path.join(res_dir, folder_name)
            os.makedirs(folder_path, exist_ok=True)
            png_bytes = create_png_square(size, size, p_rgb)
            with open(os.path.join(folder_path, "ic_launcher.png"), "wb") as fp:
                fp.write(png_bytes)
            with open(os.path.join(folder_path, "ic_launcher_round.png"), "wb") as fp:
                fp.write(png_bytes)

    print(f"Berhasil menghasilkan Adaptive Vector Icons (Background + Foreground + Monochrome Themed Icons) di: {res_dir}")

def main():
    parser = argparse.ArgumentParser(description="Modern Android Adaptive & Themed Vector Icon Generator")
    parser.add_argument("res_dir", nargs="?", default="app/src/main/res", help="Path to res directory")
    parser.add_argument("--app-name", default="Sboard", help="Nama aplikasi untuk monogram otomatis")
    parser.add_argument("--letter", default=None, help="Karakter huruf spesifik untuk monogram (misal 'S')")
    parser.add_argument("--type", choices=["letter", "keyboard", "code", "chat", "tool", "app"], default="letter", help="Preset simbol geometris")
    parser.add_argument("--primary", default="#2E3440", help="Primary background color hex (e.g. #2E3440)")
    parser.add_argument("--secondary", default="#3B4252", help="Secondary background depth hex (e.g. #3B4252)")
    parser.add_argument("--accent", default="#ECEFF4", help="Accent foreground symbol color hex (e.g. #ECEFF4)")
    parser.add_argument("--svg", default=None, help="Path to custom SVG file for foreground symbol")
    parser.add_argument("--png", action="store_true", help="Generate legacy mipmap PNGs fallback")
    args = parser.parse_args()

    if not os.path.exists(args.res_dir):
        print(f"Folder res tidak ditemukan: {args.res_dir}")
        sys.exit(1)

    generate_icons(
        res_dir=args.res_dir,
        app_name=args.app_name,
        letter=args.letter,
        preset_type=args.type,
        primary_hex=args.primary,
        secondary_hex=args.secondary,
        accent_hex=args.accent,
        svg_file=args.svg,
        generate_png=args.png
    )

if __name__ == "__main__":
    main()
