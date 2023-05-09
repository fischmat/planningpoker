#!/bin/bash

echo "Creating user 'poker' with password 'test'..."
mongosh --host "$MONGO_HOST" --u "$MONGO_ROOT_USER" --p "$MONGO_ROOT_PASSWORD" --authenticationDatabase admin --eval 'db.createUser({user: "poker", pwd: "test", roles: [{role: "readWrite", db: "poker"}]})' poker || exit 1