#!/usr/bin/env python3
"""
Android Release Keystore & GitHub Secrets Generator (Anti-Muter-Muter)
Otomatis:
1. Deteksi tool `keytool`. Jika belum ada, otomatis install `openjdk-17-jre-headless`.
2. Generate Keystore Release non-interaktif (RSA 2048-bit, 10000 days validity).
3. Encode ke Base64 (single-line).
4. Otomatis set 4 GitHub Repository Secrets via `gh secret set`:
   - KEYSTORE_BASE64
   - KEYSTORE_PASSWORD
   - KEY_ALIAS
   - KEY_PASSWORD
5. Tambahkan `*.keystore` ke `.gitignore` untuk mencegah kebocoran private key.
"""

import os
import sys
import shutil
import secrets
import string
import base64
import argparse
import subprocess
from pathlib import Path

def print_banner():
    print("\033[1;36m")
    print("  ╔═══════════════════════════════════════════════════════════╗")
    print("  ║      Android Release Keystore & GitHub Secrets Setup      ║")
    print("  ║        Anti-Muter-Muter • 100% Non-Interactive Auto       ║")
    print("  ╚═══════════════════════════════════════════════════════════╝")
    print("\033[0m")

def generate_secure_password(length=24):
    alphabet = string.ascii_letters + string.digits
    return ''.join(secrets.choice(alphabet) for _ in range(length))

def ensure_keytool():
    """Memastikan `keytool` tersedia di sistem. Jika belum ada, install otomatis."""
    if shutil.which("keytool") is not None:
        return True

    print("\033[33m[!] 'keytool' belum terpasang di sistem. Menginstall openjdk-17-jre-headless...\033[0m")
    
    # Deteksi package manager
    if shutil.which("apt-get") is not None:
        cmd = ["apt-get", "update", "-qq"]
        cmd_install = ["apt-get", "install", "-y", "-qq", "openjdk-17-jre-headless"]
        if os.geteuid() != 0 and shutil.which("sudo") is not None:
            cmd = ["sudo"] + cmd
            cmd_install = ["sudo"] + cmd_install
        
        try:
            print("    Menjalankan apt-get update & install...")
            subprocess.run(cmd, check=True)
            subprocess.run(cmd_install, check=True)
        except Exception as e:
            print(f"\033[31m[ERROR] Gagal menginstall openjdk via apt-get: {e}\033[0m")
            return False
    elif shutil.which("pkg") is not None:  # Termux
        try:
            subprocess.run(["pkg", "install", "-y", "openjdk-17"], check=True)
        except Exception as e:
            print(f"\033[31m[ERROR] Gagal menginstall openjdk via pkg: {e}\033[0m")
            return False
    elif shutil.which("pacman") is not None:  # Arch Linux
        cmd = ["pacman", "-Sy", "--noconfirm", "jre-openjdk-headless"]
        if os.geteuid() != 0 and shutil.which("sudo") is not None:
            cmd = ["sudo"] + cmd
        try:
            subprocess.run(cmd, check=True)
        except Exception as e:
            print(f"\033[31m[ERROR] Gagal menginstall openjdk via pacman: {e}\033[0m")
            return False
    else:
        print("\033[31m[ERROR] Tidak dapat mendeteksi package manager untuk menginstall keytool otomatis.\033[0m")
        print("Silakan pasang Java JRE/JDK manual (contoh: apt install -y openjdk-17-jre-headless).")
        return False

    return shutil.which("keytool") is not None

def set_github_secret(key: str, value: str, repo: str = None) -> bool:
    """Mengunggah secret ke GitHub repository via GitHub CLI (`gh`)."""
    if shutil.which("gh") is None:
        print(f"\033[33m[!] gh CLI tidak ditemukan. Lewati set secret {key}.\033[0m")
        return False

    cmd = ["gh", "secret", "set", key]
    if repo:
        cmd.extend(["-R", repo])

    try:
        proc = subprocess.run(
            cmd,
            input=value,
            text=True,
            capture_output=True,
            check=True
        )
        return True
    except subprocess.CalledProcessError as e:
        print(f"\033[31m[ERROR] Gagal mengunggah secret {key}: {e.stderr.strip()}\033[0m")
        return False

def ensure_gitignore_keystore(target_dir: Path):
    """Memastikan file *.keystore masuk ke .gitignore agar tidak ter-commit ke publik."""
    gi_path = target_dir / ".gitignore"
    entry = "\n*.keystore\n*.jks\n"
    if gi_path.exists():
        content = gi_path.read_text(encoding="utf-8", errors="ignore")
        if "*.keystore" not in content:
            with gi_path.open("a", encoding="utf-8") as fp:
                fp.write(entry)
    else:
        with gi_path.open("w", encoding="utf-8") as fp:
            fp.write(entry.strip() + "\n")

