#!/usr/bin/env python3
"""
Kotlin & XML Pre-Flight Linter, Syntax Balancer & Memory-Safe Validator
Dirancang untuk mencegah kegagalan CI dan kebocoran memori (Memory Leak):

1. HARD ERRORS (Gagal Compile / Bug Fatal -> Exit 1):
   - Keseimbangan tanda kurung {}, (), [] dengan pemahaman penuh string literal, char, komentar baris //, dan komentar blok /* ... */.
   - Validasi struktur XML (AndroidManifest.xml, res/drawables, res/values).
   - Deteksi typo parameter Compose/WorkManager umum.
   - Deteksi akses properti langsung pada variabel kotlin.Result<T> tanpa .getOrNull() / .getOrThrow().
   - Deteksi pemanggilan ekstensi Modifier Compose tanpa import (border, clip, clickable, dsb).
   - Deteksi pemanggilan suspend function di dalam non-coroutine lambda umum.
   - Deteksi penggunaan 'it' liar di dalam lambda yang sudah memiliki parameter bernama.
   - Deteksi DisposableEffect tanpa blok onDispose { ... } (kebocoran fatal listener).
   - Deteksi referensi class/object internal proyek tanpa import yang valid.

2. SOFT WARNINGS (Advisory Kebocoran Memori & Lifecycle -> Exit 0, Non-Blocking):
   - Penggunaan collectAsState() alih-alih collectAsStateWithLifecycle().
   - Penggunaan GlobalScope.launch / GlobalScope.async di luar scope terkontrol.
   - Deklarasi referensi Context, Activity, atau Composable State pada companion object / static var.
   - ViewModel yang menerima Activity/Context langsung di konstruktor tanpa Application.

3. SISTEM PENGECUALIAN (SUPPRESSION SYSTEM - ANTI MEMBABI BUTA):
   - Folder otomatis diabaikan: /build/, /.gradle/, /.git/, /generated/, /vendor/, /legacy/, /thirdparty/, /third_party/, /bin/
   - File-level ignore: baris 1-15 mengandung '// @validator-ignore-file', '// @lint-ignore-file', '@Generated', atau '@file:Suppress'
   - Line-level ignore: baris kode atau baris di atasnya mengandung '// @validator-ignore', '// @lint-ignore', atau '@Suppress(...)'
"""

import os
import sys
import re
import xml.etree.ElementTree as ET

from pathlib import Path

IGNORED_DIR_NAMES = {
    "build",
    ".gradle",
    ".git",
    "generated",
    "vendor",
    "legacy",
    "thirdparty",
    "third_party",
    "bin",
    ".idea",
}

FILE_IGNORE_TAGS = [
    "@validator-ignore-file",
    "@lint-ignore-file",
    "@validator-skip-file",
    "@file:Suppress",
    "@Generated",
]

KNOWN_PARAMETER_ERRORS = [
    ("flexTimeIntervalTimeUnit", "Gunakan 'flexTimeIntervalUnit' atau argumen posisi (12L, TimeUnit.HOURS, 2L, TimeUnit.HOURS)"),
]

SUSPEND_FUNCTIONS = [
    "scrollTo(",
    "animateScrollTo(",
    "showSnackbar(",
]

# Anggota legal dari kotlin.Result<T>
KOTLIN_RESULT_MEMBERS = {
    "isSuccess", "isFailure", "getOrNull", "getOrThrow", "getOrDefault",
    "getOrElse", "fold", "map", "mapCatching", "recover", "recoverCatching",
    "onSuccess", "onFailure", "exceptionOrNull", "toString", "hashCode", "equals"
}

