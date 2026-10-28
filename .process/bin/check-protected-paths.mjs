#!/usr/bin/env node
// Protected-path check for CI, and approval helper for the person.
//
//   node .process/bin/check-protected-paths.mjs [--base <ref>]
//       CI: fails if a protected file changed against the merge base with
//       <ref> (default origin/<integrationBranch>) and its content is not the
//       approved one recorded in .process/config.json, or if an approved file
//       does not match its hash. Changes to config.json itself are reported.
//
//   node .process/bin/check-protected-paths.mjs --approve <path>...
//       Records the current content of each path as approved (a missing path
//       is recorded as an approved deletion). For changes the person makes
//       outside Claude Code, where the hooks do not run.
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { CONFIG_PATH, contentHash, isProtected, loadConfig, saveApprovedFiles, toRel } from './process-config.mjs';

const git = (root, args) => execFileSync('git', args, { cwd: root, stdio: ['ignore', 'pipe', 'pipe'], maxBuffer: 64 * 1024 * 1024 });
const gitText = (root, args) => git(root, args).toString('utf8').replace(/\s+$/, '');
const atHead = (root, rel) => {
  try {
    return git(root, ['show', `HEAD:${rel}`]);
  } catch {
    return null;
  }
};

const args = process.argv.slice(2);
const root = gitText(process.cwd(), ['rev-parse', '--show-toplevel']);
const cfg = loadConfig(root);
if (!cfg) {
  console.log(`${CONFIG_PATH} not found: nothing to check.`);
  process.exit(0);
}

if (args[0] === '--approve') approve(args.slice(1));
else process.exit(check(args[0] === '--base' ? args[1] : `origin/${cfg.integrationBranch}`));

function approve(paths) {
  if (!paths.length) {
    console.error('usage: check-protected-paths.mjs --approve <path>...');
    process.exit(2);
  }
  for (const p of paths) {
    const rel = toRel(root, path.resolve(process.cwd(), p));
    if (!rel || rel === CONFIG_PATH) {
      console.error(`skipped: ${p}`);
      continue;
    }
    const file = path.join(root, rel);
    cfg.protected.files[rel] = fs.existsSync(file) ? contentHash(fs.readFileSync(file), rel, cfg) : null;
    console.log(`approved: ${rel}${cfg.protected.files[rel] === null ? ' (deleted)' : ''}`);
  }
  saveApprovedFiles(root, cfg);
}

function check(baseRef) {
  const base = gitText(root, ['merge-base', baseRef, 'HEAD']);
  const changed = gitText(root, ['diff', '--name-status', '--no-renames', base, 'HEAD'])
    .split('\n')
    .filter(Boolean)
    .map((l) => {
      const [status, p] = l.split('\t');
      return { status: status[0], path: p };
    });

  const violations = [];
  const checked = new Set();
  const verify = (rel) => {
    if (checked.has(rel)) return;
    checked.add(rel);
    const approved = cfg.protected.files[rel];
    const content = atHead(root, rel);
    if (content === null) {
      if (approved !== null) violations.push(`${rel}: deleted without approval`);
    } else if (approved === undefined) {
      violations.push(`${rel}: changed, no approved hash`);
    } else if (approved === null) {
      violations.push(`${rel}: present, but approved as deleted`);
    } else if (contentHash(content, rel, cfg) !== approved) {
      violations.push(`${rel}: content differs from the approved one`);
    }
  };
  for (const f of changed) if (f.path !== CONFIG_PATH && isProtected(f.path, cfg)) verify(f.path);
  for (const rel of Object.keys(cfg.protected.files)) verify(rel);

  const report = [];
  if (changed.some((f) => f.path === CONFIG_PATH)) report.push(...configChanges(base));
  if (violations.length) {
    report.push('', 'Protected paths changed without approval:', ...violations.map((v) => `- ${v}`));
    report.push('', 'If you approve them, run: node .process/bin/check-protected-paths.mjs --approve <path>... and commit .process/config.json.');
  } else {
    report.push('Protected paths: every change is approved.');
  }
  const text = report.join('\n');
  console.log(text);
  if (process.env.GITHUB_STEP_SUMMARY) fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, `## Protected paths\n\n${text}\n`);
  return violations.length ? 1 : 0;
}

// What changed in config.json's protection settings since the merge base.
function configChanges(base) {
  let before = {};
  try {
    before = JSON.parse(git(root, ['show', `${base}:${CONFIG_PATH}`]).toString('utf8'));
  } catch {}
  const after = cfg._raw;
  const lines = [`${CONFIG_PATH} changed:`];
  const diffList = (label, a = [], b = []) => {
    for (const x of b.filter((x) => !a.includes(x))) lines.push(`- ${label} added: ${x}`);
    for (const x of a.filter((x) => !b.includes(x))) lines.push(`- ${label} REMOVED: ${x}`);
  };
  diffList('pattern', before.protected?.patterns, after.protected?.patterns);
  const fa = before.protected?.files || {};
  const fb = after.protected?.files || {};
  for (const k of Object.keys(fb)) if (fa[k] !== fb[k]) lines.push(`- approved: ${k}${fb[k] === null ? ' (deleted)' : ''}`);
  for (const k of Object.keys(fa)) if (!(k in fb)) lines.push(`- approval REMOVED: ${k}`);
  for (const k of ['integrationBranch', 'mainBranch']) if (before[k] !== after[k]) lines.push(`- ${k}: ${before[k]} -> ${after[k]}`);
  return lines.length > 1 ? lines : [`${CONFIG_PATH} changed (no protection settings changed).`];
}
