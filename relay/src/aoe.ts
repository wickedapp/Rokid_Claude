import { execFile } from 'node:child_process';
import { readFile } from 'node:fs/promises';
import { join } from 'node:path';
import { homedir } from 'node:os';
import { promisify } from 'node:util';
import { WebSocket } from 'ws';

const execFileAsync = promisify(execFile);
const AOE_BIN = process.env.AOE_BINARY || 'aoe';
const AOE_PROFILE = process.env.AGENT_OF_EMPIRES_PROFILE || 'default';
const AOE_SESSIONS_JSON = process.env.AOE_SESSIONS_JSON || join(homedir(), '.agent-of-empires', 'profiles', AOE_PROFILE, 'sessions.json');

export interface AoeSession {
  id: string;
  title: string;
  tool: string;
  group: string;
  status: string;
  path: string;
  hasTerminal: boolean;
  unread: boolean;
  age: string;
  lastAccessedAt: string;
}

export interface AoeTerminal {
  id: string;
  title: string;
  tool: string;
  status: string;
  content: string;
  lines: number;
}

export interface CreateAoeSessionOptions {
  tool: string;
  path?: string;
  group?: string;
  title?: string;
}

function asRecord(v: unknown): Record<string, unknown> {
  return v && typeof v === 'object' && !Array.isArray(v) ? v as Record<string, unknown> : {};
}

function str(v: unknown): string { return typeof v === 'string' ? v : ''; }
function bool(v: unknown): boolean { return typeof v === 'boolean' ? v : false; }

async function aoe(args: string[], timeout = 8000): Promise<string> {
  const { stdout } = await execFileAsync(AOE_BIN, args, {
    timeout,
    maxBuffer: 2 * 1024 * 1024,
    env: { ...process.env, NO_COLOR: '1' },
  });
  return stdout;
}

async function showAoeSession(id: string): Promise<Record<string, unknown>> {
  try {
    return asRecord(JSON.parse(await aoe(['session', 'show', id, '--json'], 5000)) as unknown);
  } catch {
    return {};
  }
}

async function loadStoredSessions(): Promise<Map<string, Record<string, unknown>>> {
  try {
    const parsed = JSON.parse(await readFile(AOE_SESSIONS_JSON, 'utf8')) as unknown;
    const rows = Array.isArray(parsed) ? parsed : [];
    return new Map(rows.map((item) => {
      const o = asRecord(item);
      return [str(o.id), o] as const;
    }).filter(([id]) => id));
  } catch {
    return new Map();
  }
}

function statusRank(status: string): number {
  const s = status.toLowerCase();
  if (s === 'running' || s === 'waiting' || s === 'active') return 0;
  if (s === 'idle') return 1;
  if (s === 'error') return 2;
  if (s === 'stopped') return 3;
  return 4;
}

