/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 * Copyright (C) 2009 Science and Technology Facilities Council
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
 * line positions as a string with a more accurate vertical bars.
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
     * Whether suffix the short name to the labels.
     */
    private boolean suffixShortName = false;

    /**
     * Whether to just use the short name as the labels.
     */
    private boolean onlyShortName = false;

    /**
     * Whether to draw vertical lines around the label.
     */
    private boolean showVerticalMarks = true;

    /**
     * Serialization version ID string.
     */
    static final long serialVersionUID = -332954044517171663L;

    /**
     * Current up vector.
     */
    private float[] upVector = verticalUp;

    /**
     * Up vectors for drawing text;
     */
    private static float[] verticalUp = new float[] { 1.0F, 0.0F };
    private static float[] horizontalUp = new float[] { 0.0F, 1.0F };
    private boolean horizontal = false;

    /**
     * The name used to prefix, suffix or act as the labels. This is the
     * shortname, but with a trailing "_lines" removed and any underscores
     * replaced with blanks.
     */
    private String prefixName = null;

    /**
     * Mapping to transform between the current coordinates and frequency
     * in GHz.
     */
    private FrameSet frequencyMapping = null;

    /**
     * The number of positions actually drawn in the last call to drawSpec.
     */
    private int drawn = 0;

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
     * Constructor, takes a LineIDTableSpecDataImpl.
     */
   public LineIDSpecData( LineIDTableSpecDataImpl lineIDImpl )
            throws SplatException
    {
         super( lineIDImpl );
         setRange();               // Deferred from super constructor
         useInAutoRanging = false; // by default.
        // setPointSize( 1.0 );
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

    //
    //  Set the short name. Overridden so we can apply the transformation to
    //  remove "_lines" and and "_" for the version used in the labels.
    //
    public void setShortName( String shortName )
    {
        super.setShortName( shortName );
        prefixName = shortName;

        //  Trim off "_lines" from name.
        int index = prefixName.indexOf( "_lines" );
        if ( index != -1 ) {
            prefixName = prefixName.substring( 0, index );
        }

        //  Replace all underscores with blanks.
        prefixName = prefixName.replace( '_', ' ' );
    }

    /**
     * Get the labels.
     */
    public String[] getLabels()
    {
        if (impl != null) {
            if (  impl instanceof LineIDSpecDataImpl ) 
                return ((LineIDSpecDataImpl)impl).getLabels();
            else  if (  impl instanceof LineIDTableSpecDataImpl ) 
                return ((LineIDTableSpecDataImpl)impl).getLabels();
            
        } 
        return null;
    }

    /**
     * Set the labels.
     */
    public void setLabels( String[] labels )
        throws SplatException
    {
        if ( impl != null ) {
            if ( impl instanceof LineIDSpecDataImpl ) 
                ((LineIDSpecDataImpl)impl).setLabels( labels );
            else  if (  impl instanceof LineIDTableSpecDataImpl ) 
                ((LineIDTableSpecDataImpl)impl).setLabels( labels );
        }
    }

    /**
     * Set a specific label.
     */
    public void setLabel( int index, String label )
    {
        if ( impl != null ) {
            if ( impl instanceof LineIDSpecDataImpl ) 
                ((LineIDSpecDataImpl)impl).setLabel( index, label );
            else  if (  impl instanceof LineIDTableSpecDataImpl ) 
                ((LineIDTableSpecDataImpl)impl).setLabel( index, label);
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
     * Set whether to suffix the short name to the labels.
     */
    public void setSuffixShortName( boolean suffixShortName )
    {
        this.suffixShortName = suffixShortName;
    }

    /**
     * Get whether we're suffixing the short name to the labels.
     */
    public boolean isSuffixShortName()
    {
        return suffixShortName;
    }

    /**
     * Set whether to just use short name as the labels.
     */
    public void setOnlyShortName( boolean onlyShortName )
    {
        this.onlyShortName = onlyShortName;
    }

    /**
     * Get whether we're only using the short name as the labels.
     */
    public boolean isOnlyShortName()
    {
        return onlyShortName;
    }

    /**
     * Set whether to show the vertical line markers.
     */
    public void setShowVerticalMarks( boolean showVerticalMarks )
    {
        this.showVerticalMarks = showVerticalMarks;
    }

    /**
     * Get whether we're showing the vertical marks.
     */
    public boolean isShowVerticalMarks()
    {
        return showVerticalMarks;
    }

    /**
     * Set whether to draw labels horizontally.
     */
    public void setDrawHorizontal( boolean horizontal )
    {
        this.horizontal = horizontal;
        if ( horizontal ) {
            upVector = horizontalUp;
        }
        else {
            upVector = verticalUp;
        }
    }

    /**
     * Return if the backing implementation has valid positions for
     * the labels.
     */
    public boolean haveDataPositions()
    {
        if ( impl != null ) {
            if ( impl instanceof LineIDSpecDataImpl  )      
                return ((LineIDSpecDataImpl)impl).haveDataPositions();
            else if ( impl instanceof LineIDTableSpecDataImpl  ) 
            return ((LineIDTableSpecDataImpl)impl).haveDataPositions();
        }
        return false;
    }

    // Override setRange as the typical line id spectrum will not have data
    // values.
    public void setRange()
    {
        if ( impl == null || (! ( impl instanceof LineIDSpecDataImpl ) && ! (impl instanceof LineIDTableSpecDataImpl) )) return;

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
    // Draw the "spectrum". Override the default implementation to call the
    // local one that has additional params.
    //
    public void drawSpec( Grf grf, Plot plot, double[] clipLimits,
                          boolean physical, double[] fullLimits )
    {
        drawSpec( grf, plot, clipLimits, physical, fullLimits, null,
                  getLineColour() );
    }

    /**
     * Draw the line identifiers onto the given widget using a suitable
     * AST GRF object.
     *
     * @param grf Grf object that can be drawn into using AST primitives.
     * @param plot reference to Plot defining transformation from physical
     *             coordinates into graphics coordinates.
     * @param clipLimits limits of the region to draw used to clip graphics.
     *                   These can be in physical or graphics coordinates.
     *                   Note only the X limits will be used for clipping.
     * @param physical whether limits are physical or graphical.
     * @param fullLimits full limits of drawing area in graphics coordinates.
     *                   May be used for positioning when clipping limits are
     *                   not used.
     *                   Note only the X limits will be used for clipping.
     * @param postfix a string that will be postfixed to the labels.
     * @param colour a colour for labels etc.
     */
    public void drawSpec( Grf grf, Plot plot, double[] clipLimits,
                          boolean physical, double[] fullLimits,
                          String postfix, int colour )
    {
        //  Number of points drawn.
        drawn = 0;

        //  Work out the clip limits in physical coordinates.
        if ( clipLimits != null ) {
            if ( ! physical ) {
                //  Transform limits to physical for positioning of labels.
                double[][] tmp = astJ.astTran2( plot, clipLimits, true );
                clipLimits = new double[4];
                clipLimits[0] = tmp[0][0];
                clipLimits[1] = tmp[1][0];
                clipLimits[2] = tmp[0][1];
                clipLimits[3] = tmp[1][1];
            }
        }
        else {
            //  Use the full limits which are in graphics coordinates.
            double[][] tmp = astJ.astTran2( plot, fullLimits, true );
            clipLimits = new double[4];
            clipLimits[0] = tmp[0][0];
            clipLimits[1] = tmp[1][0];
            clipLimits[2] = tmp[0][1];
            clipLimits[3] = tmp[1][1];
        }

        //  Get min and max along X axis for actual clipping. This
        //  implementation does not clip in Y or just because the label
        //  extends outside the region. More important that the full label is
        //  always shown.
        double[] limits = new double[2];
        if ( clipLimits[0] > clipLimits[2] ) {
            limits[0] = clipLimits[2];
            limits[1] = clipLimits[0];
        }
        else {
            limits[0] = clipLimits[0];
            limits[1] = clipLimits[2];
        }

        //  Get all labels.
        String[] labels = getLabels();

        //  A shift from the baseline in graphics coords.
        double yshift = 0.2 * Math.abs( ( clipLimits[3] - clipLimits[1] ) );

        //  Guess a length for the lines.
        double lineLength = yshift * 0.5;

        //  Add the graphics shift, if applying (for stacking).
        if ( isApplyYOffset() ) {
            yshift += getYOffset();
        }

        //  Need to generate positions for placing the labels. The various
        //  schemes for this are use any positions read from the
        //  implementation, use the positions from a SpecData and finally
        //  put them along the top or bottom of the given limits.
        double[] xypos = new double[xPos.length * 2];
        double[] ypos = yPos;
        if ( specData != null ) {
            ypos = specData.getYData();
        }
        double defaultPos = clipLimits[1] + yshift;

        if ( ypos != null ) {

            if ( specData == null ) {
                // Our data positions, i.e. haveDataPositions and no specData.
                for ( int i = 0, j = 0; j < xPos.length; j++, i += 2 ) {
                    xypos[i] = xPos[j];
                    if ( ypos[j] == BAD ) {
                        xypos[i + 1] = defaultPos;
                    }
                    else {
                        xypos[i + 1] = ypos[j] + yshift;
                    }
                }
            }
            else if ( specDataMapping == null ) {
                // Not matching coordinates. So spectra all in same
                // coordinates and data units, or it goes wrong (tough).
                int[] bound;
                for ( int i = 0, j = 0; j < xPos.length; j++, i += 2 ) {

                    //  Find nearest coordinate in other spectrum.
                    bound = specData.bound( xPos[j] );
                    xypos[i] = xPos[j];

                    //  Record data position.
                    if ( ypos[bound[0]] == BAD ) {
                        xypos[i + 1] = defaultPos;
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
                        xypos[i + 1] = defaultPos;
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
                xypos[i + 1] = defaultPos;
            }
        }

        //  Define general parameters. Note these are "overridden" by
        //  the Plot when "strings" parameters are set (allows the
        //  selection of colour using the same buttons as for a normal
        //  spectrum). The text style can be set using the "strings"
        //  configuration options of the plot (which is why we use it
        //  and not the Grf object, which bypasses the Plot).
        setPointSize(1.0);
        DefaultGrf defaultGrf = (DefaultGrf) grf;
        DefaultGrfState oldState = setGrfAttributes( defaultGrf, false );

        
        //  No Grf clipping, we just apply an X coordinate range check.
        defaultGrf.setClipRegion( null );

        //  Apply the given line colour, may not be the default one.
        defaultGrf.attribute( Grf.GRF__COLOUR, colour, Grf.GRF__LINE );

        //  Generate the prefix and postfix strings. Quite a complex
        //  setting. If onlyShortName is set, prefixShortName and
        //  suffixShortName are ignored.
        String pre = "";
        String post = "";
        if ( ! onlyShortName ) {
            if ( prefixShortName ) {
                if ( prefixName == null || "".equals( prefixName ) ) {
                    setShortName( shortName );
                }
                pre = prefixName + ":";
            }
            if ( postfix != null ) {
                post = postfix;
            }
            else if ( suffixShortName ) {
                if ( prefixName == null || "".equals( prefixName ) ) {
                    setShortName( shortName );
                }
                post = ":" + prefixName;
            }
        }
        else if ( postfix != null ) {
            post = postfix;
        }
        else {
            setShortName( shortName );
        }

        //  Get scale factors for transforming graphics and physical
        //  coordinates. Just project a graphics pixel into physical
        //  coordinates.
        double[] scales = new double[2];
        if ( showVerticalMarks ) {
            double[] xin = new double[2];
            double[] yin = new double[2];
            xin[0] = 100.0;
            xin[1] = 101.0;
            yin[0] = 100.0;
            yin[1] = 101.0;
            double[][] tmp = plot.tran2( 2, xin, yin, true );
            scales[0] = Math.abs( tmp[0][1] - tmp[0][0] );
            scales[1] = Math.abs( tmp[1][1] - tmp[1][0] );
        }

        double[] pos = new double[2];
        if ( onlyShortName ) {
            String label = prefixName + post;

            //  Positions for vertical lines.
            double shift = 0.0;
            if ( showVerticalMarks ) {
                double[] extent = defaultGrf.textExtent( label, 0.0,
                                                         0.0, "CC",
                                                         upVector[0],
                                                         upVector[1] );
                shift = ( extent[5] - extent[1] ) * scales[1] * 0.75;
            }

            for ( int i = 0, j = 0; i < labels.length; i++, j += 2 ) {
                pos[0] = xypos[j];

                //  Don't display if clipped along X axis.
                if ( pos[0] >= limits[0] && pos[0] <= limits[1] ) {
                    drawn++;
                    pos[1] = xypos[j+1];
                  
                    plot.text( label, pos, upVector, "CC" );
                    if ( showVerticalMarks ) {
                        pos[1] = xypos[j+1] + shift;
                        plot.gridLine( 2, pos, lineLength );
                        pos[1] = xypos[j+1] - shift;
                        plot.gridLine( 2, pos, -lineLength );
                    }
               }
            }
        }
        else {
            String label = null;
            double shift = 0.0;
            for ( int i = 0, j = 0; i < labels.length; i++, j += 2 ) {
                pos[0] = xypos[j];

                //  Don't display if clipped along X axis.
                if ( pos[0] >= limits[0] && pos[0] <= limits[1] ) {
                    drawn++;
                    label = pre + labels[i] + post;
                    pos[1] = xypos[j+1];
                    plot.text( label, pos, upVector, "CC" );

                    if ( showVerticalMarks ) {
                        //  Get size of text as was drawn. Use to position
                        //  the vertical lines.
                        double[] extent = defaultGrf.textExtent( label, 0.0,
                                                                 0.0, "CC",
                                                                 upVector[0],
                                                                 upVector[1] );
                        shift = ( extent[5] - extent[1] ) * scales[1] * 0.75;

                        pos[1] = xypos[j+1] + shift;
                        plot.gridLine( 2, pos, lineLength );
                        pos[1] = xypos[j+1] - shift;
                        plot.gridLine( 2, pos, -lineLength );
                    }
                }
            }
        }

        resetGrfAttributes( defaultGrf, oldState, false );
    }

    /**
     * Return the frequency of a line identifier. Note this is in this
     * frame, the source frame, not the aligned frame which is usually seen in
     * a plot. The returned value is in GHz.
     *
     * @param index the index of the label whose frequency is required.
     * @return the frequency in GHz, -1 if out of range.
     */
    public double getFrequency( int index )
    {
        if ( index < xPos.length ) {
            if ( frequencyMapping == null ) {
                //  Need a Mapping that transforms the current coordinates
                //  into Ghz.
                FrameSet from = impl.getAst();
                FrameSet to = (FrameSet) from.copy();
                to.set( "System=FREQ,Unit=GHz" );
                frequencyMapping = from.convert( to, "" );
            }
            double xin[] = new double[1];
            xin[0] = xPos[index];
            double xout[] = frequencyMapping.tran1( 1, xin, true );
            return xout[0];
        }
        return -1.0;
    }

    /**
     *  Get the number of points drawn in last call to drawSpec.
     */
    public int getDrawn()
    {
        return drawn;
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
