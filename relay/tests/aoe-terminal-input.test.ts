import { describe, expect, it } from 'vitest';
import { AoeWatchSlot, aoeAgentTerminalWsPath, terminalInputFrame, terminalKeyInput, type AoeTerminalWatch } from '../src/aoe';

describe('AoE terminal input frames', () => {
  it('encodes navigation keys as binary terminal frames', () => {
    expect(terminalKeyInput('down')).toBe('\u001b[B');
    const frame = terminalInputFrame(terminalKeyInput('down'));
    expect(Buffer.isBuffer(frame)).toBe(true);
    expect([...frame]).toEqual([0x1b, 0x5b, 0x42]);
  });

  it('encodes enter and printable text without JSON wrapping', () => {
    expect([...terminalInputFrame(terminalKeyInput('enter'))]).toEqual([0x0d]);
    expect(terminalInputFrame('y').toString('utf8')).toBe('y');
  });

  it('targets the primary agent pane, not the auxiliary paired terminal', () => {
    const path = aoeAgentTerminalWsPath('security review');
    expect(path).toBe('/sessions/security%20review/live-ws');
    expect(path).not.toContain('/terminal/live-ws');
  });

  it('stops a watcher that resolves after a newer start or unwatch', () => {
    const slot = new AoeWatchSlot();
    const stopped: string[] = [];
    const watch = (name: string): AoeTerminalWatch => ({
      stop: () => stopped.push(name),
      sendInput: () => undefined,
      sendKey: () => undefined,
    });

    const stale = slot.begin();
    const current = slot.begin();
    expect(slot.adopt(stale, watch('stale'))).toBe(false);
    expect(slot.adopt(current, watch('current'))).toBe(true);
    expect(stopped).toEqual(['stale']);

    slot.stop();
    expect(stopped).toEqual(['stale', 'current']);
    expect(slot.current).toBeNull();
  });
});
