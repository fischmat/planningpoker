#!/bin/bash

echo "Starting application build for docker image"
APP_NAME=$(./gradlew properties --no-daemon --console=plain -q | grep "^name:" | awk '{printf $2}')
APP_VERSION=$(./gradlew properties --no-daemon --console=plain -q | grep "^version:" | awk '{printf $2}')
echo "Building $APP_NAME version $APP_VERSION"

./gradlew build -x test

BUILD_DIR=$(./gradlew properties --no-daemon --console=plain -q | grep "^buildDir:" | awk '{printf $2}')
cp "$BUILD_DIR/libs/$APP_NAME-$APP_VERSION.jar" "app.jar"