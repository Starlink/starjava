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
    private static int localCounter = 0;

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
     * @param parent the SpecData to extract
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

        //  Get dimensionality and the selected dispersion axis.
        int[] dims = specDims.getSigDims();
        int dispax = specDims.getDispAxis( false );

        //  Define position of the extraction (index is along select axis).
        int selectaxis = specDims.getSelectAxis( false );
        int[] indices = new int[2];
        indices[dispax] = 0;
        indices[selectaxis] = index;

        //  Extract the spectrum.
        int[] strides = specDims.getStrides( false );
        double[] d = parent.getYData();
        double[] e = parent.getYDataErrors();
        extractSpectrum( dims, strides, d, e, dispax, indices );

        //  Create the FrameSet for this data. Note +1 for AST axes.
        FrameSet frameSet = parent.getFrameSet();
        astref = ASTJ.extract1DFrameSet( frameSet, dispax + 1 );

        //  Create a shortname that shows the original line position in world
        //  coordinates. Assumes index is base coordinate and we have the same
        //  number of input and output coordinates and their relationship is
        //  "obvious" (i.e. watch out for PermMaps).
        try {
            int ncoord_in = frameSet.getNaxes();
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
        catch (Exception ex) {
            //  Failed, probably an AST-v-redundant axes issue.
            this.shortName = "Extracted (" + localCounter + ") :" + shortName;
            localCounter++;
        }
    }


    /**
     * Extract a data line from a 3D cube and use that as the underlying
     * spectrum.
     */
    public void extract3D( SpecData parent, SpecDims specDims,
                           int index1, int index2 )
        throws SplatException
    {
        //  Get dimensionality and the selected dispersion axis.
        int[] dims = specDims.getSigDims();
        int dispax = specDims.getDispAxis( false );

        //  Set the position to extract, these are index1 and index2 along the
        //  select and free axes.
        int selectaxis = specDims.getSelectAxis( false );
        int freeaxis = specDims.getFreeAxis( false );
        int[] indices = new int[3];
        indices[dispax] = 0;
        indices[selectaxis] = index1;
        indices[freeaxis] = index2;

        //  Extract the spectrum.
        int[] strides = specDims.getStrides( false );
        double[] d = parent.getYData();
        double[] e = parent.getYDataErrors();
        extractSpectrum( dims, strides, d, e, dispax, indices );

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
        setDataUnits( parent.getAst().getRef().getC( "unit(2)" ) );
        setDataLabel( parent.getAst().getRef().getC( "label(2)" ) );
    }

    /**
     * Extract a line of data, plus optional errors from along a given axis,
     * at the position defined by the values of indices for the other
     * dimensions. The new array and errors are set as the data values of this
     * object.
     */
    protected void extractSpectrum( int[] dims, int[] strides,
                                    double[] d, double[] e,
                                    int axis, int[] indices )
    {
        //  Allocate arrays for extracted data.
        data = new double[dims[axis]];
        if ( e != null ) {
            errors = new double[dims[axis]];
        }
        else {
            errors = null;
        }

        //  Extract data.
        int offset;
        if ( e == null ) {
            for ( int i = 0; i < data.length; i++ ) {
                offset = 0;
                indices[axis] = i;
                for ( int j = 0; j < indices.length; j++ ) {
                    offset += strides[j] * indices[j];
                }
                data[i] = d[offset];
            }
        }
        else {
            for ( int i = 0; i < data.length; i++ ) {
                offset = 0;
                indices[axis] = i;
                for ( int j = 0; j < indices.length; j++ ) {
                    offset += strides[j] * indices[j];
                }
                data[i] = d[offset];
                errors[i] = e[offset];
            }
        }
    }
}

