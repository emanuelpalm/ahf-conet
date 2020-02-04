#!/bin/bash

# Removes all keystores, certificates and truststores created by `mk_certs.sh`.

find cloud-* -regex ".*\.\(p12\|cer\|jks\)" -exec rm -f {} \;
