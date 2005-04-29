/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     11-APR-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import java.awt.Rectangle;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.IOException;
import java.util.Arrays;

import uk.ac.starlink.ast.Grf;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.grf.DefaultGrf;
import uk.ac.starlink.ast.grf.DefaultGrfState;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.ast.ASTChannel;

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
    implements Serializable
{
    /**
     * Reference to an associated SpecData.
     */
    protected transient SpecData specData = null;

    /**
     * Reference to a Mapping that transforms coordinates and data value
     * (i.e. a DATAPLOT-like mapping) from the associated SpecData to this
     * spectrum.
     */
    protected transient Mapping specDataMapping = null;

    /**
     * Serialised form of the data labels, usually null.
     */
    protected String[] serializedLabels = null;

    /**
     * Whether to prefix the short name to the labels.
     */
    private boolean prefixShortName = false;

    /**
     * Serialization version ID string.
     */
    static final long serialVersionUID = -332954044517171663L;

    /**
     * Current up vector.
     */
    private float[] upVector = vertical;

    /**
     * Up vectors for drawing text;
     */
    private static float[] vertical = new float[] { 1.0F, 0.0F };
    private static float[] horizontal = new float[] { 0.0F, 1.0F };

    /* TODO: Undoable support */

    /**
     * Constructor, takes a LineIDSpecDataImpl.
     */
    public LineIDSpecData( LineIDSpecDataImpl lineIDImpl )
        throws SplatException
    {
        super( lineIDImpl );
        setRange();               // Deferred from super constructor
        useInAutoRanging = false; // by default.
        setPointSize( 1.0 );
    }

    /**
     * Set the SpecData used to defined the relative positioning for
     * the labels.
     * <P>
     * The Mapping transforms positions from the given SpecData to our
     * spectrum. This is used so that the coordinate of a label can be mapped
     * into the specData and the data value of that spectrum used to position
     * the label.
     */
    public void setSpecData( SpecData specData, Mapping mapping )
    {
        this.specData = specData;
        this.specDataMapping = mapping;
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
        if ( impl != null && impl instanceof LineIDSpecDataImpl ) {
            return ((LineIDSpecDataImpl)impl).getLabels();
        }
        return null;
    }

    /**
     * Set the labels.
     */
    public void setLabels( String[] labels )
        throws SplatException
    {
        if ( impl != null && impl instanceof LineIDSpecDataImpl ) {
            ((LineIDSpecDataImpl)impl).setLabels( labels );
        }
    }

    /**
     * Set a specific label.
     */
    public void setLabel( int index, String label )
    {
        if ( impl != null && impl instanceof LineIDSpecDataImpl ) {
            ((LineIDSpecDataImpl)impl).setLabel( index, label );
        }
    }

    /**
     * Set whether to prefix the short name to the labels.
     */
    public void setPrefixShortName( boolean prefixShortName )
    {
        this.prefixShortName = prefixShortName;
    }

    /**
     * Get whether we're prefixing the short name to the labels.
     */
    public boolean isPrefixShortName()
    {
        return prefixShortName;
    }

    /**
     * Set whether to draw labels horizontally.
     */
    public void setDrawHorizontal( boolean drawHorizontal )
    {
        if ( drawHorizontal ) {
            upVector = horizontal;
        }
        else {
            upVector = vertical;
        }
    }

    /**
     * Return if the backing implementation has valid positions for
     * the labels.
     */
    public boolean haveDataPositions()
    {
        if ( impl != null && impl instanceof LineIDSpecDataImpl ) {
            return ((LineIDSpecDataImpl)impl).haveDataPositions();
        }
        return false;
    }

    // Override setRange as the typical line id spectrum will not have data
    // values.
    public void setRange()
    {
        if ( impl == null || ! ( impl instanceof LineIDSpecDataImpl ) ) return;

        if ( haveDataPositions() ) {
            super.setRange();
            return;
        }

        double xMin = Double.MAX_VALUE;
        double xMax = -Double.MAX_VALUE;
        for ( int i = xPos.length - 1; i >= 0 ; i-- ) {
            if ( xPos[i] != SpecData.BAD ) {
                if ( xPos[i] < xMin ) {
                    xMin = xPos[i];
                }
                if ( xPos[i] > xMax ) {
                    xMax = xPos[i];
                }
            }
        }
        if ( xMin == Double.MAX_VALUE ) {
            xMin = 0.0;
        }
        if ( xMax == -Double.MAX_VALUE ) {
            xMax = 0.0;
        }

        //  Record plain range.
        range[0] = xMin;
        range[1] = xMax;
        range[2] = -1.0;
        range[3] = 1.0;

        //  And the "full" version.
        fullRange[0] = xMin;
        fullRange[1] = xMax;
        fullRange[2] = -1.0;
        fullRange[3] = 1.0;
    }

    //
    // Draw the "spectrum". In this case it means draw the line id
    // strings.
    //
    public void drawSpec( Grf grf, Plot plot, double[] clipLimits,
                          boolean physical, double[] fullLimits )
    {
        //  Set up clip region if needed.
        Rectangle cliprect = null;
        if ( clipLimits != null ) {
            double[][] tmp = null;
            if ( physical ) {
                tmp = astJ.astTran2( plot, clipLimits, false );
                cliprect = new Rectangle( (int) tmp[0][0],
                                          (int) tmp[1][1],
                                          (int) (tmp[0][1]-tmp[0][0]),
                                          (int) (tmp[1][0]-tmp[1][1]));
            }
            else {
                cliprect = new Rectangle( (int) clipLimits[0],
                                          (int) clipLimits[3],
                                          (int) (clipLimits[2]-clipLimits[0]),
                                          (int) (clipLimits[1]-clipLimits[3]));

                //  Transform limits to physical for positioning of labels.
                tmp = astJ.astTran2( plot, clipLimits, true );
                clipLimits = new double[4];
                clipLimits[0] = tmp[0][0];
                clipLimits[1] = tmp[1][0];
                clipLimits[2] = tmp[0][1];
                clipLimits[3] = tmp[1][1];
            }
        }
        else {
            // Cannot have null limits. Use the full limits which are in
            // graphics coordinates.
            double[][] tmp = astJ.astTran2( plot, fullLimits, true );
            clipLimits = new double[4];
            clipLimits[0] = tmp[0][0];
            clipLimits[1] = tmp[1][0];
            clipLimits[2] = tmp[0][1];
            clipLimits[3] = tmp[1][1];
        }

        //  Get all labels.
        String[] labels = getLabels();
        double yshift = 0.1 * ( clipLimits[3] - clipLimits[1] );

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
            if ( specData == null ) {
                // Our data positions.
                for ( int i = 0, j = 0; j < xPos.length; j++, i += 2 ) {
                    xypos[i] = xPos[j];
                    if ( ypos[j] == BAD ) {
                        xypos[i + 1] = clipLimits[1] + yshift;
                    }
                    else {
                        xypos[i + 1] = ypos[j] + yshift;
                    }
                }
            }
            else if ( specDataMapping == null ) {
                // Not matching coordinates.

                int[] bound;
                for ( int i = 0, j = 0; j < xPos.length; j++, i += 2 ) {
                    
                    //  Find nearest coordinate in other spectrum.
                    bound = specData.bound( xPos[j] );
                    xypos[i] = xPos[j];

                    //  Record data position. 
                    if ( ypos[bound[0]] == BAD ) {
                        xypos[i + 1] = clipLimits[1] + yshift;
                    }
                    else {
                        xypos[i + 1] = ypos[bound[0]] + yshift;
                    }
                }
            }
            else {
                // Data positions from another spectrum. Need care to match
                // coordinates and data values.
                double[] inpos = new double[2];
                double[][] tmp = null;
                int[] bound;
                for ( int i = 0, j = 0; j < xPos.length; j++, i += 2 ) {
                    
                    //  Need index of a coordinate of specData near to our
                    //  coordinate. So transform our coordinate to world
                    //  coordinates of the target spectrum, if we're using one.
                    inpos[0] = xPos[j];
                    inpos[1] = clipLimits[1];
                    tmp = astJ.astTran2( specDataMapping, inpos, false );

                    //  Find the nearest pair of coordinates to this position.
                    bound = specData.bound( tmp[0][0] );

                    //  Pick a data value of one of these coordinates and
                    //  transform that into our units.
                    inpos[0] = tmp[0][0];
                    inpos[1] = ypos[bound[0]];
                    tmp = astJ.astTran2( specDataMapping, inpos, true );
                    
                    //  Record position of label.
                    xypos[i] = xPos[j];
                    if ( tmp[1][0] == BAD ) {
                        xypos[i + 1] = clipLimits[1] + yshift;
                    }
                    else {
                        xypos[i + 1] = tmp[1][0] + yshift;
                    }
                }
            }
        }
        else {
            // Generate data positions relative to the limits.
            for ( int i = 0, j = 0; j < xPos.length; j++, i += 2 ) {
                xypos[i] = xPos[j];
                xypos[i + 1] = clipLimits[1] + yshift; // or clipLimits[3]?
            }
        }

        //  Define general parameters. Note these are "overridden" by
        //  the Plot when "strings" parameters are set (allows the
        //  selection of colour using the same buttons as for a normal
        //  spectrum). The text style can be set using the "strings"
        //  configuration options of the plot (which is why we use it
        //  and not the Grf object, which bypasses the Plot).
        DefaultGrf defaultGrf = (DefaultGrf) grf;
        DefaultGrfState oldState = setGrfAttributes( defaultGrf, false );
        defaultGrf.setClipRegion( cliprect );

        double[] pos = new double[2];

        if ( prefixShortName ) {
            String prefix = shortName + ":";
            for ( int i = 0, j = 0; i < labels.length; i++, j += 2 ) {
                pos[0] = xypos[j];
                pos[1] = xypos[j+1];
                plot.text( prefix + labels[i], pos, upVector, "CC" );
            }
        }
        else {
            for ( int i = 0, j = 0; i < labels.length; i++, j += 2 ) {
                pos[0] = xypos[j];
                pos[1] = xypos[j+1];
                plot.text( labels[i], pos, upVector, "CC" );
            }
        }
        resetGrfAttributes( defaultGrf, oldState, false );
        defaultGrf.setClipRegion( null );
    }

//
//  Serializable interface.
//

    private void writeObject( ObjectOutputStream out )
        throws IOException
    {
        //  Need to write out persistent data labels.
        serializedLabels = getLabels();

        //  And store all member variables.
        out.defaultWriteObject();

        //  Finished.
        serializedLabels = null;
    }

    private void readObject( ObjectInputStream in )
        throws IOException, ClassNotFoundException
    {
        //  Note we use this method as we need a different impl object from
        //  SpecData and need to restore the data labels.
        try {
            // Restore state of member variables.
            in.defaultReadObject();

            //  Create the backing impl, this will supercede one created by
            //  the SpecData readObject.
            LineIDMEMSpecDataImpl newImpl =
                new LineIDMEMSpecDataImpl( shortName, this );
            fullName = null;

            //  Restore data labels.
            if ( serializedLabels != null ) {
                newImpl.setLabels( serializedLabels );
                serializedLabels = null;
            }
            this.impl = newImpl;

            //  Full reset of state.
            readData();
            setRange();
        }
        catch ( SplatException e ) {
            e.printStackTrace();
        }
    }
}
