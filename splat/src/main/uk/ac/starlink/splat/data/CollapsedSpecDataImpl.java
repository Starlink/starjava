/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     14-SEP-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import java.io.IOException;
import java.util.Arrays;

import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.util.SplatException;

import uk.ac.starlink.ast.FrameSet;

/**
 * This class provides an implementation of {@link SpecDataImpl} that
 * reprocesses another 2D or 3D SpecData's implementation into a collapsed 1D
 * implementation. How the collapse happens is determined by the properties of
 * a {@link SpecDims} object. This should have a dispersion axis, onto which
 * the collapse happens, and for 3D data a collapse axis, along which collapse
 * onto the dispersion axis happens. For 3D data an axis value along the other
 * axis (not the dispersion or collapse axis) picks out the actual plane that
 * is collapsed.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see SpecData
 * @see SpecDims
 * @see SpecDataImpl
 */
public class CollapsedSpecDataImpl
    extends MEMSpecDataImpl
{
    private static int localCounter = 0;

    /**
     * Constructor for 2D data.
     *
     * @param parent the SpecData to collapse.
     * @param specDims describes the dimensions of the parent
     */
    public CollapsedSpecDataImpl( SpecData parent, SpecDims specDims )
        throws SplatException
    {
        super( parent.getFullName() );
        this.shortName = "Collapsed: " + shortName;
        this.parentImpl = parent.getSpecDataImpl();
        collapse( parent, specDims );
        initMetaData( parent );
    }

    /**
     * Construct an object that uses a 2D section of data from a 3D
     * spectrum. The section is defined using the dimensionality of the given
     * SpecDims instance, which defines the dispersion and collapse axes,
     * together with an index along the other axis that the collapse onto the
     * dispersion axis will occur at.
     *
     * @param parent the SpecData to collapse.
     */
    public CollapsedSpecDataImpl( SpecData parent, SpecDims specDims,
                                  int index )
        throws SplatException
    {
        super( parent.getFullName() );
        this.parentImpl = parent.getSpecDataImpl();
        collapseSection( parent, specDims, index );
        initMetaData( parent );
    }

    /**
     * Do a 2D SpecData collapse.
     */
    protected void collapse( SpecData parent, SpecDims specDims )
        throws SplatException
    {
        //  We only handle 2D, but this can be complicated when some
        //  dimensions are redundant.
        int sigdims = specDims.getNumSigDims();

        if ( sigdims == 1 ) {
            //  Special case, nothing to do, just create a simple cloned copy.
            clone( parent );
            return;
        }
        if ( sigdims > 2 ) {
            throw new SplatException
                ( "The method chosen can only collapse 2D spectra" );
        }

        //  2D so get the first proper axis, the two dimensions and choose the
        //  dispersion axis.
        int firstax = specDims.getFirstAxis( false );
        int[] dims = specDims.getSigDims();
        int dispax = specDims.getDispAxis( false );

        //  Perform the collapse of the values onto the first or second
        //  dimension. The new data and error arrays become the current
        //  values. XXX specify some combination algorithm, ranges would be
        //  trickier without user specified axis.
        double[] d = parent.getYData();
        double[] e = parent.getYDataErrors();
        if ( dispax == firstax ) {
            collapse1( d, e, dims );
        }
        else {
            collapse2( d, e, dims );
        }

        //  Create the FrameSet for this data. Note +1 for AST axes.
        astref = ASTJ.extract1DFrameSet( parent.getFrameSet(), dispax + 1 );
    }


    /**
     * Extra a 2D section from a 3D cube and collapse that.
     */
    public void collapseSection( SpecData parent, SpecDims specDims,
                                 int index )
        throws SplatException
    {
        //  Extract the section.
        int[] dims = specDims.getSigDims();
        int[] strides = specDims.getStrides( true );
        double[] d = parent.getYData();
        double[] e = parent.getYDataErrors();

        //  These are the retained axes.
        int collapsed = specDims.getFreeAxis( true );
        int dispax = specDims.getDispAxis( true );
        int axis1 = ( collapsed > dispax ) ? dispax : collapsed;
        int axis2 = ( collapsed > dispax ) ? collapsed : dispax;
        
        //  Set initial indices, the selected axis disappears.
        int[] indices = new int[3];
        indices[axis1] = 0;
        indices[axis2] = 0;
        int picked = specDims.getSelectAxis( true );
        indices[picked] = index;
        
        //  Extract 2D section.
        Object[] res = extractImage( dims, strides, d, e, axis1, axis2, 
                                     indices );
        int[] sdims = new int[2];
        dims[0] = dims[axis1];
        dims[1] = dims[axis2];

        //  Collapse onto the dispersion axis.
        if ( dispax < collapsed ) {
            collapse1( (double [])res[0], (double [])res[1], dims );
        }
        else {
            collapse2( (double [])res[0], (double [])res[1], dims );
        }

        //  Create the FrameSet for this data. Note +1 for AST axes.
        FrameSet frameSet = parent.getFrameSet();
        dispax = specDims.getDispAxis( false );
        astref = ASTJ.extract1DFrameSet( frameSet, dispax + 1 );

        //  Create a shortname that shows the original line position in world
        //  coordinates. Assumes index is base coordinate and we have the same
        //  number of input and output coordinates and their relationship is
        //  "obvious" (i.e. watch out for PermMaps).
        try {
            int ncoord_in = frameSet.getNaxes();
            picked = specDims.getSelectAxis( false );
            double[] in = new double[ncoord_in];
            for ( int i = 0; i < ncoord_in; i++ ) {
                if ( i == dispax ) {
                    in[i] = dims[specDims.realToSigAxis( i )] / 2;
                }
                else if ( i == picked ) {
                    in[i] = (double) index;
                }
                else {
                    in[i] = 1.0;
                }
            }
            double xyt[] = frameSet.tranN( 1, ncoord_in, in, true, ncoord_in );
            frameSet.norm( xyt );

            double coord = xyt[picked];
            String fcoord = frameSet.format( picked + 1, coord );
            this.shortName = "Collapsed (" + fcoord + "):" + shortName;
        }
        catch (Exception ex) {
            //  Failed, probably an AST-v-redundant axes issue.
            this.shortName = "Collapsed (" + localCounter + ") :" + shortName;
            localCounter++;
        }
    }

    /**
     * Initialise the local meta data from the parent SpecData.
     */
    protected void initMetaData( SpecData parent )
    {
        //  Record any source of header information.
        if ( parent.getSpecDataImpl().isFITSHeaderSource() ) {
            headers =
               ((FITSHeaderSource)parent.getSpecDataImpl()).getFitsHeaders();
        }

        //  Retain the data units and label.
        setDataUnits( parent.getAst().getRef().getC( "unit(2)" ) );
        setDataLabel( parent.getAst().getRef().getC( "label(2)" ) );
    }

    /**
     * Collapse a 2D data array, plus optional errors, onto its first
     * dimension to create a 1D array plus errors. The new array and errors
     * are set as the data values of this object.
     *
     * The combination uses a weighted mean.
     */
    protected void collapse1( double[] d, double[] e, int[] dims )
    {
        //  Use the given dimensions. These should exclude any redundant axes.
        int dim1 = dims[0];
        int dim2 = dims[1];

        data = new double[dim1];
        if ( e != null ) {
            errors = new double[dim1];
        }
        else {
            errors = null;
        }
        int[] count = new int[dim1];
        double invar = 0.0;
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

                    if ( d[index] != SpecData.BAD && 
                         e[index] != SpecData.BAD ) {
                        invar = 1.0 / ( e[index] * e[index] );
                        data[i] += d[index] * invar;
                        errors[i] += invar;
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
     * Collapse a 2D data array, plus optional errors, onto its second
     * dimension to create a 1D array plus errors. The new array and errors
     * are set as the data values of this object.
     *
     * The combination uses a weighted mean.
     */
    protected void collapse2( double[] d, double[] e, int[] dims )
    {
        //  Use the given dimensions. These should exclude any redundant axes.
        int dim1 = dims[0];
        int dim2 = dims[1];

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
        double invar = 0.0;
        int index = 0;

        if ( e == null ) {
            //  No errors.
            int ngood = 0;
            for ( int j = 0; j < dim2; j++ ) {
                sum1 = 0.0;
                ngood = 0;
                for ( int i = 0; i < dim1; i++ ) {

                    //  Index into 1D vectorized arrays, note this will break
                    //  at a 2Gb index (16Gb array).
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

                    if ( d[index] != SpecData.BAD && 
                         e[index] != SpecData.BAD ) {
                        val = d[index];
                        invar = 1.0 / ( e[index] * e[index] );
                        sum1 += invar;
                        sum2 += val * invar;
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

    /**
     * Extract a 2D section of data, plus optional errors from
     * a cube of data.
     */
    protected Object[] extractImage( int[] dims, int[] strides, double[] d, 
                                     double[] e, int axis1, int axis2, 
                                     int[] indices )
    {
        Object[] results = new Object[2];

        //  Allocate arrays for extracted data.
        int size = dims[axis1] * dims[axis2];
        double[] data = new double[size];
        double[] errors = null;
        if ( e != null ) {
            errors = new double[size];
        }
        results[0] = data;
        results[1] = errors;

        //  Extract data.
        int offset;
        if ( e == null ) {
            int k = 0;
            for ( int i = 0; i < dims[axis2]; i++ ) {
                indices[axis2] = i;
                for ( int j = 0; j < dims[axis1]; j++ ) {
                    indices[axis1] = j;
                    offset = 0;
                    for ( int l = 0; l < indices.length; l++ ) {
                        offset += strides[l] * indices[l];
                    }
                    data[k] = d[offset];
                    k++;
                }
            }
        }
        else {
            int k = 0;
            for ( int i = 0; i < dims[axis2]; i++ ) {
                indices[axis2] = i;
                for ( int j = 0; j < dims[axis1]; j++ ) {
                    indices[axis1] = j;
                    offset = 0;
                    for ( int l = 0; l < indices.length; l++ ) {
                        offset += strides[l] * indices[l];
                    }
                    data[k] = d[offset];
                    errors[k] = e[offset];
                    k++;
                }
            }
        }
        return results;
    }
}
