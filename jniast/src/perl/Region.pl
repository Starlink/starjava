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
   name => ( $fName = "getRegionPoints" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   params => [],
   return => {
      type => 'double[][]',
      descrip => ArgDescrip( $fName, "points" ),
   },
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

$fName = "mask<X>";
makeJavaMethodHeader(
   name => "mask",
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'int', descrip => ReturnDescrip( $fName ), },
   params => [
      {
         name => ( $aName = "map" ),
         type => 'Mapping',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "inside" ),
         type => 'boolean',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ndim" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "lbnd" ),
         type => 'int[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ubnd" ),
         type => 'int[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "in" ),
         type => 'Object',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "val" ),
         type => 'Number',
         descrip => q{
            specifies the value used to flag the masked data.
            This should be an object of the wrapper class corresponding
            to the array type of the <code>in</code> array.
         },
      },
   ],
);

print <<'__EOT__';
{
        Class type = in.getClass().getComponentType();
        try {
            if ( type == byte.class ) {
                return maskB( map, inside, ndim, lbnd, ubnd, 
                              (byte[]) in, ((Byte) val).byteValue() );
            }
            else if ( type == short.class ) {
                return maskS( map, inside, ndim, lbnd, ubnd,
                              (short[]) in, ((Short) val).shortValue() );
            }
            else if ( type == int.class ) {
                return maskI( map, inside, ndim, lbnd, ubnd,
                              (int[]) in, ((Integer) val).intValue() );
            }
            else if ( type == long.class ) {
                return maskL( map, inside, ndim, lbnd, ubnd,
                              (long[]) in, ((Long) val).longValue() );
            }
            else if ( type == float.class ) {
                return maskF( map, inside, ndim, lbnd, ubnd,
                              (float[]) in, ((Float) val).floatValue() );
            }
            else if ( type == double.class ) {
                return maskD( map, inside, ndim, lbnd, ubnd,
                              (double[]) in, ((Double) val).doubleValue() );
            }
            else {
                throw new ClassCastException( "dummy ClassCastException" );
            }
        }
        catch ( ClassCastException e ) {
            throw new IllegalArgumentException( "Bad class " + in.getClass() +
                                                " for map 'in' param" );
        }
    }
__EOT__

my( $Xtype );
foreach $Xtype (
   [ "B", "byte" ],
   [ "S", "short" ],
   [ "I", "int" ],
   [ "L", "long" ],
   [ "F", "float" ],
   [ "D", "double" ],
) {
   my( $Xletter, $Xjtype ) = @{$Xtype};
   print <<__EOT__;
    /**
     * Masking method specific to $Xjtype data.
     *
     * \@see #mask
     */
    public native int mask$Xletter( Mapping map, boolean inside, int ndim,
                                    int\[\] lbnd, int\[\] ubnd,
                                    $Xjtype\[\] in, $Xjtype val );
__EOT__
}

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

makeNativeMethod(
   name => ( $fName = "showMesh" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void' },
   params => [
      {
         name => ( $aName = "format" ),
         type => 'boolean',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ttl" ),
         type => 'String',
         descrip => ArgDescrip( $fName, $aName ),
      }
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
