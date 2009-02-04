package uk.ac.starlink.frog.ast;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import javax.swing.JComponent;

import uk.ac.starlink.ast.*;

/**
 * Java interface for AST manipulations based on a frameset, the type
 * of operations needed to support WCS coordinates. The graphics
 * facilities (if used) are provided by the Grf class. This should
 * also be initialised before use and a reference to the object
 * passed.
 * <p>
 * Based on the SPLAT <CODE>uk.ac.starlink.ast.ASTJ</CODE> class, with 
 * some minor API modifications.
 *
 * @author Peter W. Draper, Alasdair Allan
 * @version $Id$
 * @since 09-JAN-2003
 */
 
public class AstUtilities implements Serializable
{
    //  ============
    //  Constructors
    //  ============

    /**
     *  Default constructor
     */
    public AstUtilities()
    {
        //  Do nothing.
    }

    /**
     *  Initialise from an AST frameset reference.
     */
    public AstUtilities( FrameSet astref )
    {
        setRef( astref, false );
    }

    /**
     *  Initialise from an AST frameset reference. This form takes
     *  possession of the reference and releases it when finalized.
     */
    public AstUtilities( FrameSet astref, boolean manage )
    {
        setRef( astref, manage );
    }

    /**
     *  Initialise from an AST frameset reference and a Grf object.
     */
    public AstUtilities( FrameSet astref, Grf grfref )
    {
        setRef( astref, false );
        setGraphic( grfref );
    }

    /**
     *  Finalise object. Free any resources associated with member
     *  variables.
     */
    protected void finalize() throws Throwable
    {
        grfRef = null;
        super.finalize();
    }


    //  ===================
    //  Protected variables
    //  ===================

    /**
     *  Grf object (or subclass) for controlling drawing facilities.
     */
    protected Grf grfRef = null;

    /**
     *  Reference to Ast frameset.
     */
    protected FrameSet astRef = null;

    /**
     *  Whether astRef is managed by this object (if so then is
     *  annulled as required).
     */
    protected boolean manageAstRef = false;

    /**
     *  Reference to astPlot, created by last call to astPlot.
     */
    protected Plot astPlot = null;

    /**
     *  Graphics limits of the plot region.
     */
    protected float[] graphbox = new float[4];

    /**
     * Size of buffers used when transferring potentially large chunks
     * of data.
     */
    protected static final int MAXDIM = 7;

    /**
     * Smallest value between 1.0 and next double precision value.
     */
    protected static final double EPSILON = 2.2204460492503131e-16;


    //  ====================
    //  Class public methods
    //  ====================

    /**
     *  Set the main AST frameset.
     *
     *  @param astref reference to the AST frameset.
     *  @param manage whether the astRef is managed by this object.
     */
    public void setRef( FrameSet astRef, boolean manage )
    {
        annulRef();
        this.astRef = astRef;
        this.manageAstRef = manage;
    }

    /**
     *  Get a copy of the current Ast frameset.
     */
    public FrameSet getRef()
    {
        return astRef;
    }

    /**
     *  Set the current astPlot.
     */
    public void setPlot( Plot astPlot )
    {
        annulPlot();
        this.astPlot = astPlot;
    }

    /**
     *  Get the current astPlot.
     */
    public Plot getPlot()
    {
        return astPlot;
    }

    /**
     *  Set the graphics control object.
     *
     *  @param grfRef reference to the Grf object.
     */
    public void setGraphic( Grf grfRef )
    {
        this.grfRef = grfRef;
    }

    /**
     *  Get the current graphics control object.
     */
    public Grf getGraphic()
    {
        return grfRef;
    }

    /**
     *  Annul the current plot.
     */
    protected void annulPlot()
    {
        astPlot = null;
    }

    /**
     *  Annul the current Ast frameset, if we're managing it.
     */
    protected void annulRef()
    {
        if ( manageAstRef && astRef != null ) {
            astRef = null;
        }
    }

