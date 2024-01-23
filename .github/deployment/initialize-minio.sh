#!/bin/bash

echo "Creating new alias 'server'"
mc alias set server http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"

echo "Creating new user 'poker'"
mc admin user add server poker testtesttest

echo "Granting 'readwrite' access to user poker"
mc admin policy attach server readwrite --user poker

echo "Creating service account"
OUTPUT=$(mc admin user svcacct add server poker)
ACCESS_KEY=$(echo "$OUTPUT" | grep -i "Access Key" | cut -d: -f2 | cut -d' ' -f2)
SECRET_KEY=$(echo "$OUTPUT" | grep -i "Secret Key" | cut -d: -f2 | cut -d' ' -f2)

echo "storage.s3.accessKey: $ACCESS_KEY" > /out/application-minio-ci.yml
echo "storage.s3.secretKey: $SECRET_KEY" >> /out/application-minio-ci.yml