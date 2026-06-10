import { describe, it, expect } from 'vitest';
import { cleanWhisperOutput } from '../src/transcribe';

describe('cleanWhisperOutput', () => {
  it('trim + 压缩空白', () => {
    expect(cleanWhisperOutput('  在沙盒里  建个文件  ')).toBe('在沙盒里 建个文件');
  });
  it('丢弃 [BLANK_AUDIO] 等方括号行', () => {
    expect(cleanWhisperOutput('[BLANK_AUDIO]')).toBe('');
    expect(cleanWhisperOutput('一行\n[BLANK_AUDIO]\n二行')).toBe('一行 二行');
  });
  it('空 → 空', () => {
    expect(cleanWhisperOutput('')).toBe('');
    expect(cleanWhisperOutput('\n  \n')).toBe('');
  });
});