    /**
     *  Create an AST frame and return a reference to it.
     *
     *  @param naxes  number of frame axes required.
     *  @param attrib list of attributes for the frame.
     *
     *  @return reference to the AST frame.
     */
    public Frame astFrame( int naxes, String attrib )
    {
        Frame frame = new Frame( naxes );
        frame.set( attrib );
        return frame;
    }

    /**
     *  Create an AST frameset and return a reference to it.
     *
     *  @param frame  the initial AST frame
     *  @param attrib list of attributes for the frameset.
     *
     *  @return reference to the AST frameset.
     */
    public FrameSet astFrameSet( Frame frame, String attrib )
    {
        FrameSet frameSet = new FrameSet( frame );
        frameSet.set( attrib );
        return frameSet;
    }

    /**
     *  Add a frame to an AST frameset.
     *
     *  @param frameset reference to AST frameset
     *  @param insert position of related frame in frameset
     *  @param map reference to an AST mapping
     *  @param frame reference to the new AST frame
     */
    public void astAddFrame( FrameSet frameset, int insert,
                             Mapping map, Frame frame )
    {
        frameset.addFrame( insert, map, frame );
    }

    /**
     *  Create an AST lutmap.
     *
     *  @param lut    the lut values
     *  @param start  input coordinate value that corresponds to
     *                first lut entry
     *  @param incr   lut spacing
     */
    public LutMap astLutMap( double[] lut, double start, double incr )
    {
        return new LutMap( lut, start, incr );
    }

    /**
     *  Create an AstPlot reference to fit a given component.
     *
     *  @param comp    component that the plot will fit inside.
     *  @param basebox array of 4 floating point number indicating the
     *                 corners of the region to be drawn (in the
     *                 coordinate system of the base frame).
     *  @param xleft   Fraction of component display surface to be
     *                 reserved on left for axes labels etc (can be zero, in
     *                 which case use control of the Insets to provide
     *                 required space).
     *  @param xright  Fraction of component display surface to be
     *                 reserved on right for axes labels etc (can be zero, in
     *                 which case use control of the Insets to provide
     *                 required space).
     *  @param ytop    Fraction of component display surface to be
     *                 reserved on top for axes labels etc (can be zero, in
     *                 which case use control of the Insets to provide
     *                 required space).
     *  @param ybottom Fraction of component display surface to be
     *                 reserved on bottom for axes labels etc (can be zero, 
     *                 in which case use control of the Insets to provide
     *                 required space).
     *  @param options a string of AST options to use when creating plot.
     */
    public void astPlot( JComponent comp, double basebox[],
                         double xleft, double xright, 
                         double ytop, double ybottom, String options )
    {
        //  Do nothing if no AST frameset available.
        if ( astRef == null || grfRef == null ) {
            return;
        }

        //  If we already have a plot then release it.
        annulPlot();

        //  Find out the size of the graphics component. This is used
        //  to define the base graphics coordinate system.
        Dimension size = comp.getPreferredSize();
        Insets inset = comp.getInsets();

        //  Fraction of space reserved at left/right and top/bottom.
        float tinset = (float) ( size.height * ytop );
        float binset = (float) ( size.height * ybottom );
        float linset = (float) ( size.width * xleft );
        float rinset = (float) ( size.width * xright );

        //  Bottom left-hand corner. Corrected for border insets.
        graphbox[0] = inset.left + linset;
        graphbox[1] = size.height - inset.bottom - tinset;

        //  Top right-hand corner.
        graphbox[2] = size.width - inset.right - linset;
        graphbox[3] = inset.top + tinset;

        Rectangle graphRect = new Rectangle( size );

        //  Now create the astPlot.
        astPlot = new Plot( astRef, graphRect, basebox,
                            (int) (inset.left + linset),
                            (int) (inset.right + rinset),
                            (int) (inset.bottom + binset),
                            (int) (inset.top + tinset) );
        if ( options != null ) {
            astPlot.set( options );
        }
    }

