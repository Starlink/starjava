#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "RateMap";
my( $aName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   author => "Mark Taylor (Starlink)",
);

print "public class RateMap extends Mapping {\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Create a $cName",
   descrip => "",
   params => [
      {
         name => ( $aName = "map" ),
         type => 'Mapping',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "ax1" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "ax2" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

print "}\n";

