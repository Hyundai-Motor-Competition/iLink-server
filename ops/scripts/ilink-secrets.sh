#!/usr/bin/env bash
# dev 비밀값(Secret api)을 .env처럼 편집 → 검사 → 반영 → api 재시작. 홈서버에서 root로 실행
#   ilink-secrets          편집기로 열고, 저장하면 반영
#   ilink-secrets apply    /root/ilink-ci/api.env를 편집 없이 반영
# Secret api가 아직 없으면 DB_PASSWORD · JWT_SECRET · PHONE_ENCRYPTION_KEY를 임의 값으로 채운 파일을 먼저 만든다
# 설치: install -m 700 ops/scripts/ilink-secrets.sh /usr/local/bin/ilink-secrets
set -euo pipefail
NS=ilink-dev
FILE=/root/ilink-ci/api.env
umask 077
mkdir -p "$(dirname "$FILE")"

current() {
  kubectl -n "$NS" get secret api >/dev/null 2>&1 || return 0
  kubectl -n "$NS" get secret api -o json | python3 -c '
import sys, json, base64
for k, v in sorted(json.load(sys.stdin)["data"].items()):
    print(k + "=" + base64.b64decode(v).decode())'
}

value() { grep -m1 "^$1=" "$2" | cut -d= -f2- || true; }

if [ ! -f "$FILE" ]; then
  if kubectl -n "$NS" get secret api >/dev/null 2>&1; then
    current > "$FILE"
  else
    {
      echo "DB_PASSWORD=$(openssl rand -hex 24)"
      echo "JWT_SECRET=$(openssl rand -base64 32)"
      echo "PHONE_ENCRYPTION_KEY=$(openssl rand -base64 32)"
      echo "KAKAO_REST_API_KEY="
    } > "$FILE"
  fi
fi
BEFORE=$(mktemp); trap 'rm -f "$BEFORE"' EXIT
current > "$BEFORE"

[ "${1:-}" = apply ] || "${EDITOR:-vi}" "$FILE"

python3 - "$FILE" <<'PY'
import sys, base64, binascii
env = {}
for n, line in enumerate(open(sys.argv[1], encoding="utf-8"), 1):
    line = line.rstrip("\n")
    if not line.strip() or line.lstrip().startswith("#"):
        continue
    if "=" not in line:
        sys.exit(f"{n}번째 줄이 KEY=VALUE 형식이 아닙니다: {line}")
    k, v = line.split("=", 1)
    env[k.strip()] = v

def b64len(v):
    try:
        return len(base64.b64decode(v, validate=True))
    except (binascii.Error, ValueError):
        return -1

errors = []
for k in ("DB_PASSWORD", "JWT_SECRET", "PHONE_ENCRYPTION_KEY"):
    if not env.get(k):
        errors.append(f"{k}가 비어 있습니다")
if env.get("JWT_SECRET") and b64len(env["JWT_SECRET"]) < 32:
    errors.append("JWT_SECRET은 Base64로 인코딩한 32바이트 이상이어야 합니다(openssl rand -base64 32)")
if env.get("PHONE_ENCRYPTION_KEY") and b64len(env["PHONE_ENCRYPTION_KEY"]) != 32:
    errors.append("PHONE_ENCRYPTION_KEY는 Base64로 인코딩한 32바이트여야 합니다(openssl rand -base64 32)")
if errors:
    sys.exit("반영하지 않았습니다:\n- " + "\n- ".join(errors))
PY

if diff -q <(grep -vE '^\s*(#|$)' "$FILE" | sort) <(sort "$BEFORE") >/dev/null; then
  echo "바뀐 값이 없습니다."
  exit 0
fi
echo "바뀐 키: $(diff <(grep -vE '^\s*(#|$)' "$FILE" | sort) <(sort "$BEFORE") | grep -E '^[<>]' | cut -c3- | cut -d= -f1 | sort -u | tr '\n' ' ')"

OLD_DB=$(value DB_PASSWORD "$BEFORE"); NEW_DB=$(value DB_PASSWORD "$FILE")
if [ "$OLD_DB" != "$NEW_DB" ]; then
  if kubectl -n "$NS" get pod postgres-0 >/dev/null 2>&1; then
    # PostgreSQL은 비밀번호를 처음 초기화할 때만 읽으므로 DB 안의 비밀번호도 같이 바꾼다
    # psql 변수(:'pw')는 -c가 아니라 표준 입력으로 넘긴 SQL에서만 치환된다
    echo "ALTER USER pickup PASSWORD :'pw';" \
      | kubectl -n "$NS" exec -i postgres-0 -- psql -q -v ON_ERROR_STOP=1 -U pickup -d pickup -v pw="$NEW_DB"
    echo "PostgreSQL 비밀번호도 바꿨습니다."
  fi
  kubectl -n "$NS" create secret generic postgres --from-literal=POSTGRES_PASSWORD="$NEW_DB" \
    --dry-run=client -o yaml | kubectl apply -f - >/dev/null
fi

kubectl -n "$NS" create secret generic api --from-env-file="$FILE" --dry-run=client -o yaml | kubectl apply -f - >/dev/null
if kubectl -n "$NS" get deploy api >/dev/null 2>&1; then
  kubectl -n "$NS" rollout restart deploy/api >/dev/null
  kubectl -n "$NS" rollout status deploy/api --timeout=300s
fi
echo "반영 완료."
