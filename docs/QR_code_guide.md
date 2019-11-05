# QR code guide

`qrencode -o qr_code.png "<QR content string>"`

## Wifi QR code

### Schema:
```
WIFI:S:<SSID>;T:WPA2;P:<PW>;;
```

Replace *\<SSID\>*, *\<PW\>* and notice semicolons.   
    
Use a QR code generator like <pre>qrencode</pre> or any other qr code tool for image generation.

[More details](https://github.com/zxing/zxing/wiki/Barcode-Contents#wi-fi-network-config-android-ios-11)


## Device QR code

**Schema (valid JSON-Format):** 
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

#### Description:

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


### Example qr codes:
Have a look at the QR code generation file of valid (and invalid) strings for test purposes [test_qr_codes.sh](./Testnet/test_qr_codes.sh).

