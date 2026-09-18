#!/bin/bash
# Builds (if needed) and launches MacroTracker.
set -e
cd "$(dirname "$0")"

if [ ! -d bin ]; then
    ./build.sh
fi

java --module-path lib/javafx --add-modules javafx.controls,javafx.fxml \
    -cp "bin:$(echo lib/sqlite/*.jar)" \
    application.Main
