# MIB OID catalog integration guide


# How-To generate a new MIB catalog file `oid_catalog.json` and `oid_tree.json`

*Note:* A very similar process is describe [here in this Gist](https://gist.github.com/emschu/d22dba9206b3bea99ae90af25c9eba71)

1. Checkout or download this repository and open `mib`-folder in repository root directory.
2. Install required python package `pysmi` or type `pip3 install -r requirements.txt` **or** `python3 -m pip install -r requirements.txt` 
3. Select and download your MIB files in `.txt` format (e.g. from [here](http://www.mibdepot.com)) to directory `mib/orig/`.
4. Append the name of these mibs to the `mibdump` command in `mib/mibs/converter.sh`. 
5. Go to the `mib/mibs` directory on a console and execute `converter.sh`. Scan the output carefully for possible problems and check, if new `.json` files have been created in the directory. 
6. Execute `python3 converter.py` in `mib/mibs`

# Package these files into a `.zip` archive
1. and use a meaningful name for the archive and type: `zip <good name>.zip oid_catalog.json oid_tree.json`. The `<good_name>` part is used within the app later as the MIB catalog's name.
2. Put this `.zip` archive into your mobile phone or use a file service provider on Android (e.g. Nextcloud Android App) and open the import dialog in the option menu of **MIB catalog screen**.
