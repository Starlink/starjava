#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "CmpMap";
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

print "public class CmpMap extends Mapping {\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Create a CmpMap",
   descrip => FuncDescrip( $cName ),
   params => [
      {
         name => ( $aName = "map1" ),
         type => 'Mapping',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "map2" ),
         type => 'Mapping',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "series" ),
         type => 'boolean',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

print "}\n";

