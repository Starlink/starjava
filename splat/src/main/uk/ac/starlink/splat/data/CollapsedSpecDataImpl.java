/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     14-SEP-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.SpecFrame;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.util.SplatException;

/**
 * This class provides an implementation of SpecDataImpl that reprocesses
 * another 2D SpecData's implementation into a collapsed 1D
 * implementation. The collapse happens either along the dispersion axis, if
 * one can be determined, or along the first axis.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see SpecData
 * @see SpecDataImpl
 */
public class CollapsedSpecDataImpl
    extends MEMSpecDataImpl
{
    /**
     * Constructor, accepts the SpecData to be collapsed as single
     * argument.
     *
     * @param parent the SpecData to collapse.
     */
    public CollapsedSpecDataImpl( SpecData parent )
        throws SplatException
    {
        super( parent.getFullName() );
        this.shortName = "Collapsed: " + shortName;
        this.parentImpl = parent.getSpecDataImpl();
        collapse( parent );
    }

    /**
     * Do the collapse.
     */
    protected void collapse( SpecData parent )
        throws SplatException
    {
        //  Get the real dimensionality of the original data.
        int[] realDims = parent.getSpecDataImpl().getDims();
        int[] dims = null;
        int ndims = 0;

        //  First non-redundant axis, 0 means not set.
        int firstax = 0;

        //  We only handle 2D, but this can be complicated when some
        //  dimensions are redundant.
        if ( realDims.length == 1 ) {
            //  Special case, nothing to do, just create a simple cloned copy.
            clone( parent );
            return;
        }
        if ( realDims.length > 2 ) {
            //  Only dealing with 2D spectra at present. Stop now if
            //  not. However, cubes with a redundant axis are 2D.
            ndims = 0;
            for ( int i = 0; i < realDims.length; i++ ) {
                if ( realDims[i] != 1 ) {
                    if ( firstax == 0 ) {
                        firstax = i + 1;
                    }
                    ndims++;
                }
            }
            if ( ndims > 2 ) {
                //  nD
                throw new SplatException( "Can only collapse 2D spectra" );
            }
            else if ( ndims == 1 ) {
                //  1D, but with many redundant axes. Do nothing.
                clone( parent );
                return;
            }

            //  2D
            ndims = 0;
            dims = new int[2];
            for ( int i = 0; i < realDims.length; i++ ) {
                if ( realDims[i] != 1 ) {
                    dims[ndims] = realDims[i];
                    ndims++;
                }
            }
        }
        else {
            dims = realDims;
            ndims = 2;
            firstax = 1;
        }

        //  Get the FrameSet of the original dataset. This should reflect it's
        //  dimensionality and original storage condition (SpecDataImpl
        //  vectorize nD data by default).
        FrameSet parentFrameSet = parent.getFrameSet();

        //  Get the current Frame, this defines the coordinate systems. For a
        //  mixed dispersion -versus- distance measure this should be a
        //  CmpFrame that includes a SpecFrame.
        Frame current = parentFrameSet.getFrame( FrameSet.AST__CURRENT );

        //  Locate the dispersion axis. Should be the one that's a SpecFrame.
        //  Complication here is if this just happens to be reduntant too.
        int dispax = 1;
        int[] ddims = new int[1];
        for ( int i = 1; i < realDims.length; i++ ) {
            ddims[0] = i + 1;
            Frame frame2 = current.pickAxes( 1, ddims, null );
            if ( frame2 instanceof SpecFrame ) {
                if ( realDims[i] != 1 ) {
                    dispax = i + 1;
                    break;
                }
            }
            frame2.annul();
        }

        //  Perform the collapse of the values onto the first or second
        //  dimension. The new data and error arrays become the current
        //  values. XXX specify some combination algorithm, ranges would be
        //  trickier without user specified axis.
        if ( dispax == firstax ) {
            collapse1( parent, dims );
        }
        else {
            collapse2( parent, dims );
        }

        //  Create the FrameSet for this data.
        astref = ASTJ.extract1DFrameSet( parentFrameSet, dispax );

        //  Record any source of header information.
        if ( parent.getSpecDataImpl().isFITSHeaderSource() ) {
            headers =
               ((FITSHeaderSource)parent.getSpecDataImpl()).getFitsHeaders();
        }

        //  Retain the data units and label.
        dataUnits = parent.getAst().getRef().getC( "unit(2)" );
        dataLabel = parent.getAst().getRef().getC( "label(2)" );
    }

    /**
     * Collapse the 2D data array, plus optional errors, of a SpecData object
     * onto its first dimension to create a 1D array plus errors. The new
     * array and errors are set as the data values of this object.
     *
     * The combination uses a weighted mean.
     */
    protected void collapse1( SpecData parent, int[] dims )
    {
        //  Use the given dimensions. These should exclude any redundant axes.
        int dim1 = dims[0];
        int dim2 = dims[1];
        double[] d = parent.getYData();
        double[] e = parent.getYDataErrors();

        data = new double[dim1];
        if ( e != null ) {
            errors = new double[dim1];
        }
        else {
            errors = null;
        }
        int[] count = new int[dim1];
        double val = 0.0;
        double var = 0.0;
        int index = 0;

        if ( e == null ) {
            // No errors.
            for ( int j = 0; j < dim2; j++ ) {
                for ( int i = 0; i < dim1; i++ ) {

                    index = dim1 * j + i;

                    if ( d[index] != SpecData.BAD ) {
                        data[i] += d[index];
                        count[i]++;
                    }
                }
            }
            for ( int i = 0; i < dim1; i++ ) {
                if ( count[i] > 0 ) {
                    data[i] = data[i] / count[i];
                }
                else {
                    data[i] = SpecData.BAD;
                }
            }
        }
        else {
            for ( int j = 0; j < dim2; j++ ) {
                for ( int i = 0; i < dim1; i++ ) {

                    index = dim1 * j + i;

                    if ( d[index] != SpecData.BAD && e[index] != SpecData.BAD ) {

                        val = d[index];
                        var = 1.0 / ( e[index] * e[index] );
                        data[i] += var;
                        errors[i] += val * var;
                        count[i]++;
                    }
                }
            }
            for ( int i = 0; i < dim1; i++ ) {
                if ( count[i] > 0 && errors[i] != 0.0 ) {
                    data[i] = data[i] / errors[i];
                    errors[i] = Math.sqrt( 1.0 / errors[i] );
                }
                else {
                    data[i] = SpecData.BAD;
                    errors[i] = SpecData.BAD;
                }
            }
        }
    }

    /**
     * Collapse the 2D data array, plus optional errors, of a SpecData object
     * onto its second dimension to create a 1D array plus errors. The new
     * array and errors are set as the data values of this object.
     *
     * The combination uses a weighted mean.
     */
    protected void collapse2( SpecData parent, int[] dims )
    {
        //  Use the given dimensions. These should exclude any redundant axes.
        int dim1 = dims[0];
        int dim2 = dims[1];
        double[] d = parent.getYData();
        double[] e = parent.getYDataErrors();

        data = new double[dim2];
        if ( e != null ) {
            errors = new double[dim2];
        }
        else {
            errors = null;
        }

        double sum1 = 0.0;
        double sum2 = 0.0;
        double val = 0.0;
        double var = 0.0;
        int index = 0;

        if ( e == null ) {
            //  No errors.
            int ngood = 0;
            for ( int j = 0; j < dim2; j++ ) {
                sum1 = 0.0;
                for ( int i = 0; i < dim1; i++ ) {

                    //  Index into 1D vectorized arrays, note this will break
                    //  at a 2G index (16G array).
                    index = dim1 * j + i;

                    if ( d[index] != SpecData.BAD ) {
                        sum1 += d[index];
                        ngood++;
                    }
                }
                if ( ngood == 0 ) {
                    data[j] = SpecData.BAD;
                }
                else {
                    data[j] = sum1 / (double) ngood;
                }
            }
        }
        else {
            //  have errors.
            for ( int j = 0; j < dim2; j++ ) {
                sum1 = 0.0;
                sum2 = 0.0;
                for ( int i = 0; i < dim1; i++ ) {

                    index = dim1 * j + i;

                    if ( d[index] != SpecData.BAD && e[index] != SpecData.BAD ) {
                        val = d[index];
                        var = 1.0 / ( e[index] * e[index] );
                        sum1 += var;
                        sum2 += val * var;
                    }
                }
                if ( sum1 == 0.0 ) {
                    data[j] = SpecData.BAD;
                    errors[j] = SpecData.BAD;
                }
                else {
                    data[j] = sum2 / sum1;
                    errors[j] = Math.sqrt( 1.0 / sum1 );
                }
            }
        }
    }
}
