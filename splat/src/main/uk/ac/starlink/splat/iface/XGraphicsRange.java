// Copyright (C) 2002 Central Laboratory of the Research Councils

// History:
//    07-JAN-2001 (Peter W. Draper):
//       Original version.

package uk.ac.starlink.splat.iface;

import diva.canvas.event.EventLayer;
import diva.canvas.event.LayerListener;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.plot.DivaPlotGraphicsPane;
import uk.ac.starlink.splat.plot.DragRegion;
import uk.ac.starlink.splat.plot.FigureChangedEvent;
import uk.ac.starlink.splat.plot.FigureListener;
import uk.ac.starlink.splat.plot.FigureProps;
import uk.ac.starlink.splat.plot.XRangeFigure;
import uk.ac.starlink.splat.plot.DivaPlot;

/**
 * XGraphicsRange defines a class for creating and interacting (in a DivaPlot
 * sense) with an XRangeFigure. The XRangeFigure is created on a DivaPlot's
 * DivaPlotGraphicsPane. Provision to convert between the graphics coordinates
 * of the DivaPlot and the units of the X dimension (i.e. wavelength are provided)
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class XGraphicsRange 
    implements FigureListener
{
    /**
     * The DivaPlot on which the range is to be drawn.
     */
    protected DivaPlot plot = null;

    /**
     * The DivaPlotGraphicsPane.
     */
    protected DivaPlotGraphicsPane pane = null;

    /**
     * The default cursor.
     */
    protected Cursor defaultCursor = null;

    /**
     * Reference to the XRangeFigure (used to access it via
     * DivaPlotGraphicsPane).
     */
    protected XRangeFigure figure = null;

    /**
     * Listener for creation events.
     */
    protected LayerListener listener = null;
    /**
     * Description of the Field
     */
    protected FigureListener figureListener = null;
    /**
     * Description of the Field
     */
    protected EventLayer eventLayer = null;

    /**
     * The model that stores references to all ranges for this plot.
     */
    protected XGraphicsRangesModel model = null;

    /**
     * The colour of the figure.
     */
    protected Color colour = Color.green;

    /**
     * Whether figures are free in movement and initial size.
     */
    protected boolean constrain = true;

    /**
     * Create a range interactively.
     *
     * @param plot DivaPlot that is to display the range.
     * @param model XGraphicsRangesModel model that arranges to have the
     *      properties of the range displayed (may be null).
     * @param colour the colour of any figures.
     * @param constrain whether figures are fixed to move only in X and have
     *      initial size of the full Y dimension of plot.
     */
    public XGraphicsRange( DivaPlot plot, XGraphicsRangesModel model,
                           Color colour, boolean constrain )
    {
        setPlot( plot );
        this.model = model;
        this.colour = colour;
        this.constrain = constrain;
        getInitialPlace();
    }

    /**
     * Create a range non-interactively.
     *
     * @param plot DivaPlot that is to display the range.
     * @param model XGraphicsRangesModel model that arranges to have the
     *      properties of the range displayed (may be null).
     * @param colour the colour of any figures.
     * @param constrain whether figures are contrained to move only in X and
     *      have initial size of the full Y dimension.
     * @param range a pair of doubles containing the range (on physical
     *      coordinates) to be used.
     */
    public XGraphicsRange( DivaPlot plot, XGraphicsRangesModel model,
                           Color colour, boolean constrain, double[] range )
    {
        setPlot( plot );
        this.model = model;
        this.colour = colour;
        this.constrain = constrain;
        createFromRange( range );
    }

    /**
     * Set the DivaPlot and DivaPlotGraphicsPane.
     *
     * @param plot The new plot value
     */
    protected void setPlot( DivaPlot plot )
    {
        this.plot = plot;
        this.pane = plot.getGraphicsPane();
    }

    /**
     * Return a description of the range.
     *
     * @return Description of the Return Value
     */
    public String toString()
    {
        if ( figure != null ) {
            double[] range = getRange();
            return range[0] + " : " + range[1];
        }
        else {
            return super.toString();
        }
    }

    /**
     * Report if an XRangeFigure is the one being used here.
     *
     * @param extFigure Description of the Parameter
     * @return The figure value
     */
    public boolean isFigure( XRangeFigure extFigure )
    {
        if ( figure != null ) {
            return figure.equals( extFigure );
        }
        return false;
    }

    /**
     * Return the span of the range in physical coordinates not graphics.
     *
     * @return The range value
     */
    public double[] getRange()
    {
        double[] result = new double[2];
        if ( figure != null ) {
            FigureProps props = pane.getFigureProps( figure );
            double[] gRange = new double[4];
            gRange[0] = props.getXCoordinate();
            gRange[1] = props.getYCoordinate();
            gRange[2] = props.getXCoordinate() + props.getXLength();
            gRange[3] = props.getYCoordinate() + props.getYLength();
            Mapping astMap = plot.getMapping();
            double[][] wRange = ASTJ.astTran2( astMap, gRange, true );
            result[0] = wRange[0][0];
            result[1] = wRange[0][1];
        }
        return result;
    }

    /**
     * Set the span of the range. The coordinates are physical not graphics.
     *
     * @param range a pair of values in physical coordinates.
     */
    public void setRange( double[] range )
    {
        if ( figure != null ) {

            //  Transform positions into graphics coordinates.
            double[] wRange = new double[4];
            wRange[0] = range[0];
            wRange[1] = 0.0;
            // TODO: should be a valid Y coordinate.
            wRange[2] = range[1];
            wRange[3] = 0.0;
            Mapping astMap = plot.getMapping();
            double[][] gRange = ASTJ.astTran2( astMap, wRange, false );

            //  Transform graphics coordinates from old values to new values.
            FigureProps props = pane.getFigureProps( figure );
            double scale = ( gRange[0][1] - gRange[0][0] ) / props.getXLength();
            AffineTransform at = new AffineTransform();
            at.translate( gRange[0][0], 0.0 );
            at.scale( scale, 1.0 );
            at.translate( -props.getXCoordinate(), 0.0 );
            figure.transform( at );
        }
    }

    /**
     * Get the initial place that a range should be drawn at.
     */
    protected void getInitialPlace()
    {
        figureListener =
            new FigureListener()
            {
                public void figureCreated( FigureChangedEvent e )
                {
                    // Do nothing
                }

                public void figureRemoved( FigureChangedEvent e )
                {
                    createFromDrag( e );
                }

                public void figureChanged( FigureChangedEvent e )
                {
                    // Do nothing
                }
            };
        pane.disableFigureDraggerSelection();
        pane.addFigureDraggerListener( figureListener );

        //  Change the cursor. This remains in effect until the
        //  createFromDrag method is called.
        defaultCursor = plot.getCursor();
        plot.setCursor( Cursor.getPredefinedCursor( Cursor.CROSSHAIR_CURSOR ) );
    }

    /**
     * Create the region at a place given by region dragged out on the canvas.
     *
     * @param e Description of the Parameter
     */
    protected void createFromDrag( FigureChangedEvent e )
    {
        Rectangle2D.Double figLimits =
            (Rectangle2D.Double) ( (DragRegion) e.getSource() ).getFinalShape();
        createFigure( figLimits );

        //  Remove drag region interactions.
        pane.removeFigureDraggerListener( figureListener );
        pane.enableFigureDraggerSelection();
        plot.setCursor( defaultCursor );
    }

    /**
     * Create the region using a given set of physical coordinates. The figure
     * is created with a full Y size.
     *
     * @param range Description of the Parameter
     */
    protected void createFromRange( double[] range )
    {
        Rectangle2D.Double figLimits =
            new Rectangle2D.Double( 0.0, 0.0, 1.0, 1.0 );
        boolean old = constrain;
        constrain = true;
        createFigure( figLimits );
        constrain = old;
        setRange( range );
    }

    /**
     * Create the figure with the given graphics coordinates range.
     *
     * @param limits Description of the Parameter
     */
    protected void createFigure( Rectangle2D.Double limits )
    {
        FigureProps props = null;
        if ( constrain ) {
            float[] gLimits = plot.getGraphicsLimits();
            props = new FigureProps( limits.x, gLimits[3], limits.width,
                                     gLimits[1] - gLimits[3], colour );
        }
        else {
            props = new FigureProps( limits.x, limits.y, limits.width,
                                     limits.height, colour );
        }
        if ( props.getXLength() == 0.0 ) {
            props.setXLength( 20.0 );
        }
        figure = (XRangeFigure) pane.addFigure( pane.XRANGE, props );
        figure.setConstrain( constrain );

        //  Register an interest in the fate of this figure.
        figure.addListener( this );

        //  Add this object to a suitable model.
        if ( model != null ) {
            model.addRange( this );
        }
    }

    /**
     * Delete the associated figure.
     */
    public void delete()
    {
        //  Set the figure invisible and repaint it (need to do this).
        figure.setVisible( false );
        figure.repaint();
        figure = null;
    }

    //  FigureListener interface.

    /**
     * Sent when the figure is created.
     *
     * @param e Description of the Parameter
     */
    public void figureCreated( FigureChangedEvent e )
    {
        //  Update something?
    }

    /**
     * Sent when the figure is removed.
     *
     * @param e Description of the Parameter
     */
    public void figureRemoved( FigureChangedEvent e )
    {
        // Kill this also and any references to it.
        figure = null;
    }

    /**
     * Sent when the figure is changed (i.e. moved or transformed).
     *
     * @param e Description of the Parameter
     */
    public void figureChanged( FigureChangedEvent e )
    {
        // Update any references to show new coordinates.
        model.changeRange( pane.indexOf( (XRangeFigure) e.getSource() ) );
    }

}