function ageOf(iso: string): string {
  const t = Date.parse(iso);
  if (!Number.isFinite(t)) return '';
  const seconds = Math.max(0, Math.floor((Date.now() - t) / 1000));
  if (seconds < 60) return '<1m';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m`;
  const hours = Math.floor(minutes / 60);
  if (hours < 48) return `${hours}h`;
  const days = Math.floor(hours / 24);
  return `${days}d`;
}

export async function listAoeSessions(): Promise<AoeSession[]> {
  const stored = await loadStoredSessions();
  const raw = await aoe(['list', '--json']);
  const parsed = JSON.parse(raw) as unknown;
  const rows = Array.isArray(parsed) ? parsed : [];
  const sessions = await Promise.all(rows.map(async (item) => {
    const o = asRecord(item);
    const id = str(o.id);
    const storedRow = stored.get(id) ?? {};
    const detail = id ? await showAoeSession(id) : {};
    const path = str(detail.path || storedRow.project_path || storedRow.path || o.path || o.project_path);
    const status = str(detail.status || storedRow.status || o.status || '');
    const lastAccessedAt = str(storedRow.last_accessed_at || o.last_accessed_at || storedRow.created_at || o.created_at);
    return {
      id,
      title: str(detail.title || storedRow.title || o.title),
      tool: str(detail.tool || storedRow.tool || o.tool || o.command || 'agent'),
      group: str(detail.group || storedRow.group_path || storedRow.group || o.group || o.group_path),
      status,
      path,
      hasTerminal: bool(o.has_terminal) || true,
      unread: bool(storedRow.unread) || bool(o.unread),
      age: ageOf(lastAccessedAt),
      lastAccessedAt,
    };
  }));
  return sessions
    .filter((s) => s.id && s.title && !s.path.includes('/.aoe-trash/'))
    .sort((a, b) => statusRank(a.status) - statusRank(b.status)
      || a.group.localeCompare(b.group)
      || Date.parse(b.lastAccessedAt || '0') - Date.parse(a.lastAccessedAt || '0')
      || a.title.localeCompare(b.title));
}

export async function captureAoeSession(id: string, lines = 38): Promise<AoeTerminal> {
  const safeLines = Math.max(5, Math.min(120, Math.floor(lines)));
  const raw = await aoe(['session', 'capture', id, '--json', '--strip-ansi', '-n', String(safeLines)]);
  const o = asRecord(JSON.parse(raw) as unknown);
  return {
    id: str(o.id) || id,
    title: str(o.title) || id,
    tool: str(o.tool || 'agent'),
    status: str(o.status || ''),
    content: str(o.content),
    lines: typeof o.lines === 'number' ? o.lines : safeLines,
  };
}

export async function sendAoeMessage(id: string, message: string): Promise<void> {
  if (!message.trim()) return;
  await aoe(['send', id, message], 15000);
}

export async function createAoeSession(opts: CreateAoeSessionOptions): Promise<AoeSession> {
  const tool = opts.tool.includes('codex') ? 'codex' : opts.tool.includes('claude') ? 'claude' : opts.tool;
  const title = opts.title || `Rokid-${tool}-${new Date().toISOString().slice(11, 16).replace(':', '')}`;
  const args = ['add', '--tool', tool, '--title', title, '--launch', '--yolo'];
  if (opts.group) args.push('--group', opts.group);
  if (opts.path) args.push(opts.path);
  else args.push('--scratch');
  const out = await aoe(args, 30000);
  const match = out.match(/([a-f0-9]{12,16})/i);
  const sessions = await listAoeSessions();
  return (match ? sessions.find((s) => s.id.startsWith(match[1])) : undefined)
    || sessions.find((s) => s.title === title)
    || sessions[0]
    || { id: '', title, tool, group: opts.group || 'Scratch', status: 'creating', path: opts.path || '', hasTerminal: true, unread: false, age: '<1m', lastAccessedAt: new Date().toISOString() };
}

function stripAnsi(text: string): string {
  return text.replace(/[\u001B\u009B][[\]()#;?]*(?:(?:(?:[a-zA-Z\d]*(?:;[a-zA-Z\d]*)*)?\u0007)|(?:(?:\d{1,4}(?:;\d{0,4})*)?[\dA-PR-TZcf-nq-uy=><~]))/g, '');
}

async function aoeDashboardAuth(): Promise<{ baseUrl: string; token: string }> {
  const envUrl = process.env.AOE_DAEMON_URL || process.env.AOE_DASHBOARD_URL;
  const envToken = process.env.AOE_DAEMON_TOKEN || process.env.AOE_DASHBOARD_TOKEN;
  if (envUrl && envToken) return { baseUrl: envUrl.replace(/\/$/, ''), token: envToken };

  const raw = await aoe(['url'], 5000);
  const url = new URL(raw.trim());
  const token = envToken || url.searchParams.get('token') || '';
  url.search = '';
  if (!token) throw new Error('AoE dashboard token missing; set AOE_DAEMON_TOKEN or run aoe serve with token auth');
  return { baseUrl: (envUrl || url.toString()).replace(/\/$/, ''), token };
}

async function postAoeDashboard(path: string): Promise<void> {
  const { baseUrl, token } = await aoeDashboardAuth();
  const res = await fetch(`${baseUrl}${path}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok && res.status !== 409) throw new Error(`AoE dashboard ${path} failed: ${res.status}`);
}

