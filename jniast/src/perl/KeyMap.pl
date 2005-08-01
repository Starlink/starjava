#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "KeyMap";
my( $aName );
my( $fName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   author => "Mark Taylor (Starlink)",
   extra => q{
       In Java, you are probably better off using a {@link java.util.Map}.
   },
);

print "public class KeyMap extends AstObject {\n";

print <<'__EOT__';

    /** Type constant representing <tt>int</tt> type. */
    public static final int AST__INTTYPE = 
            getAstConstantI( "AST__INTTYPE" );

    /** Type constant representing <tt>double</tt> type. */
    public static final int AST__DOUBLETYPE =
            getAstConstantI( "AST__DOUBLETYPE" );

    /** Type constant representing <tt>String</tt> type. */
    public static final int AST__STRINGTYPE =
            getAstConstantI( "AST__STRINGTYPE" );

    /** Type constant representing <tt>AstObject</tt> type. */
    public static final int AST__OBJECTTYPE =
            getAstConstantI( "AST__OBJECTTYPE" );

    /** Type constant represening no known type. */
    public static final int AST__BADTYPE =
            getAstConstantI( "AST__BADTYPE" );
__EOT__

makeNativeConstructor(
   Name => $cName,
   purpose => "Create a $cName",
   descrip => "",
   params => [],
);


makeNativeMethod(
   name => ( $fName = "mapRemove" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'void' },
);

makeNativeMethod(
   name => ( $fName = "mapSize" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [],
   return => { type => 'int', descrip => ReturnDescrip( $fName ), },
);

makeNativeMethod(
   name => ( $fName = "mapLength" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'int', descrip => ReturnDescrip( $fName ), },
);

makeNativeMethod(
   name => ( $fName = "mapHasKey" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { 
      type => 'boolean',
      descrip => q{
         true iff this map contains an entry for <tt>key</tt>
      },
   },
);

makeNativeMethod(
   name => ( $fName = "mapKey" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [
      {
         name => ( $aName = "index" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => {
      type => 'String',
      descrip => ReturnDescrip( $fName ),
   },
);

makeNativeMethod(
   name => ( $fName = "mapType" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => {
      type => 'int',
      descrip => ReturnDescrip( $fName ),
   },
);

$fName = "mapPut0<X>";
makeNativeMethod(
   name => "mapPut0D",
   purpose => "Store a double value",
   descrip => "",
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "value" ),
         type => 'double',
         descrip => "value to store"
      },
      {
         name => ( $aName = "comment" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'void' },
);

makeNativeMethod(
   name => "mapPut0I",
   purpose => "Store an integer value",
   descrip => "",
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "value" ),
         type => 'int',
         descrip => "value to store"
      },
      {
         name => ( $aName = "comment" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'void' },
);

makeNativeMethod(
   name => "mapPut0C",
   purpose => "Store a string value",
   descrip => "",
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "value" ),
         type => 'String',
         descrip => "value to store",
      },
      {
         name => ( $aName = "comment" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'void' },
);

makeNativeMethod(
   name => "mapPut0A",
   purpose => "Store an AstObject",
   descrip => "",
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "value" ),
         type => 'AstObject',
         descrip => "value to store",
      },
      {
         name => ( $aName = "comment" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'void' },
);


$fName = "mapGet0<X>";
makeNativeMethod(
   name => "mapGet0D",
   purpose => "Retrieve a double value",
   descrip => "",
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { 
      type => 'Double',
      descrip => q{
         object holding value stored in this map under <tt>key</tt>,
         or <tt>null</tt> if <tt>key</tt> was not present
      },
   },
);

makeNativeMethod(
   name => "mapGet0I",
   purpose => "Retrieve an integer value",
   descrip => "",
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { 
      type => 'Integer',
      descrip => q{
         object holding value stored in this map under <tt>key</tt>,
         or <tt>null</tt> if <tt>key</tt> was not present
      },
   },
);

makeNativeMethod(
   name => "mapGet0C",
   purpose => "Retrieve a string value",
   descrip => "",
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { 
      type => 'String',
      descrip => q{
         object holding value stored in this map under <tt>key</tt>,
         or <tt>null</tt> if <tt>key</tt> was not present
      },
   },
);

makeNativeMethod(
   name => "mapGet0A",
   purpose => "Retrieve an AstObject",
   descrip => "",
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { 
      type => 'AstObject',
      descrip => q{
         object holding value stored in this map under <tt>key</tt>,
         or <tt>null</tt> if <tt>key</tt> was not present
      },
   },
);

$fName = "mapPut1<X>";

makeNativeMethod(
   name => "mapPut1D",
   purpose => "Store a double array",
   descrip => "",
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "value" ),
         type => 'double[]',
         descrip => "array to store",
      },
      {
         name => ( $aName = "comment" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'void' },
);

makeNativeMethod(
   name => "mapPut1I",
   purpose => "Store an integer array",
   descrip => "",
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "value" ),
         type => 'int[]',
         descrip => "array to store",
      },
      {
         name => ( $aName = "comment" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'void' },
);

makeNativeMethod(
   name => "mapPut1C",
   purpose => "Store a string array",
   descrip => "",
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "value" ),
         type => 'String[]',
         descrip => "array to store",
      },
      {
         name => ( $aName = "comment" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'void' },
);

makeNativeMethod(
   name => "mapPut1A",
   purpose => "Store an AstObject array",
   descrip => "",
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "value" ),
         type => 'AstObject[]',
         descrip => "array to store",
      },
      {
         name => ( $aName = "comment" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'void' },
);

$fName = "mapGet1<X>";

makeNativeMethod(
   name => "mapGet1D",
   purpose => "Retrieve a double array",
   descrip => "",
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { 
      type => 'double[]',
      descrip => q{
         value stored in this map under <code>key</code> 
         as a double array, or null
      },
   },
);

makeNativeMethod(
   name => "mapGet1I",
   purpose => "Retrieve an integer array",
   descrip => "",
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => {
      type => 'int[]',
      descrip => q{
         value stored in this map under <code>key</code> 
         as an integer array, or null
      },
   },
);

makeNativeMethod(
   name => "mapGet1C",
   purpose => "Retrieve a string array",
   descrip => "",
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "sleng" ),
         type => 'int',
         descrip => q{
            maximum length of any of the strings in the returned array;
            any longer strings will be truncated
         },
      }
   ],
   return => {
      type => 'String[]',
      descrip => q{
         value stored in this map under <code>key</code> 
         as a String array, or null
      },
   },
);

makeNativeMethod(
   name => "mapGet1A",
   purpose => "Retrieve an array of AstObjects",
   descrip => "",
   params => [
      {
         name => ( $aName = "key" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => {
      type => 'AstObject[]',
      descrip => q{
         value stored in this map under <code>key</code> 
         as an AstObject array, or null
      },
   },
);

print "}\n";
