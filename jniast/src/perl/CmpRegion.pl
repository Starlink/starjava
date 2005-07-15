#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "CmpRegion";
my( $aName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader( 
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   author => "Mark Taylor",
);

print "public class $cName extends Region {\n";

print <<'__EOT__';

    /** Constant indicating AND-type region combination. */
    public static final int AST__AND = getAstConstantI( "AST__AND" );

    /** Constant indicating OR-type region combination. */
    public static final int AST__OR = getAstConstantI( "AST__OR" );
__EOT__

makeNativeConstructor(
   Name => $cName,
   purpose => "Create a $cName",
   descrip => FuncDescrip( $cName ),
   params => [
      {
         name => ( $aName = "region1" ),
         type => 'Region',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "region2" ),
         type => 'Region',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "oper" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

print "}\n";

