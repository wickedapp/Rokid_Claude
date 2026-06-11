import { describe, it, expect } from 'vitest';
import { WebSocket } from 'ws';
import { mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createRelayServer } from './server';

/** 起一个 relay,注入会记录被调语言的假转写器和空跑 runner。 */
function makeServer() {
  const langSeen: string[] = [];
  const transcriber = async (_wav: string, _model: string, lang: 'zh' | 'en') => {
    langSeen.push(lang);
    return '';
  };
  const dir = mkdtempSync(join(tmpdir(), 'rokid-test-'));
  const srv = createRelayServer({
    sandboxDir: dir, webDir: dir, stateDir: dir, modelPath: 'unused',
    transcriber,
    runner: () => ({ events: (async function* () {})(), stop() {} }),
  });
  return { srv, langSeen };
}

function connect(port: number): Promise<WebSocket> {
  return new Promise((resolve) => {
    const ws = new WebSocket(`ws://localhost:${port}`);
    ws.on('open', () => resolve(ws));
  });
}

/** 等下一条指定 type 的服务端消息。 */
function waitFor(ws: WebSocket, type: string): Promise<any> {
  return new Promise((resolve) => {
    const h = (raw: Buffer) => {
      const m = JSON.parse(raw.toString());
      if (m.type === type) { ws.off('message', h); resolve(m); }
    };
    ws.on('message', h);
  });
}

describe('setLang', () => {
  it('switches the whisper language for subsequent audio', async () => {
    const { srv, langSeen } = makeServer();
    await new Promise<void>((r) => srv.http.listen(0, r));
    const port = (srv.http.address() as any).port;
    const ws = await connect(port);
    const wav = Buffer.from('x').toString('base64');

    ws.send(JSON.stringify({ type: 'hello', lang: 'zh' }));
    ws.send(JSON.stringify({ type: 'audio', wav }));
    await waitFor(ws, 'transcript');
    expect(langSeen).toEqual(['zh']);

    ws.send(JSON.stringify({ type: 'setLang', lang: 'en' }));
    ws.send(JSON.stringify({ type: 'audio', wav }));
    await waitFor(ws, 'transcript');
    expect(langSeen).toEqual(['zh', 'en']);

    ws.close();
    await new Promise<void>((r) => srv.http.close(() => r()));
  });
});
