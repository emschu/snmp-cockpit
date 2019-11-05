#!/bin/bash

# variables which are replaced in code strings
IP_one="192.168.2.100"
IP_three="192.168.2.101"

if ! [ -x "$(command -v foo)" ]; then
    echo "You need qrencode installed"
    exit 1
fi

# collect correct codes here in this bash array
declare -a arr=('{"user": "public","pw": "","enc": "","naddr": {"IPv4": "IP_one","IPv6": ""}}'
                '{"user": "","pw": "","enc": "","naddr": {"IPv4": "IP_one","IPv6": ""}}'
                '{"user": "","pw": "","enc": "","naddr": {"IPv4": "IP_one:161","IPv6": ""}}'
                '{"user": "public","pw": "","enc": "","naddr": {"IPv4": "IP_one:161","IPv6": ""}}'
                '{"user": "sysadmin1","pw": "authkey1","enc": "privkey1","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin1","pw": "authkey1","enc": "0;0;0;;privkey1","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin1","pw": "authkey1","enc": "0;0;0;;privkey1","naddr": {"IPv4": "IP_three:161","IPv6": ""}}'
                '{"user": "sysadmin1","pw": "authkey1","enc": "0;0;0;null;privkey1","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin1","pw": "authkey1","enc": "10;0;0;null;privkey1","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin1","pw": "authkey1","enc": "0;10;0;null;privkey1","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin1","pw": "authkey1","enc": "0;0;10;null;privkey1","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin2","pw": "authkey2","enc": "privkey2","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin2","pw": "authkey2","enc": "0;0;1;;privkey2","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin3","pw": "authkey3","enc": "","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin3","pw": "authkey3","enc": "1;;;;","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin4","pw": "","enc": "2;;;;","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                )

mkdir -p correct-codes
n=0
for i in "${arr[@]}"
do
    fileName=correct-codes/test_"$n".png
    line=${i/IP_one/$IP_one}
    line=${line/IP_three/$IP_three}
    echo "Generating $n with string: $line"
    qrencode -o "$fileName" "$line"
    n=$((n+1))
done

# collect INCORRECT codes here in this bash array
declare -a arr=('{"user": "","pw": "authkey1","enc": "privkey1","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin1","pw": "authkey1","enc": "privkey1","naddr": {"IPv4": "IP_one:161","IPv6": ""}}'
                '{"user": "sysadmin1","pw": "","enc": "0;0;0;;privkey1","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin1","pw": "authkey1","enc": "10;0;0;null;privkey11","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin1","pw": "authkey1","enc": "10;0;0;privkey11","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin1","pw": "authkey1","enc": "10;privkey11","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin1","pw": "authkey2","enc": "privkey1","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin2","pw": "authkey2","enc": "0;1;1;;","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin3","pw": "authkey3","enc": "","naddr": {"IPv4": "IP_one","IPv6": ""}}'
                '{"user": "sysadmin3","pw": "authkey3","enc": "1;;;","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin3","pw": "authkey3","enc": "1;;","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "sysadmin4","pw": "","enc": "3;;;;","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                '{"user": "","pw": "","enc": "2;;;;","naddr": {"IPv4": "IP_three","IPv6": ""}}'
                )

mkdir -p incorrect-codes
n=0
for i in "${arr[@]}"
do
    fileName=incorrect-codes/test_"$n".png
    line=${i/IP_one/$IP_one}
    line=${line/IP_three/$IP_three}
    echo "Generating incorrect $n with string: $line"
    qrencode -o "$fileName" "$line"
    n=$((n+1))
done

# Wifi codes
declare -a arr=(
    'WIFI:S:snmpwlan;T:WPA2;P:Xkl98Ess87;;'
)

mkdir -p wifi-codes
n=0
for i in "${arr[@]}"
do
    fileName=wifi-codes/test_"$n".png
    echo "Generating wifi code $n with string: $i"
    qrencode -o "$fileName" "$i"
    n=$((n+1))
done