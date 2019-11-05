# SNMP Debugging

# Example SNMP v3 snmpwalk
```bash
snmpwalk -v3 -l authPriv -u superman -A password123 -a SHA -x AES -X batmankey 192.168.178.147 .1.3.6.1.2.1.1
```

# Example SNMP v1 snmpwalk
```bash
snmpwalk -v 1 -c public 192.168.178.62 system
```

with debug options (*-d*):
```bash
snmpwalk -v3 -d -l authPriv -L o -u batmanuser -A password123 -a SHA-1 -x AES -X batmankey 192.168.178.92 .1.3.6.1.2.1.1
```

# OpenBSD VirtualBox Setup Cheatsheet

## Step 1: Installation on OpenBSD (6.3)
## Step 2: Install packages

```bash
pkg_add -v -i vim
pkg_add -v -i net-snmp
```

## Configure snmpd

```bash
touch /etc/rc.conf.local
echo "snmpd_flags='-f /etc/snmpd.conf'" >> /etc/rc.conf.local
cp /etc/examples/snmpd.conf /etc/snmpd.conf
/etc/snmpd.conf 
> Change IP!
rcctl enable snmpd
rcctl start snmpd
```


# Docker Containers on Raspberry Pi

* Port 161: SNMPv1
* Port 162: SNMPv3

Clone this Repo in pi home folder on raspberry pi:
`git clone https://github.com/pozgo/docker-snmpd`

Replace "alpine:latest" in Dockerfile with "resin/raspberrypi3-alpine":

Build container image with name `snmpdt`:
```bash
sudo docker build -t snmpdt:latest .
```

Kopiere Dateien aus dem Ordner *./docs/config/raspbian* in den Ordner */home/pi/docker-snmpd* auf dem Raspi.

Start (and configure) containers to run:
```bash
sudo docker run -d --name snmpdv1v2 --restart always -p 161:161/udp -v /home/pi/docker-snmpd/snmpd_v1.conf:/etc/snmp/snmpd.conf snmpdt:latest
sudo docker run -d --name snmpdv3 --restart always -p 162:161/udp -v /home/pi/docker-snmpd/snmpd_v3.conf:/etc/snmp/snmpd.conf snmpdt:latest
```

To stop and remove containers run:
```bash
sudo docker stop snmpdv1v2; sudo docker rm snmpdv1v2
sudo docker stop snmpdv3; sudo docker rm snmpdv3
```

Look for udp ports 161 and 162 and open ports in general:
```bash
netstat -ulep
...
udp6       0      0 :::161                  :::*                                0          172626     18834/docker-proxy
udp6       0      0 :::162                  :::*                                0          174461     19080/docker-proxy

```

### Test SNMP functionality of started containers

**SNMP v1/v2c:**
```
snmpwalk -v1 -c public 192.168.178.68 system
snmpwalk -v1 -c public 192.168.178.68:162 system
```

**SNMP v3:**
```
snmpwalk -v3 -l authPriv -L o -u batmanuser -A batmankey3 -a SHA -x AES -X batmankey3 192.168.178.68:162 .1.3.6.1.2.1.1
```

```bash
pi@snmp-test-host:~/docker-snmpd $ sudo docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS                           NAMES
073342c4bab7        snmpdt:latest       "/bootstrap.sh"     8 minutes ago       Up 8 minutes        161/tcp, 0.0.0.0:162->161/udp   snmpdv3
952c31e557d0        snmpdt:latest       "/bootstrap.sh"     9 minutes ago       Up 9 minutes        161/tcp, 0.0.0.0:161->161/udp   snmpdv1v2
```