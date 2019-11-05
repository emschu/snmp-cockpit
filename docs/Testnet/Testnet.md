# Test device information

## Device QR Codes

For generation of a QR code run:
```bash
qrencode -o output.png 'QR-String'
```

### SNMPv1 (Port 161, UDP):

```json
{
    "user": "public",
    "pw": "",
    "enc": "",
    "naddr": {
        "IPv4": "192.168.161.1",
        "IPv6": "fe80::a7b0:aeb4:d913:ad99"
    }
}
```

*IPv6 is not yet implemented/tested!*

### SNMPv3 (Port 162, UDP):

```json
{
    "user": "batmanuser",
    "pw": "batmankey3",
    "enc": "batmankey3",
    "naddr": {
        "IPv4": "192.168.161.1:162",
        "IPv6": "[fe80::a7b0:aeb4:d913:ad99]:162"
    }
}
```
*IPv6 is not yet implemented/tested!*

## VirtualBox Net

### SNMP V1 VirtualBox, without IP port (using default 161)
```json
{
    "user": "public",
    "pw": "",
    "enc": "",
    "naddr": {
        "IPv4": "192.168.178.154",
        "IPv6": ""
    }
}
```

`{"user": "public","pw": "","enc": "","naddr": {"IPv4": "192.168.178.154","IPv6": ""}}`

### SNMP V1 VirtualBox, with explicit IP port
```json
{
    "user": "public",
    "pw": "",
    "enc": "",
    "naddr": {
        "IPv4": "192.168.178.154:161",
        "IPv6": ""
    }
}
```

`{"user": "public","pw": "","enc": "","naddr": {"IPv4": "192.168.178.154:161","IPv6": ""}}`

### SNMP V3 VirtualBox, with explicit IP port
```json
{
    "user": "batmanuser",
    "pw": "batmankey3",
    "enc": "batmankey3",
    "naddr": {
        "IPv4": "192.168.178.155",
        "IPv6": ""
    }
}
```

#### Without whitespace and line breaks:
```console
{"user": "batmanuser","pw": "batmankey3","enc": "batmankey3","naddr": {"IPv4": "192.168.178.155","IPv6": ""}}
```

#### With whitespace and line breaks
```console
{
    "user": "batmanuser",
    "pw": "batmankey3",
    "enc": "0;0;0;;batmankey3",
    "naddr": {
        "IPv4": "192.168.178.158",
        "IPv6": ""
    }
}
```

### Tested WIFI QR content strings  

```console
WIFI:S:<SSID>;T:WPA2;P:<PW>;;
```
