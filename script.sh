#!/bin/bash
# run.sh - Script to run the NAS Backup Tool

echo "Starting NAS Backup Tool..."

# Set Java options for JavaFX
JAVA_OPTS="--add-opens javafx.base/com.sun.javafx.runtime=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.lang=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.nio=ALL-UNNAMED"
JAVA_OPTS="$JAVA_OPTS --add-opens java.base/sun.nio.ch=ALL-UNNAMED"

# Run with Maven
mvn javafx:run