# Ekstensi Compose Modifier yang memerlukan import spesifik
COMPOSE_MODIFIER_EXTENSIONS = [
    (re.compile(r'\.border\s*\('), "androidx.compose.foundation.border", "Modifier.border"),
    (re.compile(r'\.clip\s*\('), "androidx.compose.ui.draw.clip", "Modifier.clip"),
    (re.compile(r'\.clickable\s*(?:<[^>]+>)?\s*\('), "androidx.compose.foundation.clickable", "Modifier.clickable"),
    (re.compile(r'\.background\s*\('), "androidx.compose.foundation.background", "Modifier.background"),
    (re.compile(r'\.heightIn\s*\('), "androidx.compose.foundation.layout.heightIn", "Modifier.heightIn"),
    (re.compile(r'\.widthIn\s*\('), "androidx.compose.foundation.layout.widthIn", "Modifier.widthIn"),
    (re.compile(r'\.sizeIn\s*\('), "androidx.compose.foundation.layout.sizeIn", "Modifier.sizeIn"),
    (re.compile(r'\.imePadding\s*\('), "androidx.compose.foundation.layout.imePadding", "Modifier.imePadding"),
    (re.compile(r'\.systemBarsPadding\s*\('), "androidx.compose.foundation.layout.systemBarsPadding", "Modifier.systemBarsPadding"),
]

def is_line_suppressed(lines, line_idx_1based, tag=None):
    """
    Memeriksa apakah baris saat ini atau baris non-kosong sebelumnya
    memiliki tag pengecualian (@validator-ignore, @lint-ignore, atau @Suppress).
    """
    idx = line_idx_1based - 1
    if idx < 0 or idx >= len(lines):
        return False

    def matches_tag(txt):
        if not tag:
            return True
        return tag in txt or "all" in txt.lower()

    # Periksa baris saat ini
    curr = lines[idx]
    if "// @validator-ignore" in curr or "// @lint-ignore" in curr:
        if matches_tag(curr):
            return True
    if "@Suppress" in curr and matches_tag(curr):
        return True

    # Periksa baris non-kosong sebelumnya
    prev_idx = idx - 1
    while prev_idx >= 0 and not lines[prev_idx].strip():
        prev_idx -= 1
    if prev_idx >= 0:
        prev = lines[prev_idx]
        if "// @validator-ignore" in prev or "// @lint-ignore" in prev:
            if matches_tag(prev):
                return True
        if "@Suppress" in prev and matches_tag(prev):
            return True

    return False

def check_kotlin_brackets(filepath, content):
    """
    Tokenizer kurung kurawal, kurung siku, dan kurung biasa yang memahami:
    - Triple-quoted multiline strings (\"\"\" ... \"\"\")
    - Single-line strings (\" ... \") dengan escape sequence
    - Char literals (' ... ')
    - Line comments (// ...) dan block comments (/* ... */)
    """
    stack = []
    i = 0
    n = len(content)
    line_no = 1

    while i < n:
        if content[i] == '\n':
            line_no += 1
            i += 1
            continue

        # Block comments /* ... */
        if content[i:i+2] == '/*':
            i += 2
            while i < n and content[i:i+2] != '*/':
                if content[i] == '\n':
                    line_no += 1
                i += 1
            i += 2
            continue

        # Line comments // ...
        if content[i:i+2] == '//':
            i += 2
            while i < n and content[i] != '\n':
                i += 1
            continue

        # Triple-quoted strings """ ... """
        if content[i:i+3] == '"""':
            i += 3
            while i < n and content[i:i+3] != '"""':
                if content[i] == '\n':
                    line_no += 1
                i += 1
            i += 3
            continue

        # Normal strings " ... "
        if content[i] == '"':
            i += 1
            while i < n and content[i] != '"':
                if content[i] == '\\':
                    i += 2
                    continue
                if content[i] == '\n':
                    line_no += 1
                i += 1
            i += 1
            continue

        # Char literal ' ... '
        if content[i] == "'":
            i += 1
            while i < n and content[i] != "'":
                if content[i] == '\\':
                    i += 2
                    continue
                if content[i] == '\n':
                    line_no += 1
                i += 1
            i += 1
            continue

        ch = content[i]
        if ch in '{[(':
            stack.append((ch, line_no))
        elif ch in '}])':
            matching = {'}': '{', ']': '[', ')': '('}[ch]
            if not stack:
                return False, f"{filepath}:{line_no}: Karakter penutup '{ch}' tidak memiliki pasangan pembuka."
            open_ch, open_line = stack.pop()
            if open_ch != matching:
                return False, f"{filepath}:{line_no}: Penutup '{ch}' tidak cocok dengan pembuka '{open_ch}' pada baris {open_line}."
        i += 1

    if stack:
        unclosed_desc = ", ".join([f"'{ch}' (baris {l})" for ch, l in stack[-5:]])
        return False, f"{filepath}: Ditemukan simbol pembuka yang belum ditutup: {unclosed_desc}"
    return True, None

