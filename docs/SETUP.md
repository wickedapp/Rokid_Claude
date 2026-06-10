# Setup

This guide covers running Rokid Claude locally over USB and remotely over an
ngrok tunnel.

## Prerequisites

- **macOS** (the relay and Claude Code run on your home Mac).
- **Node.js ≥ 18**.
- **Claude Code CLI** (`claude`) — installed and logged in. The relay spawns
  `claude`, so you must run `claude` once interactively and `/login` first;
  otherwise the relay only gets "Not logged in".
- **whisper.cpp** — `brew install whisper-cpp` (provides the `whisper-cli`
  binary).
- **Whisper model** — download `ggml-small.bin` from
  [ggerganov/whisper.cpp](https://huggingface.co/ggerganov/whisper.cpp/tree/main)
  and place it at `relay/models/ggml-small.bin`. This directory is gitignored and
  is **not** shipped with the repo.
- **Android client** — either sideload the prebuilt APK from the GitHub Release,
  or build it yourself with `./gradlew :app:assembleDebug` (needs JDK 17 + the
  Android SDK). `adb` must be on your PATH or at
  `$HOME/Library/Android/sdk/platform-tools/adb`.

## Local setup (USB)

1. **Install relay dependencies:**
   ```bash
   cd relay && npm install
   ```
2. **Install the Android app** on the glasses/phone (Release APK or your own
   build):
   ```bash
   adb install -r app-debug.apk
   ```
3. **Configure the client.** Copy the example and edit it:
   ```bash
   cp config.example.json config.json
   ```
   - Same Wi-Fi LAN: set `"serverUrl": "ws://<your-Mac-LAN-IP>:8787"`.
   - Or over USB only: run `adb reverse tcp:8787 tcp:8787` and use
     `"serverUrl": "ws://localhost:8787"`.
   - Leave `"token": ""` for local (no auth).

   Push it to the app's external files directory:
   ```bash
   adb push config.json /sdcard/Android/data/com.rokid.relayhud/files/config.json
   ```
4. **Start everything** with the helper script (sets up `adb reverse` and starts
   the relay in the foreground):
   ```bash
   ./start.command
   ```
   Open Rokid Claude on the device — the footer should read "已连接" (connected).

## Remote setup (ngrok)

1. **Install and authenticate ngrok:**
   ```bash
   brew install ngrok
   ngrok config add-authtoken <your-ngrok-authtoken>
   ```
   A free static domain works well.
2. **Create the relay environment file:**
   ```bash
   cp relay/.remote.env.example relay/.remote.env
   ```
   Fill in:
   - `ROKID_TOKEN` — a long random secret, e.g. `openssl rand -hex 24`.
   - `NGROK_DOMAIN` — your fixed ngrok domain (without the protocol), e.g.
     `your-subdomain.ngrok-free.dev`.
3. **Point the client at the tunnel.** In `config.json` set
   `"serverUrl": "wss://<your-ngrok-domain>"` and `"token": "<same ROKID_TOKEN>"`,
   then push it (same `adb push` command as above).
4. **Start the remote stack:**
   ```bash
   ./start-remote.command
   ```
   This launches ngrok plus the token-protected relay. When you're out of the
   house, connect the glasses to your phone's hotspot and they reach the Mac
   through the tunnel.

### Security

The `ROKID_TOKEN` is equivalent to remote code execution on your Mac: anyone who
reaches the tunnel with it can make Claude Code run commands. Never commit
`relay/.remote.env` or your real `config.json` (both are gitignored). The
on-glasses permission confirmation for risky tools is your second line of
defense.

## Voice commands

- **Tap** — start talking / stop recording / interrupt a running task.
- **Double-tap** — blank the screen (the task keeps running).
- Say **"新会话"** to start a new session, **"退出"** to close the app.
- Say **"切换模型"** to open the model picker (opus / sonnet / fable), then swipe
  to choose and tap to confirm.
- Spoken shortcuts are defined in `relay/dictionary.json` (edit it live).
