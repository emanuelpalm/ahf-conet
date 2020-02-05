#!/bin/bash

# Removes all keystores, certificates and truststores created by `mk_certs.sh`.

cd "$(dirname "$0")" || exit
cd ..
find cloud-* -regex ".*\.\(p12\|cer\|jks\)" -exec rm -f {} \;
