import { spawn } from 'node:child_process';
import { tr, type Lang } from './i18n';

/** 清洗 whisper-cli 输出:去方括号行(如 [BLANK_AUDIO])、trim、压缩空白。纯函数。 */
export function cleanWhisperOutput(raw: string): string {
  return raw
    .split('\n')
    .map((l) => l.trim())
    .filter((l) => l.length > 0 && !/^\[.*\]$/.test(l))
    .join(' ')
    .replace(/\s+/g, ' ')
    .trim();
}

/** 按语言拼 whisper-cli 参数。en 用 -l en 且不加引导词;zh 用 -l zh + 简体引导词，
 * 让 multilingual Whisper 稳定输出简体中文并减少指令词繁简体漂移。 */
export function whisperArgs(wavPath: string, modelPath: string, lang: Lang): string[] {
  const t = tr(lang);
  const args = ['-m', modelPath, '-f', wavPath, '-l', t.whisperLang, '-nt'];
  if (t.whisperPrompt) args.push('--prompt', t.whisperPrompt);
  return args;
}

/** 调 whisper-cli 转写 WAV → 文本;失败/空 → ''。 */
export function transcribe(wavPath: string, modelPath: string, lang: Lang = 'zh', binary = 'whisper-cli'): Promise<string> {
  return new Promise((resolve) => {
    const child = spawn(binary, whisperArgs(wavPath, modelPath, lang), {
      stdio: ['ignore', 'pipe', 'ignore'],
    });
    let out = '';
    child.stdout.on('data', (d: Buffer) => { out += d.toString('utf8'); });
    child.on('error', () => resolve(''));
    child.on('close', () => resolve(cleanWhisperOutput(out)));
  });
}
