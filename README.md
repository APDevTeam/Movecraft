Movecraft
======
![Java CI](https://github.com/eirik1996/Movecraft/workflows/Java%20CI/badge.svg?branch=master)
[![codebeat badge](https://codebeat.co/badges/77751ae4-80f7-460a-a225-0e3ae8cbbab1)](https://codebeat.co/projects/github-com-apdevteam-movecraft-master)
[![Build Status](https://travis-ci.org/APDevTeam/Movecraft.svg?branch=master)](https://travis-ci.org/APDevTeam/Movecraft)

This is a maintained fork of Movecraft, which aims to add legacy version support as well as performance fixes.

**Movecraft requires Java 8**

## Download

Development builds can be found on [Travis](https://travis-ci.org/APDevTeam/Movecraft).

Older builds (1.10 and below) are located on the [Spigot forums](https://www.spigotmc.org/resources/movecraft.31321/)

## Building
Movecraft uses multiple versions of the Spigot server software for legacy support. As such, you need to run [BuildTools](https://www.spigotmc.org/wiki/buildtools/) for several versions before building the plugin. It doesn't matter where you do this, but inside the Movecraft directory is probably a bad place.

```
java -jar BuildTools.jar --rev 1.8.8
java -jar BuildTools.jar --rev 1.9.4
java -jar BuildTools.jar --rev 1.10.2
java -jar BuildTools.jar --rev 1.11.2
java -jar BuildTools.jar --rev 1.12.1
java -jar BuildTools.jar --rev 1.13.2
java -jar BuildTools.jar --rev 1.14.4 --compile craftbukkit
java -jar BuildTools.jar --rev 1.15 --compile craftbukkit
java -jar BuildTools.jar --rev 1.16.1 --compile craftbukkit
java -jar BuildTools.jar --rev 1.16.2 --compile craftbukkit
```

Then, run the following to build Movecraft through `maven`.
```
mvn clean install
```
Jars are located in `/target`.

## Support
[Github Issues](https://github.com/eirikh1996/Movecraft/issues)

[Discord](https://discord.gg/ukYYG2T)

The plugin is released here under the GNU General Public License V3. 