def main():
    parser = argparse.ArgumentParser(
        description="Android Release Keystore & GitHub Secrets Generator (Anti-Muter-Muter)"
    )
    parser.add_argument("--alias", default="releaseKey", help="Key alias name (default: releaseKey)")
    parser.add_argument("--password", help="Keystore & Key password (jika kosong, auto-generate aman)")
    parser.add_argument("--dname", default="CN=Android App, OU=Mobile, O=Arbdev, C=ID", help="Distinguished name untuk certificate")
    parser.add_argument("--output", default="release.keystore", help="Output keystore filename (default: release.keystore)")
    parser.add_argument("--repo", help="Target GitHub repo (contoh: owner/repo). Jika kosong, deteksi otomatis dari git remote")
    parser.add_argument("--no-gh", action="store_true", help="Lewati pengunggahan ke GitHub Secrets")
    parser.add_argument("--keep", action="store_true", help="Simpan file keystore lokal di disk (default: disimpan)")
    args = parser.parse_args()

    print_banner()

    # 1. Pastikan keytool ada
    if not ensure_keytool():
        print("\033[31m[ERROR] keytool tidak dapat ditemukan atau diinstall. Hentikan eksekusi.\033[0m")
        sys.exit(1)
    print("\033[32m[✓] keytool siap digunakan.\033[0m")

    # 2. Tentukan password & alias
    password = args.password if args.password else generate_secure_password(24)
    alias = args.alias
    keystore_path = Path(args.output).resolve()

    # Hapus file lama jika ada
    if keystore_path.exists():
        keystore_path.unlink()

    # 3. Jalankan keytool secara non-interaktif
    print(f"\033[34m[*] Menghasilkan release keystore: {keystore_path.name}...\033[0m")
    keytool_cmd = [
        "keytool",
        "-genkeypair",
        "-v",
        "-keystore", str(keystore_path),
        "-alias", alias,
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-storepass", password,
        "-keypass", password,
        "-dname", args.dname,
        "-noprompt"
    ]

    try:
        subprocess.run(keytool_cmd, check=True, capture_output=True, text=True)
    except subprocess.CalledProcessError as e:
        print(f"\033[31m[ERROR] Gagal menjalankan keytool: {e.stderr}\033[0m")
        sys.exit(1)

    print("\033[32m[✓] Release Keystore berhasil dibuat secara non-interaktif.\033[0m")

    # 4. Encode ke Base64 (single line)
    raw_bytes = keystore_path.read_bytes()
    keystore_base64 = base64.b64encode(raw_bytes).decode("ascii")

    # 5. Tambahkan ke .gitignore
    ensure_gitignore_keystore(keystore_path.parent)

    # 6. Unggah ke GitHub Secrets jika tidak di-skip
    if not args.no_gh:
        print("\033[34m[*] Mengunggah rahasia signing ke GitHub Repository Secrets...\033[0m")
        secrets_to_upload = {
            "KEYSTORE_BASE64": keystore_base64,
            "KEYSTORE_PASSWORD": password,
            "KEY_ALIAS": alias,
            "KEY_PASSWORD": password,
        }

        all_ok = True
        for sec_name, sec_val in secrets_to_upload.items():
            ok = set_github_secret(sec_name, sec_val, repo=args.repo)
            if ok:
                print(f"    [✓] Secret '{sec_name}' berhasil disetel.")
            else:
                all_ok = False

        if all_ok:
            print("\033[32m[✓] Seluruh GitHub Secrets signing (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD) berhasil disetel!\033[0m")
        else:
            print("\033[33m[!] Sebagian atau seluruh secret gagal disetel ke GitHub. Pastikan 'gh auth login' aktif dan repo terhubung.\033[0m")
    else:
        print("\033[33m[i] Pengunggahan ke GitHub Secrets dilewati (--no-gh).\033[0m")

    # 7. Informasi Ringkasan
    print("\n\033[1;32m═══════════════════════════════════════════════════════════════\033[0m")
    print("\033[1;32m  Signing Setup Selesai!                                        \033[0m")
    print("\033[1;32m═══════════════════════════════════════════════════════════════\033[0m")
    print(f"File Keystore : {keystore_path}")
    print(f"Key Alias     : {alias}")
    print(f"Key Password  : {password}")
    print(f"Keystore Size : {len(raw_bytes)} bytes (Base64: {len(keystore_base64)} chars)")
    print("\nFile release.keystore otomatis diproteksi via .gitignore.")
    print("GitHub Actions Workflow CI sekarang siap membangun Signed Release APK secara otomatis!\n")

if __name__ == "__main__":
    main()
