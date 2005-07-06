#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "Region";
my( $aName );
my( $fName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader(
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   author => "Mark Taylor",
);

print "public abstract class Region extends Frame {\n";

print <<__EOT__;

    /**
     * Package-private default constructor for abstract class.
     */
    Region() {
    }
__EOT__

makeNativeMethod(
   name => ( $fName = "getRegionBounds" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { 
      type => 'double[][]',
      descrip => q{
         A two-element array of <tt>naxes</tt>-element arrays giving
         the bounds of a box which contains this region.  The first
         element is the lower bounds, and the second element is the
         upper bounds.  If there is no limit on a lower/upper bound
         on a given axis, the corresponding value will be the
         lowest negative/highest positive possible value.
      },
   },
   params => [],
);

makeNativeMethod(
   name => ( $fName = "getRegionFrame" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'Frame', descrip => ReturnDescrip( $fName ), },
   params => [],
);

makeNativeMethod(
   name => ( $fName = "getUnc" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'Region', descrip => ReturnDescrip( $fName ), },
   params => [
      {
         name => ( $aName = "def" ),
         type => 'boolean',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "mapRegion" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'Region', descrip => ReturnDescrip( $fName ), },
   params => [
      {
         name => ( $aName = "map" ),
         type => 'Mapping',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "frame" ),
         type => 'Frame',
         descrip => ArgDescrip( $fName, $aName ),
      }
   ],
);

makeNativeMethod(
   name => ( $fName = "negate" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void', },
   params => [],
);

makeNativeMethod(
   name => ( $fName = "overlap" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => {
      type => 'int',
      descrip => q{
         value indicating overlap status - it will be one of the
         OVERLAP_* static final integers defined in the Region class
      },
   },
   params => [
      {
         name => "other",
         type => 'Region',
         descrip => "Other region for comparison with this one",
      },
   ],
);

print <<__EOT__;

    /** No overlap could be determined because the other region could not
     *  be mapped into the coordinate system of this one. */
    public static final int OVERLAP_UNKNOWN = 0;

    /** There is no overlap between this region and the other. */
    public static final int OVERLAP_NONE = 1;

    /** This region is completely inside the other one. */
    public static final int OVERLAP_INSIDE = 2;

    /** The other region is completely inside this one. */
    public static final int OVERLAP_OUTSIDE = 3;

    /** There is partial overlap between this region and the other. */
    public static final int OVERLAP_PARTIAL = 4;

    /** The regions are identical to within their uncertainties. */
    public static final int OVERLAP_SAME = 5;

    /** The other region is the exact negation of this one to within 
     *  their uncertainties. */
    public static final int OVERLAP_NEGATE = 6;
__EOT__

makeNativeMethod(
   name => ( $fName = "setUnc" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void' },
   params => [
      {
         name => ( $aName = "unc" ),
         type => 'Region',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);


my( @args );

@args = (
   name => ( $aName = "adaptive" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "negated" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "closed" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "meshSize" ),
   type => 'int',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "fillFactor" ),
   type => 'double',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "bounded" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
);
makeGetAttrib( @args );
 
print "}\n";
