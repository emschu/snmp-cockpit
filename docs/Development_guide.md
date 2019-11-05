# Development guide

## General

Clone and open this project in *Android Studio* IDE and start development.

## Contributing
Fork this repository on *GitHub* and use the default Pull Request process against *master* branch of this repository.

## SNMP daemons
[net-snmp](http://net-snmp.org/)
[Mini SNMP Daemon](http://troglobit.com/projects/mini-snmpd/)

## How to build SNMP4J jar for this app

* Download [original snmp4j 3.3.1 dist](https://snmp.app/dist/release/org/snmp4j/snmp4j/3.3.1/snmp4j-3.3.1-distribution.zip)
* Run `rm src/main/java/module-info.java`
* Apply [patch file](./snmp4j_patch_for_jdk11_build_for_jdk8.patch)
* Build .jar file with JDK 11 (maven handles `--release 8`) with `mvn clean package` and copy it to `apps/libs` and replace other snmp4j .jar file. (you may want to add `-DskipTests=true`).


## Known exceptions:

"By default" Android throws some exceptions on some devices (including emulator) during execution of this app, you can ignore them safely.

At startup:
```
I/zygote: Rejecting re-init on previously-failed class java.lang.Class<androidx.core.view.ViewCompat$2>: java.lang.NoClassDefFoundError: Failed resolution of: Landroid/view/View$OnUnhandledKeyEventListener;
```
Explained [here](https://issuetracker.google.com/issues/117685087)


```
I/zygote: Rejecting re-init on previously-failed class java.lang.Class<uJ>: java.lang.NoClassDefFoundError: Failed resolution of: Landroid/webkit/TracingController;
```
Explained [here](https://stackoverflow.com/a/53017302)
