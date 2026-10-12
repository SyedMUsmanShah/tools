#!/usr/bin/env python3
"""Convert Word documents to PDF and PDFs to Word documents.

Word -> PDF uses LibreOffice (headless), which keeps fonts, headers, footers,
columns and page layout. PDF -> Word uses pdf2docx, which rebuilds text,
tables and images as editable content.

Usage:
    python word_pdf_converter.py report.docx           # -> report.pdf
    python word_pdf_converter.py scan.pdf              # -> scan.docx
    python word_pdf_converter.py *.docx *.pdf -o out/  # batch, into out/
    python word_pdf_converter.py big.pdf --pages 1-5   # only some pages
"""

import argparse
import logging
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

WORD_EXTENSIONS = {".docx", ".doc", ".odt", ".rtf"}
PDF_EXTENSIONS = {".pdf"}


def find_libreoffice():
    for name in ("soffice", "libreoffice"):
        path = shutil.which(name)
        if path:
            return path
    for path in (
        "/Applications/LibreOffice.app/Contents/MacOS/soffice",
        r"C:\Program Files\LibreOffice\program\soffice.exe",
        r"C:\Program Files (x86)\LibreOffice\program\soffice.exe",
    ):
        if Path(path).exists():
            return path
    return None


def word_to_pdf(src: Path, dest: Path) -> None:
    soffice = find_libreoffice()
    if not soffice:
        raise RuntimeError(
            "LibreOffice is needed for Word -> PDF. Install it from "
            "https://www.libreoffice.org/download/ (or `apt install libreoffice-writer`, "
            "`brew install --cask libreoffice`)."
        )
    with tempfile.TemporaryDirectory() as tmp:
        # A private profile lets this run even while LibreOffice is open on the desktop.
        profile = Path(tmp, "profile").as_uri()
        result = subprocess.run(
            [soffice, f"-env:UserInstallation={profile}", "--headless",
             "--convert-to", "pdf", "--outdir", tmp, str(src)],
            capture_output=True, text=True, timeout=300,
        )
        produced = Path(tmp, src.stem + ".pdf")
        if result.returncode != 0 or not produced.exists():
            detail = (result.stderr or result.stdout).strip()
            raise RuntimeError(f"LibreOffice could not convert the file. {detail}")
        shutil.move(str(produced), dest)


def parse_pages(spec):
    """'1-3,7' -> [0, 1, 2, 6] (pdf2docx uses 0-based page indexes)."""
    pages = []
    for part in spec.split(","):
        part = part.strip()
        if "-" in part:
            a, b = part.split("-", 1)
            pages.extend(range(int(a) - 1, int(b)))
        elif part:
            pages.append(int(part) - 1)
    return pages


def pdf_to_word(src: Path, dest: Path, pages=None, password=None) -> None:
    try:
        from pdf2docx import Converter
    except ImportError:
        raise RuntimeError("pdf2docx is needed for PDF -> Word. Install it with `pip install pdf2docx`.")
    logging.getLogger().setLevel(logging.ERROR)  # pdf2docx logs every step at INFO
    cv = Converter(str(src), password=password)
    try:
        cv.convert(str(dest), pages=pages)
    finally:
        cv.close()


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description="Convert Word files to PDF and PDF files to Word.")
    parser.add_argument("files", nargs="+", type=Path, help=".docx/.doc/.odt/.rtf files become PDFs; .pdf files become .docx")
    parser.add_argument("-o", "--output-dir", type=Path, help="folder for the converted files (default: next to each input)")
    parser.add_argument("--pages", help="PDF -> Word only: pages to convert, e.g. 1-3,7")
    parser.add_argument("--password", help="PDF -> Word only: password for an encrypted PDF")
    parser.add_argument("-f", "--force", action="store_true", help="overwrite existing output files")
    args = parser.parse_args(argv)

    if args.output_dir:
        args.output_dir.mkdir(parents=True, exist_ok=True)

    failures = 0
    for src in args.files:
        ext = src.suffix.lower()
        if not src.is_file():
            print(f"✗ {src}: file not found", file=sys.stderr)
            failures += 1
            continue
        if ext in WORD_EXTENSIONS:
            out_ext, convert = ".pdf", lambda s, d: word_to_pdf(s, d)
        elif ext in PDF_EXTENSIONS:
            pages = parse_pages(args.pages) if args.pages else None
            out_ext, convert = ".docx", lambda s, d: pdf_to_word(s, d, pages, args.password)
        else:
            print(f"✗ {src}: unsupported file type '{src.suffix}'", file=sys.stderr)
            failures += 1
            continue

        dest = (args.output_dir or src.parent) / (src.stem + out_ext)
        if dest.exists() and not args.force:
            print(f"✗ {src}: {dest} already exists (use --force to overwrite)", file=sys.stderr)
            failures += 1
            continue
        try:
            convert(src, dest)
            print(f"✓ {src} -> {dest}")
        except Exception as exc:  # report and carry on with the rest of the batch
            print(f"✗ {src}: {exc}", file=sys.stderr)
            failures += 1

    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