def check_disposable_effects(filepath, content, lines):
    """
    HARD ERROR: Memastikan DisposableEffect wajib memiliki blok onDispose { ... }.
    DisposableEffect tanpa onDispose menyebabkan callback/listener bocor selamanya.
    """
    errors = []
    for m in re.finditer(r'\bDisposableEffect\s*[(<{]', content):
        start_idx = m.start()
        line_no = content[:start_idx].count('\n') + 1

        # Lewati jika baris import
        line_start = content.rfind('\n', 0, start_idx) + 1
        line_end = content.find('\n', start_idx)
        if line_end == -1:
            line_end = len(content)
        if content[line_start:line_end].strip().startswith("import "):
            continue

        if is_line_suppressed(lines, line_no, "disposable-effect"):
            continue

        brace_start = content.find('{', start_idx)
        if brace_start == -1:
            continue

        depth = 0
        i = brace_start
        found_close = False
        while i < len(content):
            if content[i] == '{':
                depth += 1
            elif content[i] == '}':
                depth -= 1
                if depth == 0:
                    found_close = True
                    break
            i += 1

        if found_close:
            body = content[brace_start:i]
            if not re.search(r'\bonDispose\s*\{', body):
                errors.append(
                    f"{filepath}:{line_no}: 'DisposableEffect' wajib menyertakan blok 'onDispose {{ ... }}' untuk membersihkan listener/observer (cegah memory leak)."
                )
    return errors

