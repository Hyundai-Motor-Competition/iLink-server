#!/usr/bin/env bash
# 프로필 사진 저장소 준비(한 번만). 홈서버에서 root로 실행
#   designmodu-dev의 MinIO에 ilink 전용 비공개 버킷 · 계정(ilink 버킷만 읽기 · 쓰기)을 만들고,
#   기본 아바타(defaults/avatar.png)를 올리고, 계정 키를 ilink-dev Secret api에 넣는다(ilink-secrets apply).
# 루트 계정 값은 MinIO 파드 안의 환경 변수로만 쓰고 화면 · 파일에 남기지 않는다.
set -euo pipefail
MINIO_NS=designmodu-dev
BUCKET=ilink
APP_USER=ilink-app
ENV_FILE=/root/ilink-ci/api.env
umask 077

mcx() { kubectl -n "$MINIO_NS" exec -i deploy/minio -- sh -c "mc alias set local http://localhost:9000 \"\$MINIO_ROOT_USER\" \"\$MINIO_ROOT_PASSWORD\" >/dev/null && $1"; }

echo "1) 비공개 버킷 $BUCKET"
mcx "mc mb --ignore-existing local/$BUCKET && mc anonymous set none local/$BUCKET"

echo "2) 전용 계정 $APP_USER (ilink 버킷만)"
mcx "cat > /tmp/ilink-rw.json <<'JSON'
{\"Version\":\"2012-10-17\",\"Statement\":[
 {\"Effect\":\"Allow\",\"Action\":[\"s3:GetObject\",\"s3:PutObject\",\"s3:DeleteObject\"],\"Resource\":[\"arn:aws:s3:::$BUCKET/*\"]},
 {\"Effect\":\"Allow\",\"Action\":[\"s3:ListBucket\",\"s3:GetBucketLocation\"],\"Resource\":[\"arn:aws:s3:::$BUCKET\"]}]}
JSON
mc admin policy create local ilink-rw /tmp/ilink-rw.json >/dev/null && rm -f /tmp/ilink-rw.json"
if grep -q '^MINIO_SECRET_KEY=.' "$ENV_FILE" 2>/dev/null; then
  SECRET=$(grep -m1 '^MINIO_SECRET_KEY=' "$ENV_FILE" | cut -d= -f2-)
else
  SECRET=$(openssl rand -hex 24)
fi
printf '%s' "$SECRET" | kubectl -n "$MINIO_NS" exec -i deploy/minio -- sh -c \
  "read -r S; mc alias set local http://localhost:9000 \"\$MINIO_ROOT_USER\" \"\$MINIO_ROOT_PASSWORD\" >/dev/null && mc admin user add local $APP_USER \"\$S\" >/dev/null"
mcx "mc admin policy attach local ilink-rw --user $APP_USER >/dev/null 2>&1 || true"

echo "3) 기본 아바타 defaults/avatar.png"
python3 - <<'PY' | kubectl -n "$MINIO_NS" exec -i deploy/minio -- sh -c \
  "cat > /tmp/avatar.png && mc alias set local http://localhost:9000 \"\$MINIO_ROOT_USER\" \"\$MINIO_ROOT_PASSWORD\" >/dev/null && mc cp --attr Content-Type=image/png /tmp/avatar.png local/$BUCKET/defaults/avatar.png >/dev/null && rm -f /tmp/avatar.png"
# 256x256 회색 원 안에 사람 실루엣(머리 · 어깨). 표준 라이브러리만 쓴다
import struct, sys, zlib
W = H = 256
bg, circle, fg = (255, 255, 255, 0), (226, 230, 236, 255), (160, 168, 180, 255)
rows = []
for y in range(H):
    row = bytearray([0])
    for x in range(W):
        c = bg
        if (x - 128) ** 2 + (y - 128) ** 2 <= 128 ** 2:
            c = circle
            head = (x - 128) ** 2 + (y - 100) ** 2 <= 44 ** 2
            body = y >= 158 and (x - 128) ** 2 / 82 ** 2 + (y - 230) ** 2 / 72 ** 2 <= 1
            if head or body:
                c = fg
        row += bytes(c)
    rows.append(bytes(row))
def chunk(t, d): return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', W, H, 8, 6, 0, 0, 0)) \
    + chunk(b'IDAT', zlib.compress(b''.join(rows), 9)) + chunk(b'IEND', b'')
sys.stdout.buffer.write(png)
PY

echo "4) 계정 키를 ilink-dev Secret api에 넣고 앱 재시작"
touch "$ENV_FILE"
sed -i '/^MINIO_ACCESS_KEY=/d; /^MINIO_SECRET_KEY=/d' "$ENV_FILE"
printf 'MINIO_ACCESS_KEY=%s\nMINIO_SECRET_KEY=%s\n' "$APP_USER" "$SECRET" >> "$ENV_FILE"
ilink-secrets apply

echo "완료: 버킷 $BUCKET(비공개), 계정 $APP_USER, 기본 아바타 업로드, Secret api 반영"
