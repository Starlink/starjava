#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "TimeFrame";
my( $aName );
my( $fName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   author => "Mark Taylor",
);

print "public class $cName extends Frame {\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Create a $cName",
   descrip => FuncDescrip( $cName ),
   params => [],
);

makeNativeMethod(
   name => ( $fName = "currentTime" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'double', descrip => ReturnDescrip( $fName ), },
   params => [],
);

my( @args );

@args = (
   name => ( $aName = "alignTimeScale" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "clockLat" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "clockLon" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "timeOrigin" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   stringtoo => 1,
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "timeScale" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

print "}\n";
