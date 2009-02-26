#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "SpecFrame";
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

print "public class SpecFrame extends Frame {\n";

makeNativeConstructor(
   Name => $cName,
   purpose => "Creates a $cName",
   descrip => "",
   params => [],
);

makeNativeMethod(
   name => ( $fName = "getRefPos" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [
      {
         name => ( $aName = 'frm' ),
         type => 'SkyFrame',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { 
      type => 'double[]',
      descrip => q{
         a 2-element array giving the (longitude,latitude) in radians 
         of the reference point, in the coordinate frame represented by
         <tt>frm</tt>
      },
   },
);

makeNativeMethod(
   name => ( $fName = "setRefPos" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [
      {
         name => ( $aName = 'frm' ),
         type => 'SkyFrame',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = 'lon' ),
         type => 'double',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = 'lat' ),
         type => 'double',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
   return => { type => 'void' },
);


my( @args );

@args = (
   name => ( $aName = "alignStdOfRest" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "refDec" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "refRA" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "restFreq" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => q{
      This attribute specifies the frequency corresponding to zero velocity.
      It is used when converting between velocity-based coordinate systems
      and other coordinate systems (such as frequency, wavelength, energy).
      The units are GHz.
      The default value is 1.0E5&nbsp;GHz.
   }
);
makeGetAttrib( @args );
makeSetAttrib( @args );
my( $setRestFreqDescrip ) = jdocize( AttDescrip( $aName ) );

print <<"__EOT__";
    /**
     * Sets the value of the <tt>restFreq</tt> attribute optionally 
     * with a unit string. $setRestFreqDescrip
     *
     * \@param  restFreq  a string representing the new <tt>restFreq</tt> value
     */
    public void setRestFreq( String restFreq ) {
        set( "RestFreq=" + restFreq );
    }

__EOT__

print <<'__EOT__';
    /**
     * Synonym for {@link #setObsLat}.
     */
    public void setGeoLat( String geoLat ) {
        setObsLat( geoLat );
    }

    /**
     * Synonym for {@link #getObsLat}.
     */
    public String getGeoLat() {
        return getObsLat();
    }

    /**
     * Synonym for {@link #setObsLon}.
     */
    public void setGeoLon( String geoLon ) {
        setObsLon( geoLon );
    }

    /**
     * Synonym for {@link #getObsLon}.
     */
    public String getGeoLon() {
        return getObsLon();
    }
__EOT__

@args = (
   name => ( $aName = "sourceSys" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "sourceVel" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "sourceVRF" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "stdOfRest" ),
   type => 'String',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

print "}\n";

