#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "PcdMap";
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

print "public class PcdMap extends Mapping {\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Creates a $cName",
   descrip => "",
   params => [
      {
         name => ( $aName = "disco" ), 
         type => 'double', 
         descrip => ArgDescrip( $cName, $aName ),
      },
      { 
         name => ( $aName = "pcdcen" ),
         type => 'double[]',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);


my( @args );

@args = (
   name => ( $aName = "disco" ),
   type => "double",
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );


$aName = "pcdCen";

my( $purpose ) = jdocize( deSentence( AttPurpose( $aName ) ) );
my( $descrip ) = jdocize( AttDescrip( $aName ) );

print <<__EOT__;
    /**
     * Get $purpose by axis. $descrip 
     * \@param   axis  the index of the axis to get the value for (1 or 2)
     * \@return        the PcdCen attribute for the indicated axis of this
     *                mapping
     * \@throws  IndexOutOfBoundsException  if <code>axis</code> is not in
     *                                     the range 1..2.
     */
    public double getPcdCen( int axis ) {
        if ( axis >= 1 && axis <= 2 ) {
            return getD( "PcdCen" + "(" + axis + ")" );
        }
        else {
            throw new IndexOutOfBoundsException(
               "axis value " + axis + " is not in the range 1..2" );
        }
    }

    /**
     * Set $purpose by axis. $descrip
     * \@param   axis  the index of the axis to set the value for (1 or 2)
     * \@param   pcdCen  the PcdCen attribute for the indicated axis of
     *                  this mapping
     * \@throws  IndexOutOfBoundsException  if <code>axis</code> is not in
     *                                     the range 1..2.
     */
    public void setPcdCen( int axis, double pcdCen ) {
        if ( axis >= 1 && axis <= 2 ) {
            setD( "PcdCen" + "(" + axis + ")", pcdCen );
        }
        else {
            throw new IndexOutOfBoundsException(
               "axis value " + axis + " is not in the range 1..2" );
        }
    }

__EOT__

print "}\n";

