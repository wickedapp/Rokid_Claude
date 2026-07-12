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
  const safeLines = Math.max(5, Math.min(500, Math.floor(lines)));
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


function renderAnsiFrame(text: string, cols = 46, rows = 36): string {
  const width = Math.max(10, Math.min(160, cols));
  const height = Math.max(5, Math.min(80, rows));
  const grid: string[][] = Array.from({ length: height }, () => Array(width).fill(' '));
  let row = 0;
  let col = 0;
  const clampCursor = () => {
    row = Math.max(0, Math.min(height - 1, row));
    col = Math.max(0, Math.min(width - 1, col));
  };
  const clearLineFromCursor = () => { for (let x = col; x < width; x += 1) grid[row][x] = ' '; };
  const clearLineToCursor = () => { for (let x = 0; x <= col; x += 1) grid[row][x] = ' '; };
  const clearLine = () => { for (let x = 0; x < width; x += 1) grid[row][x] = ' '; };
  const clearScreen = () => { for (let y = 0; y < height; y += 1) for (let x = 0; x < width; x += 1) grid[y][x] = ' '; };
  const newline = () => {
    row += 1;
    col = 0;
    if (row >= height) {
      grid.shift();
      grid.push(Array(width).fill(' '));
      row = height - 1;
    }
  };
  const writeChar = (ch: string) => {
    if (ch === '\n') { newline(); return; }
    if (ch === '\r') { col = 0; return; }
    if (ch === '\b') { col = Math.max(0, col - 1); return; }
    if (ch < ' ') return;
    grid[row][col] = ch;
    col += 1;
    if (col >= width) newline();
  };

  for (let i = 0; i < text.length; i += 1) {
    const ch = text[i];
    if (ch === '\u001b' && text[i + 1] === '[') {
      let j = i + 2;
      while (j < text.length && !/[A-Za-z~]/.test(text[j] ?? '')) j += 1;
      const final = text[j] ?? '';
      const params = text.slice(i + 2, j).replace(/[?=]/g, '').split(';').filter(Boolean).map((v) => Number(v));
      const n = (idx: number, def: number) => Number.isFinite(params[idx]) && params[idx] > 0 ? params[idx] : def;
      switch (final) {
        case 'A': row -= n(0, 1); break;
        case 'B': row += n(0, 1); break;
        case 'C': col += n(0, 1); break;
        case 'D': col -= n(0, 1); break;
        case 'G': col = n(0, 1) - 1; break;
        case 'H':
        case 'f': row = n(0, 1) - 1; col = n(1, 1) - 1; break;
        case 'K':
          if ((params[0] ?? 0) === 1) clearLineToCursor();
          else if ((params[0] ?? 0) === 2) clearLine();
          else clearLineFromCursor();
          break;
        case 'J':
          if ((params[0] ?? 0) === 2 || (params[0] ?? 0) === 3) { clearScreen(); row = 0; col = 0; }
          break;
        case 'm':
        case 'h':
        case 'l':
          break;
        default:
          break;
      }
      clampCursor();
      i = j;
      continue;
    }
    writeChar(ch);
  }
  return grid.map((line) => line.join('').replace(/\s+$/g, '')).join('\n');
}

function terminalKeyInput(key: string): string {
  switch (key) {
    case 'enter': return '\r';
    case 'up': return '\u001b[A';
    case 'down': return '\u001b[B';
    case 'left': return '\u001b[D';
    case 'right': return '\u001b[C';
    case 'escape': return '\u001b';
    case 'backspace': return '\u007f';
    default: return key;
  }
}

export interface AoeTerminalFrame extends AoeTerminal {
  rows: number;
  history: number;
}

export interface AoeTerminalWatch {
  stop(): void;
  sendInput(input: string): void;
  sendKey(key: string): void;
}

export async function watchAoeTerminal(
  id: string,
  opts: {
    lines?: number;
    cols?: number;
    rows?: number;
    heartbeatMs?: number;
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
  let closed = false;
  let captureInFlight = false;
  let capturePending = false;
  const ws = new WebSocket(wsUrl, ['aoe-auth', token]);
  let heartbeat: ReturnType<typeof setInterval> | null = null;

  const sendCapturedTerminal = (history = 0) => {
    if (closed) return;
    if (captureInFlight) { capturePending = true; return; }
    captureInFlight = true;
    captureAoeSession(id, opts.lines ?? 300)
      .then((latest) => {
        if (closed) return;
        opts.onFrame({
          ...latest,
          lines: opts.lines ?? latest.lines,
          rows: opts.rows ?? 28,
          history,
        });
      })
      .catch((err) => opts.onError(err instanceof Error ? err : new Error(String(err))))
      .finally(() => {
        captureInFlight = false;
        if (capturePending) {
          capturePending = false;
          sendCapturedTerminal(history);
        }
      });
  };

  ws.on('open', () => {
    ws.send(JSON.stringify({ type: 'resize', cols: opts.cols ?? 52, rows: opts.rows ?? 28 }));
    ws.send(JSON.stringify({ type: 'window', lines: opts.lines ?? 300 }));
    ws.send(JSON.stringify({ type: 'cadence', fast: true }));
    sendCapturedTerminal(0);
    heartbeat = setInterval(() => sendCapturedTerminal(0), opts.heartbeatMs ?? 1000);
  });
  ws.on('message', (data) => {
    if (closed) return;
    try {
      const msg = JSON.parse(data.toString()) as { type?: string; history?: number };
      if (msg.type !== 'frame') return;
      // Use AoE live-ws as the low-latency change signal, but keep AoE capture
      // as the source of truth for scrollback. The live frame is only the current
      // terminal viewport; replacing the capture tail with it can hide transcript
      // rows that are still present in `aoe session capture`.
      sendCapturedTerminal(typeof msg.history === 'number' ? msg.history : 0);
    } catch (err) {
      opts.onError(err instanceof Error ? err : new Error(String(err)));
    }
  });
  ws.on('error', (err) => { if (!closed) opts.onError(err instanceof Error ? err : new Error(String(err))); });
  ws.on('close', (code, reason) => {
    if (!closed && code !== 1000) opts.onError(new Error(`AoE terminal stream closed: ${code} ${reason.toString()}`));
  });

  return {
    stop() { closed = true; if (heartbeat) clearInterval(heartbeat); try { ws.close(1000); } catch { ws.terminate(); } },
    sendInput(input: string) { if (!closed && ws.readyState === WebSocket.OPEN) ws.send(input); },
    sendKey(key: string) { if (!closed && ws.readyState === WebSocket.OPEN) ws.send(terminalKeyInput(key)); },
  };
}
