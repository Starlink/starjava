#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "FrameSet";
my( $aName );
my( $fName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   version => '$Id$',
   author => "Mark Taylor (Starlink)",
);

print <<'__EOT__';
public class FrameSet extends Frame {

    /** Frame index of Base coordinate frame of FrameSet. */
    public static final int AST__BASE = getAstConstantI( "AST__BASE" );

    /** Frame index of Current coordinate frame of FrameSet. */
    public static final int AST__CURRENT = getAstConstantI( "AST__CURRENT" );

    /** Frame index which applies to no frame in the FrameSet. */
    public static final int AST__NOFRAME = getAstConstantI( "AST__NOFRAME" );

__EOT__

makeNativeConstructor(
   Name => $cName,
   purpose => "Creates a $cName.",
   descrip => "",
   params => [
      {
         name => ( $aName = "frame" ),
         type => 'Frame',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

print <<'__EOT__';
    /**
     * Dummy constructor.  This constructor does not create a valid
     * FrameSet object, but is required for inheritance by FrameSet's
     * subclasses.
     */
    protected FrameSet() {
    }

__EOT__

makeNativeMethod(
   name => ( $fName = "addFrame" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [
      { 
         name => ( $aName = "iframe" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      { 
         name => ( $aName = "map" ),
         type => 'Mapping',
         descrip => ArgDescrip( $fName, $aName ),
      },
      { 
         name => ( $aName = "frame" ),
         type => 'Frame',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'void' },
);

makeNativeMethod(
   name => ( $fName = "getFrame" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [
      { 
         name => ( $aName = "iframe" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'Frame', descrip => ReturnDescrip( $fName ), },
);
   
makeNativeMethod(
   name => ( $fName = "getMapping" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [
      { 
         name => ( $aName = 'iframe1' ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      { 
         name => ( $aName = 'iframe2' ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'Mapping', descrip => ReturnDescrip( $fName ), },
);

makeNativeMethod(
   name => ( $fName = "remapFrame" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [
      { 
         name => ( $aName = 'iframe' ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      { 
         name => ( $aName = 'map' ),
         type => 'Mapping',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'void' },
);

makeNativeMethod(
   name => ( $fName = "removeFrame" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [
      { 
         name => ( $aName = 'iframe' ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'void' },
);


my( @baseArgs ) = (
   name => ( $aName = "base" ),
   type => "int",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @baseArgs );
makeSetAttrib( @baseArgs );

my( @currentArgs ) = (
   name => ( $aName = "current" ),
   type => "int",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @currentArgs );
makeSetAttrib( @currentArgs );

my( @nframeArgs ) = (
   name => ( $aName = "nframe" ),
   type => "int",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @nframeArgs );

print "}\n";



