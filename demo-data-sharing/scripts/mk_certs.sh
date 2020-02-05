#!/bin/bash

# Uses `keytool` to generate and sign all keystores, certificates and truststores
# required to run the data sharing demo.

# This password is used for everything.
export PASSWORD="123456"

exit_script() {
  echo -e "\e[1;31mAborting ...\e[0m"
  trap - SIGINT SIGTERM
  kill -- -$$
}
trap exit_script SIGINT SIGTERM

create_root_keystore() {
  local MASTER_KEYSTORE=$1
  local MASTER_KEY_ALIAS=$2
  local MASTER_CERT_FILE="${MASTER_KEYSTORE%.*}.crt"

  echo -e "\e[34mCreating \e[33m${MASTER_KEYSTORE}\e[34m and \e[33m${MASTER_CERT_FILE}\e[34m ...\e[0m"

  rm -f "${MASTER_KEYSTORE}"
  rm -f "${MASTER_CERT_FILE}"
  mkdir -p "$(dirname "${MASTER_CERT_FILE}")"

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
    -file "${MASTER_CERT_FILE}" \
    -rfc
}

create_cloud_keystore() {
  local MASTER_KEYSTORE=$1
  local MASTER_KEY_ALIAS=$2
  local MASTER_CERT_FILE="${MASTER_KEYSTORE%.*}.crt"
  local CLOUD_KEYSTORE=$3
  local CLOUD_KEY_ALIAS=$4
  local CLOUD_CERT_FILE="${CLOUD_KEYSTORE%.*}.crt"

  echo -e "\e[34mCreating \e[33m${CLOUD_KEYSTORE}\e[34m and \e[33m${CLOUD_CERT_FILE}\e[34m ...\e[0m"

  rm -f "${CLOUD_KEYSTORE}"
  rm -f "${CLOUD_CERT_FILE}"
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
    -file "${MASTER_CERT_FILE}" \
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
    -file "${CLOUD_CERT_FILE}" \
    -rfc
}

create_system_keystore() {
  local MASTER_KEYSTORE=$1
  local MASTER_KEY_ALIAS=$2
  local MASTER_CERT_FILE="${MASTER_KEYSTORE%.*}.crt"
  local CLOUD_KEYSTORE=$3
  local CLOUD_KEY_ALIAS=$4
  local CLOUD_CERT_FILE="${CLOUD_KEYSTORE%.*}.crt"
  local SYSTEM_KEYSTORE=$5
  local SYSTEM_KEY_ALIAS=$6
  local SYSTEM_PUB_FILE="${SYSTEM_KEYSTORE%.*}.pub"
  local SAN=$7

  echo -e "\e[34mCreating \e[33m${SYSTEM_KEYSTORE}\e[34m and \e[33m${SYSTEM_PUB_FILE}\e[34m ...\e[0m"

  rm -f "${SYSTEM_KEYSTORE}"
  rm -f "${SYSTEM_PUB_FILE}"
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
    -file "${MASTER_CERT_FILE}" \
    -trustcacerts \
    -noprompt

  keytool -importcert -v \
    -keystore "${SYSTEM_KEYSTORE}" \
    -storepass:env "PASSWORD" \
    -alias "${CLOUD_KEY_ALIAS}" \
    -file "${CLOUD_CERT_FILE}" \
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

  keytool -list \
    -keystore "${SYSTEM_KEYSTORE}" \
    -storepass:env "PASSWORD" \
    -alias "${SYSTEM_KEY_ALIAS}" \
    -rfc |
    openssl x509 \
      -inform pem \
      -pubkey \
      -noout >"${SYSTEM_PUB_FILE}"
}

create_sysop_keystore() {
  local MASTER_KEYSTORE=$1
  local MASTER_CERT_FILE="${MASTER_KEYSTORE%.*}.crt"
  local CLOUD_KEYSTORE=$3
  local CLOUD_CERT_FILE="${CLOUD_KEYSTORE%.*}.crt"
  local SYSOP_KEYSTORE=$5
  local SYSOP_KEY_ALIAS=$6
  local SYSOP_CA_FILE="${SYSOP_KEYSTORE%.*}.ca"
  local SYSOP_CERT_FILE="${SYSOP_KEYSTORE%.*}.crt"
  local SYSOP_KEY_FILE="${SYSOP_KEYSTORE%.*}.key"

  rm -f "${SYSOP_CA_FILE}"
  rm -f "${SYSOP_CERT_FILE}"
  rm -f "${SYSOP_KEY_FILE}"

  create_system_keystore "$1" "$2" "$3" "$4" "$5" "$6" "dns:localost,ip:127.0.0.1"

  echo -e "\e[34mCreating \e[33m${SYSOP_CA_FILE}\e[34m, \e[33m${SYSOP_CERT_FILE}\e[34m and \e[33m${SYSOP_KEY_FILE}\e[34m ...\e[0m"

  cat "${MASTER_CERT_FILE}" >"${SYSOP_CA_FILE}"
  cat "${CLOUD_CERT_FILE}" >>"${SYSOP_CA_FILE}"

  keytool -exportcert -v \
    -keystore "${SYSOP_KEYSTORE}" \
    -storepass:env "PASSWORD" \
    -alias "${SYSOP_KEY_ALIAS}" \
    -keypass:env "PASSWORD" \
    -rfc >>"${SYSOP_CERT_FILE}"

  openssl pkcs12 \
    -in "${SYSOP_KEYSTORE}" \
    -passin env:PASSWORD \
    -out "${SYSOP_KEY_FILE}" \
    -nocerts \
    -nodes
}

