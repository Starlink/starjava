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
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.ArrayArrayImpl;
import uk.ac.starlink.array.WindowArrayImpl;

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
        //  Implementation notes:
        //  Use NDArrays to pick out (index:index,*,*), (*,index:index,*) or
        //  (*,*,index:index) 2D section. Index is along the picked axis held
        //  the by SpecDims object, this cannot be the dispersion axis. The
        //  collapse is along the other axis and onto the dispersion axis.

        //  Copy int[] significant dims to long[] for NDArray API.
        int[] dims = specDims.getSigDims();
        long[] ldims = new long[dims.length];
        for ( int i = 0; i < dims.length; i++ ) {
            ldims[i] = (long) dims[i];
        }

        //  Create an NDArray of the current data, with the current real shape
        //  (the significant dimensions).
        double[] d = parent.getYData();
        double[] e = parent.getYDataErrors();
        OrderedNDShape baseShape = new OrderedNDShape( ldims, null );

        // Data.
        ArrayArrayImpl adImpl =
            new ArrayArrayImpl( d, baseShape, new Double( SpecData.BAD ) );
        BridgeNDArray fullDNDArray = new BridgeNDArray( adImpl );

        // Errors.
        BridgeNDArray fullENDArray = null;
        if ( e != null ) {
            ArrayArrayImpl aeImpl =
                new ArrayArrayImpl( e, baseShape, new Double( SpecData.BAD ) );
            fullENDArray = new BridgeNDArray( aeImpl );
        }

        //  Select the image section from the cube using a WindowArrayImpl.
        //  The section is identified by making the picked axis offset equal
        //  to the given index along that axis (plus 1) and the size of that
        //  dimension is set to 1.
        int picked = specDims.getSelectAxis( false );
        long[] origin = new long[dims.length];
        Arrays.fill( origin, 1L );
        origin[picked] = (long) index + 1;
        ldims[picked] = 1L;

        OrderedNDShape sectionShape =
            new OrderedNDShape( origin, ldims, baseShape.getOrder() );

        //  Data.
        WindowArrayImpl wdImpl = new WindowArrayImpl( fullDNDArray,
                                                      sectionShape );
        BridgeNDArray winDNDArray = new BridgeNDArray( wdImpl );

        //  Errors.
        BridgeNDArray winENDArray = null;
        if ( e != null ) {
            WindowArrayImpl weImpl = new WindowArrayImpl( fullENDArray,
                                                          sectionShape );
            winENDArray = new BridgeNDArray( weImpl );
        }

        //  Need the actual dimensions of section for collapse operation.
        int[] wdims = new int[dims.length - 1];
        int size = 1;
        for ( int i = 0, j = 0; i < dims.length; i++ ) {
            if ( i != picked ) {
                wdims[j++] = dims[i];
                size *= dims[i];
            }
        }

        //  Now access the section data.
        //  XXX look at using pixel iterators for more efficient access?
        double[] ds = new double[size];
        double[] es = null;
        if ( e != null ) {
            es = new double[size];
        }
        try {
            winDNDArray.getAccess().read( ds, 0, size );
            if ( e != null ) {
                winENDArray.getAccess().read( es, 0, size );
            }
        }
        catch (IOException io) {
            io.printStackTrace();
            throw new SplatException( io );
        }

        //  Collapse onto the dispersion axis. The collapsed axis is the not
        //  picked one.
        int collapsed = specDims.getFreeAxis( true );
        int dispax = specDims.getDispAxis( true );
        if ( dispax < collapsed ) {
            collapse1( ds, es, wdims );
        }
        else {
            collapse2( ds, es, wdims );
        }

        //  Create the FrameSet for this data. Note +1 for AST axes.
        FrameSet frameSet = parent.getFrameSet();
        astref = ASTJ.extract1DFrameSet( frameSet, dispax + 1 );

        //  Create a shortname that shows the original line position in world
        //  coordinates. Assumes index is base coordinate and we have the same
        //  number of input and output coordinates and their relationship is
        //  "obvious" (i.e. watch out for PermMaps).
        int ncoord_in = frameSet.getNaxes();
        int selectaxis = specDims.getSelectAxis( false );
        double[] in = new double[ncoord_in];
        for ( int i = 0; i < ncoord_in; i++ ) {
            if ( i == dispax ) {
                in[i] = dims[specDims.realToSigAxis( i )] / 2;
            }
            else if ( i == selectaxis ) {
                in[i] = (double) index;
            }
            else {
                in[i] = 1.0;
            }
        }
        double xyt[] = frameSet.tranN( 1, ncoord_in, in, true, ncoord_in );
        frameSet.norm( xyt );

        double coord = xyt[selectaxis];
        String fcoord = frameSet.format( selectaxis + 1, coord );
        this.shortName = "Collapsed (" + fcoord + "):" + shortName;
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
        dataUnits = parent.getAst().getRef().getC( "unit(2)" );
        dataLabel = parent.getAst().getRef().getC( "label(2)" );
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
        double var = 0.0;
        int index = 0;

        if ( e == null ) {
            //  No errors.
            int ngood = 0;
            for ( int j = 0; j < dim2; j++ ) {
                sum1 = 0.0;
                ngood = 0;
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
