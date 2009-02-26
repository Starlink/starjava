#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "SwitchMap";
my( $aName );
my( $fName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
);

print "public class SwitchMap extends Mapping {\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Creates a $cName",
   descrip => "",
   params => [
      {
         name => ( $aName = "fsmap" ),
         type => 'Mapping',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "ismap" ),
         type => 'Mapping',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "routemaps" ),
         type => 'Mapping[]',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

print "}\n";

