#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "Frame";

my( $fName );
my( $aName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   version => '$Id$',
   author => "Mark Taylor (Starlink)",
);

print "public class Frame extends Mapping {\n";

makeNativeConstructor(
   Name => ( $cName ),
   purpose => "Creates a new $cName",
   descrip => "",
   params => [
      {
         type => 'int', name => ( $aName = "naxes" ),
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);


print <<'__EOT__';
    /**
     * Dummy constructor.  This constructor does not create a valid
     * Frame object, but is required for inheritance by Frame's
     * subclasses.
     */
    protected Frame() {
    }

__EOT__


makeNativeMethod(
   name => ( $fName = "angle" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'double', descrip => ReturnDescrip( $fName ) },
   params => [
      { 
         type => 'double[]', name => ( $aName = "a" ), 
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         type => 'double[]', name => ( $aName = "b" ),
         descrip => ArgDescrip( $fName, $aName, ),
      },
      {
         type => 'double[]', name => ( $aName = "c" ),
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "axAngle" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'double', descrip => ReturnDescrip( $fName ) },
   params => [
      {
         name => ( $aName = "a" ), 
         type => 'double[]', 
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "b" ),
         type => 'double[]', 
         descrip => ArgDescrip( $fName, $aName, ),
      },
      {
         name => ( $aName = "axis" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName, ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "axDistance" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'double', descrip => ReturnDescrip( $fName ) },
   params => [
      {
         name => ( $aName = "axis" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "v1" ),
         type => 'double',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "v2" ),
         type => 'double',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "intersect" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'double[]', descrip => ArgDescrip( $fName, "cross" ) },
   params => [
      {
         name => ( $aName = "a1" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "a2" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "b1" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "b2" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "axOffset" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'double', descrip => ReturnDescrip( $fName ) },
   params => [
      {
         name => ( $aName = "axis" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "v1" ),
         type => 'double',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "dist" ),
         type => 'double',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

         



makeNativeMethod(
   name => ( $fName = "convert" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'FrameSet', descrip => ReturnDescrip( $fName ) },
   params => [ 
      {
         type => 'Frame', name => ( $aName = "to" ), 
         descrip => ArgDescrip( $fName, $aName ),
      }, 
      { 
         type => 'String', name => ( $aName = "domainlist" ),
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "distance" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'double', descrip => ReturnDescrip( $fName ) },
   params => [ 
      { 
         type => 'double[]', name =>  "point1",
         descrip => ArgDescrip( $fName, $aName ),
      }, 
      { 
         type => 'double[]', name =>  "point2" ,
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "findFrame" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'FrameSet', descrip => ReturnDescrip( $fName ) },
   params => [ 
      {
         type => 'Frame', name => ( $aName = "template" ),
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         type => 'String', name => ( $aName = "domainlist" ),
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "format" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'String', descrip => ReturnDescrip( $fName ) },
   params => [
      { 
         type => 'int', name => ( $aName = "axis" ),
         descrip => ArgDescrip( $fName, $aName ),
      }, 
      {
         type => 'double', name => ( $aName = "value" ),
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "getActiveUnit" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'boolean', descrip => ReturnDescrip( $fName ), },
   params => [],
);

makeNativeMethod(
   name => ( $fName = "norm" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void' },
   params => [ 
      { 
         type => 'double[]', name => ( $aName = "value" ),
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod( 
   name => ( $fName = "offset" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'double[]', descrip => ArgDescrip( $fName, "point3" ), },
   params => [ 
      { 
         type => 'double[]', name => ( $aName = "point1" ),
         descrip => ArgDescrip( $fName, $aName ),
      }, 
      {
         type => 'double[]', name => ( $aName = "point2" ),
         descrip => ArgDescrip( $fName, $aName ),
      }, 
      {
         type => 'double', name => ( $aName = "offset" ),
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "offset2" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'double', descrip => ReturnDescrip( $fName ), },
   params => [ 
      {
         type => 'double[]', name => ( $aName = "point1" ),
         descrip => ArgDescrip( $fName, $aName ),
      }, 
      {
         type => 'double', name => ( $aName = "angle" ),
         descrip => ArgDescrip( $fName, $aName ),
      }, 
      {
         type => 'double', name => ( $aName = "offset" ),
         descrip => ArgDescrip( $fName, $aName ),
      }, 
      {
         type => 'double[]', name => ( $aName = "point2" ),
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "permAxes" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void' },
   params => [ 
      { 
         type => 'int[]', name => ( $aName = "perm" ),
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "pickAxes" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => "Frame", descrip => ReturnDescrip( $fName ), },
   params => [ 
      {
         type => 'int', name => ( $aName = "naxes" ),
         descrip => ArgDescrip( $fName, $aName ),
      },
      { 
         type => 'int[]', name => ( $aName = "axes" ),
         descrip => ArgDescrip( $fName, $aName ),
      },
      { 
         type => 'Mapping[]', name => ( $aName = "map" ),
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "resolve" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { 
      type=> 'double[]',
      descrip => "a two element array in which to return the results.  " .
                 "The first element is " . ArgDescrip( $fName, "d1" ) . 
                 "<p>The second element is " . ArgDescrip( $fName, "d2" )
   },
   params => [
      {
         name => ( $aName = "point1" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "point2" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "point3" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "point4" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);
   
makeNativeMethod(
   name => ( $fName = "setActiveUnit" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void' },
   params => [
      {
         type => 'boolean', name => ( $aName = "value" ),
         descrip => ArgDescrip( $fName, $aName ),
      }
   ],
);

makeNativeMethod(
   name => ( $fName = "unformat" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'double', descrip => ArgDescrip( $fName, "value" ) },
   params => [ 
      {
         type => 'int', name => ( $aName = "axis" ),
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         type => 'String', name => ( $aName = "string" ),
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);


my( @args );

@args = (
   name => ( $aName = "alignSystem" ),
   type => "String",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "bottom" ),
   type => "double",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );

@args = (
   name => ( $aName = "digits" ),
   type => "int",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );

@args = (
   name => ( $aName = "direction" ),
   type => "boolean",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );

@args = (
   name => ( $aName = "domain" ),
   type => "String",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "dut1" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "epoch" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
   stringtoo => 1,
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "format" ),
   type => "String",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );

@args = (
   name => ( $aName = "label" ),
   type => "String",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );

@args = (
   name => ( $aName = "matchEnd" ),
   type => "boolean",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "maxAxes" ),
   type => "int",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "minAxes" ),
   type => "int",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "naxes" ),
   type => "int",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );

@args = (
   name => ( $aName = "obsLat" ),
   type => "String",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "obsLon" ),
   type => "String",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "permute" ),
   type => "boolean",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "preserveAxes" ),
   type => "boolean",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "symbol" ),
   type => "String",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );

@args = (
   name => ( $aName = "system" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "title" ),
   type => "String",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = ( 
   name => ( $aName = "top" ),
   type => "double",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );

@args = (
   name => ( $aName = "unit" ),
   type => "String",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttribByAxis( @args );
makeSetAttribByAxis( @args );

@args = (
   name => ( $aName = "normUnit" ),
   type => "String",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttribByAxis( @args );

print "}\n";
