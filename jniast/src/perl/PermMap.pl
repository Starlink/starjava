#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "PermMap";
my( $aName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   version => '$Id$',
   author => "Mark Taylor (Starlink)",
);

print "public class PermMap extends Mapping {\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Creates a $cName.",
   descrip => "",
   params => [
      {
         name => ( $aName = "nin" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "inperm" ),
         type => 'int[]',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "nout" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "outperm" ),
         type => 'int[]',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "constant" ),
         type => 'double[]',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

print "}\n";
