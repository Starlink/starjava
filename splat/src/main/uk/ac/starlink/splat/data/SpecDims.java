/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
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
 * underlying dimensionality of a SpecData object.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public final class SpecDims
{
    private SpecData specData = null;
    private int[] realDims = null;
    private int ndims = 0;
    private int firstaxis = 0;
    private int dispax = -1;
    private int collapseax = -1;

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

        // Or the non-dispersion axis we're going to collapse along.
        collapseax = -1;
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
    public int getFirstAxis()
    {
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
        if ( exclude ) {
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
            //  should be a CmpFrame that includes a SpecFrame.
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
                frame2.annul();
            }
        }
        if ( exclude ) {
            return realToSigAxis( dispax );
        }
        return dispax;
    }

    /**
     * Set the non-dispersion axis. Collapses will happen along this axis,
     * onto the dispersion axis. If not set a default axis will be picked from
     * amongst the significant axes. If exclude is true then the index will
     * be taken as meaning a significant axis.
     */
    public void setCollapseAxis( int collapseax, boolean exclude )
    {
        if ( exclude ) {
            this.collapseax = sigToRealAxis( collapseax );
        }
        else {
            this.collapseax = collapseax;
        }
    }

    /**
     * Return the axis that any collapse should occur along. If not explicitly
     * set the first axis which isn't the dispersion axis will be chosen. The
     * axis numbers returned do not exclude non-significant dimensions, unless
     * exclude is set to true.
     */
    public int getNonDispAxis( boolean exclude )
    {
        if ( collapseax == -1 ) {
            //  No choice made yet. Invoke getDispAxis to make sure of
            //  initialisation.
            int sigaxis = getDispAxis( false );
            for ( int i = 0; i < realDims.length; i++ ) {
                if ( i != sigaxis && realDims[i] != 1 ) {
                    collapseax = i;
                    break;
                }
            }
        }

        if ( exclude ) {
            return realToSigAxis( collapseax );
        }
        return collapseax;
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
}
