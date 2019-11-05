#!/usr/bin/env bash

# get current directory path
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd $DIR
rm *.json

# cmd for rfc1213 mib + x:
mibdump.py --destination-format=json --no-python-compile --generate-mib-texts --keep-texts-layout --build-index --mib-stub= --mib-source=file://$DIR/../orig/ RFC1213-MIB IP-MIB IP-FORWARD-MIB SNMP-FRAMEWORK-MIB SNMP-MPD-MIB TRANSPORT-ADDRESS-MIB IF-MIB IANA-RTPROTO-MIB TCP-MIB UDP-MIB HOST-RESOURCES-MIB HOST-RESOURCES-TYPES OPENBSD-BASE-MIB OPENBSD-CARP-MIB OPENBSD-MEM-MIB OPENBSD-PF-MIB OPENBSD-RELAYD-MIB OPENBSD-SENSORS-MIB UCD-SNMP-MIB