create_truststore() {
  local TRUSTSTORE=$1
  local ARGC=$#
  local ARGV=("$@")

  rm -f "${TRUSTSTORE}"
  mkdir -p "$(dirname "${TRUSTSTORE}")"

  echo -e "\e[34mCreating \e[33m${TRUSTSTORE}\e[34m ...\e[0m"

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

cd "$(dirname "$0")" || exit
cd ..

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
  "cloud-root/root.crt" "arrowhead.eu"

# CONSUMER CLOUD

create_cloud_keystore \
  "cloud-root/root.p12" "arrowhead.eu" \
  "cloud-data-consumer/conet-demo-consumer.p12" "conet-demo-consumer.ltu.arrowhead.eu"

create_consumer_system_keystore() {
  SYSTEM_NAME=$1
  SYSTEM_IP=$2

  create_system_keystore \
    "cloud-root/root.p12" "arrowhead.eu" \
    "cloud-data-consumer/conet-demo-consumer.p12" "conet-demo-consumer.ltu.arrowhead.eu" \
    "cloud-data-consumer/${SYSTEM_NAME}.p12" "${SYSTEM_NAME}.conet-demo-consumer.ltu.arrowhead.eu" \
    "dns:${SYSTEM_NAME}.consumer,ip:${SYSTEM_IP},dns:localhost,ip:127.0.0.1"
}

create_consumer_system_keystore "authorization" "172.23.2.13"
create_consumer_system_keystore "contractproxy" "172.23.2.14"
create_consumer_system_keystore "dataconsumer" "172.23.2.15"
create_consumer_system_keystore "eventhandler" "172.23.2.16"
create_consumer_system_keystore "gatekeeper" "172.23.2.17"
create_consumer_system_keystore "gateway" "172.23.2.18"
create_consumer_system_keystore "orchestrator" "172.23.2.19"
create_consumer_system_keystore "serviceregistry" "172.23.2.20"

create_sysop_keystore \
  "cloud-root/root.p12" "arrowhead.eu" \
  "cloud-data-consumer/conet-demo-consumer.p12" "conet-demo-consumer.ltu.arrowhead.eu" \
  "cloud-data-consumer/sysop.p12" "sysop.conet-demo-consumer.ltu.arrowhead.eu"

create_truststore \
  "cloud-data-consumer/truststore.p12" \
  "cloud-data-consumer/conet-demo-consumer.crt" "conet-demo-consumer.ltu.arrowhead.eu" \
  "cloud-relay/conet-demo-relay.crt" "conet-demo-relay.ltu.arrowhead.eu"

# PRODUCER CLOUD

create_cloud_keystore \
  "cloud-root/root.p12" "arrowhead.eu" \
  "cloud-data-producer/conet-demo-producer.p12" "conet-demo-producer.ltu.arrowhead.eu"

create_producer_system_keystore() {
  SYSTEM_NAME=$1
  SYSTEM_IP=$2

  create_system_keystore \
    "cloud-root/root.p12" "arrowhead.eu" \
    "cloud-data-producer/conet-demo-producer.p12" "conet-demo-producer.ltu.arrowhead.eu" \
    "cloud-data-producer/${SYSTEM_NAME}.p12" "${SYSTEM_NAME}.conet-demo-producer.ltu.arrowhead.eu" \
    "dns:${SYSTEM_NAME}.producer,ip:${SYSTEM_IP},dns:localhost,ip:127.0.0.1"
}

create_producer_system_keystore "authorization" "172.23.3.13"
create_producer_system_keystore "contractproxy" "172.23.3.14"
create_producer_system_keystore "dataconsumer" "172.23.3.15"
create_producer_system_keystore "eventhandler" "172.23.3.16"
create_producer_system_keystore "gatekeeper" "172.23.3.17"
create_producer_system_keystore "gateway" "172.23.3.18"
create_producer_system_keystore "orchestrator" "172.23.3.19"
create_producer_system_keystore "serviceregistry" "172.23.3.20"

create_sysop_keystore \
  "cloud-root/root.p12" "arrowhead.eu" \
  "cloud-data-producer/conet-demo-producer.p12" "conet-demo-producer.ltu.arrowhead.eu" \
  "cloud-data-producer/sysop.p12" "sysop.conet-demo-producer.ltu.arrowhead.eu"

create_truststore \
  "cloud-data-producer/truststore.p12" \
  "cloud-data-producer/conet-demo-producer.crt" "conet-demo-producer.ltu.arrowhead.eu" \
  "cloud-relay/conet-demo-relay.crt" "conet-demo-relay.ltu.arrowhead.eu"
