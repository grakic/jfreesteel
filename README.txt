JFreesteel README
=================

JFreesteel is a reusable open source Java library for reading public data from
the Serbian eID card, build on top of javax.smartcardio and released under the
GNU LGPLv3 license.

Using this library it is possible to access data such as personal number, full
name, place of residence or date of birth. It is also possible to read personal
photo from the eID card.

It is built on top of native Java smart card interface and does not require
installation of the middleware or other libraries. Underlay, Java is using
system PC/SC interface (bundled with MS Windows and Apple Mac OS X, pcsc-lite
available on GNU/Linux)

In the app module there is a complete GUI viewer, Java Swing application using
the JFreesteel library.

Get the source code from the Git repository at http://gitorious.org/freesteel
Send your patches and comments to Goran Rakic <grakic@devbase.net>

