import sys
from pathlib import Path

from docx import Document


def extract_text(docx_path: str, out_txt_path: str) -> None:
    docx_file = Path(docx_path)
    out_file = Path(out_txt_path)
    out_file.parent.mkdir(parents=True, exist_ok=True)

    doc = Document(str(docx_file))
    lines = []
    for p in doc.paragraphs:
        t = (p.text or "").strip("\ufeff")
        # 保留空行（用于分段/章节间距）
        lines.append(t)

    out_file.write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: python extract_docx_text.py <input.docx> <output.txt>")

    extract_text(sys.argv[1], sys.argv[2])

