/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-SEP-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import java.io.IOException;
import java.util.Arrays;

import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.UnitUtilities;

import uk.ac.starlink.array.ArrayArrayImpl;
import uk.ac.starlink.array.BridgeNDArray;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.WindowArrayImpl;
import uk.ac.starlink.ast.FrameSet;

/**
 * This class provides an SpecDataImpl that extracts a 1D section from another
 * 2D or 3D SpecData's implementation.  The section runs along the selected
 * dispersion axis.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see SpecData
 * @see SpecDataImpl
 */
public class ExtractedSpecDataImpl
    extends MEMSpecDataImpl
{
    /**
     * Extract a line from a 2D image. The line lies along the dispersion axis
     * (defined by the SpecDims object) and is selected by an index along the
     * non-dispersion axis.
     *
     * @param parent the SpecData to extract a line from.
     * @param index the index of the line to extract.
     */
    public ExtractedSpecDataImpl( SpecData parent, SpecDims specDims,
                                  int index )
        throws SplatException
    {
        super( parent.getFullName() );
        this.parentImpl = parent.getSpecDataImpl();
        extract2D( parent, specDims, index );
        initMetaData( parent );
    }

    /**
     * Extract a line from a 3D cube. The line lies along the dispersion axis
     * (defined by the SpecDims object) and is selected by the two coordinates
     * given. These should be of the other two significant axis in order.
     *
     * @param parent the SpecData to collapse.
     */
    public ExtractedSpecDataImpl( SpecData parent, SpecDims specDims,
                                  int index1, int index2 )
        throws SplatException
    {
        super( parent.getFullName() );
        this.parentImpl = parent.getSpecDataImpl();
        extract3D( parent, specDims, index1, index2 );
        initMetaData( parent );
    }

    /**
     * Do a 2D extract.
     */
    protected void extract2D( SpecData parent, SpecDims specDims, int index )
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
                ( "The method chosen can only extract from 2D spectra" );
        }

        //  2D so get the first proper axis, the two dimensions and choose the
        //  dispersion axis.
        int firstax = specDims.getFirstAxis( false );
        int[] dims = specDims.getSigDims();
        int dispax = specDims.getDispAxis( false );

        //  Perform the extraction of the values along the first or second
        //  dimension. The new data and error arrays become the current
        //  values.
        double[] d = parent.getYData();
        double[] e = parent.getYDataErrors();
        if ( dispax == firstax ) {
            extract2D1( d, e, index, dims );
        }
        else {
            extract2D2( d, e, index, dims );
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
        this.shortName = "Extracted (" + fcoord + "):" + shortName;
    }


    /**
     * Extract from a 3D cube.
     */
    public void extract3D( SpecData parent, SpecDims specDims,
                           int index1, int index2 )
        throws SplatException
    {
        //  Implementation notes:
        //  Use NDArrays to pick out (index1:index1,*,*), (*,index1:index1,*)
        //  or (*,*,index1:index1) 2D section. Index1 is along the select axis
        //  held the by SpecDims object, this cannot be the dispersion
        //  axis.

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
        //  The section is identified by making the select axis offset equal
        //  to the given index along that axis (plus 1) and the size of that
        //  dimension is set to 1.
        int selectaxis = specDims.getSelectAxis( false );
        long[] origin = new long[dims.length];
        Arrays.fill( origin, 1L );
        origin[selectaxis] = (long) index1 + 1;
        ldims[selectaxis] = 1L;

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
            if ( i != selectaxis ) {
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

        //  Now extract the spectrum.
        int dispax = specDims.getDispAxis( false );
        int freeaxis = specDims.getFreeAxis( false );
        if ( dispax < freeaxis ) {
            extract2D1( ds, es, index2, wdims );
        }
        else {
            extract2D2( ds, es, index2, wdims );
        }

        //  Create the FrameSet for this data. Note +1 for AST axes.
        FrameSet frameSet = parent.getFrameSet();
        astref = ASTJ.extract1DFrameSet( frameSet, dispax + 1 );

        //  Create a shortname that shows the original line position in world
        //  coordinates. Assumes indices are in base coordinate and we have
        //  the same number of input and output coordinates and their
        //  relationship is "obvious" (i.e. watch out for PermMaps).
        int ncoord_in = frameSet.getNaxes();
        double[] in = new double[ncoord_in];
        for ( int i = 0; i < ncoord_in; i++ ) {
            if ( i == dispax ) {
                in[i] = dims[specDims.realToSigAxis( i )] / 2;
            }
            else if ( i == selectaxis ) {
                in[i] = (double) index1;
            }
            else if ( i == freeaxis ) {
                in[i] = (double) index2;
            }
            else {
                in[i] = 1.0;
            }
        }
        double xyt[] = frameSet.tranN( 1, ncoord_in, in, true, ncoord_in );
        frameSet.norm( xyt );
        String scoord = frameSet.format( selectaxis + 1, xyt[selectaxis] );
        String fcoord = frameSet.format( freeaxis + 1, xyt[freeaxis] );
        if ( selectaxis < freeaxis ) {
            this.shortName = "Extracted (" + scoord + "," + fcoord + "): " +
                             shortName;
        }
        else {
            this.shortName = "Extracted (" + fcoord + "," + scoord + "): " +
                             shortName;
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
        dataUnits = UnitUtilities.fixUpUnits
            ( parent.getAst().getRef().getC( "unit(2)" ) );
        dataLabel = parent.getAst().getRef().getC( "label(2)" );
    }

    /**
     * Extract a line of data, plus optional errors from long the first
     * dimension at the position along the second dimension. The new array and
     * errors are set as the data values of this object.
     */
    protected void extract2D1( double[] d, double[] e, int index, int[] dims )
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
        int offset = 0;

        if ( e == null ) {
            // No errors.
            for ( int i = 0; i < dim1; i++ ) {
                offset = dim1 * index + i;
                data[i] = d[offset];
            }
        }
        else {
            for ( int i = 0; i < dim1; i++ ) {
                offset = dim1 * index + i;
                data[i] = d[offset];
                errors[i] = e[offset];
            }
        }
    }

    /**
     * Extract a line of data, plus optional errors from long the second
     * dimension at the position along the first dimension. The new array and
     * errors are set as the data values of this object.
     *
     * The combination uses a weighted mean.
     */
    protected void extract2D2( double[] d, double[] e, int index, int[] dims )
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

        int offset = 0;

        if ( e == null ) {
            //  No errors.
            for ( int j = 0; j < dim2; j++ ) {
                offset = dim1 * j + index;
                data[j] = d[offset];
            }
        }
        else {
            for ( int j = 0; j < dim2; j++ ) {
                offset = dim1 * j + index;
                data[j] = d[offset];
                errors[j] = e[offset];
            }
        }
    }
}
