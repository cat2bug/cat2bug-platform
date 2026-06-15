#!/usr/bin/env python3
"""Minimal WebSocket handshake smoke (stdlib only). Exit 0 on HTTP 101."""
import base64
import os
import socket
import sys


def main() -> int:
    if len(sys.argv) != 4:
        print("usage: native-websocket-smoke.py <host> <port> <path>", file=sys.stderr)
        return 2
    host, port_s, path = sys.argv[1], int(sys.argv[2]), sys.argv[3]
    key = base64.b64encode(os.urandom(16)).decode()
    req = (
        f"GET {path} HTTP/1.1\r\n"
        f"Host: {host}:{port_s}\r\n"
        f"Upgrade: websocket\r\n"
        f"Connection: Upgrade\r\n"
        f"Sec-WebSocket-Key: {key}\r\n"
        f"Sec-WebSocket-Version: 13\r\n"
        f"\r\n"
    )
    with socket.create_connection((host, port_s), timeout=10) as sock:
        sock.sendall(req.encode())
        resp = sock.recv(4096).decode("latin-1", errors="replace")
    if " 101 " in resp.split("\r\n", 1)[0]:
        return 0
    print(resp[:300], file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
