#!/bin/bash
set -euo pipefail
cd /Volumes/DATA/Development/Rokid_Claude
export PATH="/opt/homebrew/bin:/opt/homebrew/Cellar/node/25.8.1_1/bin:/Users/wickedapp/.brv-cli/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH"
mkdir -p logs
exec /opt/homebrew/bin/aoe serve --host 127.0.0.1 --port 8790
