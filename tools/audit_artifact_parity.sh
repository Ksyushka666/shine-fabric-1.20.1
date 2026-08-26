#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
JAR="build/libs/shine-1.0.0.jar"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
jar tf "$JAR" | sed '/\/$/d' | sort > "$TMP/jar.list"
find src/main/resources -type f -printf '%P\n' | sort > "$TMP/src.list"
comm -23 "$TMP/src.list" "$TMP/jar.list" > "$TMP/missing.list"
source_count=$(wc -l < "$TMP/src.list")
jar_count=$(wc -l < "$TMP/jar.list")
missing_count=$(wc -l < "$TMP/missing.list")
echo "source_resource_count=$source_count"
echo "jar_file_count=$jar_count"
echo "missing_resource_count=$missing_count"
if [ "$missing_count" -ne 0 ]; then
  cat "$TMP/missing.list"
  exit 1
fi
for required in fabric.mod.json shine.mixins.json assets/shine/post_effect/bloom_poc.json assets/shine/defaults/shine.json assets/shine/defaults/experimental.json assets/minecraft/shaders/core/terrain.fsh assets/minecraft/shaders/core/particle.fsh; do
  grep -Fxq "$required" "$TMP/jar.list" || { echo "MISSING $required"; exit 1; }
done
echo "required_resources=PASS"