export interface AoeTerminalFrame extends AoeTerminal {
  rows: number;
  history: number;
}

export interface AoeTerminalWatch {
  stop(): void;
}

export async function watchAoeTerminal(
  id: string,
  opts: {
    lines?: number;
    cols?: number;
    rows?: number;
    onFrame: (terminal: AoeTerminalFrame) => void;
    onError: (err: Error) => void;
  },
): Promise<AoeTerminalWatch> {
  const meta = await captureAoeSession(id, opts.lines ?? 80).catch(() => ({
    id,
    title: id,
    tool: 'agent',
    status: '',
    content: '',
    lines: opts.lines ?? 80,
  }));
  await postAoeDashboard(`/api/sessions/${encodeURIComponent(id)}/ensure`).catch(() => undefined);
  await postAoeDashboard(`/api/sessions/${encodeURIComponent(id)}/terminal?index=0`).catch(() => undefined);

  const { baseUrl, token } = await aoeDashboardAuth();
  const httpUrl = new URL(baseUrl);
  const wsProtocol = httpUrl.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${wsProtocol}//${httpUrl.host}/sessions/${encodeURIComponent(id)}/terminal/live-ws?index=0`;
  const baseLines = meta.content.split('\n');
  let scrollbackHead = baseLines.slice(0, Math.max(0, baseLines.length - (opts.rows ?? 36)));
  let lastFrameLines = opts.rows ?? 36;
  let closed = false;
  const ws = new WebSocket(wsUrl, ['aoe-auth', token]);

  ws.on('open', () => {
    ws.send(JSON.stringify({ type: 'resize', cols: opts.cols ?? 46, rows: opts.rows ?? 36 }));
    ws.send(JSON.stringify({ type: 'window', lines: opts.lines ?? 120 }));
    ws.send(JSON.stringify({ type: 'cadence', fast: true }));
  });
  ws.on('message', (data) => {
    if (closed) return;
    try {
      const msg = JSON.parse(data.toString()) as { type?: string; content?: string; rows?: number; history?: number };
      if (msg.type !== 'frame') return;
      const frameContent = stripAnsi(msg.content ?? '');
      const rawFrameLines = frameContent.split('\n');
      const firstContent = rawFrameLines.findIndex((line) => line.trim().length > 0);
      let lastContent = -1;
      for (let i = rawFrameLines.length - 1; i >= 0; i -= 1) {
        if (rawFrameLines[i]?.trim().length) { lastContent = i; break; }
      }
      const frameLines = firstContent >= 0 ? rawFrameLines.slice(firstContent, lastContent + 1) : rawFrameLines;
      const nonBlank = frameLines.some((line) => line.trim().length > 0);
      if (!nonBlank && meta.content.trim()) return;
      const rows = typeof msg.rows === 'number' && msg.rows > 0 ? msg.rows : (opts.rows ?? 36);
      if (scrollbackHead.length + lastFrameLines > baseLines.length) {
        scrollbackHead = [...scrollbackHead, ...frameLines].slice(0, Math.max(0, (opts.lines ?? 120) - rows));
      }
      lastFrameLines = frameLines.length;
      const mergedContent = meta.content.trim()
        ? [...scrollbackHead, ...frameLines].join('\n')
        : frameContent;
      opts.onFrame({
        ...meta,
        content: mergedContent,
        lines: opts.lines ?? meta.lines,
        rows,
        history: typeof msg.history === 'number' ? msg.history : 0,
      });
    } catch (err) {
      opts.onError(err instanceof Error ? err : new Error(String(err)));
    }
  });
  ws.on('error', (err) => { if (!closed) opts.onError(err instanceof Error ? err : new Error(String(err))); });
  ws.on('close', (code, reason) => {
    if (!closed && code !== 1000) opts.onError(new Error(`AoE terminal stream closed: ${code} ${reason.toString()}`));
  });

  return { stop() { closed = true; try { ws.close(1000); } catch { ws.terminate(); } } };
}
