#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "SkyFrame";
my( $aName );
my( $fName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   version => '$Id$',
   author => "Mark Taylor (Starlink)",
);

print "public class SkyFrame extends Frame {\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Create a $cName",
   descrip => "",
   params => [],
);

my( @args );

@args = (
   name => ( $aName = "asTime" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );

@args = (
   name => ( $aName = "equinox" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "latAxis" ),
   type => 'int',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );

@args = (
   name => ( $aName = "lonAxis" ),
   type => 'int',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );

@args = (
   name => ( $aName = "negLon" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "projection" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "alignOffset" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "skyRefIs" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "skyRef" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );
@args = (
   name => ( $aName = "skyRef" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "skyRefP" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );
@args = (
   name => ( $aName = "skyRefP" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );


print "}\n";

