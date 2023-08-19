## Device QR code

**Schema (valid JSON-Format)**
```json
{
     "user": "",
     "pw": "",
     "enc": "",
     "naddr": {
         "IPv4": "",
         "IPv6": ""
     }
}
```

#### Description

- **user**:
    - *v1/v2c*: Community
    - *v3*: User
- **pw**:
    - *v1/v2c*: Empty
    - *v3/v2c*: Auth password
- **enc**:
    - *v1/v2c*: Empty
    - *v3 simple*: Priv password
    - *v3 custom*: Schema: `SecurityLevel;AuthProtocol;PrivProtocol;Context;Password`
        - **SecurityLevel**: `int 0-2`
            - **0**: AUTH_PRIV
            - **1**: AUTH_NO_PRIV
            - **2**: NO_AUTH_NO_PRIV
        - **AuthProtocol**: `int 0-5`
            - **0**: SHA-1
            - **1**: MD5
            - **2**: HMAC128SHA224
            - **3**: HMAC192SHA256
            - **4**: HMAC256SHA384
            - **5**: HMAC384SHA512
        - **PrivProtocol**: `int 0-4`
            - **0**: AES-128
            - **1**: DES
            - **2**: AES-192
            - **3**: AES-256
            - **4**: 3DES
        - **Context**: empty or string
        - **Password**: Priv password
- **naddr**:
    - *v1 and v3*:
        - **IPv4**: IPv4-Addresse of a SNMP Daemon software
            - **Custom Port**: suffix string with ":\<Port\>" (as usual)
        - **IPv6**: Use "\[Address\]:Port" Schema
    - *Note:* If you provide both IPv4 and IPv6 address, the app will use the latter.


### Example QR codes

The following are valid JSON strings for device QR codes:

```
'{"user": "public","pw": "","enc": "","naddr": {"IPv4": "10.10.10.1:161","IPv6": ""}}'
'{"user": "sysadmin1","pw": "authkey1","enc": "privkey1","naddr": {"IPv4": "10.10.10.3","IPv6": ""}}'
'{"user": "sysadmin1","pw": "authkey1","enc": "0;0;0;;privkey1","naddr": {"IPv4": "10.10.10.3","IPv6": ""}}'
'{"user": "sysadmin1","pw": "authkey1","enc": "0;0;0;;privkey1","naddr": {"IPv4": "10.10.10.3:161","IPv6": ""}}'
'{"user": "sysadmin1","pw": "authkey1","enc": "0;0;0;null;privkey1","naddr": {"IPv4": "10.10.10.3","IPv6": ""}}'
'{"user": "sysadmin1","pw": "authkey1","enc": "10;0;0;null;privkey1","naddr": {"IPv4": "10.10.10.3","IPv6": ""}}'
'{"user": "sysadmin1","pw": "authkey1","enc": "0;10;0;null;privkey1","naddr": {"IPv4": "10.10.10.3","IPv6": ""}}'
'{"user": "sysadmin1","pw": "authkey1","enc": "0;0;10;null;privkey1","naddr": {"IPv4": "10.10.10.3","IPv6": ""}}'
'{"user": "sysadmin2","pw": "authkey2","enc": "privkey2","naddr": {"IPv4": "10.10.10.3","IPv6": ""}}'
'{"user": "sysadmin2","pw": "authkey2","enc": "0;0;1;;privkey2","naddr": {"IPv4": "10.10.10.3","IPv6": ""}}'
'{"user": "sysadmin3","pw": "authkey3","enc": "","naddr": {"IPv4": "10.10.10.3","IPv6": ""}}'
'{"user": "sysadmin3","pw": "authkey3","enc": "1;;;;","naddr": {"IPv4": "10.10.10.3","IPv6": ""}}'
'{"user": "sysadmin4","pw": "","enc": "2;;;;","naddr": {"IPv4": "10.10.10.3","IPv6": ""}}'
```
