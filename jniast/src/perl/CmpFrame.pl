#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "CmpFrame";
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

print "public class CmpFrame extends Frame{\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Create a $cName",
   descrip => "",
   params => [
      {
         name => ( $aName = "frame1" ),
         type => 'Frame',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "frame2" ),
         type => 'Frame',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

print <<'__EOT__';
    /**
     * Gets the component Frames from this CmpFrame.
     * This method returns pointers to two Frames which, when applied in
     * parallel, are equivalent to this CmpFrame.
     *
     * @return  a two-element array giving the component Frames
     * @throws  AstException  if an error occurs in the AST library
     */
    public Frame[] decompose() {
        Mapping[] frms = decompose( null, null );
        return new Frame[] { (Frame) frms[ 0 ], (Frame) frms[ 1 ] };
    }

__EOT__

print "}\n";