def check_kotlin_file(filepath):
    errors = []
    warnings = []

    try:
        with open(filepath, "r", encoding="utf-8", errors="ignore") as fp:
            content = fp.read()
    except Exception as e:
        return [f"{filepath}: Gagal membaca file ({e})"], []

    lines = content.splitlines()

    # Periksa File-Level Suppression pada 15 baris awal
    header_sample = "\n".join(lines[:15])
    for tag in FILE_IGNORE_TAGS:
        if tag in header_sample:
            # File sepenuhnya diabaikan dari pemeriksaan
            return [], []

    # 1. Parameter typo umum (HARD ERROR)
    for bad_param, suggestion in KNOWN_PARAMETER_ERRORS:
        if bad_param in content:
            errors.append(f"{filepath}: Ditemukan parameter salah '{bad_param}'. Solusi: {suggestion}")

    # 2. Cek pemanggilan fungsi suspend di dalam non-coroutine lambda umum (HARD ERROR)
    for line_idx, line in enumerate(lines, 1):
        stripped = line.strip()
        if not stripped.startswith("//") and not stripped.startswith("*"):
            for sf in SUSPEND_FUNCTIONS:
                if sf in line and ("onValueChange" in line or "onKeyEvent" in line):
                    if not is_line_suppressed(lines, line_idx, "suspend"):
                        errors.append(
                            f"{filepath}:{line_idx}: Pemanggilan fungsi suspend '{sf}' langsung di dalam non-coroutine lambda. Bungkus dengan 'coroutineScope.launch {{ ... }}'."
                        )

    # 3. Deteksi DisposableEffect tanpa onDispose (HARD ERROR)
    disp_errors = check_disposable_effects(filepath, content, lines)
    errors.extend(disp_errors)

    # 4. Deteksi akses properti langsung pada variabel kotlin.Result<T> secara scope-aware (HARD ERROR)
    scope_stack = [{}]
    res_assign_re = re.compile(r'\bval\s+([a-zA-Z0-9_]+)\s*(?::\s*Result<[^>]+>)?\s*=\s*(?:[a-zA-Z0-9_.]*(?:execute|executeSync|runCatching)\b|Result\.(?:success|failure))')
    other_val_re = re.compile(r'\bval\s+([a-zA-Z0-9_]+)\s*[:=]')

    for line_idx, line in enumerate(lines, 1):
        stripped = line.strip()
        if stripped.startswith('//') or stripped.startswith('*'):
            continue
        for char in line:
            if char == '{':
                scope_stack.append({})
            elif char == '}' and len(scope_stack) > 1:
                scope_stack.pop()
        m_res = res_assign_re.search(line)
        if m_res:
            scope_stack[-1][m_res.group(1)] = True
        else:
            m_other = other_val_re.search(line)
            if m_other:
                scope_stack[-1][m_other.group(1)] = False

        active_result_vars = set()
        for scope in scope_stack:
            for v, is_res in scope.items():
                if is_res:
                    active_result_vars.add(v)
                elif v in active_result_vars:
                    active_result_vars.remove(v)

        for v in active_result_vars:
            for m in re.finditer(r'\b' + re.escape(v) + r'\.([a-zA-Z0-9_]+)', line):
                prop = m.group(1)
                if prop not in KOTLIN_RESULT_MEMBERS:
                    if not is_line_suppressed(lines, line_idx, "result"):
                        errors.append(
                            f"{filepath}:{line_idx}: Variabel '{v}' bertipe Result<T>. Properti '{prop}' tidak dapat diakses langsung tanpa '.getOrNull()?.{prop}' atau '.getOrThrow().{prop}'."
                        )

    # 5. Deteksi Modifier extension functions tanpa import yang sesuai (HARD ERROR)
    for ext_regex, req_import, desc in COMPOSE_MODIFIER_EXTENSIONS:
        if ext_regex.search(content):
            pkg_prefix = req_import.rsplit('.', 1)[0]
            wildcard_import = f"import {pkg_prefix}.*"
            direct_import = f"import {req_import}"
            if direct_import not in content and wildcard_import not in content:
                errors.append(
                    f"{filepath}: Memanggil {desc} tetapi tidak menemukan import '{direct_import}' atau '{wildcard_import}'."
                )

    # 6. Deteksi penggunaan 'it' di dalam lambda dengan parameter bernama ({ param -> it.something }) (HARD ERROR)
    named_lambda_re = re.compile(r'\{\s*([a-zA-Z0-9_,\s()]+)\s*->')
    it_providers_re = re.compile(r'\b(?:let|also|takeIf|takeUnless|forEach|forEachIndexed|map|mapIndexed|filter|filterNot|find|firstOrNull|any|all|count|sortBy|sortWith|minByOrNull|maxByOrNull|groupBy|distinctBy|associateBy|onValueChange|onCheckedChange)\b')
    brace_depth = 0
    stack = [(0, False, 'file')]
    for line_idx, line in enumerate(lines, 1):
        stripped = line.strip()
        if stripped.startswith('//') or stripped.startswith('*'):
            continue
        masked_line = re.sub(r'\{[^{}>\-]*\bit\b[^{}>\-]*\}', '{ __SAFE_IT__ }', line)

        m_named = named_lambda_re.search(line)
        named_param = None
        if m_named:
            p = m_named.group(1).strip()
            if p == 'it' or ' it' in p or 'it ' in p:
                named_param = 'it'
            else:
                named_param = p

        has_it_provider = bool(it_providers_re.search(line) and '{' in line)

        for char in line:
            if char == '{':
                brace_depth += 1
                if named_param:
                    if named_param == 'it':
                        stack.append((brace_depth, True, 'lambda_it'))
                    else:
                        stack.append((brace_depth, False, f'lambda:{named_param}'))
                elif has_it_provider:
                    stack.append((brace_depth, True, 'it_provider'))
                else:
                    parent_has_it = stack[-1][1]
                    parent_src = stack[-1][2]
                    stack.append((brace_depth, parent_has_it, parent_src))
            elif char == '}':
                if len(stack) > 1 and stack[-1][0] == brace_depth:
                    stack.pop()
                brace_depth = max(0, brace_depth - 1)

        if re.search(r'\bit\b(?!\s*->)', masked_line) and '->' not in masked_line:
            curr_has_it = stack[-1][1]
            curr_src = stack[-1][2]
            if not curr_has_it and curr_src.startswith('lambda:'):
                param_name = curr_src.split(':', 1)[1]
                if not is_line_suppressed(lines, line_idx, "it"):
                    errors.append(
                        f"{filepath}:{line_idx}: Penggunaan 'it' tidak valid di dalam lambda dengan parameter bernama '{param_name} ->'."
                    )

    # 7. Keseimbangan tanda kurung kurawal {}, (), [] (HARD ERROR)
    bracket_ok, bracket_err = check_kotlin_brackets(filepath, content)
    if not bracket_ok:
        errors.append(bracket_err)

    # =========================================================================
    # SOFT WARNINGS (MEMORY LEAK & LIFECYCLE ADVISORY - NON-BLOCKING)
    # =========================================================================

    # A. collectAsState() tanpa lifecycle
    is_service_file = bool(re.search(r':\s*(?:InputMethodService|LifecycleService|Service|AccessibilityService)\b', content))
    for line_idx, line in enumerate(lines, 1):
        if re.search(r'\bcollectAsState\s*\(', line) and 'collectAsStateWithLifecycle' not in line:
            if not is_service_file and not is_line_suppressed(lines, line_idx, "lifecycle-flow"):
                warnings.append(
                    f"{filepath}:{line_idx}: [Advisory] Penggunaan 'collectAsState()' terdeteksi. Disarankan beralih ke 'collectAsStateWithLifecycle()' agar Flow berhenti mengonsumsi RAM/CPU saat layar di background. (Gunakan '// @validator-ignore: lifecycle-flow' jika disengaja)."
                )

    # B. GlobalScope.launch / async
    for line_idx, line in enumerate(lines, 1):
        if re.search(r'\bGlobalScope\.(?:launch|async)\b', line):
            if not is_line_suppressed(lines, line_idx, "global-scope"):
                warnings.append(
                    f"{filepath}:{line_idx}: [Advisory] 'GlobalScope' terdeteksi. Berpotensi membocorkan coroutine saat Composable/ViewModel dihancurkan. Gunakan 'viewModelScope' atau 'rememberCoroutineScope()'."
                )

    # C. Referensi Context/Activity pada Companion Object / Static Var
    in_companion = False
    comp_brace_level = 0
    for line_idx, line in enumerate(lines, 1):
        stripped = line.strip()
        if "companion object" in stripped:
            in_companion = True
            comp_brace_level = line.count('{') - line.count('}')
            continue
        if in_companion:
            comp_brace_level += line.count('{') - line.count('}')
            if comp_brace_level <= 0:
                in_companion = False
            if re.search(r'\bvar\s+[a-zA-Z0-9_]+\s*:\s*(?:Context|Activity|ComponentActivity|View)\b', line):
                if not is_line_suppressed(lines, line_idx, "static-context"):
                    warnings.append(
                        f"{filepath}:{line_idx}: [Advisory] Companion object memegang referensi ke Context/Activity/View. Berpotensi fatal menahan seluruh instance Activity di memori saat rotasi layar."
                    )

    # D. ViewModel konstruktor menerima Context non-Application
    m_vm = re.search(r'class\s+([a-zA-Z0-9_]+ViewModel)\s*\([^)]*\b(?:context\s*:\s*(?!Application\b)Context|activity\s*:\s*Activity)', content)
    if m_vm:
        line_no = content[:m_vm.start()].count('\n') + 1
        if not is_line_suppressed(lines, line_no, "vm-context"):
            warnings.append(
                f"{filepath}:{line_no}: [Advisory] ViewModel '{m_vm.group(1)}' menerima referensi 'Context'/'Activity' langsung di konstruktor. Disarankan gunakan 'AndroidViewModel(application)' atau 'context.applicationContext' untuk menghindari Activity Context leak."
            )

    return errors, warnings

