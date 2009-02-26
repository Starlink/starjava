#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "SelectorMap";
my( $aName );
my( $fName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   author => "Mark Taylor",
);

print "public class $cName extends Mapping {\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Creates a $cName",
   descrip => "",
   params => [
      {
         name => ( $aName = "regs" ),
         type => 'Region[]',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "badval" ),
         type => 'double',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

print "}\n";
