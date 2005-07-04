#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "TimeMap";
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

print <<__EOT__;
    /**
     * Create a TimeMap.
     */
    public TimeMap() {
        this( 0 );
    }

__EOT__

makeNativeConstructor(
   Name => $cName,
   purpose => "Create a $cName",
   descrip => FuncDescrip( $cName ),
   params => [
      {
         name => ( $aName = "flags" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "timeAdd" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void' },
   params => [
      {
         name => ( $aName = "cvt" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "args" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

print "}\n";

