#!/bin/sh
# Simple gradlew - delegates to installed Gradle 9.4.0
# (Full wrapper requires network to download distribution)
GRADLE_HOME="$HOME/workspace/tools/gradle-9.4.0"
export JAVA_HOME="$HOME/workspace/tools/jdk-25.0.4.1+1"
export GRADLE_USER_HOME="$HOME/.gradle"
export GRADLE_OPTS="-Djava.net.preferIPv4Stack=true"
exec "$GRADLE_HOME/bin/gradle" "$@"
