/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     29-MAR-2004 (Peter W. Draper):
 *        Original version.
 */
package uk.ac.starlink.splat.iface;

import diva.canvas.event.LayerListener;

import java.awt.Color;

import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.diva.DrawActions;
import uk.ac.starlink.diva.DrawFigureFactory;
import uk.ac.starlink.diva.FigureChangedEvent;
import uk.ac.starlink.diva.FigureListener;
import uk.ac.starlink.diva.FigureProps;
import uk.ac.starlink.diva.InterpolatedCurveFigure;
import uk.ac.starlink.diva.geom.InterpolatedCurve2D;
import uk.ac.starlink.diva.interp.Interpolator;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.plot.DivaPlot;
import uk.ac.starlink.splat.plot.DivaPlotGraphicsPane;
import uk.ac.starlink.splat.plot.PlotInterpolatorFactory;
import uk.ac.starlink.splat.util.LineInterpolator;

/**
 * ModelLine defines a class for creating and interacting (in a
 * {@link DivaPlot} sense) with an interpolated curve that displays a model
 * spectral line of some kind. The interpolated line is created on a
 * {@link DivaPlot}'s {@link DivaPlotGraphicsPane}. Provision to convert
 * between the graphics coordinates of the {@link DivaPlot} and the
 * natural (in this case the spectral line position, width and scale)
 * physical units of a spectrum are provided.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class ModelLine
    implements FigureListener
{
    /**
     * The DivaPlot on which the model line is drawn.
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
     * Reference to the InterpolatedCurveFigure (used to access it via
     * DivaPlotGraphicsPane).
     */
    protected InterpolatedCurveFigure figure = null;

    /**
     * Listener for creation events.
     */
    protected LayerListener listener = null;

    /**
     * Listener for changes to the figure.
     */
    protected FigureListener figureListener = null;

    /**
     * The JTable model that stores references to figures used by this object.
     * Also provides a view of the current states and the ability to
     * edit values.
     */
    protected ModelLineTableModel model = null;

    /**
     * Colour of the rendered model line.
     */
    protected Color colour = Color.red;

    /**
     * Create a range. Can be interactively created or non-interactively.
     *
     * @param plot DivaPlot that is to display the range.
     * @param model model that arranges to have the properties of the figure
     *              displayed (may be null).
     * @param colour the colour of any figures.
     * @param interpolator an Interpolator to use for creating the
     *                     figure.
     * @param interactive whether to create figure interactively,
     *                    otherwise it just uses the properties of the
     *                    interpolator.
     */
    public ModelLine( DivaPlot plot, ModelLineTableModel model,
                      Color colour, Interpolator interpolator, 
                      boolean interactive )
    {
        setPlot( plot );
        this.model = model;
        this.colour = colour;
        if ( interactive ) {
            startInteraction( interpolator );
        }
        else {
            createFigure( interpolator );
        }
    }

    /**
     * Create a range non-interactively.
     *
     * @param plot DivaPlot that is to display the range.
     * @param model model that arranges to have the properties of the figure
     *              displayed (may be null).
     * @param colour the colour of any figures.
     * @param interpolator an Interpolator to use for creating thge figure.
     */
    public ModelLine( DivaPlot plot, ModelLineTableModel model,
                      Color colour, Interpolator interpolator,
                      double[] props )
    {
        setPlot( plot );
        this.model = model;
        this.colour = colour;
        createFigure( interpolator );
        setProps( props );
    }

    /**
     * Set the {@link DivaPlot} used for drawing and interactions.
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
     * Return a description of the figure.
     */
    public String toString()
    {
        if ( figure != null ) {
            return figure.toString();
        }
        return "";
    }

    /**
     * Report if a InterpolatedCurveFigure is the one being used here.
     *
     * @param extFigure figure to check
     */
    public boolean isFigure( InterpolatedCurveFigure extFigure )
    {
        if ( figure != null ) {
            return figure.equals( extFigure );
        }
        return false;
    }

    /**
     * Return the physical properties of the figure. This
     * depends on the figure type, but can be assumed to be the X
     * coordinate of the line centre, the scale of the line, followed
     * by up to two widths, the Gaussian and Lorentzian widths,
     * whichever are appropriate. These are indexed by the CENTRE,
     * SCALE, GWIDTH and LWIDTH constants of the {@link LineInterpolator}
     * class.
     */
    public double[] getProps()
    {
        if ( figure != null ) {
            LineInterpolator interp = (LineInterpolator)
                ((InterpolatedCurve2D)figure.getShape()).getInterpolator();
            double[] xCoords = (double[]) interp.getXCoords().clone();
            double[] yCoords = (double[]) interp.getYCoords().clone();
            int n = xCoords.length;
            double xy[] = new double[n*2];
            for ( int i = 0, j = 0; i < n; i++ ) {
                xy[j++] = xCoords[i];
                xy[j++] = yCoords[i];
            }

            //  Transform to physical coordinates.
            Mapping astMap = plot.getMapping();
            double[][] xyt = ASTJ.astTran2( astMap, xy, true );

            for ( int i = 0; i < n; i++ ) {
                xCoords[i] = xyt[0][i];
                yCoords[i] = xyt[1][i];
            }

            //  Get interpolator to convert to line properties.
            return interp.getProps( xCoords, yCoords );
        }
        return null;
    }

    /**
     * Set the physical properties of the figure. This should be an
     * array of values indexed by the CENTRE, SCALE, GWIDTH and LWIDTH
     * constants of the {@link LineInterpolator} class.
     */
    public void setProps( double[] props )
    {
        if ( figure != null ) {
            LineInterpolator interp = (LineInterpolator)
                ((InterpolatedCurve2D)figure.getShape()).getInterpolator();

            //  Need to convert from physical to graphics coordinates
            //  for all values. Get the line to produce an array of
            //  coordinates that would describe this line, if the
            //  values were already in graphics coordinates.
            interp.setProps( props );

            double[] xCoords = (double[]) interp.getXCoords().clone();
            double[] yCoords = (double[]) interp.getYCoords().clone();
            int n = xCoords.length;
            double xy[] = new double[n*2];
            for ( int i = 0, j = 0; i < n; i++ ) {
                xy[j++] = xCoords[i];
                xy[j++] = yCoords[i];
            }

            //  Transform to graphics coordinates.
            Mapping astMap = plot.getMapping();
            double[][] xyt = ASTJ.astTran2( astMap, xy, false );

            for ( int i = 0; i < n; i++ ) {
                xCoords[i] = xyt[i][0];
                yCoords[i] = xyt[i][1];
            }

            //  Get interpolator to convert to line properties from
            //  coordinates.
            interp.setValues( xCoords, yCoords );
        }
    }

    /**
     * Configure the DivaPlot DrawActions instance to create an
     * InterpolatedCurveFigure with the correct Interpolator.
     */
    protected void startInteraction( Interpolator interpolator )
    {
        drawActions.setDrawingMode( DrawActions.CURVE );
        drawActions.setCurve
            ( PlotInterpolatorFactory.getInstance()
              .getInterpolatorType( interpolator ) );

        //  Need to see the completed event, this should be issued by the
        //  DivaPlotGraphicsPane and will be dispatched to figureCreated().
        drawActions.addFigureListener( this );
    }

    /**
     * Create the figure with the given Interpolator.
     *
     * @param limits Description of the Parameter
     */
    protected void createFigure( Interpolator interp )
    {
        FigureProps props = new FigureProps();
        props.setInterpolator( interp );
        figure = (InterpolatedCurveFigure)
            drawActions.createDrawFigure( DrawFigureFactory.CURVE, props );
        ((InterpolatedCurve2D)figure.getShape()).setInterpolator( interp );
        registerFigure( figure );
    }

    /**
     * Register the Figure after it has been created.
     */
    protected void registerFigure( InterpolatedCurveFigure figure )
    {
        this.figure = figure;
        figure.setFillPaint( colour );

        //  Register an interest in the fate of this figure.
        figure.addListener( this );

        //  Add this object to a suitable model.
        if ( model != null ) {
            model.addModelLine( this );
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
        //  Is this an InterpolatedCurveFigure?
        if ( e.getSource() instanceof InterpolatedCurveFigure ) {

            registerFigure( (InterpolatedCurveFigure) e.getSource() );

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
        model.change(pane.indexOf( (InterpolatedCurveFigure) e.getSource() ));
    }
}
