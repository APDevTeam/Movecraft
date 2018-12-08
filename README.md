Movecraft
======
[![codebeat badge](https://codebeat.co/badges/77751ae4-80f7-460a-a225-0e3ae8cbbab1)](https://codebeat.co/projects/github-com-apdevteam-movecraft-master)
[![Build Status](https://travis-ci.org/APDevTeam/Movecraft.svg?branch=master)](https://travis-ci.org/APDevTeam/Movecraft)

This is a maintained fork of Movecraft, which aims to add legacy version support as well as performance fixes.

**Movecraft requires Java 8**

## Download

Development builds can be found on [Travis](https://travis-ci.org/APDevTeam/Movecraft).

Older builds (1.10 and below) are located on the [Spigot forums](https://www.spigotmc.org/resources/movecraft.31321/)

## Building
Movecraft uses multiple versions of the Spigot server software for legacy support. As such, you need to run [BuildTools](https://www.spigotmc.org/wiki/buildtools/) for several versions before building the plugin.

```
java -jar BuildTools.jar --rev 1.10
java -jar BuildTools.jar --rev 1.11
java -jar BuildTools.jar --rev 1.12 
```
Download this repository as a zip file, then unzip the "movecraft-master".
In the "movecraft-master"-directory, locate the "libs" folder and add CraftBukkit 1.10 to 1.12 to it. Otherwise, Maven will be unable to be built.
Be sure to have Git Bash and Maven installed. Right-click in the "movecraft-master" directory, choose Git Bash here to open Git bash.
Then, run the following to build Movecraft through `maven`.
```
mvn clean instal
```
Jars are located in `/target`.

## Support
[Github issues](https://github.com/apdevteam/movecraft/issues)

The plugin is released here under the GNU General Public License V3. 
