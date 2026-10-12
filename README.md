# tools

## Word ⇄ PDF Converter

Two ways to convert Word documents to PDF and PDFs back to Word.

### 1. In the browser: `word-pdf-converter.html`

Open the file in any modern browser (Chrome, Edge, Firefox, Safari). No install, no server, and no uploads: everything runs on your own machine.

- **Word → PDF**: drop one or more `.docx` files, pick a page size (A4, Letter, Legal), save the PDFs. The PDFs have real, selectable text.
- **PDF → Word**: drop one or more PDFs and choose the output:
  - *Editable text* rebuilds paragraphs, headings, bold/italic and font sizes.
  - *Exact look* puts each page into Word as a picture, so it looks identical but isn't editable.
  - Scanned pages with no text layer are always placed as pictures.

It loads its libraries ([mammoth](https://github.com/mwilliamson/mammoth.js), [pdfmake](https://pdfmake.github.io/), [html-to-pdfmake](https://github.com/Aymkdn/html-to-pdfmake), [pdf.js](https://mozilla.github.io/pdf.js/), [docx](https://docx.js.org/)) from jsDelivr, so it needs an internet connection the first time.

Limits: Word → PDF doesn't reproduce headers/footers, columns, text boxes or the exact fonts. PDF → Word turns tables and multi-column layouts into plain paragraphs. For those, use the command-line version.

### 2. On the command line: `word_pdf_converter.py` (higher fidelity)

Uses [LibreOffice](https://www.libreoffice.org/) for Word → PDF (keeps fonts, headers, footers and layout) and [pdf2docx](https://github.com/ArtifexSoftware/pdf2docx) for PDF → Word (rebuilds tables and images).

```bash
pip install -r requirements.txt          # pdf2docx, for PDF -> Word
# and install LibreOffice for Word -> PDF:
#   macOS:   brew install --cask libreoffice
#   Ubuntu:  sudo apt install libreoffice-writer
#   Windows: https://www.libreoffice.org/download/

python word_pdf_converter.py report.docx            # -> report.pdf
python word_pdf_converter.py statement.pdf          # -> statement.docx
python word_pdf_converter.py *.docx *.pdf -o out/   # batch into a folder
python word_pdf_converter.py big.pdf --pages 1-5    # only some pages
python word_pdf_converter.py locked.pdf --password secret
```

The direction is picked from each file's extension (`.docx`, `.doc`, `.odt`, `.rtf` → PDF; `.pdf` → `.docx`). Existing files are not overwritten unless you pass `--force`.
