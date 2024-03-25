#!/bin/bash

java -jar server.jar &

# Capture the PID of the last background process
JAVA_PID=$!

# Wait a bit and check if the Java app is still running
sleep 10
if ! kill -0 $JAVA_PID > /dev/null 2>&1; then
    echo "Java application failed to start, exiting."
    exit 1
fi

# Start NGINX in the foreground
nginx -g 'daemon off;'
