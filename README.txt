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

To start using the library API, call methods readEidInfo() and readEidPhoto() on
the EidCard object. There is a low-level API where you create a new EidCard
object directly from a card currently inserted in a smartcard reader (terminal).
A sample code is included in the sample/JFreesteel.java file inside the
jfreesteel module. You will need to handle card insertions and removals yourself
when using the low-level API.

It is better to use a high-level API where you have to implement a listener
following the ReaderListener interface with methods inserted() and removed() and
subscribe this listener to a Reader object wrapping a smartcard reader. Your
listener will be notified when the eID smart card is inserted or removed from a
smart card reader, with a new EidCard object given on insert.

For more details on the API, you may find these slides useful:
    https://speakerdeck.com/u/grakic/p/jfreesteel-citanje-elektronske-licne-karte-u-javi

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

