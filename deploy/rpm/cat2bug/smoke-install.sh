#!/usr/bin/env bash
# AlmaLinux 9 / RHEL 9 Spring Native RPM 安装冒烟（在目标机或容器内执行）
#
# 用法:
#   sudo ./deploy/rpm/cat2bug/smoke-install.sh deploy/rpm/cat2bug/dist/cat2bug-platform_1.0.0_x86_64.rpm
set -euo pipefail

RPM="${1:?Usage: smoke-install.sh <path-to.rpm>}"
PORT="${SMOKE_PORT:-2020}"
BASE="http://127.0.0.1:${PORT}"

if command -v dnf >/dev/null 2>&1; then
  dnf install -y "$RPM"
elif command -v yum >/dev/null 2>&1; then
  yum install -y "$RPM"
else
  rpm -Uvh "$RPM"
fi

systemctl daemon-reload
systemctl enable cat2bug-platform
systemctl restart cat2bug-platform

echo "Waiting for /version..."
for i in $(seq 1 60); do
  if curl -sf "${BASE}/version" >/dev/null 2>&1; then
    break
  fi
  sleep 0.5
done

curl -sf "${BASE}/version"
echo ""
curl -sf "${BASE}/setup/status" | head -c 400
echo ""
echo "Smoke OK: ${BASE}/setup (first-run wizard)"
