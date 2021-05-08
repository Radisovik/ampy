#!/bin/bash
docker build -t amplus:latest .
docker run -p8080:8080 -it amplus:latest
