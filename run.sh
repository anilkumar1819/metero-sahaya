#!/bin/sh
# Usage: ./run.sh [port]      (needs JDK 17+ and the H2 jar)
H2=${H2_JAR:-/usr/share/java/h2.jar}
mkdir -p out && javac -encoding UTF-8 -cp "$H2" -d out src/MetroSahaya.java && \
java -Dfile.encoding=UTF-8 -cp "out:$H2" MetroSahaya "${1:-8080}"
