#!/bin/bash

# Uses `keytool` to generate and sign all keystores, certificates and truststores
# required to run the data sharing demo.

# This password is used for everything.
export PASSWORD="123456"

create_root_keystore() {
  local MASTER_KEYSTORE=$1
  local MASTER_KEY_ALIAS=$2
  local MASTER_CERTIFICATE="${MASTER_KEYSTORE%.*}.cer"

  rm -f "${MASTER_KEYSTORE}"
  rm -f "${MASTER_CERTIFICATE}"
  mkdir -p "$(dirname "${MASTER_CERTIFICATE}")"

  keytool -genkeypair -v \
    -keystore "${MASTER_KEYSTORE}" \
    -storepass:env "PASSWORD" \
    -keyalg "RSA" \
    -keysize "2048" \
    -validity "3650" \
    -alias "${MASTER_KEY_ALIAS}" \
    -keypass:env "PASSWORD" \
    -dname "CN=${MASTER_KEY_ALIAS}" \
    -ext "BasicConstraints=ca:true,pathlen:3"

  keytool -exportcert -v \
    -keystore "${MASTER_KEYSTORE}" \
    -storepass:env "PASSWORD" \
    -alias "${MASTER_KEY_ALIAS}" \
    -keypass:env "PASSWORD" \
    -file "${MASTER_CERTIFICATE}" \
    -rfc
}

create_cloud_keystore() {
  local MASTER_KEYSTORE=$1
  local MASTER_KEY_ALIAS=$2
  local MASTER_CERTIFICATE="${MASTER_KEYSTORE%.*}.cer"
  local CLOUD_KEYSTORE=$3
  local CLOUD_KEY_ALIAS=$4
  local CLOUD_CERTIFICATE="${CLOUD_KEYSTORE%.*}.cer"

  rm -f "${CLOUD_KEYSTORE}"
  rm -f "${CLOUD_CERTIFICATE}"
  mkdir -p "$(dirname "${CLOUD_KEYSTORE}")"

  keytool -genkeypair -v \
    -keystore "${CLOUD_KEYSTORE}" \
    -storepass:env "PASSWORD" \
    -keyalg "RSA" \
    -keysize "2048" \
    -validity "3650" \
    -alias "${CLOUD_KEY_ALIAS}" \
    -keypass:env "PASSWORD" \
    -dname "CN=${CLOUD_KEY_ALIAS}" \
    -ext "BasicConstraints=ca:true,pathlen:2"

  keytool -importcert -v \
    -keystore "${CLOUD_KEYSTORE}" \
    -storepass:env "PASSWORD" \
    -alias "${MASTER_KEY_ALIAS}" \
    -file "${MASTER_CERTIFICATE}" \
    -trustcacerts \
    -noprompt

  keytool -certreq -v \
    -keystore "${CLOUD_KEYSTORE}" \
    -storepass:env "PASSWORD" \
    -alias "${CLOUD_KEY_ALIAS}" \
    -keypass:env "PASSWORD" |
    keytool -gencert -v \
      -keystore "${MASTER_KEYSTORE}" \
      -storepass:env "PASSWORD" \
      -validity "3650" \
      -alias "${MASTER_KEY_ALIAS}" \
      -keypass:env "PASSWORD" \
      -ext "BasicConstraints=ca:true,pathlen:2" \
      -rfc |
    keytool -importcert \
      -keystore "${CLOUD_KEYSTORE}" \
      -storepass:env "PASSWORD" \
      -alias "${CLOUD_KEY_ALIAS}" \
      -keypass:env "PASSWORD" \
      -trustcacerts \
      -noprompt

  keytool -exportcert -v \
    -keystore "${CLOUD_KEYSTORE}" \
    -storepass:env "PASSWORD" \
    -alias "${CLOUD_KEY_ALIAS}" \
    -keypass:env "PASSWORD" \
    -file "${CLOUD_CERTIFICATE}" \
    -rfc
}

