#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "WcsMap";
my( $aName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   version => '$Id$',
   author => "Mark Taylor (Starlink)",
   extra => '@see "the FITS-WCS paper"',
);

print "public class WcsMap extends Mapping {\n";

my( $type );
foreach $type ( qw( AZP TAN SIN STG ARC ZPN ZEA AIR CYP CAR
                    MER CEA COP COD COE COO BON PCO GLS SFL
                    PAR AIT MOL CSC QSC NCP TSC TPN SZP WCSBAD ) ) {
   print "   /** Indicates FITS-WCS mapping of type $type. */\n";
   print "   public static final int AST__$type = "
       . "getAstConstantI( \"AST__$type\" );\n\n"
}


makeNativeConstructor(
   Name => $cName,
   purpose => "Creates a $cName.",
   descrip => "",
   params => [
      {
         name => ( $aName = "ncoord" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "type" ),
         type => 'int',
         descrip => q{
            the type of FITS-WCS projection to apply.  
            One of the WcsMap constants (static final int fields) should
            be given for this value, e.g. WcsMap.AST__TAN for a tangent plane
            projection.  You should consult the FITS-WCS paper for the
            meaning of the available projections.
         },
      },
      {
         name => ( $aName = "lonax" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
      {
         name => ( $aName = "latax" ),
         type => 'int',
         descrip => ArgDescrip( $cName, $aName ),
      },
   ],
);

my( @args );

@args = (
   name => ( $aName = "natLat" ),
   type => 'float',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );

@args = (
   name => ( $aName = "natLon" ),
   type => 'float',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );

@args = (
   name => ( $aName = "pVi_m" ),
   type => 'float',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "wcsType" ),
   type => 'int',
   purpose => AttPurpose( $aName ),
   descrip => q{
      The <code>WcsType</code> attribute specifies which type of 
      FITS-WCS projection will be performed by this <code>WcsMap</code>.
      Its value will be one of the constants (static final int fields)
      defined by this class, e.g. WcsMap.AST__TAN for a tangent plane
      projection.  Its value is the one set when the WcsMap was created.
   },
);
makeGetAttrib( @args );


$aName = "wcsAxis";
my( $purpose ) = jdocize( deSentence( AttPurpose( $aName ) ) );
my( $descrip ) = jdocize( AttDescrip( $aName ) );

print <<__EOT__;
    /**
     * Get $purpose. $descrip
     * \@param  lonlat  if lonlat=1 the index of the longitude axis is returned,
     *                  if lonlat=2 the index of the latitude axis is returned.
     * \@return  the index of the longitude (lonlat=1) or latitude (lonlat=2)
     *           axis
     * \@throws  IndexOutOfBoundsException if lonlat is neither 1 or 2.
     */
    public int getWcsAxis( int lonlat ) {
        if ( lonlat == 1 ) {
            return getI( "WcsAxis(1)" );
        }
        else if ( lonlat == 2 ) {
            return getI( "WcsAxis(2)" );
        }
        else {
            throw new IndexOutOfBoundsException( 
                "lonlat value " + lonlat + " is not equal to 1 or 2" );
        }
    }

__EOT__

print "}\n";

