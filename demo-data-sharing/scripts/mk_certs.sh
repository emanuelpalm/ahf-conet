#!/bin/bash

cd "$(dirname "$0")" || exit
source "lib_certs.sh"
cd ..

# ROOT

create_root_keystore \
  "cloud-root/crypto/root.p12" "arrowhead.eu"

# RELAY "CLOUD"

create_cloud_keystore \
  "cloud-root/crypto/root.p12" "arrowhead.eu" \
  "cloud-relay/crypto/conet-demo-relay.p12" "conet-demo-relay.ltu.arrowhead.eu"

create_system_keystore \
  "cloud-root/crypto/root.p12" "arrowhead.eu" \
  "cloud-relay/crypto/conet-demo-relay.p12" "conet-demo-relay.ltu.arrowhead.eu" \
  "cloud-relay/crypto/alpha.p12" "alpha.conet-demo-relay.ltu.arrowhead.eu" \
  "dns:alpha.relay,ip:172.23.1.11,dns:localhost,ip:127.0.0.1"

create_truststore \
  "cloud-relay/crypto/truststore.p12" \
  "cloud-root/crypto/root.crt" "arrowhead.eu"

# CONSUMER CLOUD

create_cloud_keystore \
  "cloud-root/crypto/root.p12" "arrowhead.eu" \
  "cloud-data-consumer/crypto/conet-demo-consumer.p12" "conet-demo-consumer.ltu.arrowhead.eu"

create_consumer_system_keystore() {
  SYSTEM_NAME=$1
  SYSTEM_SAN=$2

  create_system_keystore \
    "cloud-root/crypto/root.p12" "arrowhead.eu" \
    "cloud-data-consumer/crypto/conet-demo-consumer.p12" "conet-demo-consumer.ltu.arrowhead.eu" \
    "cloud-data-consumer/crypto/${SYSTEM_NAME}.p12" "${SYSTEM_NAME}.conet-demo-consumer.ltu.arrowhead.eu" \
    "${SYSTEM_SAN}"
}

create_consumer_system_keystore "authorization"    "ip:172.23.2.13,dns:localhost,ip:127.0.0.1"
create_consumer_system_keystore "contract-proxy"   "ip:172.23.2.14,dns:localhost,ip:127.0.0.1"
create_consumer_system_keystore "data-consumer"    "ip:172.23.2.15,dns:localhost,ip:127.0.0.1"
create_consumer_system_keystore "event_handler"    "ip:172.23.2.16,dns:localhost,ip:127.0.0.1"
create_consumer_system_keystore "gatekeeper"       "ip:172.23.2.17,dns:localhost,ip:127.0.0.1"
create_consumer_system_keystore "gateway"          "ip:172.23.2.18,dns:localhost,ip:127.0.0.1"
create_consumer_system_keystore "orchestrator"     "ip:172.23.2.19,dns:localhost,ip:127.0.0.1"
create_consumer_system_keystore "service_registry" "ip:172.23.2.20,dns:localhost,ip:127.0.0.1"

create_sysop_keystore \
  "cloud-root/crypto/root.p12" "arrowhead.eu" \
  "cloud-data-consumer/crypto/conet-demo-consumer.p12" "conet-demo-consumer.ltu.arrowhead.eu" \
  "cloud-data-consumer/crypto/sysop.p12" "sysop.conet-demo-consumer.ltu.arrowhead.eu"

create_truststore \
  "cloud-data-consumer/crypto/truststore.p12" \
  "cloud-data-consumer/crypto/conet-demo-consumer.crt" "conet-demo-consumer.ltu.arrowhead.eu" \
  "cloud-relay/crypto/conet-demo-relay.crt" "conet-demo-relay.ltu.arrowhead.eu"

# PRODUCER CLOUD

create_cloud_keystore \
  "cloud-root/crypto/root.p12" "arrowhead.eu" \
  "cloud-data-producer/crypto/conet-demo-producer.p12" "conet-demo-producer.ltu.arrowhead.eu"

create_producer_system_keystore() {
  SYSTEM_NAME=$1

  create_system_keystore \
    "cloud-root/crypto/root.p12" "arrowhead.eu" \
    "cloud-data-producer/crypto/conet-demo-producer.p12" "conet-demo-producer.ltu.arrowhead.eu" \
    "cloud-data-producer/crypto/${SYSTEM_NAME}.p12" "${SYSTEM_NAME}.conet-demo-producer.ltu.arrowhead.eu" \
    "dns:core.producer,ip:172.23.3.13,dns:localhost,ip:127.0.0.1"
}

create_producer_system_keystore "authorization"
create_producer_system_keystore "contract-proxy"
create_producer_system_keystore "data-consumer"
create_producer_system_keystore "event_handler"
create_producer_system_keystore "gatekeeper"
create_producer_system_keystore "gateway"
create_producer_system_keystore "orchestrator"
create_producer_system_keystore "service_registry"

create_sysop_keystore \
  "cloud-root/crypto/root.p12" "arrowhead.eu" \
  "cloud-data-producer/crypto/conet-demo-producer.p12" "conet-demo-producer.ltu.arrowhead.eu" \
  "cloud-data-producer/crypto/sysop.p12" "sysop.conet-demo-producer.ltu.arrowhead.eu"

create_truststore \
  "cloud-data-producer/crypto/truststore.p12" \
  "cloud-data-producer/crypto/conet-demo-producer.crt" "conet-demo-producer.ltu.arrowhead.eu" \
  "cloud-relay/crypto/conet-demo-relay.crt" "conet-demo-relay.ltu.arrowhead.eu"
