#!/usr/bin/perl -w

use strict;

use JMaker;
use SrcReader;

my( $cName ) = "MatrixMap";
my( $aName );

print "package uk.ac.starlink.ast;\n\n";

makeClassHeader( 
   Name => $cName,
   purpose => ClassPurpose( $cName ),
   descrip => ClassDescrip( $cName ),
   version => '$Id$',
   author => "Mark Taylor (Starlink)",
);

print "public class MatrixMap extends Mapping {\n";

print <<'__EOT__';

    /* Private native construction function - invokes the underlying 
     * construction function in the AST library. */
    private native void construct( int nin, int nout, int form, 
                                   double[] matrix );

    /**
     * Creates a MatrixMap using a fully specified matrix.
     *
     * @param  nin    the number of input coordinates
     * @param  nout   the number of output coordinates
     * @param  fullmatrix the matrix defining the transformation.
     *                <code>fullmatrix</code> must have <code>nout</code>
     *                elements, each of which is an array of doubles
     *                with <code>nin</code> elements.
     * @throws AstException  if there is an error in the AST library, or
     *                       if the supplied matrix is the wrong shape
     */
    public MatrixMap( int nin, int nout, double[][] fullmatrix ) {
        double[] matrix;

        /* Set up arguments for generic constructor, validating matrix shape. */
        if ( fullmatrix.length == nout ) {
            matrix = new double[ nin * nout ];
            for ( int i = 0; i < nout; i++ ) {
                if ( fullmatrix[ i ].length == nin ) {
                    System.arraycopy( fullmatrix[ i ], 0, 
                                      matrix, nin * i, nin );
                }
                else { 
                    throw new AstException( 
                        "construction matrix is the wrong shape" );
                }
            }
        }
        else {
            throw new AstException( "construction matrix is the wrong shape" );
        }

        /* Call the generic constructor. */
        construct( nin, nout, 0, matrix );
    }

    /**
     * Creates a MatrixMap using a diagonal matrix.  All off-diagonal
     * elements are considered equal to zero.
     *
     * @param  nin    the number of input coordinates
     * @param  nout   the number of output coordinates
     * @param  diag   the diagonal elements of the matrix defining the 
     *                transformation.  Must have at least 
     *                <code>min(nin,nout)</code> elements.
     * @throws AstException  if there is an error in the AST library, or
     *                       if the supplied matrix is the wrong shape
     */
    public MatrixMap( int nin, int nout, double[] diag ) {
        double[] matrix;

        /* Set up arguments for generic constructor, validating matrix shape. */
        if ( diag.length >= nin || diag.length >= nout ) {
            matrix = diag;
        }
        else {
            throw new AstException( "construction matrix is the wrong shape" );
        }

        /* Call the generic constructor. */
        construct( nin, nout, 1, matrix );
    }

    /**
     * Creates a MatrixMap using a unit matrix.
     *
     * @param  nin    the number of input coordinates
     * @param  nout   the number of output coordinates
     * @throws AstException  if there is an error in the AST library
     */
    public MatrixMap( int nin, int nout ) {
        construct( nin, nout, 2, (double[]) null );
    }
}
__EOT__


