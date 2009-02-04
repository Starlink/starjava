/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 * Copyright (C) 2009 Science and Technology Facilities Council
 *
 *  History:
 *     16-SEP-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.SpecFrame;

/**
 * Utility class for determing various useful characteristics about the
 * underlying dimensionality of a SpecData object. It also provides for the
 * choice of a dispersion axis and another axis, if appropriate, that
 * positions are chosen along (collapses should start at this postion, down
 * onto the dispersion axis).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public final class SpecDims
{
    private SpecData specData = null;
    private int[] realDims = null;
    private int[] realStrides = null;
    private int[] sigStrides = null;
    private int ndims = 0;
    private int firstaxis = 0;
    private int dispax = -1;
    private int selectax = -1;

    /**
     * Constructor.
     */
    public SpecDims( SpecData specData )
    {
        setSpecData( specData );
    }

    /**
     * Set the SpecData object used by this instance.
     */
    public void setSpecData( SpecData specData )
    {
        this.specData = specData;

        //  Get the real dimensionality of the original data.
        realDims = specData.getSpecDataImpl().getDims();

        //  Determine number of significant dimensions.
        ndims = 0;
        firstaxis = -1;
        for ( int i = 0; i < realDims.length; i++ ) {
            if ( realDims[i] != 1 ) {
                if ( firstaxis == -1 ) {
                    firstaxis = i;
                }
                ndims++;
            }
        }
        if ( firstaxis == -1 ) {
            //  All redundant.
            firstaxis = 0;
        }

        //  Don't know the dispersion axis yet.
        dispax = -1;

        // Or the non-dispersion axis we're going to select along.
        selectax = -1;
    }

    /**
     * Return the number of significant dimensions.
     */
    public int getNumSigDims()
    {
        return ndims;
    }

    /**
     * Return the index of the first significant axis.
     */
    public int getFirstAxis( boolean exclude )
    {
        if ( exclude ) {
            return realToSigAxis( firstaxis );
        }
        return firstaxis;
    }

    /**
     * Return the indices of any significant dimensions for the underlying
     * data of a SpecData. So the length of the return array indicates the
     * number of significant dimensions and each element the index of the
     * origin dimension.
     */
    public int[] getSigIndices()
    {
        int[] dims = new int[ndims];

        int count = 0;
        for ( int i = 0; i < realDims.length; i++ ) {
            if ( realDims[i] != 1 ) {
                dims[count++] = i;
            }
        }
        return dims;
    }

    /**
     * Return the significant dimensions for the underlying
     * data of a SpecData. On output the length of the return array indicates
     * the number of significant dimensions and each element is set the the
     * length of that dimension.
     */
    public int[] getSigDims()
    {
        int[] dims = new int[ndims];
        ndims = 0;
        for ( int i = 0; i < realDims.length; i++ ) {
            if ( realDims[i] != 1 ) {
                dims[ndims++] = realDims[i];
            }
        }
        return dims;
    }

    /**
     * Set the dispersion axis. If not set (which is the default condition)
     * then the a value will be determined by looking through the AST frameset
     * of the associated SpecData for a spectral axis. To get that behaviour
     * back just set the value to -1. If exclude is true then the value will
     * be taken as a significant axis.
     */
    public void setDispAxis( int dispax, boolean exclude )
    {
        if ( exclude && dispax != -1 ) {
            this.dispax = sigToRealAxis( dispax );
        }
        else {
            this.dispax = dispax;
        }
    }

    /**
     * Get the dispersion axis of the data backing a SpecData. If not found,
     * or the dispersion axis isn't significant then 1 is returned.
     * All dimension are counted unless exclude is set true.
     */
    public int getDispAxis( boolean exclude )
    {
        //  Don't repeatably determine this, the guess will not change unless
        //  the SpecData changes.
        if ( dispax == -1 ) {
            dispax = 0;

            //  Get the current Frame, this defines the coordinate
            //  systems. For a mixed dispersion -versus- distance measure this
            //  should be a CmpFrame or SpecFluxFrame that includes a
            //  SpecFrame defining the dispersion axis.
            Frame current =
                specData.getFrameSet().getFrame( FrameSet.AST__CURRENT );

            int[] ddims = new int[1];
            for ( int i = 1; i < realDims.length; i++ ) {
                ddims[0] = i + 1;
                Frame frame2 = current.pickAxes( 1, ddims, null );
                if ( frame2 instanceof SpecFrame ) {
                    if ( realDims[i] != 1 ) {
                        dispax = i;
                        break;
                    }
                }
            }
        }
        if ( exclude ) {
            return realToSigAxis( dispax );
        }
        return dispax;
    }

    /**
     * Set the non-dispersion axis used to select a position. Any collapses
     * should happen along the other non-dispersion axis, onto the dispersion
     * axis, starting at an index along this axis.. If not set a default axis
     * will be picked from amongst the significant axes. If exclude is true
     * then the index will be taken as meaning a significant axis.
     */
    public void setSelectAxis( int selectax, boolean exclude )
    {
        if ( exclude && selectax != -1 ) {
            this.selectax = sigToRealAxis( selectax );
        }
        else {
            this.selectax = selectax;
        }
    }

    /**
     * Return the axis that any positions used to indicate where to start a
     * collapse should be defined along. If not explicitly set the first axis
     * which isn't the dispersion axis will be chosen. The axis numbers
     * returned do not exclude non-significant dimensions, unless exclude is
     * set to true.
     */
    public int getSelectAxis( boolean exclude )
    {
        if ( selectax == -1 ) {
            //  No choice made yet. Invoke getDispAxis to make sure of
            //  initialisation.
            int sigaxis = getDispAxis( false );
            for ( int i = 0; i < realDims.length; i++ ) {
                if ( i != sigaxis && realDims[i] != 1 ) {
                    selectax = i;
                    break;
                }
            }
        }

        if ( exclude ) {
            return realToSigAxis( selectax );
        }
        return selectax;
    }

    /**
     * Return a significant axis which isn't the dispersion axis, or the
     * selected axis. For a 3D cube this should be the remaining axis. If none
     * can be found this returns -1.
     */
    public int getFreeAxis( boolean exclude )
    {
        int freeax = -1;
        int[] dims = getSigDims();
        if ( dims.length >= 3 ) {
            int dispax = getDispAxis( true );
            int selectax = getSelectAxis( true );
            for ( int i = 0; i < dims.length; i++ ) {
                if ( dispax != i && selectax != i ) {
                    freeax = i;
                }
            }
        }
        if ( exclude ) {
            return realToSigAxis( freeax );
        }
        return freeax;
    }


    /**
     * Convert a real axis number into a significant dimensions axis number.
     */
    public int realToSigAxis( int realIndex )
    {
        int[] indices = getSigIndices();
        for ( int i = 0; i < indices.length; i++ ) {
            if ( indices[i] == realIndex ) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Convert a significant dimensions axis number into a real axis index.
     */
    public int sigToRealAxis( int sigIndex )
    {
        return getSigIndices()[sigIndex];
    }

    /**
     * Return a set of column major (Fortran/FITS/NDF) order strides for
     * stepping around a vectorised array of the dimensionality we're
     * representing.
     * <p>
     * Once returned you can access the array element data(i,j,k) element, as
     * in the following code segement:
     *
     *    int[] indices = new int[3];
     *    indices[0] = i;
     *    indices[1] = j;
     *    indices[2] = k;
     *    int offset = 0;
     *    for ( int l = 0; l < indices.length; l++ ) {
     *       offset += strides[l] * indices[l];
     *    }
     *    value = data[offset];
     *
     * @param exclude whether to exclude non-significant dimensions (shouldn't
     *                matter in practice).
     * @return the strides for each dimension in column major order.
     *
     */
    public int[] getStrides( boolean exclude )
    {
        int[] strides = null;
        if ( exclude ) {
            if ( sigStrides == null ) {
                int[] sigDims = getSigDims();
                sigStrides = new int[sigDims.length];
                int count = 1;
                for ( int i = 0; i< sigDims.length; i++ ) {
                    sigStrides[i] = count;
                    count *= sigDims[i];
                }
            }
            strides = sigStrides;
        }
        else {
            if ( realStrides == null ) {
                realStrides = new int[realDims.length];
                int count = 1;
                for ( int i = 0; i< realDims.length; i++ ) {
                    realStrides[i] = count;
                    count *= realDims[i];
                }
            }
            strides = realStrides;
        }
        return strides;
    }
}
