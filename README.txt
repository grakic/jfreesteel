JFreesteel README
=================

JFreesteel is a reusable open source Java library for reading public data from
the Serbian eID card, build on top of javax.smartcardio and released under the
GNU LGPLv3 license.

With this library it is possible to read data such as personal number, full
name, place of residence or date of birth. It is also possible to read personal
photo from the eID card.

It is built on top of native Java smart card interface and does not require
installation of the middleware software or other libraries. Underlay, Java is
using system PC/SC interface (bundled with MS Windows and Apple Mac OS X,
and pcsc-lite available on GNU/Linux)

A simple sample code for using the library is included in sample/JFreesteel.java
file inside the jfreesteel module.

The eidapplet module contains a Java applet providing JavaScript interface that
can be used in web applications to read smartcard data from a web browser.

For non-developers, in the eidviewer module there is a full-featured GUI viewer
built as a Java Swing application using the JFreesteel library. The viewer is
released under the GNU Affero GPLv3 license and has the iText library as a
dependency. To download the current viewer release for Windows, GNU/Linux or
Mac OS visit http://devbase.net/jfreesteel/


Resources
---------

  * Get the source code from the Git repository at Gitorious or Github
    https://gitorious.org/freesteel
    https://github.com/grakic/jfreesteel

  * Check out the library web page at http://devbase.net/jfreesteel (in Serbian)

  * Project's wiki page:
    https://gitorious.org/freesteel/pages/Home or
    https://github.com/grakic/jfreesteel/wiki

  * Send your patches and comments to Goran Rakic <grakic@devbase.net> or use
    merge/pull request on Gitorious or Github

