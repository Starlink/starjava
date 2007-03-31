/*
 * Copyright (C) 2001-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     07-JAN-2001 (Peter W. Draper):
 *        Original version.
 */
package uk.ac.starlink.splat.iface;

import diva.canvas.event.LayerListener;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.diva.DrawActions;
import uk.ac.starlink.diva.DrawFigureFactory;
import uk.ac.starlink.diva.FigureChangedEvent;
import uk.ac.starlink.diva.FigureListener;
import uk.ac.starlink.diva.FigureProps;
import uk.ac.starlink.diva.XRangeFigure;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.plot.DivaPlot;
import uk.ac.starlink.splat.plot.DivaPlotGraphicsPane;

/**
 * XGraphicsRange defines a class for creating and interacting (in a DivaPlot
 * sense) with an XRangeFigure. The XRangeFigure is created on a DivaPlot's
 * DivaPlotGraphicsPane. Provision to convert between the graphics coordinates
 * of the DivaPlot and the units of the X dimension (i.e. wavelength are
 * provided)
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
     * DivaPlot DrawActions.
     */
    protected DrawActions drawActions = null;

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
     * Listener for changes to the figure.
     */
    protected FigureListener figureListener = null;

    /**
     * The model that stores references to all ranges for this plot.
     */
    protected XGraphicsRangesModel model = null;

    /**
     * The fill colour of the figure, XRangeFigures are not filled by
     * default, which isn't so good.
     */
    protected Color colour = Color.green;

    /**
     * Whether figures are free in movement and initial size.
     */
    protected boolean constrain = true;

    /**
     * Create a range interactively or non-interactively.
     *
     * @param plot DivaPlot that is to display the range.
     * @param model XGraphicsRangesModel model that arranges to have the
     *              properties of the range displayed (may be null).
     * @param colour the colour of any figures.
     * @param constrain whether figures are fixed to move only in X and have
     *                  initial size of the full Y dimension of plot.
     * @param range a pair of doubles containing the range (in physical
     *              coordinates) to be used. Set null if the figure is to be
     *              created interactively.
     */
    public XGraphicsRange( DivaPlot plot, XGraphicsRangesModel model,
                           Color colour, boolean constrain, double[] range )
    {
        this( plot, model, colour, constrain );
        if ( range == null ) {
            startInteraction();
        }
        else {
            createFromRange( range );
        }
    }

    /**
     * Create a range, without any coordinates. Needed for subclasses that
     * want to create ranges, but need to defer the side-effects of creation
     * until the scope returns to their constructor. Must be followed by a
     * call to {@link createFromRange} or {@link startInteraction}.
     *
     * @param plot DivaPlot that is to display the range.
     * @param model XGraphicsRangesModel model that arranges to have the
     *              properties of the range displayed (may be null).
     * @param colour the colour of any figures.
     * @param constrain whether figures are fixed to move only in X and have
     *                  initial size of the full Y dimension of plot.
     */
    protected XGraphicsRange( DivaPlot plot, XGraphicsRangesModel model,
                              Color colour, boolean constrain )
    {
        setPlot( plot );
        this.model = model;
        this.colour = colour;
        this.constrain = constrain;
    }

    /**
     * Set the DivaPlot and DivaPlotGraphicsPane.
     *
     * @param plot The new plot value
     */
    protected void setPlot( DivaPlot plot )
    {
        this.plot = plot;
        pane = (DivaPlotGraphicsPane) plot.getGraphicsPane();
        drawActions = plot.getDrawActions();
    }

    /**
     * Return a description of the range.
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
     * @param extFigure figure to check
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
            gRange[0] = props.getX1();
            gRange[1] = props.getY1();
            gRange[2] = props.getX1() + props.getWidth();
            gRange[3] = props.getY1() + props.getHeight();
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
            wRange[2] = range[1];
            wRange[3] = 0.0;
            Mapping astMap = plot.getMapping();
            double[][] gRange = ASTJ.astTran2( astMap, wRange, false );

            //  Transform graphics coordinates from old values to new values.
            FigureProps props = pane.getFigureProps( figure );
            double scale = ( gRange[0][1] - gRange[0][0] ) / props.getWidth();
            AffineTransform at = new AffineTransform();
            at.translate( gRange[0][0], 0.0 );
            at.scale( scale, 1.0 );
            at.translate( -props.getX1(), 0.0 );
            figure.transform( at );
        }
    }

    /**
     * Configure the DivaPlot DrawActions instance to create an XRangeFigure.
     */
    protected void startInteraction()
    {
        drawActions.setDrawingMode( DrawActions.XRANGE );

        //  Need to see the completed event, this should be issued by the
        //  DivaPlotGraphicsPane and will be dispatched to figureCreated().
        drawActions.addFigureListener( this );
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
        FigureProps props = new FigureProps( limits.x, limits.y, limits.width,
                                             limits.height, colour );
        if ( props.getWidth() == 0.0 ) {
            props.setWidth( 20.0 );
        }
        figure = (XRangeFigure) 
            drawActions.createDrawFigure( DrawFigureFactory.XRANGE, props );
        registerFigure( figure );
    }

    /**
     * Register the XRangeFigure after it has been created.
     */
    protected void registerFigure( XRangeFigure figure )
    {
        this.figure = figure;
        figure.setFillPaint( colour );

        // If figure is contrained, fit it to the Y axis range.
        if ( constrain ) {
            float[] gLimits = plot.getGraphicsLimits();
            Rectangle2D r = (Rectangle2D) figure.getShape();
            r.setFrame( r.getX(), gLimits[3], r.getWidth(), 
                        gLimits[1] - gLimits[3] );
        }
        figure.setConstrain( constrain );

        //  Register an interest in the fate of this figure.
        figure.addListener( this );

        //  Add this object to a suitable model.
        if ( model != null ) {
            model.addRange( this );
        }

        //  Lower behind spectra and repaint.
        drawActions.lowerFigure( figure );
        figure.repaint();
    }

    /**
     * Delete the associated figure.
     */
    public void delete()
    {
        drawActions.deleteFigure( figure );
        figure = null;
    }

    //  FigureListener interface.

    /**
     * Sent when the figure is created.
     */
    public void figureCreated( FigureChangedEvent e )
    {
        //  Is this an XRangeFigure?
        if ( e.getSource() instanceof XRangeFigure ) {
            registerFigure( (XRangeFigure) e.getSource() );

            //  Remove interest in further creations.
            drawActions.removeFigureListener( this );
        }
    }

    /**
     * Sent when the figure is removed elsewhere.
     *
     * @param e Description of the Parameter
     */
    public void figureRemoved( FigureChangedEvent e )
    {
        // Kill this also and any references to it.
        figure = null;
    }

    /**
     * Sent when the figure is changed (i.e.&nbsp;moved or transformed).
     */
    public void figureChanged( FigureChangedEvent e )
    {
        // Update any references to show new coordinates.
        model.changeRange( pane.indexOf( (XRangeFigure) e.getSource() ) );
    }
}