    /**
     *  Set the graphics clipping region for the AstPlot.
     *
     *  @param xlower lower bound for clipping.
     *  @param ylower lower bound for clipping.
     *  @param xupper upper bound for clipping.
     *  @param yupper upper bound for clipping.
     */
    public void astPlotClip( double xlower, double ylower,
                             double xupper, double yupper )
    {
        if ( astPlot != null ) {
            double[] lbnd = new double[2];
            double[] ubnd = new double[2];
            lbnd[0] = xlower;
            lbnd[1] = ylower;
            ubnd[0] = xupper;
            ubnd[1] = yupper;
            astPlot.clip( FrameSet.AST__BASE, lbnd, ubnd );
        }
    }

    /**
     *  Get the limits of the graphics region of the current Plot.
     *  Used to clip any drawing operations not performed by AST.
     *
     *  @return array of four floating point values. These are the
     *          coordinates of the lower left-hand corner and the top
     *          right-hand corner.
     */
    public float[] getGraphicsLimits()
    {
        return graphbox;
    }

    /**
     *  Draw an astGrid, using last astPlot.
     */
    public void astGrid()
    {
        if ( astPlot == null || grfRef == null ) {
            return;
        }
        astPlot.grid();
    }

    /**
     *  Clear graphics (does not erase drawing). TODO: should
     *  only do this for graphics created by this object.
     */
    public void astReset()
    {
        if ( grfRef == null ) {
            return;
        }
        grfRef.clear();
    }

    /**
     *  Draw a text string at a given position.
     *
     *  @param text the string of text to plot.
     *
     *  @param position a pair (or more) of coordinates that define
     *                  the physical position that the text should be
     *                  plotted at.
     *  @param up the upvector of the text orientation (two values in
     *            graphics coordinates).
     *  @param just justification position of the text (two characters
     *              from the pairs {T,C,B} - {L,C,R}).
     *
     *  @notes a call to astPlot must be made before attempting to use
     *         this method.
     */
    public void astText( String text, double[] position, float up[],
                         String just )
    {
        if ( astPlot == null || grfRef == null ) {
            return;
        }
        astPlot.text( text, position, up, just );
    }

    /**
     *  Draw a graphics marker at given positions.
     *
     *  @param points the set of 2D positions to draw marker at,
     *                these coordinates are in the current frame of
     *                the AstPlot.
     *  @param type   the type of marker to be drawn, these are
     *                defined by the class GrfMarker.
     *
     *  @notes the points are stored in the arrays like [x0,y0,x1,y1...]
     */
    public void astMark2( double[] points, int type )
    {
        if ( astPlot == null || grfRef == null ) {
            return;
        }
        int nmark = points.length / 2;
        double[][] in = new double[2][MAXDIM];

        // Dispatch markers in groups of up to MAXDIM
        double[] ptr = points;
        int n = 0;
        int upper = 0;
        for ( int lower = 0; lower < nmark; lower += MAXDIM ) {
            upper = lower + MAXDIM;
            if ( upper > nmark ) upper = nmark;

            // Copy coordinates into local buffer
            for ( int i = lower, j = 0; i < upper; i++, j++ ) {
                in[0][j] = points[n++];
                in[1][j] = points[n++];
            }

            // Plot the markers
            astPlot.mark( upper-lower, 2, in, type );
        }
    }

    /*
     *  Draw a series of connected geodesic curves (i.e. polyline)
     *
     *  @param xpos the set of "X" positions defining the endpoints of
     *              each curve. The these coordinates are in the
     *              current frame of the AstPlot.
     *
     *  @param ypos the set of "Y" positions defining the endpoints of
     *              each curve. The these coordinates are in the
     *              current frame of the AstPlot.
     */
    public void astPolyCurve( double[] xpos, double[] ypos )
    {
        if ( astPlot == null || grfRef == null ) {
            return;
        }

        // Construct local buffer for all positions
        int npoint = xpos.length;
        double[][] in = new double[2][npoint];

        for ( int i = 0; i < npoint; i++ ) {
            in[0][i] = xpos[i];
            in[1][i] = ypos[i];
        }
        astPlot.polyCurve( npoint, 2, in );
    }

