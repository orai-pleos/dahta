#!/usr/bin/env bash
# git submodule(res) 가 없으면 clone, 있으면 최신으로 pull.
# 오프라인/충돌 등으로 실패해도 빌드는 막지 않고 경고만 출력한다.
set -u

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT" || exit 0

# submodule 이 아직 등록되지 않았으면 할 일 없음
[ -f .gitmodules ] || exit 0

echo "[submodules] init / clone"
git submodule update --init --recursive || echo "[submodules] WARN: init failed (offline?)"

for dir in $(git config -f .gitmodules --get-regexp '\.path$' | awk '{print $2}'); do
  [ -d "$dir/.git" ] || [ -f "$dir/.git" ] || continue

  if [ -n "$(git -C "$dir" status --porcelain)" ]; then
    echo "[submodules] $dir: 로컬 변경 있음 -> pull 건너뜀"
    continue
  fi

  if git -C "$dir" symbolic-ref -q HEAD >/dev/null; then
    # 브랜치 위에 있으면 fast-forward pull (로컬 커밋은 보존)
    echo "[submodules] $dir: git pull --ff-only"
    git -C "$dir" pull --ff-only || echo "[submodules] WARN: $dir pull failed"
  else
    # detached HEAD(submodule 기본 상태) -> 원격 최신 커밋으로 이동
    echo "[submodules] $dir: git submodule update --remote"
    git submodule update --remote -- "$dir" || echo "[submodules] WARN: $dir update failed"
  fi
done

exit 0
