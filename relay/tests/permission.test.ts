import { describe, it, expect } from 'vitest';
import { allowKey, decide, summarize } from '../src/permission';

describe('permission 纯逻辑', () => {
  it('allowKey 由工具名+命令稳定生成', () => {
    expect(allowKey('Bash', 'npm test')).toBe('Bash::npm test');
    expect(allowKey('Write', '/a/b.txt')).toBe('Write::/a/b.txt');
  });
  it('decide:在已允许集合内→allow', () => {
    const set = new Set<string>(['Bash::npm test']);
    expect(decide('Bash::npm test', set)).toBe('allow');
  });
  it('decide:不在集合内→ask', () => {
    expect(decide('Bash::rm -rf /', new Set())).toBe('ask');
  });
  it('summarize:Bash 显命令,Write 显路径,其他显工具名', () => {
    expect(summarize('Bash', { command: 'npm test' })).toEqual({ summary: '运行: npm test', command: 'npm test' });
    expect(summarize('Write', { file_path: '/a.txt' })).toEqual({ summary: '写: /a.txt', command: '/a.txt' });
    expect(summarize('Glob', {})).toEqual({ summary: 'Glob', command: 'Glob' });
  });
});