    /**
     *  Set plot attributes.
     *
     *  @param settings the attribute settings to apply to the current
     *                  plot.
     *
     *  @deprecated Use direct ".set()" method
     */
    public void astSetPlot( String settings )
    {
        if ( astPlot == null ) {
            return;
        }
        astPlot.set( settings );
    }

    /**
     *  Show current AstPlot on standard output (debugging).
     *
     *  @deprecated Use direct ".show()" method
     */
    public void astShowPlot()
    {
        if ( astPlot == null ) {
            return;
        }
        astPlot.show();
    }

    /**
     *  Copy an AST reference of some kind.
     *
     *  @deprecated Use direct ".copy()" method
     */
    public static AstObject astCopy( AstObject ref )
    {
        if ( ref != null ) {
            return ref.copy();
        }
        return null;
    }

    /**
     *  Set the attributes of an AST object
     *
     *  @param astRef reference to AST object (such as an AstFrame,
     *                AstPlot etc.)
     *  @param settings the settings to apply.
     *
     *  @deprecated Use direct ".set" method
     */
    public static void astSet( AstObject astRef, String settings )
    {
        astRef.set( settings );
    }

    /**
     *  Get an attribute of an AST object as String.
     *
     *  @param astref reference to AST object (such as an AstFrame,
     *                AstPlot etc.)
     *  @param attrib the AST attribute to return.
     *
     *  @return value of attribute
     *
     *  @deprecated Use direct ".getC" method
     */
    public static String astGet( AstObject astRef, String attrib )
    {
        return astRef.getC( attrib );
    }

    /**
     *  Get an attribute of an AST object as floating point value.
     *
     *  @param astref reference to AST object (such as an AstFrame,
     *                AstPlot etc.)
     *  @param attrib the AST attribute to return.
     *
     *  @return value of attribute
     *
     *  @deprecated Use direct ".getD" method
     */
    public static double astGetD( AstObject astRef, String attrib )
    {
        return astRef.getD( attrib );
    }

    /**
     *  Show an AST object on standard output (debugging).
     *
     *  @deprecated Use direct ".show()" method
     */
    public static void astShow( AstObject astRef )
    {
        astRef.show();
    }

    /**
     *  Transform a set of 1D positions using an AstMapping.
     *
     *  @param mapping the AstMapping (frameset or Plot).
     *  @param points  the set of positions to transform.
     *  @param forward whether to use the forward or inverse
     *                 transform.
     *
     *  @return double [] array of transformed positions.
     *
     *  @notes the mapping must have nin and nout equal to 1.
     *
     *  @deprecated Use direct ".tran1" method of mapping
     */
    public static double[] astTran1( Mapping mapping,
                                     double[] points,
                                     boolean forward )
    {
        return mapping.tran1( points.length, points, forward );
    }

    /**
     *  Transform a set of 2D positions using an AstMapping.
     *
     *  @param mapping the AstMapping (frameset or Plot).
     *  @param points  the set of 2D positions to transform.
     *  @param forward whether to use the forward or inverse
     *                 transform.
     *
     *  @return double [] array of transformed positions.
     *
     *  @notes the points are stored in the arrays like [x0,y0,x1,y1...]
     *
     *  @deprecated Use direct ".tran2" method of mapping.
     */
    public static double[][] astTran2( Mapping mapping, double[] points,
                                     boolean forward )
    {
        //  Unpack the points.
        int npoints = points.length / 2;
        double[] x = new double[npoints];
        double[] y = new double[npoints];
        int n = 0;
        for ( int i = 0; i < npoints; i++ ) {
            x[i] = points[n++];
            y[i] = points[n++];
        }
        return mapping.tran2( npoints, x, y, forward );
    }

    /**
     *  Format a coordinate value for a frame axis.
     *
     *  @param axis the axis number (start at 1).
     *  @param frame an AST frame or frameset.
     *  @param value the value to be formatted.
     *
     *  @return the formatted value.
     *
     *  @deprecated Use the direct ".format()" method of Frame
     */
    public static String astFormat( int axis, Frame frame, double value )
    {
        return frame.format( axis, value );
    }

