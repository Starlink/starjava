#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "DSBSpecFrame";
my( $aName );
my( $fName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   author => "Mark Taylor (Starlink)",
);

print "public class DSBSpecFrame extends SpecFrame {\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Creates a DSBSpecFrame",
   descrip => "",
   params => [],
);

my( @alignSideBandArgs ) = (
   name => ( $aName = "alignSideBand" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @alignSideBandArgs );
makeSetAttrib( @alignSideBandArgs );

my( @dsbcentreArgs ) = (
   name => ( $aName = "dsbCentre" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @dsbcentreArgs );
makeSetAttrib( @dsbcentreArgs );

my( @ifArgs ) = (
   name => ( $aName = "if" ),
   varname => "ifreq",
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @ifArgs );
makeSetAttrib( @ifArgs );

my( @sidebandArgs ) = (
   name => ( $aName = "sideBand" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @sidebandArgs );
makeSetAttrib( @sidebandArgs );

print "}\n";

