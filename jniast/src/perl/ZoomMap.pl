#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "ZoomMap";
my( $aName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   version => '$Id$',
   author => "Mark Taylor (Starlink)",
);

print "public class ZoomMap extends Mapping {\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Creates a $cName",
   descrip => "",
   params => [
      {
         name => ( $aName = "ncoord" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "zoom" ),
         type => 'double',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);


my( @args );

@args = (
   name => ( $aName = "zoom" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

print "}\n";