    /**
     *  Unformat a coordinate value for a frame axis.
     *
     *  @param axis the axis number (start at 1).
     *  @param frame an AST frame or frameset.
     *  @param value the value to be unformatted.
     *
     *  @return a double precision conversion of input value.
     *
     *  @deprecated Use the direct ".unformat()" method of Frame
     */
    public static double astUnFormat( int axis, Frame frame, String value )
    {
        return frame.unformat( axis, value );
    }

    /**
     *  Extract a 1D mapping from the current AST frameset.
     *
     *  @param axis the axis whose mapping is to be extracted.
     *
     *  @return the mapping
     */
    public Mapping get1DMapping( int axis )
    {
        return get1DFrameSet( astRef, axis );
    }

    /**
     *  Extract a 1D mapping from a FrameSet.
     *
     *  @param mapping reference to the AST frameset.
     *  @param axis the axis whose mapping is to be extracted.
     *
     *  @return the 1D mapping
     */
    static public Mapping get1DFrameSet( FrameSet frameset, int axis )
    {
        try {
            // Extract a 1D mapping
            FrameSet framecopy = extract1DFrameSet( frameset, axis );

            // And return the new, simplified, mapping
            Mapping map1 = framecopy.getMapping( FrameSet.AST__BASE,
                                                 FrameSet.AST__CURRENT );
            Mapping map2 = map1.simplify();
            return map2;
        }
        catch (Exception e) {
            // Let it go.
        }
        return null;
    }

    /**
     *  Extract a 1D time stamp axis frameset from a makeSeries frameset.
     *
     *  @param mapping reference to the makeSeries frameset.
     *
     *  @return the 1D frameset
     */
    static public FrameSet get1DFrameSet( FrameSet frameset )
    {
        return extract1DFrameSet( frameset, 1 );
    }

    /**
     * Convert the current framset into one suitable for displaying
     * a Periodogram
     */
     public FrameSet makeGram( int axis, int start, int end,
                                 String label, String units, boolean dist )
    {
        return makeSeries( axis, start, end, label, units, dist );
        
    }    

