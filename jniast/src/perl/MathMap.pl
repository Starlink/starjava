#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "MathMap";
my( $aName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   version => '$Id$',
   author => "Mark Taylor (Starlink)",
);

print "public class MathMap extends Mapping {\n";

makeNativeConstructor( 
   Name => $cName,
   purpose => "Create a $cName.",
   descrip => FuncDescrip( $cName ),
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
         name => ( $aName = "fwd" ),
         type => 'String[]',
         descrip => q{
            an array of Strings describing the forward transformations. 
            There should be at least <code>nout</code> of these, but
            there may be more 
            (see the note on "Calculating intermediate values").
         },
      },
      {
         name => ( $aName = "inv" ),
         type => 'String[]',
         descrip => q{
            an array of Strings describing the inverse transformations.
            There should be at least <code>nin</code> of these, but
            there may be more
            (see the note on "Calculating intermediate values").
         },
      },
   ],
);

my( @args );

@args = (
   name => ( $aName = "seed" ),
   type => 'int',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );
 
@args = (
   name => ( $aName = "simpFI" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "simpIF" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

print "}\n";

