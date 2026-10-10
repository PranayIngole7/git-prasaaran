#!/usr/bin/env node

import { readFile } from "node:fs/promises";
import path from "node:path";

const files = process.argv.slice(2);

if (files.length === 0) {
  console.error(
    "Usage: node agent-hooks/validate-markdown.mjs <file.md> [file.md ...]"
  );
  process.exit(2);
}

let failures = 0;

for (const input of files) {
  const file = path.resolve(input);

  if (path.extname(file).toLowerCase() !== ".md") {
    console.error(`FAIL ${input}: only .md files are accepted`);
    failures++;
    continue;
  }

  let content;

  try {
    content = await readFile(file, "utf8");
  } catch {
    console.error(`FAIL ${input}: file cannot be read`);
    failures++;
    continue;
  }

  const issues = [];

  if (content.trim().length === 0) {
    issues.push("file is empty");
  }

  const lines = content.split(/\r?\n/);
  let openFence = null;

  for (let index = 0; index < lines.length; index++) {
    const line = lines[index];

    if (/[ \t]+$/.test(line)) {
      issues.push(`line ${index + 1}: trailing whitespace`);
    }

    const fence = line.match(/^\s{0,3}(`{3,}|~{3,})/);

    if (fence) {
      const marker = fence[1][0];
      const length = fence[1].length;

      if (openFence === null) {
        openFence = { marker, length, line: index + 1 };
      } else if (
        marker === openFence.marker &&
        length >= openFence.length &&
        line.trim() === marker.repeat(length)
      ) {
        openFence = null;
      }
    }
  }

  if (openFence !== null) {
    issues.push(`code fence opened on line ${openFence.line} is not closed`);
  }

  if (issues.length > 0) {
    console.error(`FAIL ${input}`);
    for (const issue of issues) {
      console.error(`  - ${issue}`);
    }
    failures++;
  } else {
    console.log(`PASS ${input}`);
  }
}

if (failures > 0) {
  console.error(`Validation failed for ${failures} file(s).`);
  process.exit(1);
}

console.log(`Markdown validation passed for ${files.length} file(s).`);
