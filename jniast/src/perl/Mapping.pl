#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "Mapping";

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

print "public class Mapping extends AstObject {\n";

print <<'__EOT__';

    /**
     * A nearest-neighbour interpolator for use in the resampling methods.
     * Provided static for convenience.
     */
    public static final Interpolator NEAREST_INTERPOLATOR =
        Interpolator.nearest();

    /**
     * A linear interpolator for use in the resampling methods.
     * Provided static for convenience.
     */
    public static final Interpolator LINEAR_INTERPOLATOR =
        Interpolator.linear();

    /**
     * A nearest-neighbour spreader for use in the rebinning methods.
     * Provided static for convenience.
     */
    public static final Spreader NEAREST_SPREADER =
        Spreader.nearest();

    /**
     * A linear spreader for use in the rebinning methods.
     * Provided static for convenience.
     */
    public static final Spreader LINEAR_SPREADER =
        Spreader.linear();

    /**
     * Perform initialization required for JNI code at class load time.
     */
    static {
        nativeInitializeMapping();
    }
    private native static void nativeInitializeMapping();

    /**
     * Dummy constructor.  This constructor does not create a valid
     * Mapping object, but is required for inheritance by Mapping's
     * subclasses.
     */
    protected Mapping() {
    }

    /**
     * Inverts a mapping by reversing the sense of its Invert attribute.
     */
    public void invert() {
        setInvert( ! getInvert() );
    }

__EOT__

makeNativeMethod(
   name => ( $fName = "simplify" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'Mapping', descrip => ReturnDescrip( $fName ), },
   params => [],
);

