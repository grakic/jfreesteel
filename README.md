JFreesteel
==========

JFreesteel is a reusable open-source Java library for reading public data from
the Serbian eID card. It is built on top of javax.smartcardio and released under
the GNU LGPLv3 license.

[![Build Status](https://travis-ci.org/grakic/jfreesteel.svg?branch=master)](https://travis-ci.org/grakic/jfreesteel)

Using this library your Java desktop applications and general web applications
running in a browser (via "invisible" Java applet) can access the smartcard
reader to get data such as personal number, full name, place of residence or
date of birth. It is also possible to read personal photo from the eID card.

It is built on top of native Java smart card interface and does not require
installation of the middleware software or other libraries. Underlay, Java is
using system PC/SC interface (bundled with MS Windows and Apple Mac OS X, and
pcsc-lite is available on GNU/Linux)


Java API
--------

To start using the library API, call methods readEidInfo() and readEidPhoto()
on the EidCard instance. There are two options to get EidCard instance.

There is a low-level API where you get an instance from a card that is currently
inserted in a smartcard reader (terminal) with EidCard.fromCard([Card]) factory
method. A sample code [sample/JFreesteel.java] is availabile in the jfreesteel
module. When using the low-level API, you would need to handle card insertions
and removals.

It is better to use a high-level API where you just have to implement a listener
following the ReaderListener interface, adding your code to methods inserted()
and removed(), then subscribe this listener to a Reader object wrapping a
[smartcard reader][CardTerminal]. Your listener will be notified whenever the
eID card is inserted or removed from a smart card reader, receiving a new
EidCard instance on insert.

For more API details, you may find these slides useful (in Serbian):
[Čitanje elektronske lične karte u Javi][Slides].
> **Note:** After the latest API update, EidCard is an abstract class. If you do
> not use a Reader object, to get an instance call EidCard.fromCard(Card).

[Code Review] wiki page has some more info in Serbian.


[sample/JFreesteel.java]: https://github.com/grakic/jfreesteel/blob/master/jfreesteel/src/main/java/net/devbase/jfreesteel/sample/JFreesteel.java
[Slides]: https://speakerdeck.com/u/grakic/p/jfreesteel-citanje-elektronske-licne-karte-u-javi
[Code Review]: https://github.com/grakic/jfreesteel/wiki/CodeReview
[Card]: http://docs.oracle.com/javase/7/docs/jre/api/security/smartcardio/spec/javax/smartcardio/Card.html
[CardTerminal]: http://docs.oracle.com/javase/7/docs/jre/api/security/smartcardio/spec/javax/smartcardio/CardTerminal.html


### Maven Repository

    <repositories>
        <repository>
            <id>jfreesteel-repository</id>
            <name>JFreesteel Maven Repository</name>
            <url>http://jfreesteel.devbase.net/maven/</url>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>
    <dependencies>
        <dependency>
            <groupId>net.devbase.jfreesteel</groupId>
            <artifactId>jfreesteel</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>



WebExtension (Native Messaging)
-------------------------------

Proof-of-concept WebExtension implementation (tested in Google Chrome) is available
in the **eidnativemessaging module**. The code is given as-is and more work is required
to package and distribute Java host application and Google Chrome extension. Sample
web page is provided that is using this extension to read public data from the smartcard.

  * [Čitanje lične karte bez Java apleta](http://blog.goranrakic.com/2016/12/citanje-licne-karte-bez-java-apleta.html) (in Serbian)


Applet
------

The **eidapplet module** contains an "invisible" Java applet providing a
JavaScript interface that can be used in web applications to read smartcard data
from a web browser.


Application
------

For non-developers, in the **eidviewer module** there is a full-featured GUI
viewer built as a Java Swing application using the JFreesteel library. The
viewer is released under the GNU Affero GPLv3 license and has the iText library
as a dependency.

To download the current viewer release for Windows, GNU/Linux
or Mac OS X please visit http://jfreesteel.devbase.net


Resources
---------

  * Get the source code from the Git repository at 
    [Github](https://github.com/grakic/jfreesteel) (prefered) or 
    [Gitorious](https://gitorious.org/freesteel)

  * Check out the library [web page](http://jfreesteel.devbase.net) (in Serbian)

  * Browse the project's [wiki page](https://github.com/grakic/jfreesteel/wiki)
    
  * Send your patches and comments to Goran Rakic &lt;grakic@devbase.net&gt; or
    create a merge/pull request on Github or Gitorious

