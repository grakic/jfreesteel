HOW TO COMPILE THE APPLET

The applet in this repository uses the classes from the
netscape.javascript.* java package.  This package, in turn, is
distributed with Sun's JDK within the jar plugin.jar, which is located
in the directory $JDK_HOME/jre/lib/plugin.jar, where $JDK_HOME is
whatever home directory is the root directory of the JDK installation.

Unfortunately, there is no Maven repository that contains this jar, so
there is no automated way to pull it into the compilation.  So we need
to install it manually.

We added a dummy dependency into the pom.xml file for the applet, as
follows:

<dependency>
    <groupId>local-java</groupId>
    <artifactId>local-java-plugin</artifactId>
    <version>0.0.0</version>
    <scope>provided</scope>
</dependency>

This dummy dependency won't resolve to anything unless we install the
said jar manually from the command line.  Here's how to do it.

The following example is of a command line used to install the missing
plugin.jar dependency on an Ubuntu Natty 11.04 machine, that has Sun's
JDK6 installed, version 1.6.0.26:

mvn install:install-file \
  -DgroupId=local-java \
  -DartifactId=local-java-plugin \
  -Dversion=0.0.0 \
  -Dpackaging=jar \
  -Dfile=/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/plugin.jar

This will copy the plugin.jar file into your local repository.  Note
that you need to figure out the correct path that should be inserted into
the -Dfile=... command line flag instead of the sample path
/usr/lib/jvm/java-6-sun-1.6.0.26/jre/lib/plugin.jar seen above.

After installing the plugin.jar, you can proceed with the compilation.



MIXED CODE WARNING

You may have to fiddle with manifest attributes to avoid the mixed code
warning. See http://stackoverflow.com/a/19451594


