Movecraft - TTE Edition
======

Maintained former fork of Movecraft.
Features long overdue overhauls and rewrites of multiple systems (e.g. Signs, Crafttypes) as well as extended API for developers (e.g. CraftInitiateTranslateEvent).

Focus lies on the demands of the TTE server.

You are free to use this version of Movecraft as long as you give credit to us.
However, there is no guarantee given that this 100% fits your needs and demands.

**Movecraft requires at least Java 17**

## Download

Releases can be found on the [releases tab](https://github.com/TTE-DevTeam/Movecraft/releases).

Development builds aren't provided, if you want one, compile it yourself using the instructions below.  Use at your own risk!

## Support
As of now, the links below relay to the old APDev wiki. We are currently more or less busy writing our own. Most stuff there should be applicable to this edition though.
Crafttypes however are not compatible as we broke away from the old system!

Please check the [Wiki](https://github.com/APDevTeam/Movecraft/wiki) and [FAQ](https://github.com/APDevTeam/Movecraft/wiki/Frequently-Asked-Questions) pages before asking for help!

[Github Issues](https://github.com/TTE-DevTeam/movecraft/issues)

## Development Environment
Building Movecraft is as easy as downloading the source code and executing the following command:
```
./gradlew clean shadowJar --parallel
```
Compiled jars can be found in the `Movecraft/build/libs` directory.

#### Movecraft is released under the GNU General Public License V3. 
