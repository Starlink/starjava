#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "PolyMap";
my( $aName );
my( $fName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   author => "Mark Taylor (Starlink)",
);

print "public class PolyMap extends Mapping {\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Creates a $cName",
   descrip => "",
   params => [
      {
         name => ( $aName = "nin" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "nout" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "ncoeff_f" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "coeff_f" ),
         type => 'double[]',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "ncoeff_i" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "coeff_i" ),
         type => 'double[]',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

print "}\n";

