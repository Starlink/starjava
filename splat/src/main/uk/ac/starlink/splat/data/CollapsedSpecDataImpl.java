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
        //  Get the FrameSet of the original dataset. This should reflect it's
        //  dimensionality and original storage condition.
        FrameSet parentFrameSet = parent.getFrameSet();

        //  Get the current Frame, this defines the coordinate systems. For a
        //  mixed dispersion -versus- distance measure this should be a
        //  CmpFrame that includes a SpecFrame.
        Frame current = parentFrameSet.getFrame( FrameSet.AST__CURRENT );
        int naxis = current.getNaxes();

        //  Only dealing with 2D spectra at present. Stop now if not.
        if ( naxis > 2 ) {
            throw new SplatException( "Can only collapse 2D spectra" );
        }
        else if ( naxis == 1 ) {
            //  Special case, nothing to do, just create a simple cloned copy.
            clone( parent );
            return;
        }

        //  Locate the dispersion axis. Should be the one that's a SpecFrame.
        int dispax = 1;
        Frame frame2 = current.pickAxes( 1, new int[]{ 2 }, null );
        if ( frame2 instanceof SpecFrame ) {
            dispax = 2;
        }

        //  Perform the collapse of the values onto the first or second
        //  dimension. The new data and error arrays become the current
        //  values. XXX specify some combination algorithm.
        if ( dispax == 1 ) {
            collapse1( parent );
        }
        else {
            collapse2( parent );
        }

        //  Create the FrameSet for this data.
        astref = ASTJ.extract1DFrameSet( parentFrameSet, dispax );

        //  Record any source of header information.
        if ( parent.getSpecDataImpl().isFITSHeaderSource() ) {
            headers =
               ((FITSHeaderSource)parent.getSpecDataImpl()).getFitsHeaders();
        }

        //  Retain the data units and label.
        dataUnits = parentFrameSet.getC( "unit(2)" );
        dataLabel = parentFrameSet.getC( "label(2)" );
    }

    /**
     * Collapse the 2D data array, plus optional errors, of a SpecData object
     * onto its first dimension to create a 1D array plus errors. The new
     * array and errors are set as the data values of this object.
     *
     * The combination uses a weighted mean.
     */
    protected void collapse1( SpecData parent )
    {
        SpecDataImpl impl = parent.getSpecDataImpl();
        int[] dims = impl.getDims();
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
    protected void collapse2( SpecData parent )
    {
        //  Need to access the raw dimensions.
        SpecDataImpl impl = parent.getSpecDataImpl();
        int[] dims = impl.getDims();
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
