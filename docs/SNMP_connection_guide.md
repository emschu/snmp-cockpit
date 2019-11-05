# SNMP Connection guide

- A response to OID 1.3.6.1.2.1.1.5.0 (sysName, RFC 1213) needs to be present
- OIDs for queries within the app are handled without trailing zero (".0").

## SNMP daemon configuration
- [SNMPv3 Users guide](https://www.sysadmin.md/snmpv3-users-cheatsheet.html) 

## Integrated MIBs by default
- RFC1213-MIB 
- IP-MIB 
- IP-FORWARD-MIB
- SNMP-FRAMEWORK-MIB
- SNMP-MPD-MIB 
- TRANSPORT-ADDRESS-MIB 
- IF-MIB 
- IANA-RTPROTO-MIB 
- TCP-MIB 
- UDP-MIB 
- HOST-RESOURCES-MIB 
- HOST-RESOURCES-TYPES
- OPENBSD-BASE-MIB 
- OPENBSD-CARP-MIB 
- OPENBSD-MEM-MIB 
- OPENBSD-PF-MIB 
- OPENBSD-RELAYD-MIB 
- OPENBSD-SENSORS-MIB 
- UCD-SNMP-MIB


## Supported SNMP Auth protocols
- SHA-1
- MD5
- HMAC128SHA224
- HMAC192SHA256
- HMAC256SHA384
- HMAC384SHA512

## Supported transport security/"privacy" protocols
- AES-128
- DES
- AES-192
- AES-256
- 3DES

## Displayed SNMP tables/information in device detail activity

```
Tab: General
ifTable
ipDefaultRouterTable
ipAddrTable
ipRouteTable
ipNetToMediaTable
atTable
udpTable
tcpTable
egpNeighTable

Showed single OIDs:
        ip: 1.3.6.1.2.1.4
        icmp: 1.3.6.1.2.1.5
        tcp: 1.3.6.1.2.1.6
        udp: 1.3.6.1.2.1.7
        egp: 1.3.6.1.2.1.8

Tab: Hardware
sensorTable
dskTable
hrDeviceTable
hrDiskStorageTable
hrPartitionTable


Tab: Status
laTable
prTable
ipIfSystemStatsTable
ipIfStatsTable
icmpStatsTable

Tab: Usage
sysOrTable
mrTable
SnmpUsageStatistics
```