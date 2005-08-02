#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "StcSearchLocation";

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   author => "Mark Taylor",
);

print "public class $cName extends Stc {\n";

print <<__EOT__;

   /**
    * Constructs a new $cName.
    *
    * \@param   region  the encapsulated region
    * \@param   coords  the AstroCoords elements associated with this Stc
    */
   public $cName( Region region, AstroCoords\[\] coords ) {
       construct( region, astroCoordsToKeyMaps( coords ) );
   }
   private native void construct( Region region, KeyMap\[\] coordMaps );
__EOT__

print "}\n";

