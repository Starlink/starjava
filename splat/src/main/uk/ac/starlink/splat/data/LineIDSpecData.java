/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     11-APR-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import java.awt.Rectangle;

import uk.ac.starlink.ast.Grf;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.grf.DefaultGrf;
import uk.ac.starlink.ast.grf.DefaultGrfState;
import uk.ac.starlink.splat.util.SplatException;

/**
 * A type of EditableSpecData that draws a string at a "spectral"
 * position. The expected use of these facilities is for identifying 
 * line positions, (TODO: as a vertical bar marker, plus ) as a string
 * (TODO: at some orientation and scale).
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
public class LineIDSpecData 
    extends EditableSpecData
{
    /**
     * Reference to the LineIDSpecDataImpl.
     */
    protected transient LineIDSpecDataImpl lineIDImpl = null;

    /**
     * Reference to an associated SpecData.
     */
    protected transient SpecData specData = null;

    /** TODO: Undoable support */

    /**
     * Constructor, takes a LineIDSpecDataImpl.
     */
    public LineIDSpecData( LineIDSpecDataImpl lineIDImpl ) 
        throws SplatException
    {
        super( lineIDImpl );
        this.lineIDImpl = lineIDImpl;
        useInAutoRanging = false; // by default.
    }

    /**
     * Constructor, takes a LineIDSpecDataImpl and a SpecData
     * instance. The SpecData instance is used to supply the relative
     * positions of the labels.
     */
    public LineIDSpecData( LineIDSpecDataImpl lineIDImpl, 
                           SpecData specData )
        throws SplatException
    {
        this( lineIDImpl );
        setSpecData( specData );
    }

    /**
     * Set the SpecData used to defined the relative positioning for
     * the labels.
     */
    public void setSpecData( SpecData specData )
    {
        this.specData = specData;
    }
    
    /**
     * Get the SpecData in use. If none then null is returned.
     */
    public SpecData getSpecData()
    {
        return specData;
    }

    /**
     * Get the labels.
     */
    public String[] getLabels()
    {
        return lineIDImpl.getLabels();
    }

    /**
     * Return if the backing implementation has valid positions for
     * the labels.
     */
    public boolean haveDataPositions()
    {
        return lineIDImpl.haveDataPositions();
    }

    /**
     * Draw the "spectrum". In this case it means draw the line id
     * strings. 
     */
    public void drawSpec( Grf grf, Plot plot, double[] limits )
    {
        String[] labels = getLabels();
        
        //  Need to generate positions for placing the labels. The
        //  various schemes for this are use any positions read from
        //  the implementation, use the positions from a SpecData and
        //  finally put them along the given limits.
        double[] xypos = new double[xPos.length * 2];
        double[] ypos = yPos;
        if ( ! haveDataPositions() ) {
            if ( specData != null ) {
                ypos = specData.getYData();
            }
            else {
                ypos = null;
            }
        }
        if ( ypos != null ) {
            for ( int i = 0, j = 0; j < xPos.length; j++, i += 2 ) {
                xypos[i] = xPos[j];
                xypos[i + 1] = ypos[j];
            }
        }
        else {
            for ( int i = 0, j = 0; j < xPos.length; j++, i += 2 ) {
                xypos[i] = xPos[j];
                xypos[i + 1] = limits[1]; // or [3]?
            }
        }

        //  Transform positions into graphics coordinates.
        double[][] xygpos = astJ.astTran2( (Mapping) plot, xypos, false );

        //  Do the same for the clip region.
        Rectangle cliprect = null;
        if ( limits != null ) {
            double[][] clippos = astJ.astTran2( plot, limits, false );
            cliprect =
                new Rectangle( (int) clippos[0][0],
                               (int) clippos[1][1],
                               (int) ( clippos[0][1] - clippos[0][0] ),
                               (int) ( clippos[1][0] - clippos[1][1] ) );
        }

        //  Need to do this for text...
        DefaultGrf defaultGrf = (DefaultGrf) grf;
        DefaultGrfState oldState = setGrfAttributes( defaultGrf );

        defaultGrf.setClipRegion( cliprect );

        for ( int i = 0; i < labels.length; i++ ) {
            defaultGrf.text( labels[i], xygpos[0][i], xygpos[1][i], 
                             "C", 1.0, 0.0 );
        }

        defaultGrf.setClipRegion( null );

        resetGrfAttributes( defaultGrf, oldState );
    }
}
