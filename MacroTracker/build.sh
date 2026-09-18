#!/bin/bash
# Compiles MacroTracker from the command line — no Eclipse needed.
set -e
cd "$(dirname "$0")"

./setup.sh

rm -rf bin
mkdir -p bin

javac --module-path lib/javafx --add-modules javafx.controls,javafx.fxml \
    -cp "$(echo lib/sqlite/*.jar)" \
    -d bin $(find src -name "*.java")

cp -R src/view bin/view
cp -R src/styles bin/styles

echo "Build complete. Run with ./run.sh"
