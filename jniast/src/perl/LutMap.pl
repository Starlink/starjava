#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "LutMap";
my( $aName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   version => '$Id$',
   author => "Mark Taylor (Starlink)",
);

print "public class LutMap extends Mapping {\n";

makeNativeConstructor( 
   Name => $cName,
   purpose => "Create a $cName.",
   descrip => "",
   params => [
      {
         name => ( $aName = "lut" ),
         type => 'double[]',
         descrip => q{
            An array containing the lookup table entries.  There must be
            at least two elements.
         },
      },
      {
         name => ( $aName = "start" ),
         type => 'double',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "inc" ),
         type => 'double',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

print "}\n";


