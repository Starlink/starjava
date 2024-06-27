# Starjava

The starjava package consists of some Java-based applications and
libraries including
[TOPCAT](http://www.starlink.ac.uk/topcat/),
[STILTS](http://www.starlink.ac.uk/stilts/),
[SPLAT](http://www.starlink.ac.uk/splat/),
their dependencies, and a few other things, some of them obsolete.
These packages were originally developed within the UK's
[Starlink project](https://en.wikipedia.org/wiki/Starlink_Project) (1980-2005),
hence the name.

## Building from source

### Background

Most people do not need to build starjava.
If you just want to use the code, you can find pre-built platform-neutral
jar files elsewhere, e.g. the
[TOPCAT](http://www.starlink.ac.uk/topcat/),
[STILTS](http://www.starlink.ac.uk/stilts/),
[STIL](http://www.starlink.ac.uk/stil/) or
[SPLAT](http://www.starlink.ac.uk/splat/)
web pages.

### Requisites

Most of the packages can be built from source using just a
Java Development Kit (JDK).
Note that you need a JDK not just a JRE (Java Runtime Environment),
i.e. there must be a `javac` command as well as `java`.
A JDK of Java 8 (a.k.a. 1.8) or later is required.
The build has been tested using the Oracle
JDK LTS releases Java 8, Java 11, Java 17 and Java 21, but any JDK
between 8 and 21 (and perhaps later) is expected to work.

In order to build SPLAT and certain other packages,
Java Advanced Imaging (JAI) must be installed in the JDK.
JAI can be obtained from
https://www.oracle.com/java/technologies/advanced-imaging-api.html.
But if you only need TOPCAT, this is not necessary.

### Instructions

Here is how to do the build.  A `bash`-like shell is assumed.

Clone the git repository into `~/starjava/source` (or similar):
```
sj=~/starjava  # or whatever
mkdir -p $sj
cd $sj
git clone git@github.com:Starlink/starjava.git source
# or git clone https://github.com/Starlink/starjava.git source
cd source
```

Set the `STAR_JAVA` environment variable to the location of the `java`
binary in your JDK of choice.  Just setting it to `/usr/bin/java`
may do the trick.
```
export STAR_JAVA=/usr/lib/jvm/java-8-openjdk-amd64/bin/java  # or wherever
$STAR_JAVA -version
```

Build the jar files using the bundled `ant` binary, installing them into
directories under `$sj` like `lib/`, `bin/` and `docs/`.  This will take a few
minutes.
```
$sj/source/ant/bin/ant build install
```

You can then run e.g. `$sj/bin/topcat` or `java -jar $sj/lib/topcat/topcat.jar`.
The javadocs, and in some cases other documentation,
for each package can be found in subdirectories under `$sj/docs/`.

Some other targets are available as well (run `ant -projecthelp`
to see what they are).
You can run tests with the `test` target, or build a single javadocs page
that links all the library packages in `$sj/source/docs/javadocs/index.html`)
using the `javadocs-full` target.
For instance:
```
$sj/source/ant/bin/ant test
$sj/source/ant/bin/ant javadocs-full
```

Individual packages are in their own directories under `$sj/source`.
You can run ant from within these subdirectories
to perform the build in each package separately.
Each package has ant targets `build`, `install`, `test`, `deinstall`, `clean`.
Some packages have other targets too (see `ant -projecthelp`);
topcat and stilts have the `build-standalone` target to build the
monolithic jar files (containing all dependencies) `topcat-full.jar`
and `stilts.jar` respectively, for instance:
```
cd $sj/source/topcat
$sj/source/ant/bin/ant build-standalone install
$STAR_JAVA -jar $sj/lib/topcat/topcat-full.jar
```

## More information

For more details on the build system, including how to build
other components from source like the ant binary itself and the
native code required by some of the packages, see the
[README.starlink](README.starlink) file.


