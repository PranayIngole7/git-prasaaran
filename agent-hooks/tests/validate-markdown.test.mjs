import { test } from "node:test";
import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import {
  mkdtempSync,
  mkdirSync,
  readFileSync,
  rmSync,
  writeFileSync
} from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const hook = path.join(root, "agent-hooks/validate-markdown.mjs");

function runHook(args) {
  return spawnSync(process.execPath, [hook, ...args], {
    cwd: root,
    encoding: "utf8"
  });
}

function withTempDir(run) {
  const dir = mkdtempSync(path.join(os.tmpdir(), "git-prasaaran-hook-"));
  try {
    run(dir);
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
}

test("accepts valid Markdown", () => withTempDir((dir) => {
  const file = path.join(dir, "valid.md");
  writeFileSync(file, "# Title\n\nText.\n\n```text\nhello\n```\n");
  const result = runHook([file]);
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /PASS/);
}));

test("rejects trailing whitespace and unclosed code fences", () => withTempDir((dir) => {
  const file = path.join(dir, "invalid.md");
  writeFileSync(file, "# Title   \n\n```text\nunclosed\n");
  const result = runHook([file]);
  assert.equal(result.status, 1);
  assert.match(result.stderr, /trailing whitespace/);
  assert.match(result.stderr, /not closed/);
}));

test("rejects missing files", () => withTempDir((dir) => {
  const result = runHook([path.join(dir, "missing.md")]);
  assert.equal(result.status, 1);
  assert.match(result.stderr, /cannot be read/);
}));

test("rejects unsupported file extensions", () => withTempDir((dir) => {
  const file = path.join(dir, "input.txt");
  writeFileSync(file, "text");
  const result = runHook([file]);
  assert.equal(result.status, 1);
  assert.match(result.stderr, /only \.md files/);
}));

test("rejects empty files", () => withTempDir((dir) => {
  const file = path.join(dir, "empty.md");
  writeFileSync(file, "");
  const result = runHook([file]);
  assert.equal(result.status, 1);
  assert.match(result.stderr, /file is empty/);
}));

test("requires at least one input file", () => {
  const result = runHook([]);
  assert.equal(result.status, 2);
  assert.match(result.stderr, /Usage:/);
});

test("does not modify the Markdown input", () => withTempDir((dir) => {
  const file = path.join(dir, "unchanged.md");
  const original = "# Title\n\nText.\n";
  writeFileSync(file, original);
  const result = runHook([file]);
  assert.equal(result.status, 0, result.stderr);
  assert.equal(readFileSync(file, "utf8"), original);
}));
