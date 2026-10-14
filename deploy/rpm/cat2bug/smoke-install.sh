#!/usr/bin/env bash
# AlmaLinux 9 / RHEL 9 Native RPM 安装冒烟（在目标机或容器内执行，需 systemd）
#
# 用法:
#   sudo ./deploy/rpm/cat2bug/smoke-install.sh deploy/rpm/cat2bug/dist/cat2bug-platform_1.1.0_x86_64.rpm
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

test -x /usr/bin/cat2bug-admin
test -f /etc/cat2bug/platform.properties
test -L /var/lib/cat2bug-platform/config/application.properties

systemctl daemon-reload
systemctl enable cat2bug-platform
systemctl restart cat2bug-platform

echo "Waiting for /q/health/ready..."
for i in $(seq 1 60); do
  if curl -sf "${BASE}/q/health/ready" >/dev/null 2>&1; then
    break
  fi
  sleep 0.5
done

curl -sf "${BASE}/q/health/ready"
echo ""
curl -sf "${BASE}/version"
echo ""
curl -sf "${BASE}/setup/status" | head -c 400
echo ""
echo "Smoke OK: ${BASE}/setup (first-run wizard)"
echo "Logs: journalctl -u cat2bug-platform / /var/log/cat2bug-platform/cat2bug-admin.log"
