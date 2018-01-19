Movecraft
======
[![codebeat badge](https://codebeat.co/badges/77751ae4-80f7-460a-a225-0e3ae8cbbab1)](https://codebeat.co/projects/github-com-apdevteam-movecraft-master)
[![Build Status](https://travis-ci.org/APDevTeam/Movecraft.svg?branch=master)](https://travis-ci.org/APDevTeam/Movecraft)

This is a maintained fork of Movecraft which aims to add legacy version suppourt as well performance fixes.

**Movecraft requires java 8**

## Download
~~Release builds can be found on the spigot forums [here](https://www.spigotmc.org/resources/movecraft.31321/).~~

Development builds can be found on Travis [here](https://travis-ci.org/APDevTeam/Movecraft).

## Building
Movecraft uses multiple versions of the Spigot server software for legacy suppourt. As such you need to run Buildtools for several versions before building the plugin.

```
java -jar BuildTools.jar --rev 1.10
java -jar BuildTools.jar --rev 1.11
java -jar BuildTools.jar --rev 1.12 
```
Then, run the following to build Movecraft through maven.
```
mvn clean instal
```
Jars are located in the /target folder

## Suppourt
[Github issues](https://github.com/apdevteam/movecraft/issues)

The plugin is released here under the GNU General Public License V3. 
