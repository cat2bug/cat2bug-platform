#!/bin/sh
systemctl daemon-reload 2>/dev/null || true
echo "Cat2Bug Platform installed. Enable: systemctl enable --now cat2bug-platform"
echo "Then open http://<host>:2020/setup for first-run wizard."
