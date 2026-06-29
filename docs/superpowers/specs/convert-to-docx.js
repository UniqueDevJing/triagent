const fs = require("fs");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  HeadingLevel, AlignmentType, BorderStyle, WidthType, ShadingType,
  PageBreak
} = require("docx");

const md = fs.readFileSync("2026-06-26-health-system-redesign.md", "utf-8");
const lines = md.split("\n");

const border = { style: BorderStyle.SINGLE, size: 1, color: "CCCCCC" };
const borders = { top: border, bottom: border, left: border, right: border };
const cellMargins = { top: 60, bottom: 60, left: 100, right: 100 };
const headerShading = { fill: "E8F0FE", type: ShadingType.CLEAR };

function makeHeaderCell(text, width) {
  return new TableCell({
    borders, width: { size: width, type: WidthType.DXA }, margins: cellMargins, shading: headerShading,
    children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text, bold: true, font: "Arial", size: 20 })] })]
  });
}

function makeCell(text, width) {
  return new TableCell({
    borders, width: { size: width, type: WidthType.DXA }, margins: cellMargins,
    children: [new Paragraph({ children: [new TextRun({ text, font: "Arial", size: 20 })] })]
  });
}

function makeTable(headers, rows, colWidths) {
  const tableWidth = colWidths.reduce((a, b) => a + b, 0);
  return new Table({
    width: { size: tableWidth, type: WidthType.DXA },
    columnWidths: colWidths,
    rows: [
      new TableRow({ children: headers.map((h, i) => makeHeaderCell(h, colWidths[i])) }),
      ...rows.map(row => new TableRow({ children: row.map((cell, i) => makeCell(cell, colWidths[i])) }))
    ]
  });
}

function parseTable(startIdx) {
  let i = startIdx;
  const rows = [];
  while (i < lines.length && lines[i].startsWith("|")) {
    const cells = lines[i].split("|").filter(c => c !== "").map(c => c.trim());
    if (!cells.every(c => /^[-: ]+$/.test(c))) rows.push(cells);
    i++;
  }
  return { headers: rows[0], rows: rows.slice(1), endIdx: i };
}

const children = [];
let i = 0;

while (i < lines.length) {
  const line = lines[i];

  if (line.startsWith("# ") && !line.startsWith("## ")) {
    children.push(new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun({ text: line.slice(2), font: "Arial" })] }));
    i++;
  } else if (line.startsWith("## ")) {
    children.push(new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun({ text: line.slice(3), font: "Arial" })] }));
    i++;
  } else if (line.startsWith("### ")) {
    children.push(new Paragraph({ heading: HeadingLevel.HEADING_3, children: [new TextRun({ text: line.slice(4), font: "Arial" })] }));
    i++;
  } else if (line.startsWith("|")) {
    const { headers, rows, endIdx } = parseTable(i);
    const n = headers.length;
    const contentWidth = 9360;
    const colWidth = Math.floor(contentWidth / n);
    const colWidths = headers.map(() => colWidth);
    children.push(makeTable(headers, rows, colWidths));
    i = endIdx;
  } else if (line.startsWith("```")) {
    const codeLines = [];
    i++;
    while (i < lines.length && !lines[i].startsWith("```")) {
      codeLines.push(lines[i]);
      i++;
    }
    i++;
    codeLines.forEach(cl => {
      children.push(new Paragraph({
        spacing: { before: 0, after: 0 },
        children: [new TextRun({ text: cl, font: "Courier New", size: 18 })]
      }));
    });
  } else if (line.startsWith("---")) {
    children.push(new Paragraph({ children: [new PageBreak()] }));
    i++;
  } else if (line.startsWith("- **") || line.startsWith("- ")) {
    const text = line.slice(2).replace(/\*\*/g, "");
    children.push(new Paragraph({
      spacing: { before: 40, after: 40 },
      children: [new TextRun({ text: "\u2022 " + text, font: "Arial", size: 20 })]
    }));
    i++;
  } else if (line.startsWith("_")) {
    children.push(new Paragraph({
      spacing: { before: 120, after: 120 },
      children: [new TextRun({ text: line.slice(1, -1), font: "Arial", size: 19, italics: true, color: "888888" })]
    }));
    i++;
  } else if (line.trim() === "") {
    i++;
  } else {
    // Regular paragraph
    const cleaned = line.replace(/\*\*(.+?)\*\*/g, "$1").replace(/`(.+?)`/g, "$1");
    if (cleaned.trim()) {
      children.push(new Paragraph({
        spacing: { before: 40, after: 40 },
        children: [new TextRun({ text: cleaned, font: "Arial", size: 20 })]
      }));
    }
    i++;
  }
}

const doc = new Document({
  styles: {
    default: { document: { run: { font: "Arial", size: 24 } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 36, bold: true, font: "Arial", color: "1A3C6D" },
        paragraph: { spacing: { before: 240, after: 120 }, outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 30, bold: true, font: "Arial", color: "2B5797" },
        paragraph: { spacing: { before: 200, after: 100 }, outlineLevel: 1 } },
      { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 26, bold: true, font: "Arial", color: "333333" },
        paragraph: { spacing: { before: 160, after: 80 }, outlineLevel: 2 } },
    ]
  },
  sections: [{
    properties: {
      page: { size: { width: 11906, height: 16838 }, margin: { top: 1440, right: 1200, bottom: 1440, left: 1200 } }
    },
    children
  }]
});

Packer.toBuffer(doc).then(buffer => {
  fs.writeFileSync("2026-06-26-health-system-redesign.docx", buffer);
  console.log("Done: 2026-06-26-health-system-redesign.docx");
});
