Movecraft
======
![Java CI](https://github.com/APDevTeam/Movecraft/workflows/Java%20CI/badge.svg?branch=main)

This is a maintained fork of Movecraft, which aims to add legacy version support as well as performance fixes.

**Movecraft requires at least Java 17**

## Download

Releases can be found on the [releases tab](https://github.com/APDevTeam/Movecraft/releases).

Previous builds between v5.0 (for 1.9) and v7 (for 1.12), are located on the [Spigot forums](https://www.spigotmc.org/resources/movecraft.31321/).

Legacy builds as old as v0.7.1 (for 1.0.0) can be found on the original [Bukkit page](https://dev.bukkit.org/projects/movecraft).

Development builds can be found under the [actions tab](https://github.com/APDevTeam/Movecraft/actions?query=workflow%3A%22Java+CI%22).  Use at your own risk!

## Support
Please check the [Wiki](https://github.com/APDevTeam/Movecraft/wiki) and [FAQ](https://github.com/APDevTeam/Movecraft/wiki/Frequently-Asked-Questions) pages before asking for help!

[Github Issues](https://github.com/apdevteam/movecraft/issues)

[Discord](http://bit.ly/JoinAP-Dev)

## Development Environment
Movecraft uses multiple versions of the Spigot server software for legacy support.  As such, you need to run [BuildTools](https://www.spigotmc.org/wiki/buildtools/) for several versions before building the plugin.  It doesn't matter where you do this, but inside the Movecraft directory is probably a bad place.  We recommend building Spigot 1.18.2 with Java 17, and 1.20.6 with Java 21.

```
java -jar BuildTools.jar --rev 1.18.2 --remapped
java -jar BuildTools.jar --rev 1.20.6 --remapped
```

Once you have compiled CraftBukkit, it should continue to exist in your local maven repository, and thus you should need to compile each version at most one time. Once complete, run the following to build Movecraft through `maven`.
```
mvn -T 1C clean install
```
Compiled jars can be found in the `/target` directory.

#### Movecraft is released under the GNU General Public License V3. 
