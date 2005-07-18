#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "Interval";
my( $aName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   author => "Mark Taylor",
);

print "public class $cName extends Region {\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Create a $cName",
   descrip => FuncDescrip( $cName ),
   params => [
      {
         name => ( $aName = "frame" ),
         type => 'Frame',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "lbnd" ),
         type => 'double[]',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "ubnd" ),
         type => 'double[]',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "unc" ),
         type => 'Region',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

print "}\n";