    /**
     *  Convert the current frameset into a one suitable for
     *  displaying a time series
     *
     *  @param axis  the axis to select as temporal dimension.
     *  @param start first position in input base frame coordinates (GRID)
     *  @param end   last position in input base frame coordinates (GRID)
     *  @param label label for the data units (can be blank).
     *  @param units data units (can be blank).
     *  @param dist  Time series coordinates should be shown as a
     *               distance (rather than coordinate).
     *
     *  @return the new frameset for displaying a time series
     */
    public FrameSet makeSeries( int axis, int start, int end,
                                 String label, String units, boolean dist )
    {
        if ( astRef == null ) {
            return null;
        }
        FrameSet result = null;
        try {

            // Create a mapping that has the coordinate measure for one
            // axis and a unitmap to plot the data values. Use a LutMap
            // for the coordinate measurement along the GRID positions as
            // we want to avoid the case when just using a PermMap to lose
            // a second axis makes the mapping not invertable (i.e. for
            // sky coordinates no providing RA and DEC inputs will always
            // give AST__BAD).

            // Get a simplified mapping from the base to current frames
            Mapping map = astRef.getMapping( FrameSet.AST__BASE,
                                             FrameSet.AST__CURRENT );
            Mapping smap = map.simplify();

            // Save a pointer to the current frame
            Frame cfrm = astRef.getFrame( FrameSet.AST__CURRENT );

            // See how many axes the current frame has
            int nax = cfrm.getI( "Naxes" );

            // And how input coordinates the base frame needs (ideally 1)
            int nin = astRef.getI( "Nin" );

            // Get memory for positions and LutMap along GRID axis
            int dim = end - start + 1;
            double[] grid = new double[ dim * nin ];

            // Generate dim positions stepped along the GRID axis pixels
            for ( int i = 0; i < dim; i++ ) {
                for ( int j = 0; j < nin; j++ ) {
                    grid[dim*j+i] = i + 1;
                }
            }

            // Transform these GRID positions into the current frame XXX check.
            double[] coords = smap.tranN( dim, nin, grid, true, nax );

            if ( dist ) {
                // Get the distance from the first GRID position to each
                // of the others (remember the projected positions could
                // be multidimensional and we need to "remove" this by
                // converting to a distance along the projected GRID line)
                coord2Dist( cfrm, dim, nax, coords, grid );
            }
            else {

                // Want coordinate, not distance so again transform from
                // coords to this measure (for same reason as above).
                coord2Oned( cfrm, axis - 1, dim, nax, coords, grid );
            }

            // Create the LutMap for axis 1
            Mapping xmap = new LutMap( grid, 1.0, 1.0 );

            // Create a CmpMap using a unit mapping for the second axis.
            map = new CmpMap( xmap, new UnitMap( 1 ), false );

            // Frame representing input coordinates, uses GRID axis of
            // base frame and a default axis
            int[] iaxes = new int[2];
            iaxes[0] = 1;
            iaxes[1] = 0;
            Frame frame1 = astRef.getFrame( FrameSet.AST__BASE );
            frame1 = frame1.pickAxes( iaxes.length, iaxes, null );

            // Set up label, symbol and units for axis 2
            frame1.setC( "Symbol(2)", "Data" );

            if ( ! label.equals( "")  ) {
                frame1.setC( "Label(2)", label );
            }
            else {
                frame1.setC( "Label(2)", "Data value" );
            }

            if ( ! units.equals( "" ) ) {
                frame1.setC( "Unit(2)", units );
            }

            // Clear domain and title which are now incorrect
            frame1.clear( "Domain" );
            frame1.clear( "Title" );

            // Coordinate or distance-v-data frame, uses selected axis from
            // the current frame and a default axis */
            iaxes[0] = axis;
            iaxes[1] = 0;
            Frame frame2 = cfrm.pickAxes( iaxes.length, iaxes, null );

            // Clear digits and format, unless set in the input
            // frameset. These can make a mess of SkyFrame formatting
            // otherwise.
            if ( ! cfrm.test( "Format(" + axis + ")" ) ) {
                frame2.clear( "format(1)" );
            }
            if ( ! cfrm.test( "Format(" + axis + ")" ) ) {
                frame2.clear( "format(1)" );
            }
            if ( ! cfrm.test( "Digits(" + axis + ")" ) ) {
                frame2.clear( "digits(1)" );
            }

            // If using distance then set appropriate attributes
            if ( dist ) {
                frame2.setC( "Label(1)", "Offset" );
                frame2.setC( "Symbol(1)", "Offset" );
            }

            // Set symbol, label and units for second axis */
            frame2.setC( "Symbol(2)", "Data" );
            if ( ! label.equals( "" ) ) {
                frame2.setC( "Label(2)", label );
            }
            else {
                frame2.setC( "Label(2)", "Data value" );
            }
            if ( !units.equals( "" ) ) {
                frame2.setC( "Unit(2)", units );
            }

            // The domain of this frame is "DATAPLOT" (as in KAPPA)
            frame2.setC( "Domain", "DATAPLOT" );

            // Now create the output frameset, which has frame2 as current
            // and frame1 as base.
            result = new FrameSet( frame1 );
            result.addFrame( FrameSet.AST__BASE, map, frame2 );
        }
        catch (Exception e) {
            // Just let it pass;
        }
        return result;
    }

