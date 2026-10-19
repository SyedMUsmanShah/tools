# tools

## Calculator: `calculator.html`

A simple calculator that runs in the browser. Open the file, no install or internet needed.

- Add, subtract, multiply, divide, with correct order of operations and brackets
- Percent, change sign (±), square (x²), square root (√x), reciprocal (1/x)
- Memory keys (MC, MR, M+, M−), backspace, clear
- Shows a live preview of the answer as you type, and keeps a history of past results (click one to reuse it)
- Keyboard: `0–9 . + - * / ( ) %`, `Enter` or `=` for the answer, `Backspace`, `Esc` to clear
- Works on phones and follows your system's light/dark mode

## Currency Converter: `currency-converter.html`

Open the file in any browser to convert between about 160 currencies using the latest daily exchange rates.

- Type an amount (commas are fine), pick the From and To currencies, and the result updates as you type
- Swap button, plus a list showing the amount in other popular currencies (click one to convert to it)
- Shows the rate both ways (1 USD = … PKR and 1 PKR = … USD) and when the rates were last updated
- Rates come from free public sources with no API key: [ExchangeRate-API](https://www.exchangerate-api.com/docs/free), falling back to [Frankfurter](https://frankfurter.dev/) and [fawazahmed0/currency-api](https://github.com/fawazahmed0/exchange-api) if one is down
- Needs internet to fetch rates. It saves the last rates it fetched, so it still works offline with those
- Remembers your last currencies and amount

Rates are mid-market reference rates. Banks and exchange services add their own margin.

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
