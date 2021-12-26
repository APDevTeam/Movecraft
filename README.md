Movecraft
======
![Java CI](https://github.com/APDevTeam/Movecraft/workflows/Java%20CI/badge.svg?branch=main)

This is a maintained fork of Movecraft, which aims to add legacy version support as well as performance fixes.

**Movecraft requires at least Java 13**

## Download

Public builds, as well as older builds (1.10 and below), are located on the [Spigot forums](https://www.spigotmc.org/resources/movecraft.31321/)

Development builds can be found under the [actions tab](https://github.com/APDevTeam/Movecraft/actions?query=workflow%3A%22Java+CI%22).

## Building
Movecraft uses multiple versions of the Spigot server software for legacy support. As such, you need to run [BuildTools](https://www.spigotmc.org/wiki/buildtools/) for several versions before building the plugin. It doesn't matter where you do this, but inside the Movecraft directory is probably a bad place.  Note: Spigot will require Java 13 to build 1.14.4, 1.15.2 & 1.16.5 and Java 17 to build 1.17.1 & 1.18.1.

```
java -jar BuildTools.jar --rev 1.14.4 --compile craftbukkit
java -jar BuildTools.jar --rev 1.15.2 --compile craftbukkit
java -jar BuildTools.jar --rev 1.16.5 --compile craftbukkit
java -jar BuildTools.jar --rev 1.17.1 --remapped
java -jar BuildTools.jar --rev 1.18.1 --remapped
```

Once you have compiled craftbukkit, it should continue to exist in your local maven repository, and thus you should need to compile each verson at most one time. Once complete, run the following to build Movecraft through `maven`.
```
mvn -T 1C clean install
```
Compiled jars can be found in the `/target` directory.

## Support
[Github Issues](https://github.com/apdevteam/movecraft/issues)

[Discord](http://bit.ly/JoinAP-Dev)

Movecraft is released under the GNU General Public License V3. 