def check_xml_file(filepath):
    try:
        ET.parse(filepath)
        return True, None
    except Exception as e:
        return False, f"{filepath}: Format XML tidak valid ({e})"

def main():
    root_dir = sys.argv[1] if len(sys.argv) > 1 else "."
    errors = []
    warnings = []
    checked_kt = 0
    checked_xml = 0

    kt_files = []
    class_map = {}
    class_decl_re = re.compile(r'^(?:data\s+class|class|object|interface|enum\s+class)\s+([a-zA-Z0-9_]+)', re.MULTILINE)
    pkg_re = re.compile(r'^\s*package\s+([a-zA-Z0-9_.]+)', re.MULTILINE)

    for root, _, files in os.walk(root_dir):
        # Abaikan direktori build, caches, vendor, legacy, third_party, dan git
        if any(p in IGNORED_DIR_NAMES for p in Path(root).parts):
            continue

        for f in files:
            path = os.path.join(root, f)
            if f.endswith(".kt"):
                checked_kt += 1
                kt_files.append(path)
                f_errors, f_warnings = check_kotlin_file(path)
                errors.extend(f_errors)
                warnings.extend(f_warnings)

                try:
                    with open(path, "r", encoding="utf-8") as fp:
                        txt = fp.read()
                    m_pkg = pkg_re.search(txt)
                    pkg = m_pkg.group(1) if m_pkg else ""
                    for m in class_decl_re.finditer(txt):
                        cname = m.group(1)
                        if cname not in class_map:
                            class_map[cname] = set()
                        if pkg:
                            class_map[cname].add(pkg)
                except Exception:
                    pass
            elif f.endswith(".xml") and ("res/" in path or "AndroidManifest.xml" in path):
                checked_xml += 1
                ok, err = check_xml_file(path)
                if not ok:
                    errors.append(err)

    # 8. Deteksi referensi class internal proyek tanpa import yang valid (Cross-Reference Resolution) (HARD ERROR)
    local_decl_re = re.compile(r'^\s*(?:@\w+\s+)*(?:data\s+class|class|object|interface|enum\s+class|fun(?:\s+<[^>]+>)?)\s+([a-zA-Z0-9_]+)', re.MULTILINE)
    for path in kt_files:
        try:
            with open(path, "r", encoding="utf-8") as fp:
                txt = fp.read()
            m_pkg = pkg_re.search(txt)
            cur_pkg = m_pkg.group(1) if m_pkg else ""

            # Kumpulkan nama yang dideklarasikan secara lokal di file ini
            local_names = set(m.group(1) for m in local_decl_re.finditer(txt))

            for cname, pkgs in class_map.items():
                if not pkgs:
                    continue
                # Jika nama ini dideklarasikan di file ini sendiri, tidak butuh import
                if cname in local_names:
                    continue
                # Jika package file ini sama dengan salah satu package pemilik class, tidak butuh import
                if cur_pkg in pkgs:
                    continue
                if re.search(r'\b' + re.escape(cname) + r'\b', txt):
                    # Cek apakah ada import yang valid
                    has_valid_import = False
                    for cpkg in pkgs:
                        direct_import = f"import {cpkg}.{cname}"
                        wildcard_import = f"import {cpkg}.*"
                        fqcn = f"{cpkg}.{cname}"
                        if direct_import in txt or wildcard_import in txt or fqcn in txt:
                            has_valid_import = True
                            break
                    # Cek jika ada import eksplisit untuk class/fun bernama sama dari package lain
                    if not has_valid_import:
                        if re.search(r'^\s*import\s+[a-zA-Z0-9_.]+\.' + re.escape(cname) + r'\b', txt, re.MULTILINE):
                            has_valid_import = True
                    # Cek apakah disupresi via komentar validator-ignore
                    if not has_valid_import:
                        if "@validator-ignore: import" in txt or "@validator-ignore: imports" in txt:
                            has_valid_import = True

                    if not has_valid_import:
                        pkg_list = " / ".join(sorted(pkgs))
                        errors.append(
                            f"{path}: Memanggil class/object '{cname}' dari package '{pkg_list}', tetapi tidak menemukan import 'import {pkg_list}.{cname}' atau 'import {pkg_list}.*'."
                        )
        except Exception:
            pass

    print(f"Checked {checked_kt} Kotlin files and {checked_xml} XML files.")

    # Tampilkan Soft Warnings jika ada
    if warnings:
        print("\n--- MEMORY & LIFECYCLE ADVISORY WARNINGS (Non-Blocking) ---")
        for warn in warnings:
            print(f"[WARN] {warn}")
        print("------------------------------------------------------------")

    # Tampilkan Hard Errors jika ada
    if errors:
        print("\n--- PRE-FLIGHT LINTER ERRORS FOUND (Blocking CI) ---")
        for err in errors:
            print(f"[ERROR] {err}")
        print("\nPerbaiki error di atas atau sertakan komentar '// @validator-ignore' sebelum commit.")
        sys.exit(1)
    else:
        if warnings:
            print("All Kotlin and XML files are syntactically balanced and valid! (Advisory warnings displayed above, build will proceed).")
        else:
            print("All Kotlin and XML files are syntactically balanced, memory-safe, imports valid, and pre-flight checks passed!")
        sys.exit(0)

if __name__ == "__main__":
    main()