makeNativeMethod(
   name => ( $fName = "decompose" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => {
      type => 'Mapping[]',
      descrip => q{
         An array of Mappings giving the components of this Mapping.
         If this Mapping is a CmpMap, it will be a 2-element array
         holding the two constituent Mappings which were used to create
         this Mapping, either in series or in parallel. 
         If this Mapping is a CmpFrame, it will be a 2-element array 
         containing the two constituent Frames which were used to 
         create this CmpFrame in parallel.
         Otherwise, a 1-element array containing a clone of this 
         object will be returned.
      }
   },
   params => [
      {
         name => ( $aName = "series" ),
         type => 'boolean[]',
         descrip => q{
            A one-element array to hold a boolean result indicating whether
            the component Mappings are in series or not.  If true, the
            two Mappings should be joined in series to form this CmpMap,
            if false then in parallel.  For any object other than a 
            CmpMap, the returned value will be zero.
            If <code>null</code> is supplied, then the series information 
            is not returned.
         },
      },
      {
         name => ( $aName = "inverts" ),
         type => 'boolean[]',
         descrip => q{
            A two-element array to hold the Invert attribute vales for
            the constituent Mappings.  
            If <code>null</code> is supplied, then the inversion information
            is not returned.
         },
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "mapBox" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { 
      type => 'double[]', 
      descrip => q{
         a two-element array giving the lowest (element 0) and highest
         (element 1) value taken by the nominated output coordinate
         within the specified region of input space
      },
   },
   params => [
      {
         name => ( $aName = "lbnd_in" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ubnd_in" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "forward" ),
         type => 'boolean',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "coord_out" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "xl" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "xu" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "mapSplit" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => {
      type => 'Mapping',
      descrip => q{
         the returned mapping
      },
   },
   params => [
      {
         name => ( $aName = "in" ),
         type => 'int[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "out" ),
         type => 'int[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

$fName = "resample<X>";
makeJavaMethodHeader(
   name => ( "resample" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'int', descrip => ReturnDescrip( $fName ), },
   params => [
      {
         name => ( $aName = "ndim_in" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "lbnd_in" ),
         type => 'int[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ubnd_in" ),
         type => 'int[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "in" ),
         type => 'Object',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "in_var" ),
         type => 'Object',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => "interp",
         type => 'Mapping.Interpolator',
         descrip => q{
            an <code>Interpolator</code> object which determines what
            sub-pixel interpolation scheme should be used for the
            resampling
         },
      },
      {
         name => "flags",
         type => 'ResampleFlags',
         descrip => q{
            flags object giving additional details about the resampling
            procedure
         },
      },
      {
         name => ( $aName = "tol" ),
         type => 'double',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "maxpix" ),
         type => 'int', 
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "badval" ),
         type => 'Number',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ndim_out" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "lbnd_out" ),
         type => 'int[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ubnd_out" ),
         type => 'int[]',
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
         name => ( $aName = "out" ),
         type => 'Object',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "out_var" ),
         type => 'Object',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

print <<'__EOT__';
{
        Class type = in.getClass().getComponentType();
        try {
            if ( type == byte.class ) {
                return resampleB( ndim_in, lbnd_in, ubnd_in,
                                  (byte[]) in, (byte[]) in_var,
                                  interp, flags, tol, maxpix,
                                  ((Byte) badval).byteValue(),
                                  ndim_out, lbnd_out, ubnd_out, lbnd, ubnd,
                                  (byte[]) out, (byte[]) out_var );
            }
            else if ( type == short.class ) {
                return resampleS( ndim_in, lbnd_in, ubnd_in,
                                  (short[]) in, (short[]) in_var,
                                  interp, flags, tol, maxpix,
                                  ((Short) badval).shortValue(),
                                  ndim_out, lbnd_out, ubnd_out, lbnd, ubnd,
                                  (short[]) out, (short[]) out_var );
            }
            else if ( type == int.class ) {
                return resampleI( ndim_in, lbnd_in, ubnd_in,
                                  (int[]) in, (int[]) in_var,
                                  interp, flags, tol, maxpix,
                                  ((Integer) badval).intValue(),
                                  ndim_out, lbnd_out, ubnd_out, lbnd, ubnd,
                                  (int[]) out, (int[]) out_var );
            }
            else if ( type == long.class ) {
                return resampleL( ndim_in, lbnd_in, ubnd_in,
                                  (long[]) in, (long[]) in_var,
                                  interp, flags, tol, maxpix,
                                  ((Long) badval).longValue(),
                                  ndim_out, lbnd_out, ubnd_out, lbnd, ubnd,
                                  (long[]) out, (long[]) out_var );
            }
            else if ( type == float.class ) {
                return resampleF( ndim_in, lbnd_in, ubnd_in,
                                  (float[]) in, (float[]) in_var,
                                  interp, flags, tol, maxpix,
                                  ((Float) badval).floatValue(),
                                  ndim_out, lbnd_out, ubnd_out, lbnd, ubnd,
                                  (float[]) out, (float[]) out_var );
            }
            else if ( type == double.class ) {
                return resampleD( ndim_in, lbnd_in, ubnd_in,
                                  (double[]) in, (double[]) in_var,
                                  interp, flags, tol, maxpix,
                                  ((Double) badval).doubleValue(),
                                  ndim_out, lbnd_out, ubnd_out, lbnd, ubnd,
                                  (double[]) out, (double[]) out_var );
            }
            else {
                throw new ClassCastException( "dummy ClassCastException" );
            }
        }
        catch ( ClassCastException e ) {
                throw new IllegalArgumentException(
                    "in, in_var, out, out_var must be all arrays of the same "
                  + "primitive type, and badval a matching Number type" );
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
     * Resampling method specific to $Xjtype data - see the resample method.
     */
    public native int resample$Xletter( 
        int ndim_in, int\[\] lbnd_in, int\[\] ubnd_in,
        $Xjtype\[\] in, $Xjtype\[\] in_var,
        Mapping.Interpolator interp, ResampleFlags flags, double tol,
        int maxpix,
        $Xjtype badval, int ndim_out, int\[\] lbnd_out, int\[\] ubnd_out,
        int\[\] lbnd, int\[\] ubnd,
        $Xjtype\[\] out, $Xjtype\[\] out_var );

__EOT__
}


# my( $Xtype );
# foreach $Xtype (
#    [ "B", "byte" ],
#    [ "D", "double" ],
#    [ "F", "float" ],
#    [ "I", "int" ],
#    [ "L", "long" ],
#    [ "S", "short" ],
# ) {
#    my( $Xletter, $Xjtype ) = @{$Xtype};
#    $fName = "astResample<X>",
#    makeNativeMethod(
#       name => "resample$Xletter",
#       purpose => 
#          "Use this Mapping to resample <code>$Xjtype</code> gridded data.",
#       descrip => FuncDescrip( $fName ),
#       return => { type => 'int', descrip => ReturnDescrip( $fName ), },
#       params => [
#          {
#             name => ( $aName = "ndim_in" ),
#             type => 'int',
#             descrip => ArgDescrip( $fName, $aName ),
#          },
#          {
#             name => ( $aName = "lbnd_in" ),
#             type => 'int[]',
#             descrip => ArgDescrip( $fName, $aName ),
#          },
#          {
#             name => ( $aName = "ubnd_in" ),
#             type => 'int[]',
#             descrip => ArgDescrip( $fName, $aName ),
#          },
#          {
#             name => ( $aName = "in" ),
#             type => $Xjtype . '[]',
#             descrip => ArgDescrip( $fName, $aName ),
#          },
#          {
#             name => ( $aName = "in_var" ),
#             type => $Xjtype . '[]',
#             descrip => ArgDescrip( $fName, $aName ),
#          },
#          {
#             name => "interp",
#             type => 'Mapping.Interpolator',
#             descrip => q{
#                an <code>Interpolator</code> object which determines what
#                sub-pixel interpolation scheme should be used for the
#                resampling
#             },
#          },
#          {
#             name => "usebad",
#             type => 'boolean',
#             descrip => q{
#                if true, indicates that there may be bad
#                pixels in the input array(s) which must be
#                recognised by comparing with the value given for
#                <code>badval</code> and propagated to the
#                output array(s). If
#                this flag is not set, all input values are treated
#                literally and the <code>badval</code>
#                value is only used for
#                flagging output array values.
#             },
#          },
#          {
#             name => ( $aName = "tol" ),
#             type => 'double',
#             descrip => ArgDescrip( $fName, $aName ),
#          },
#          {
#             name => ( $aName = "maxpix" ),
#             type => 'int', 
#             descrip => ArgDescrip( $fName, $aName ),
#          },
#          {
#             name => ( $aName = "badval" ),
#             type => $Xjtype,
#             descrip => ArgDescrip( $fName, $aName ),
#          },
#          {
#             name => ( $aName = "ndim_out" ),
#             type => 'int',
#             descrip => ArgDescrip( $fName, $aName ),
#          },
#          {
#             name => ( $aName = "lbnd_out" ),
#             type => 'int[]',
#             descrip => ArgDescrip( $fName, $aName ),
#          },
#          {
#             name => ( $aName = "ubnd_out" ),
#             type => 'int[]',
#             descrip => ArgDescrip( $fName, $aName ),
#          },
#          {
#             name => ( $aName = "lbnd" ),
#             type => 'int[]',
#             descrip => ArgDescrip( $fName, $aName ),
#          },
#          {
#             name => ( $aName = "ubnd" ),
#             type => 'int[]',
#             descrip => ArgDescrip( $fName, $aName ),
#          },
#          {
#             name => ( $aName = "out" ),
#             type => $Xjtype . '[]',
#             descrip => ArgDescrip( $fName, $aName ),
#          },
#          {
#             name => ( $aName = "out_var" ),
#             type => $Xjtype . '[]',
#             descrip => ArgDescrip( $fName, $aName ),
#          },
#       ],
#    );
# }


$fName = "rebin<X>";
makeJavaMethodHeader(
   name => "rebin",
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'void' },
   params => [
      {
         name => ( $aName = "wlim" ),
         type => 'double',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ndim_in" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "lbnd_in" ),
         type => 'int[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ubnd_in" ),
         type => 'int[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "in" ),
         type => 'Object',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "in_var" ),
         type => 'Object',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => "spread",
         type => 'Mapping.Spreader',
         descrip => q{
            a <code>Spreader</code> object which determines how each
            input data value is divided up amongst the corresponding
            output pixels
         },
      },
      {
         name => "usebad",
         type => 'boolean',
         descrip => q{
            if true, indicates that there may be bad
            pixels in the input array(s) which must be
            recognised by comparing with the value given for
            <code>badval</code> and propagated to the
            output array(s). If
            this flag is not set, all input values are treated
            literally and the <code>badval</code>
            value is only used for
            flagging output array values.
         },
      },
      {
         name => ( $aName = "tol" ),
         type => 'double',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "maxpix" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "badval" ),
         type => 'Number',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ndim_out" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "lbnd_out" ),
         type => 'int[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ubnd_out" ),
         type => 'int[]',
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
         name => ( $aName = "out" ),
         type => 'Object',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "out_var" ),
         type => 'Object',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

print <<'__EOT__';
{
        Class type = in.getClass().getComponentType();
        try {
            if ( type == int.class ) {
                rebinI( wlim, ndim_in, lbnd_in, ubnd_in, 
                        (int[]) in, (int[]) in_var,
                        spread, usebad, tol, maxpix, 
                        ((Integer) badval).intValue(), ndim_out, 
                        lbnd_out, ubnd_out, lbnd, ubnd,
                        (int[]) out, (int[]) out_var );
            }
            else if ( type == float.class ) {
                rebinF( wlim, ndim_in, lbnd_in, ubnd_in, 
                        (float[]) in, (float[]) in_var,
                        spread, usebad, tol, maxpix,
                        ((Float) badval).floatValue(), ndim_out,
                        lbnd_out, ubnd_out, lbnd, ubnd,
                        (float[]) out, (float[]) out_var );
            }
            else if ( type == double.class ) {
                rebinD( wlim, ndim_in, lbnd_in, ubnd_in,
                        (double[]) in, (double[]) in_var,
                        spread, usebad, tol, maxpix,
                        ((Double) badval).doubleValue(), ndim_out,
                        lbnd_out, ubnd_out, lbnd, ubnd,
                        (double[]) out, (double[]) out_var );
            }
            else {
                throw new ClassCastException( "Dummy class cast exception" );
            }
        }
        catch ( ClassCastException e ) {
            throw new IllegalArgumentException(
                "in, in_var, out and out_var must all be arrays of the same "
              + "primitive type, and badval a matching Number type" );
        }
    }
__EOT__

foreach $Xtype (
   [ "I", "int" ],
   [ "F", "float" ],
   [ "D", "double" ],
) {
   my( $Xletter, $Xjtype ) = @{$Xtype};
   print <<__EOT__;
   /**
    * Rebinning method specific to $Xjtype data.
    *
    * \@see  \#rebin
    */
   public native void rebin$Xletter(
      double wlim, int ndim_in, int\[\] lbnd_in, int\[\] ubnd_in,
      $Xjtype\[\] in, $Xjtype\[\] in_var,
      Mapping.Spreader spread, boolean usebad, double tol, int maxpix,
      $Xjtype badval, int ndim_out, int\[\] lbnd_out, int\[\] ubnd_out,
      int\[\] lbnd, int\[\] ubnd,
      $Xjtype\[\] out, $Xjtype\[\] out_var );

__EOT__
}


makeNativeMethod(
   name => ( $fName = "tran1" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { 
      type => 'double[]',
      descrip => q{
         an array of npoint elements representing the transformed points
      }
   },
   params => [
      {
         name => ( $aName = "npoint" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "xin" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "forward" ),
         type => 'boolean',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);


makeNativeMethod(
   name => ( $fName = "tran2" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { 
      type => 'double[][]',
      descrip => q{
         a two-element array of arrays of doubles.  The first
         element is an <code>npoint</code>-element array giving
         the transformed X coordinates, and the second element
         is an <code>npoint</code>-element array giving the
         transformed Y coordinates.
      },
   },
   params => [
      {
         name => ( $aName = "npoint" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "xin" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "yin" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "forward" ),
         type => 'boolean',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "tranN" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => {
      type => 'double[]',
      descrip => q{
         an array of <code>ncoord_out*npoint</code>
         coordinates representing the transformed points.
         Coordinate number <code>coord</code>
         for point <code>point</code> must be stored at
         <code>in[npoint*coord+point]</code>, that is the 
         all the coordinates for the first dimension are stored
         first, then all the coordinates for the second dimension ...
         This is the same order as for the <code>in</code> array.
      },
   },
   params => [
      {
         name => ( $aName = "npoint" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ncoord_in" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "in" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "forward" ),
         type => 'boolean',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ncoord_out" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "tranP" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => {
      type => 'double[][]',
      descrip => q{
         an <code>ncoord_out</code>-element array of 
         <code>npoint</code>-element arrays.
         These give the coordinates of the points to transform. 
      },
   },
   params => [
      {
         name => ( $aName = "npoint" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ncoord_in" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "in" ),
         type => 'double[][]',
         descrip => q{
            An <code>ncoord_in</code>-element array of 
            <code>npoint</code>-element arrays.  These give the 
            coordinates of the transformed points.
         },
      },
      {
         name => ( $aName = "forward" ),
         type => 'boolean',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ncoord_out" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "tranGrid" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'double[][]', descrip => ArgDescrip( $fName, "out" ), },
   params => [
      {
         name => ( $aName = "ncoord_in" ),
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
         name => ( $aName = "tol" ),
         type => 'double',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "maxpix" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "forward" ),
         type => 'boolean',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ncoord_out" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "rate" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   return => { type => 'double', descrip => ReturnDescrip( $fName ), },
   params => [
      {
         name => ( $aName = "at" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ax1" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ax2" ),
         type => 'int',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

makeNativeMethod(
   name => ( $fName = "linearApprox" ),
   purpose => FuncPurpose( $fName ),
   descrip => FuncDescrip( $fName ),
   extra => q{
      If this mapping is not linear to the given tolerance, <tt>null</tt>
      will be returned.
   },
   return => {
      type => 'double[]',
      descrip => ArgDescrip( $fName, "fit" ),
   },
   params => [
      {
         name => ( $aName = "lbnd" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "ubnd" ),
         type => 'double[]',
         descrip => ArgDescrip( $fName, $aName ),
      },
      {
         name => ( $aName = "tol" ),
         type => 'double',
         descrip => ArgDescrip( $fName, $aName ),
      },
   ],
);

my( @args );

@args = (
   name => ( $aName = "invert" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );

@args = (
   name => ( $aName = "nin" ),
   type => 'int',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );
   
@args = (
   name => ( $aName = "nout" ),
   type => 'int',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );
   
@args = (
   name => ( $aName = "report" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );
makeSetAttrib( @args );
   
@args = (
   name => ( $aName = "tranForward" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );
   
@args = (
   name => ( $aName = "tranInverse" ),
   type => 'boolean',
   purpose => AttPurpose( $aName ),
   descrip => AttDescrip( $aName ),
   applic => AttApplic( $aName ),
);
makeGetAttrib( @args );
   
print <<'__EOT__';

    /*
     * Inner classes.
     */

    /**
     * Controls the interpolation scheme used by <code>Mapping</code>'s
     * resampling methods.  This class has no public constructors, but
     * provides static factory methods which generate <code>Interpolator</code>
     * objects that can be passed to the <code>resample*</code> methods.
     * There are a number of standard schemes, and users may supply their
     * own by implementing the {@link Ukern1Calculator} or
     * {@link UinterpCalculator} interfaces.
     */
    public static class Interpolator {

        /*
         * Private fields written by the factory methods and read by
         * AstMapping native code.
         */
        private int scheme;
        private Ukern1Calculator ukern1er;
        private UinterpCalculator uinterper;

        /* Used as a buffer by native code as well as resampleX documented
         * use. */
        private double params[];

        /*
         * Values of resampling scheme identifiers in AST C library.
         */
        private static final int AST__NEAREST =
                getAstConstantI( "AST__NEAREST" );
        private static final int AST__LINEAR =
                getAstConstantI( "AST__LINEAR" );
        private static final int AST__SINC =
                getAstConstantI( "AST__SINC" );
        private static final int AST__SINCSINC =
                getAstConstantI( "AST__SINCSINC" );
        private static final int AST__SINCCOS =
                getAstConstantI( "AST__SINCCOS" );
        private static final int AST__SOMB =
                getAstConstantI( "AST__SOMB" );
        private static final int AST__SOMBCOS =
                getAstConstantI( "AST__SOMBCOS" );
        private static final int AST__SINCGAUSS =
                getAstConstantI( "AST__SINCGAUSS" );
        private static final int AST__BLOCKAVE =
                getAstConstantI( "AST__BLOCKAVE" );
        private static final int AST__UKERN1 =
                getAstConstantI( "AST__UKERN1" );
        private static final int AST__UINTERP =
                getAstConstantI( "AST__UINTERP" );

        /*
         * Sole private constructor.
         */
        private Interpolator( int scheme, double[] params ) {
            this.scheme = scheme;

            /* Ensure that the params array has some spare space, since
             * it is used by native code to store one or two pointers
             * as well as its documented resampleX use.  8 is plenty. */
            this.params = new double[ 8 ];
            if ( params != null ) {
                for ( int i = 0; i < params.length; i++ ) {
                    this.params[ i ] = params[ i ];
                }
            }
        }

        /*
         * Public static methods which return the various kinds of 
         * interpolators.
         */

        /**
         * Returns a resampling interpolator which does
         * nearest-neighbour 'interpolation'.
         *
         * @return  a nearest-neighbour resampling Interpolator
         */
        public static Interpolator nearest() {
            return new Interpolator( AST__NEAREST, null );
        }

        /**
         * Returns a resampling interpolator which does linear interpolation.
         *
         * @return  a linear resampling Interpolator
         */
        public static Interpolator linear() {
            return new Interpolator( AST__LINEAR, null );
        }

        /**
         * Returns a resampling interpolator which uses a
         * <code>sinc(pi*x)</code> 1-dimensional kernel.
         *
         * @param   npix  the number of pixels to contribute to the 
         *                interpolated result on either side of the
         *                interpolation point in each dimension.
         *                Execution time increases rapidly with this number.
         *                Typically, a value of 2 is appropriate and the
         *                minimum value used will be 1.  A value of zero
         *                or less may be given to indicate that a suitable
         *                number of pixels should be calculated automatically.
         * @return  a sinc-type resampling Interpolator
         */
        public static Interpolator sinc( int npix ) {
            return new Interpolator( AST__SINC,
                                     new double[] { (double) npix } );
        }

        /**
         * Returns a resampling interpolator which uses a
         * <code>sinc(pi*x).sinc(k*pi*x)</code> 1-dimensional kernel.
         *
         * @param   npix  the number of pixels to contribute to the 
         *                interpolated result on either side of the
         *                interpolation point in each dimension.
         *                Execution time increases rapidly with this number.
         *                Typically, a value of 2 is appropriate and the
         *                minimum value used will be 1.  A value of zero
         *                or less may be given to indicate that a suitable
         *                number of pixels should be calculated automatically.
         * @param  width  the number of pixels at which the envelope goes
         *                to zero.  Should be at least 1.0.
         * @return  a sinc-sinc-type resampling Interpolator
         */
        public static Interpolator sincSinc( int npix, double width ) {
            return new Interpolator( AST__SINCSINC,
                                     new double[] { (double) npix, width } );
        }

        /**
         * Returns a resampling interpolator which uses a
         * <code>sinc(pi*x).cos(k*pi*x)</code> 1-dimensional kernel.
         *
         * @param   npix  the number of pixels to contribute to the 
         *                interpolated result on either side of the
         *                interpolation point in each dimension.
         *                Execution time increases rapidly with this number.
         *                Typically, a value of 2 is appropriate and the
         *                minimum value used will be 1.  A value of zero
         *                or less may be given to indicate that a suitable
         *                number of pixels should be calculated automatically.
         * @param  width  the number of pixels at which the envelope goes
         *                to zero.  Should be at least 1.0.
         * @return  a sinc-cos-type resampling Interpolator
         */
        public static Interpolator sincCos( int npix, double width ) {
            return new Interpolator( AST__SINCCOS,
                                     new double[] { (double) npix, width } );
        }

        /**
         * Returns a resampling interpolator which uses a
         * <code>somb(pi*x)</code> 1-dimensional kernel 
         * (a "sombrero" function).
         * <code>x</code> is the pixel offset from the interpolation point
         * and <code>somb(z)=2*1(z)/z</code> (J1 is a Bessel function
         * of the first kind of order 1).
         *
         * @param   npix  the number of pixels to contribute to the 
         *                interpolated result on either side of the
         *                interpolation point in each dimension.
         *                Execution time increases rapidly with this number.
         *                Typically, a value of 2 is appropriate and the
         *                minimum value used will be 1.  A value of zero
         *                or less may be given to indicate that a suitable
         *                number of pixels should be calculated automatically.
         * @return  a somb-type resampling Interpolator
         */
        public static Interpolator somb( int npix ) {
            return new Interpolator( AST__SOMB,
                                     new double[] { (double) npix } );
        }

        /**
         * Returns a resampling interpolator which uses a
         * <code>somb(pi*x).cos(k*pi*x)</code> 1-dimensional kernel.
         * <code>k</code> is a constant, out to the point where
         * <code>cos(k*pi*x) goes to zero, and zero beyond,
         * and <code>somb(z)=2*1(z)/z</code> (J1 is a Bessel function
         * of the first kind of order 1).
         *
         * @param   npix  the number of pixels to contribute to the 
         *                interpolated result on either side of the
         *                interpolation point in each dimension.
         *                Execution time increases rapidly with this number.
         *                Typically, a value of 2 is appropriate and the
         *                minimum value used will be 1.  A value of zero
         *                or less may be given to indicate that a suitable
         *                number of pixels should be calculated automatically.
         * @param  width  the number of pixels at which the envelope goes
         *                to zero.  Should be at least 1.0.
         * @return  a sinc-cos-type resampling Interpolator
         */
        public static Interpolator sombCos( int npix, double width ) {
            return new Interpolator( AST__SOMBCOS,
                                     new double[] { (double) npix, width } );
        }

        /**
         * Returns a resampling interpolator which uses a
         * <code>sinc(pi*x).exp(-k*x*x)</code> 1-dimensional kernel.
         *
         * @param   npix  the number of pixels to contribute to the 
         *                interpolated result on either side of the
         *                interpolation point in each dimension.
         *                Execution time increases rapidly with this number.
         *                Typically, a value of 2 is appropriate and the
         *                minimum value used will be 1.  A value of zero
         *                or less may be given to indicate that a suitable
         *                number of pixels should be calculated automatically.
         * @param  fwhm   the full width at half maximum of the Gaussian
         *                envelope.  Should be at least 0.1.
         * @return a sinc-Gauss-type resampling Interpolator
         */
        public static Interpolator sincGauss( int npix, double fwhm ) {
            return new Interpolator( AST__SINCGAUSS,
                                     new double[] { (double) npix, fwhm } );
        }

        /**
         * Returns a block averaging resampling interpolator.
         *
         * @param   npix  the number of pixels to contribute to the 
         *                interpolated result on either side of the
         *                interpolation point in each dimension.
         *                Execution time increases rapidly with this number.
         * @return  a block averaging resampling Interpolator
         */
        public static Interpolator blockAve( int npix ) {
            return new Interpolator( AST__BLOCKAVE,
                                     new double[] { (double) npix } );
        }

        /**
         * Returns a resampling interpolator which uses a given user-defined
         * 1-dimensional kernel.
         *
         * @param   ukern1er  a Ukern1Calculator object which defines a 
         *                    1-dimensional kernel function
         * @param   npix  the number of pixels to contribute to the 
         *                interpolated result on either side of the
         *                interpolation point in each dimension.
         *                Execution time increases rapidly with this number.
         *                Typically, a value of 2 is appropriate and the
         *                minimum value used will be 1.  A value of zero
         *                or less may be given to indicate that a suitable
         *                number of pixels should be calculated automatically.
         * @return  a user 1-d kernel resampling Interpolator
         */
        public static Interpolator ukern1( Ukern1Calculator ukern1er, 
                                           int npix ) {
            Interpolator interp =
                new Interpolator( AST__UKERN1, new double[] { (double) npix } );
            interp.ukern1er = ukern1er;
            return interp;
        }

        /**
         * Returns a resampling interpolator which uses a given user-defined
         * generic resampling function.
         *
         * @param uinterper  a UinterpCalculator object which defines a generic
         *          resampling function
         * @return  a generic user-defined resampling Interpolator
         */
        public static Interpolator uinterp( UinterpCalculator uinterper ) {
            Interpolator interp = new Interpolator( AST__UINTERP, null );
            interp.uinterper = uinterper;
            return interp;
        }
    }

    /**
     * Controls the spreading scheme used by <code>Mapping</code>'s
     * rebinning methods.  This class has no public constructors,
     * but provides static factory methods which generate <code>Spreader</code>
     * objects that can be passed to the <code>rebin*</code> methods.
     */
    public static class Spreader {

        /*
         * Private fields written by the factory methods and read by
         * AstMapping native code.
         */
        private int scheme_;

        /*
         * Used as a buffer by native code as well sa rebinX documented use.
         */
        private double[] params_;

        /*
         * Values of spreading scheme identifiers in C library.
         */
        private static final int AST__NEAREST =
                getAstConstantI( "AST__NEAREST" );
        private static final int AST__LINEAR =
                getAstConstantI( "AST__LINEAR" );
        private static final int AST__SINC =
                getAstConstantI( "AST__SINC" );
        private static final int AST__SINCSINC =
                getAstConstantI( "AST__SINCSINC" );
        private static final int AST__SINCCOS =
                getAstConstantI( "AST__SINCCOS" );
        private static final int AST__SINCGAUSS =
                getAstConstantI( "AST__SINCGAUSS" );
        private static final int AST__GAUSS =
                getAstConstantI( "AST__GAUSS" );

        /**
         * Sole private constructor.
         */
        private Spreader( int scheme, double[] params ) {
            scheme_ = scheme;
            params_ = params;
        }

        /*
         * Public static methods which return the various kinds of spreaders.
         */

        /**
         * Returns a resampling spreader which samples from the
         * nearest neighbour.
         *
         * @return  a nearest-neighbour resampling Spreader
         */
        public static Spreader nearest() {
            return new Spreader( AST__NEAREST, null );
        }

        /**
         * Returns a resampling spreader which samples using 
         * linear interpolation.
         *
         * @return  a linear interpolation resampling Spreader
         */
        public static Spreader linear() {
            return new Spreader( AST__LINEAR, null );
        }

        /**
         * Returns a resampling spreader which uses a
         * <code>sinc(pi*x)</code> 1-dimensional kernel.
         *
         * @param   npix  the number of pixels to contribute to the
         *                interpolated result on either side of the
         *                interpolation point in each dimension.
         *                Execution time increases rapidly with this number.
         *                Typically, a value of 2 is appropriate and the
         *                minimum value used will be 1.  A value of zero
         *                or less may be given to indicate that a suitable
         *                number of pixels should be calculated automatically.
         * @return  a sinc-type resampling Spreader
         */
        public static Spreader sinc( int npix ) {
            return new Spreader( AST__SINC,
                                 new double[] { (double) npix } );
        }

        /**
         * Returns a resampling spreader which uses a
         * <code>sinc(pi*x).sinc(k*pi*x)</code> 1-dimensional kernel.
         *
         * @param   npix  the number of pixels to contribute to the
         *                interpolated result on either side of the
         *                interpolation point in each dimension.
         *                Execution time increases rapidly with this number.
         *                Typically, a value of 2 is appropriate and the
         *                minimum value used will be 1.  A value of zero
         *                or less may be given to indicate that a suitable
         *                number of pixels should be calculated automatically.
         * @param  width  the number of pixels at which the envelope goes
         *                to zero.  Should be at least 1.0.
         * @return  a sinc-sinc-type resampling Spreader
         */
        public static Spreader sincSinc( int npix, double width ) {
            return new Spreader( AST__SINCSINC,
                                 new double[] { (double) npix, width } );
        }

        /**
         * Returns a resampling spreader which uses a
         * <code>sinc(pi*x).cos(k*pi*x)</code> 1-dimensional kernel.
         *
         * @param   npix  the number of pixels to contribute to the
         *                interpolated result on either side of the
         *                interpolation point in each dimension.
         *                Execution time increases rapidly with this number.
         *                Typically, a value of 2 is appropriate and the
         *                minimum value used will be 1.  A value of zero
         *                or less may be given to indicate that a suitable
         *                number of pixels should be calculated automatically.
         * @param  width  the number of pixels at which the envelope goes
         *                to zero.  Should be at least 1.0.
         * @return  a sinc-cos-type resampling Spreader
         */
        public static Spreader sincCos( int npix, double width ) {
            return new Spreader( AST__SINCCOS,
                                 new double[] { (double) npix, width } );
        }

        /**
         * Returns a resampling spreader which uses a
         * <code>sinc(pi*x).exp(-k*x*x)</code> 1-dimensional kernel.
         *
         * @param   npix  the number of pixels to contribute to the
         *                interpolated result on either side of the
         *                interpolation point in each dimension.
         *                Execution time increases rapidly with this number.
         *                Typically, a value of 2 is appropriate and the
         *                minimum value used will be 1.  A value of zero
         *                or less may be given to indicate that a suitable
         *                number of pixels should be calculated automatically.
         * @param  fwhm   the full width at half maximum of the Gaussian
         *                envelope.  Should be at least 0.1.
         * @return a sinc-Gauss-type resampling Spreader
         */
        public static Spreader sincGauss( int npix, double fwhm ) {
            return new Spreader( AST__SINCGAUSS,
                                 new double[] { (double) npix, fwhm } );
        }

        /**
         * Returns a resampling spreader which uses a
         * <code>exp(-k*x*x)</code> 1-dimensional kernel.
         *
         * @param   npix  the number of pixels to contribute to the
         *                interpolated result on either side of the
         *                interpolation point in each dimension.
         * @param   fwhm  the full width at half maximum of the Gaussian
         *                envelope.  Should be at least 0.1.
         * @return  a Gauss-type resampling Spreader
         */
        public static Spreader gauss( int npix, double fwhm ) {
            return new Spreader( AST__GAUSS,
                                 new double[] { (double) npix, fwhm } );
        }
    }

}

__EOT__

