Movecraft
======
![Java CI](https://github.com/APDevTeam/Movecraft/workflows/Java%20CI/badge.svg?branch=master)

This is a maintained fork of Movecraft, which aims to add legacy version support as well as performance fixes.

**Movecraft requires Java 11**

## Download

Public builds, as well as older builds (1.10 and below), are located on the [Spigot forums](https://www.spigotmc.org/resources/movecraft.31321/)

Development builds can be found under the [actions tab](https://github.com/APDevTeam/Movecraft/actions?query=workflow%3A%22Java+CI%22).

## Building
Movecraft uses multiple versions of the Spigot server software for legacy support. As such, you need to run [BuildTools](https://www.spigotmc.org/wiki/buildtools/) for several versions before building the plugin. It doesn't matter where you do this, but inside the Movecraft directory is probably a bad place.

```
java -jar BuildTools.jar --rev 1.16.1
java -jar BuildTools.jar --rev 1.16.5
```

Then, run the following to build Movecraft through `maven`.
```
mvn -T 1C clean install
```
Jars are located in `/target`.

## Support
[Github Issues](https://github.com/apdevteam/movecraft/issues)

[Discord](http://bit.ly/JoinAP-Dev)

The plugin is released here under the GNU General Public License V3. 
