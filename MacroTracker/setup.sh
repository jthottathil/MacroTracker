#!/bin/bash
# Downloads the JavaFX and SQLite JDBC jars this project needs, into lib/.
# Run once (or whenever lib/ is missing) before build.sh.
set -e
cd "$(dirname "$0")"

JFX_VER=23.0.2
SQLITE_VER=3.53.4.0

case "$(uname -m)" in
    arm64) JFX_CLASSIFIER=mac-aarch64 ;;
    *)     JFX_CLASSIFIER=mac ;;
esac

mkdir -p lib/javafx lib/sqlite

for mod in base graphics controls fxml; do
    jar="lib/javafx/javafx-${mod}-${JFX_VER}-${JFX_CLASSIFIER}.jar"
    if [ ! -f "$jar" ]; then
        echo "Downloading javafx-${mod}..."
        curl -sSL -o "$jar" \
            "https://repo1.maven.org/maven2/org/openjfx/javafx-${mod}/${JFX_VER}/javafx-${mod}-${JFX_VER}-${JFX_CLASSIFIER}.jar"
    fi
done

sqlite_jar="lib/sqlite/sqlite-jdbc-${SQLITE_VER}.jar"
if [ ! -f "$sqlite_jar" ]; then
    echo "Downloading sqlite-jdbc..."
    curl -sSL -o "$sqlite_jar" \
        "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/${SQLITE_VER}/sqlite-jdbc-${SQLITE_VER}.jar"
fi

echo "Dependencies ready in lib/."
