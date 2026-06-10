import { spawn } from 'node:child_process';

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

/** 调 whisper-cli 转写 WAV → 文本;失败/空 → ''。 */
// 简体引导词:whisper-small 默认会忽繁忽简,给一句简体 initial prompt 可稳定偏向简体输出,
// 避免「切換/会話」等繁体让指令词表(新会话/退出/审查代码…)匹配失效。
const ZH_PROMPT = '以下是普通话句子的简体中文转写。';

export function transcribe(wavPath: string, modelPath: string, binary = 'whisper-cli'): Promise<string> {
  return new Promise((resolve) => {
    const child = spawn(binary, ['-m', modelPath, '-f', wavPath, '-l', 'zh', '-nt', '--prompt', ZH_PROMPT], {
      stdio: ['ignore', 'pipe', 'ignore'],
    });
    let out = '';
    child.stdout.on('data', (d: Buffer) => { out += d.toString('utf8'); });
    child.on('error', () => resolve(''));
    child.on('close', () => resolve(cleanWhisperOutput(out)));
  });
}