create_system_keystore() {
  local MASTER_KEYSTORE=$1
  local MASTER_KEY_ALIAS=$2
  local MASTER_CERTIFICATE="${MASTER_KEYSTORE%.*}.cer"
  local CLOUD_KEYSTORE=$3
  local CLOUD_KEY_ALIAS=$4
  local CLOUD_CERTIFICATE="${CLOUD_KEYSTORE%.*}.cer"
  local SYSTEM_KEYSTORE=$5
  local SYSTEM_KEY_ALIAS=$6
  local SAN=$7

  rm -f "${SYSTEM_KEYSTORE}"
  rm -f "${SYSTEM_CERTIFICATE}"
  mkdir -p "$(dirname "${SYSTEM_KEYSTORE}")"

  keytool -genkeypair -v \
    -keystore "${SYSTEM_KEYSTORE}" \
    -storepass:env "PASSWORD" \
    -keyalg "RSA" \
    -keysize "2048" \
    -validity "3650" \
    -alias "${SYSTEM_KEY_ALIAS}" \
    -keypass:env "PASSWORD" \
    -dname "CN=${SYSTEM_KEY_ALIAS}" \
    -ext "SubjectAlternativeName=${SAN}"

  keytool -importcert -v \
    -keystore "${SYSTEM_KEYSTORE}" \
    -storepass:env "PASSWORD" \
    -alias "${MASTER_KEY_ALIAS}" \
    -file "${MASTER_CERTIFICATE}" \
    -trustcacerts \
    -noprompt

  keytool -importcert -v \
    -keystore "${SYSTEM_KEYSTORE}" \
    -storepass:env "PASSWORD" \
    -alias "${CLOUD_KEY_ALIAS}" \
    -file "${CLOUD_CERTIFICATE}" \
    -trustcacerts \
    -noprompt

  keytool -certreq -v \
    -keystore "${SYSTEM_KEYSTORE}" \
    -storepass:env "PASSWORD" \
    -alias "${SYSTEM_KEY_ALIAS}" \
    -keypass:env "PASSWORD" |
    keytool -gencert -v \
      -keystore "${CLOUD_KEYSTORE}" \
      -storepass:env "PASSWORD" \
      -validity "3650" \
      -alias "${CLOUD_KEY_ALIAS}" \
      -keypass:env "PASSWORD" \
      -ext "SubjectAlternativeName=${SAN}" \
      -rfc |
    keytool -importcert \
      -keystore "${SYSTEM_KEYSTORE}" \
      -storepass:env "PASSWORD" \
      -alias "${SYSTEM_KEY_ALIAS}" \
      -keypass:env "PASSWORD" \
      -trustcacerts \
      -noprompt
}

create_truststore() {
  local TRUSTSTORE=$1
  local ARGC=$#
  local ARGV=("$@")

  for ((j = 1; j < ARGC; j = j + 2)); do
    keytool -importcert -v \
      -keystore "${TRUSTSTORE}" \
      -storepass:env "PASSWORD" \
      -file "${ARGV[j]}" \
      -alias "${ARGV[j + 1]}" \
      -trustcacerts \
      -noprompt
  done
}

# ROOT

create_root_keystore \
  "cloud-root/root.p12" "arrowhead.eu"

# RELAY "CLOUD"

create_cloud_keystore \
  "cloud-root/root.p12" "arrowhead.eu" \
  "cloud-relay/conet-demo-relay.p12" "conet-demo-relay.ltu.arrowhead.eu"

create_system_keystore \
  "cloud-root/root.p12" "arrowhead.eu" \
  "cloud-relay/conet-demo-relay.p12" "conet-demo-relay.ltu.arrowhead.eu" \
  "cloud-relay/alpha.p12" "alpha.conet-demo-relay.ltu.arrowhead.eu" \
  "dns:alpha.relay,ip:172.23.1.11,dns:localhost,ip:127.0.0.1"

create_truststore \
  "cloud-relay/truststore.p12" \
  "cloud-root/root.cer" "arrowhead.eu"

# CONSUMER CLOUD

create_cloud_keystore \
  "cloud-root/root.p12" "arrowhead.eu" \
  "cloud-data-consumer/conet-demo-consumer.p12" "conet-demo-consumer.ltu.arrowhead.eu"

