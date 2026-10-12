#!/usr/bin/env bash
# Script to launch Googlebook IDE Bridge Daemon in the Linux VM
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
PIDFILE="$DIR/.bridge.pid"

if [ -f "$PIDFILE" ]; then
    PID=$(cat "$PIDFILE")
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "Googlebook IDE Bridge Daemon is already running (PID $PID)."
        exit 0
    else
        rm "$PIDFILE"
    fi
fi

echo "Starting Googlebook IDE Bridge Daemon on port 8765..."
nohup python3 "$DIR/googlebook_bridge.py" > "$DIR/bridge.log" 2>&1 &
echo $! > "$PIDFILE"
echo "Bridge Daemon started successfully (PID $(cat "$PIDFILE"))."
echo "Logs available at: $DIR/bridge.log"
