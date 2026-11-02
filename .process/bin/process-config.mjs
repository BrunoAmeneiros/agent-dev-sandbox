// Process configuration (.process/config.json), protected-path matching and
// content hashing. Shared by the plugin hooks and by the CI check, which runs
// a copy of this file from the project's .process/bin/. Node built-ins only.
import { execFileSync } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';

export const CONFIG_PATH = '.process/config.json';

export const DEFAULT_CONFIG = {
  integrationBranch: 'develop',
  mainBranch: 'main',
  // Language the skills use until the person has written something.
  language: 'es',
  branchPrefixes: ['feature/', 'bugfix/', 'hotfix/'],
  protected: {
    // Always protected, approved or not.
    patterns: [CONFIG_PATH, '.process/bin/**', '.claude/**', '.github/workflows/**'],
    // Files whose task checkboxes are normalized to [ ] before hashing.
    normalizeCheckboxes: ['docs/superpowers/plans/**'],
    // Approved files: repo-relative path -> content hash (null = approved deletion).
    files: {},
  },
  usage: {
    // Skill invoked in the main session -> process phase.
    phases: {
      'agent-dev-process:feature': 'feature',
      'agent-dev-process:need': 'need',
      'agent-dev-process:rails': 'rails',
      'superpowers:brainstorming': 'design',
      'superpowers:writing-plans': 'tasks',
      'superpowers:subagent-driven-development': 'execution',
      'superpowers:executing-plans': 'execution',
      'superpowers:test-driven-development': 'execution',
      'superpowers:systematic-debugging': 'execution',
      'superpowers:finishing-a-development-branch': 'closing',
    },
    // Agent dispatched from the main session -> process phase. The first
    // entry whose type matches, and whose match (regex on the description)
    // matches if there is one, wins.
    agentPhases: [
      { type: 'agent-dev-process:mapper', match: '^Impact', phase: 'impact' },
      { type: 'agent-dev-process:mapper', phase: 'map' },
    ],
    // Subagent description (regex) -> role, for general-purpose subagents.
    roles: [
      { match: '^Implement Task', role: 'implementer' },
      { match: '^(Re-review|Review) Task', role: 'task-reviewer' },
      { match: '^Review code changes', role: 'final-reviewer' },
      { match: 'builder', role: 'builder-check' },
    ],
  },
};

export const CONFLICT_REASON = 'config.json tiene un conflicto de merge sin resolver';
const CONFLICT_MARKERS = /^(<{7}|={7}|>{7})(\s|$)/m;

// The project's config merged over the defaults, or null if there is none.
// With merge conflict markers, the HEAD version decides (conflict: true), so
// the rails keep working until the person resolves it.
export function loadConfig(root) {
  const file = path.join(root, CONFIG_PATH);
  if (!fs.existsSync(file)) return null;
  let text = fs.readFileSync(file, 'utf8');
  const conflict = CONFLICT_MARKERS.test(text);
  if (conflict) {
    try {
      text = execFileSync('git', ['show', `HEAD:${CONFIG_PATH}`], { cwd: root, stdio: ['ignore', 'pipe', 'ignore'] }).toString('utf8');
    } catch {
      throw new Error(`${CONFLICT_REASON}, y HEAD no tiene versión con la que decidir: resuélvelo`);
    }
  }
  const raw = JSON.parse(text);
  return {
    ...DEFAULT_CONFIG,
    ...raw,
    protected: { ...DEFAULT_CONFIG.protected, ...raw.protected, files: { ...(raw.protected?.files || {}) } },
    usage: { ...DEFAULT_CONFIG.usage, ...raw.usage },
    conflict,
    _raw: raw,
  };
}

// Appended to a hook's reason while config.json is in conflict.
export function conflictNote(cfg) {
  return cfg?.conflict ? ` Aviso: ${CONFLICT_REASON}; se ha decidido con la versión de HEAD.` : '';
}

// Writes back only what the project file had, plus the approved hashes.
// Never over a conflicted file: it would discard the merge.
export function saveApprovedFiles(root, cfg) {
  if (cfg.conflict) throw new Error(`${CONFLICT_REASON}: resuélvelo (y haz git add ${CONFIG_PATH}) antes de aprobar`);
  const raw = { ...cfg._raw, protected: { ...cfg._raw.protected, files: sortKeys(cfg.protected.files) } };
  const file = path.join(root, CONFIG_PATH);
  fs.writeFileSync(file, JSON.stringify(raw, null, 2) + '\n');
  return fs.readFileSync(file);
}

function sortKeys(obj) {
  return Object.fromEntries(Object.entries(obj).sort(([a], [b]) => a.localeCompare(b)));
}

export function globToRegExp(glob) {
  let re = '';
  for (let i = 0; i < glob.length; i++) {
    const c = glob[i];
    if (glob.startsWith('/**', i) && i + 3 === glob.length) {
      re += '(?:/.*)?'; // dir/** also matches dir itself
      break;
    } else if (glob.startsWith('**/', i)) {
      re += '(?:.*/)?';
      i += 2;
    } else if (c === '*' && glob[i + 1] === '*') {
      re += '.*';
      i++;
    } else if (c === '*') re += '[^/]*';
    else if (c === '?') re += '[^/]';
    else re += c.replace(/[.+^${}()|[\]\\]/g, '\\$&');
  }
  return new RegExp(`^${re}$`);
}

export function matchesAny(rel, globs) {
  return globs.some((g) => globToRegExp(g).test(rel));
}

// Literal prefix of a glob, used to spot protected paths inside shell commands.
export function globPrefix(glob) {
  const i = glob.search(/[*?]/);
  return i === -1 ? glob : glob.slice(0, i);
}

export function isProtected(rel, cfg) {
  return Object.hasOwn(cfg.protected.files, rel) || matchesAny(rel, cfg.protected.patterns);
}

// Hash of a file's content with line endings normalized to LF (git may check
// out CRLF on Windows) and, for plans, task checkboxes normalized to [ ].
export function contentHash(content, rel, cfg) {
  let text = Buffer.isBuffer(content) ? content.toString('utf8') : content;
  text = text.replace(/\r\n/g, '\n');
  if (matchesAny(rel, cfg.protected.normalizeCheckboxes)) {
    text = text.replace(/^(\s*(?:[-*+]|\d+[.)])\s+)\[[ xX]\]/gm, '$1[ ]');
  }
  return 'sha256:' + crypto.createHash('sha256').update(text, 'utf8').digest('hex');
}

export function toRel(root, file) {
  const rel = path.relative(root, path.resolve(root, file));
  if (!rel || rel.startsWith('..') || path.isAbsolute(rel)) return null;
  return rel.split(path.sep).join('/');
}
