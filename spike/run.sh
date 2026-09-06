#!/bin/sh
# M2 spike runner. Downloads Chicory artifacts once, compiles, runs.
set -e
cd "$(dirname "$0")"
LIBS=.libs CLASSES=.classes VER=1.4.0 BASE=https://repo1.maven.org/maven2/com/dylibso/chicory
mkdir -p "$LIBS" "$CLASSES"
for a in runtime wasm wabt log wasi; do
  [ -f "$LIBS/chicory-$a-$VER.jar" ] || curl -sSL -o "$LIBS/chicory-$a-$VER.jar" "$BASE/$a/$VER/$a-$VER.jar"
done
[ -f "$LIBS/zerofs-0.1.0.jar" ] || curl -sSL -o "$LIBS/zerofs-0.1.0.jar" \
  https://repo1.maven.org/maven2/io/roastedroot/zerofs/0.1.0/zerofs-0.1.0.jar
CP="$(ls "$LIBS"/*.jar | tr '\n' ':')"
find src -name '*.java' > .classes/sources.txt
javac -cp "$CP" -d "$CLASSES" @.classes/sources.txt
cp wat/streams.wat "$CLASSES/"
java -cp "$CLASSES:$CP" demo.spike.Main
