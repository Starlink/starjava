#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "ShiftMap";
my( $aName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   author => "Mark Taylor (Starlink)",
);

print "public class ShiftMap extends Mapping {\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Creates a $cName.",
   descrip => q{
      The number of input coordinates is equal to the number of output 
      coordinates, and is equal to the number of elements in the 
      supplied <code>shift</code> array.
   },
   params => [
      {
         name => ( $aName = "shift" ),
         type => 'double[]',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

print "}\n";

