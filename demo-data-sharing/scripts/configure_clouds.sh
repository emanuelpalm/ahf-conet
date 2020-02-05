#!/bin/bash

# Configures the systems of both the consumer and producer clouds to make them
# able to communicate. Run this script after bringing up the Docker network and
# waiting for all Docker containers to finish initializing.

cd "$(dirname "$0")" || exit
cd ..

register_relay() {
  local CACERT=$1
  local CERT=$2
  local KEY=$3
  local GATEKEEPER_DOMAIN=$4
  local RELAY_ADDRESS=$5
  local RELAY_PORT=$6

  echo -e "\e[34mRegistering relay \e[33m${RELAY_ADDRESS}:${RELAY_PORT}\e[34m with \e[33m${GATEKEEPER_DOMAIN}\e[34m ...\e[0m"

  curl -X "POST" "https://${GATEKEEPER_DOMAIN}/gatekeeper/mgmt/relays" \
    --header "Content-Type: application/json;charset=UTF-8" \
    --data-binary "[{\"address\":\"${RELAY_ADDRESS}\",\"port\":\"${RELAY_PORT}\",\"type\":\"GENERAL_RELAY\",\"secure\":true,\"exclusive\":false}]" \
    --cacert "${CACERT}" \
    --cert "${CERT}" \
    --key "${KEY}" \
    --compressed
}

read_public_key() {
  local FILE=$1

  local BUFFER=()
  while IFS=$' \t\r\n' read -r LINE; do
    [[ $LINE == *'-END PUBLIC KEY-'* ]] && P=0
    ((P)) && BUFFER+=("$LINE")
    [[ $LINE == *'-BEGIN PUBLIC KEY-'* ]] && P=1
  done <"$FILE"

  IFS= eval 'MERGED_KEY="${BUFFER[*]}"' ## Merge key without spaces.
  echo "$MERGED_KEY"
}

function jsonValue() {
KEY=$1
num=$2
awk -F"[,:}]" '{for(i=1;i<=NF;i++){if($i~/'$KEY'\042/){print $(i+1)}}}' | tr -d '"' | sed -n ${num}p
}

get_json_values_by_key() {
  local KEY=$1
  local JSON=$2

  echo "$JSON" | awk -F"[,:}]" '{for(i=1;i<=NF;i++){if($i~/'"$KEY"'\042/){print $(i+1)}}}' | tr -d '"'
}

register_neighbor_cloud() {
  local CACERT=$1
  local CERT=$2
  local KEY=$3

  curl -X "POST" "https://${GATEKEEPER_DOMAIN}/gatekeeper/mgmt/relays" \
    --header "Content-Type: application/json;charset=UTF-8" \
    --data-binary "[{\"address\":\"${RELAY_ADDRESS}\",\"port\":\"${RELAY_PORT}\",\"type\":\"GENERAL_RELAY\",\"secure\":true,\"exclusive\":false}]" \
    --cacert "${CACERT}" \
    --cert "${CERT}" \
    --key "${KEY}" \
    --compressed
}

## CONSUMER CLOUD
#
#register_relay \
#  "./cloud-data-consumer/sysop.ca" "./cloud-data-consumer/sysop.crt" "./cloud-data-consumer/sysop.key" \
#  "172.23.2.17:8445" \
#  "172.23.1.11" "61617"
#
## PRODUCER CLOUD
#
#register_relay \
#  "./cloud-data-producer/sysop.ca" "./cloud-data-producer/sysop.crt" "./cloud-data-producer/sysop.key" \
#  "172.23.3.17:8545" \
#  "172.23.1.11" "61617"

#read_public_key "./cloud-data-producer/gatekeeper.pub"

get_json_values_by_key "id" '{"data": [{"id": 1,"address": "10.0.0.85","port": 61617,"id": 12,"exclusive": false,"type": "GENERAL_RELAY","createdAt": "2019-10-17 10:16:33","updatedAt": "2019-10-17 10:16:33"}],"count": 1}'