create_consumer_system_keystore () {
  SYSTEM_NAME=$1
  SYSTEM_IP=$2

  create_system_keystore \
    "cloud-root/root.p12" "arrowhead.eu" \
    "cloud-data-consumer/conet-demo-consumer.p12" "conet-demo-consumer.ltu.arrowhead.eu" \
    "cloud-data-consumer/${SYSTEM_NAME}.p12" "${SYSTEM_NAME}.conet-demo-consumer.ltu.arrowhead.eu" \
    "dns:${SYSTEM_NAME}.consumer,ip:${SYSTEM_IP},dns:localhost,ip:127.0.0.1"
}

create_consumer_system_keystore "authorization"   "172.23.2.13"
create_consumer_system_keystore "contractproxy"   "172.23.2.14"
create_consumer_system_keystore "dataconsumer"    "172.23.2.15"
create_consumer_system_keystore "eventhandler"    "172.23.2.16"
create_consumer_system_keystore "gatekeeper"      "172.23.2.17"
create_consumer_system_keystore "gateway"         "172.23.2.18"
create_consumer_system_keystore "orchestrator"    "172.23.2.19"
create_consumer_system_keystore "serviceregistry" "172.23.2.20"

create_system_keystore \
  "cloud-root/root.p12" "arrowhead.eu" \
  "cloud-data-consumer/conet-demo-consumer.p12" "conet-demo-consumer.ltu.arrowhead.eu" \
  "cloud-data-consumer/sysop.p12" "sysop.conet-demo-consumer.ltu.arrowhead.eu" \
  "dns:localhost,ip:127.0.0.1"

create_truststore \
  "cloud-data-consumer/truststore.p12" \
  "cloud-data-consumer/conet-demo-consumer.cer" "conet-demo-consumer.ltu.arrowhead.eu" \
  "cloud-relay/conet-demo-relay.cer" "conet-demo-relay.ltu.arrowhead.eu"

# PRODUCER CLOUD

create_cloud_keystore \
  "cloud-root/root.p12" "arrowhead.eu" \
  "cloud-data-producer/conet-demo-producer.p12" "conet-demo-producer.ltu.arrowhead.eu"

create_producer_system_keystore () {
  SYSTEM_NAME=$1
  SYSTEM_IP=$2

  create_system_keystore \
    "cloud-root/root.p12" "arrowhead.eu" \
    "cloud-data-producer/conet-demo-producer.p12" "conet-demo-producer.ltu.arrowhead.eu" \
    "cloud-data-producer/${SYSTEM_NAME}.p12" "${SYSTEM_NAME}.conet-demo-producer.ltu.arrowhead.eu" \
    "dns:${SYSTEM_NAME}.producer,ip:${SYSTEM_IP},dns:localhost,ip:127.0.0.1"
}

create_producer_system_keystore "authorization"   "172.23.3.13"
create_producer_system_keystore "contractproxy"   "172.23.3.14"
create_producer_system_keystore "dataconsumer"    "172.23.3.15"
create_producer_system_keystore "eventhandler"    "172.23.3.16"
create_producer_system_keystore "gatekeeper"      "172.23.3.17"
create_producer_system_keystore "gateway"         "172.23.3.18"
create_producer_system_keystore "orchestrator"    "172.23.3.19"
create_producer_system_keystore "serviceregistry" "172.23.3.20"

create_system_keystore \
  "cloud-root/root.p12" "arrowhead.eu" \
  "cloud-data-producer/conet-demo-producer.p12" "conet-demo-producer.ltu.arrowhead.eu" \
  "cloud-data-producer/sysop.p12" "sysop.conet-demo-producer.ltu.arrowhead.eu" \
  "dns:localhost,ip:127.0.0.1"

create_truststore \
  "cloud-data-producer/truststore.p12" \
  "cloud-data-producer/conet-demo-producer.cer" "conet-demo-producer.ltu.arrowhead.eu" \
  "cloud-relay/conet-demo-relay.cer" "conet-demo-relay.ltu.arrowhead.eu"
