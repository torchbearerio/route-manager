#!/bin/bash

# Build jar
sbt clean update assembly

# Deploy to EB
eb deploy