    /**
     * Creates a FrameSet that only has one input and one output axis
     * using a given base axis as the selector.
     */
    public static FrameSet extract1DFrameSet( FrameSet frameset, int axis )
    {
        // Determine the current number of input and output axes and
        // take a copy of the input frameset
        int nin = frameset.getI( "Nin" );
        int nout = frameset.getI( "Nout" );
        FrameSet framecopy = (FrameSet) frameset.copy();

        // The requested axis must be valid, if not we adopt the
        // default of axis 1
        int[] iaxis = { axis };
        if ( iaxis[0] > nin ) {
            iaxis[0] = 1;
        }
        else if ( iaxis[0] < 1 ) {
            iaxis[0] = 1;
        }

        // If base frame has more than one axis then select the given
        // one.  This is easy, just pick a frame with the appropriate
        // axes and put it back, note that we have to pick the current
        // frame, so swap things around a little.
        if ( nin != 1 ) {
            framecopy.invert();
            Mapping[] joined = new Mapping[1];
            Frame tmpframe = framecopy.pickAxes( iaxis.length, iaxis, joined );
            framecopy.addFrame( FrameSet.AST__CURRENT, joined[0], tmpframe );
            framecopy.invert();
        }

        // Select an axis in the current frame and tack this onto the
        // end. Same procedure as above, just no inversion. This used
        // to attempt to pick the most significant axes, but the core
        // was actually broken and isn't repeated here.
        if ( nout != 1 ) {
            Mapping[] joined = new Mapping[1];
            Frame tmpframe = framecopy.pickAxes( iaxis.length, iaxis, joined );
            framecopy.addFrame( FrameSet.AST__CURRENT, joined[0], tmpframe );
        }
        return framecopy;
    }

    /**
     * Convert and normalize a series of coordinates measured along a
     * line (in some N-d frame coordinate system) into coordinates of
     * one of the axis of the frame (say wavelength, RA or DEC).
     *
     * @param frame the Frame
     * @param iaxis the selected axis
     * @param nax the number of axes in the frame.
     * @param dim the number of positions
     * @param pos an array holding the co-ordinates at npos positions
     *            within the supplied Frame (shape [nax, dim]).
     *     axval  The normalised coordinates
     */
    protected static void coord2Oned( Frame frame, int iaxis,
                                      int dim, int nax,
                                      double[] pos, double[] axval )
    {
        double[] work = new double[MAXDIM];

        for ( int i = 0; i < dim; i++ ) {

            /*  Copy and normalize this position */
            for ( int j = 0; j < nax; j++ ) {
                work[j] = pos[dim*j+i];
            }
            frame.norm( work );

            /*  Store the selected coordinate */
            axval[i] = work[iaxis];
        }
    }

    /**
     * Convert a series of coordinates measured along a line (in some
     * N-d frame coordinate system) into distances between the
     * positions. Note that Geodesic distances within the supplied
     * Frame are used.
     *
     * @param frame  An AST pointer to the frame.
     * @param npos   The size of the first dimension of the pos array.
     * @param nax    The number of axes in the frame.
     * @param pos    An array holding the co-ordinates at npos positions
     *               within the supplied Frame (shape [nax, npos]).
     * @param dis    The distance along the path to each position, starting
     *               at the first position.
     */
    static void coord2Dist( Frame frame, int npos, int nax, double[] pos,
                            double[] dis )
    {
        double[] p1 = new double[MAXDIM];
        double[] p2 = new double[MAXDIM];

        // Store the first position
        for ( int j = 0; j < nax; j++ ) {
            p1[j] = pos[npos*j];
        }

        // First distance is zero
        dis[0] = 0.0;

        // Now offset to all other positions
        for ( int i = 1; i < npos; i++ ) {

            // If the previous distance is known
            if ( dis[i-1] != AstObject.AST__BAD ) {

                // Store next position
                for ( int j = 0; j < nax; j++ ) {
                    p2[j] = pos[npos*j+i];
                }

                // Add distance between these positions to the
                // distance to the last position

                double inc = frame.distance( p1, p2 );
                if ( inc != AstObject.AST__BAD ) {
                    dis[i] = dis[i-1] + inc;
                }
                else {
                    dis[i] = AstObject.AST__BAD;
                }

                // This position becomes previous one
                for ( int j = 0; j < nax; j++ ) {
                    p1[j] = p2[j];
                }
            }
            else {
                // Previous distance was BAD, so this is
                dis[i] = AstObject.AST__BAD;
            }
        }
    }
}
