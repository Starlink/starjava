#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "SlaMap";
my( $aName );
my( $fName );
my( $seeSun67 ) = '@see <a href="http://star-www.rl.ac.uk/star/'
                . 'docs/sun67.htx/sun67.html">SUN/67 - SLALIB</a>';

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader( 
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   version => '$Id$',
   author => "Mark Taylor (Starlink)",
   extra => $seeSun67,
);

print "public class SlaMap extends Mapping {\n";

print <<'__EOT__';

   /**
    * Creates a default SlaMap.
    */
   public SlaMap() {
      construct( 0 );
   }

__EOT__

makeNativeConstructor(
   Name => $cName,
   purpose => "Creates a $cName.",
   descrip => "",
   params => [
      {
         name => ( $aName = "flags" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

$fName = "slaAdd";
makeNativeMethod(
   name => ( "add" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void', },
   extra => $seeSun67,
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

