#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "WinMap";
my( $aName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   version => '$Id$',
   author => "Mark Taylor (Starlink)",
);

print "public class WinMap extends Mapping {\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Creates a $cName.",
   descrip => "",
   params => [
      {
         name => ( $aName = "ncoord" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "ina" ),
         type => 'double[]',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "inb" ),
         type => 'double[]',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "outa" ),
         type => 'double[]',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "outb" ),
         type => 'double[]',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

print "}\n";


