package uk.ac.starlink.frog.plot;

import diva.canvas.Figure;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.event.MouseFilter;
import diva.canvas.interactor.BoundsManipulator;
import diva.canvas.interactor.Manipulator;
import diva.canvas.interactor.PathManipulator;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionListener;
import diva.canvas.toolbox.TypedDecorator;
import diva.util.java2d.Polyline2D;

import java.awt.event.InputEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import uk.ac.starlink.frog.ast.AstUtilities;
import uk.ac.starlink.ast.Mapping;

/**
 * The pane for displaying any interactive graphics for a Plot that
 * should be resized.
 * <p>
 * Known figure types can be created and removed from the Pane.
 *
 * @since $Date$
 * @since 11-JAN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see Plot, PlotConfigurator
 */
public class DivaPlotGraphicsPane extends GraphicsPane
{
    /**
     * The controller
     */
    private DivaController controller;

    /**
     * The interactor to give to all figures
     */
    private SelectionInteractor selectionInteractor;

    /**
     * The layer to draw all figures in
     */
    private FigureLayer figureLayer;

    /**
     * Create an XRangeFigure Figure. This is useful for selecting a
     * wavelength range.
     */
    public static final int XRANGE = 0;

    /**
     * Create a rectangle with interior vertical line. This is useful
     * for selecting a wavelength range with a single special interior
     * position (i.e. a spectral line).
     */
    public static final int CENTERED_XRANGE = 1;

    /**
     * Create a simple rectangle.
     */
    public static final int RECTANGLE = 3;

    /**
     * Create a PolylineFigure.
     */
    public static final int POLYLINE = 4;

    /**
     * List of all figures.
     */
    private ArrayList figureList = new ArrayList();

    /**
     * DragRegion, used in addition to SelectionDragger. This is used
     * for implementing the interactive zoom functions.
     */
    private DragRegion dragRegion;

    /**
     *  Constructor.
     */
    public DivaPlotGraphicsPane()
    {
        super();

        // Get the figure layer
        figureLayer = getForegroundLayer();

        // Construct a simple controller and get the default selection
        // interactor.
        controller = new DivaController( this );
        selectionInteractor = controller.getSelectionInteractor();

	// Use a generic decorator that can be tuned to use
        // different actual manipulators according to the type of
        // figure. The default manipulator is a BoundsManipulator.
        Manipulator manipulator = new BoundsManipulator();
        TypedDecorator decorator = new TypedDecorator( manipulator );

        // Tell the controller to use this decorator for deciding how
        // to wrap selected figures.
        selectionInteractor.setPrototypeDecorator( decorator );

        //  Set manipulators for each figure type, if required.
        Manipulator man = new RangeManipulator();
        decorator.addDecorator( XRangeFigure.class, man );
        man = new PathManipulator();
        decorator.addDecorator( PolylineFigure.class, man );

        // Add the additional drag region interactor to work with
        // mouse button 2.
        MouseFilter mFilter = new MouseFilter( InputEvent.BUTTON2_MASK|
                                               InputEvent.BUTTON3_MASK );
        dragRegion = new DragRegion( this );
        dragRegion.setSelectionFilter( mFilter );
    }

    /**
     *  Add a figure to the canvas.
     *
     *  @param type the type of figure to create.
     *  @param props the initial properties of the figure.
     */
    public Figure addFigure( int type, FigureProps props )
    {
        Figure newFigure = null;
        switch ( type ) {
           case XRANGE:
               newFigure = createXRange( props );
               break;
           case CENTERED_XRANGE:
               newFigure = createXRangeWithFeature( props );
               break;
           case RECTANGLE:
               newFigure = createRectangle( props );
               break;
           case POLYLINE:
               newFigure = createPolyline( props );
               break;
        }
        recordFigure( newFigure );
        return newFigure;
    }

    /**
     *  Get the selection interactor.
     */
    public SelectionInteractor getSelectionInteractor()
    {
        return selectionInteractor;
    }

    /**
     *  Add a listener for any SelectionEvents.
     */
    public void addSelectionListener( SelectionListener l )
    {
        selectionInteractor.getSelectionModel().addSelectionListener(l);
    }

    /**
     *  Get a list of the currently selected Figures.
     */
    public Object[] getSelectionAsArray() {
        return selectionInteractor.getSelectionModel().getSelectionAsArray();
    }

    /**
     *  Get the figure layer that we draw into.
     */
    public FigureLayer getFigureLayer()
    {
        return figureLayer;
    }

    /**
     *  Add a FigureListener to the DragRegion used for interacting
     * with figures.
     */
  //  public void addFigureDraggerListener( FigureListener l )
  //  {
  //      controller.getSelectionDragger().addListener( l );
  //  }

    /**
     * Remove a FigureListener to the DragRegion used for interacting
     * with figures.
     */
  //  public void removeFigureDraggerListener( FigureListener l )
  //  {
 //       controller.getSelectionDragger().removeListener( l );
  //  }

    /**
     * Add a FigureListener to the DragRegion used for non-figure
     * selection work.
     */
   // public void addZoomDraggerListener( FigureListener l )
   // {
    //    dragRegion.addListener( l );
    //}

    /**
     * Remove a FigureListener from the DragRegion used for non-figure
     * selection work.
     */
  //  public void removeZoomDraggerListener( FigureListener l )
   // {
  //      dragRegion.removeListener( l );
  //  }

    /**
     *  Add a new XRangeFigure to the canvas
     */
    protected Figure createXRange( FigureProps props )
    {
        return new XRangeFigure( props.getXCoordinate(),
                                 props.getYCoordinate(),
                                 props.getXLength(),
                                 props.getYLength(),
                                 props.getMainColour() );
    }

    /**
     *  Add a new XRangeWithFeatureFigure to the canvas
     */
    protected Figure createXRangeWithFeature( FigureProps props )
    {
        return (Figure)
            new XRangeWithFeatureFigure( props.getXCoordinate(),
                                         props.getYCoordinate(),
                                         props.getXSecondary(),
                                         props.getXLength(),
                                         props.getYLength(),
                                         props.getMainColour(),
                                         props.getSecondaryColour() );
    }

    /**
     *  Add a new PlotRectangle to the canvas
     */
    protected Figure createRectangle( FigureProps props )
    {
        return new PlotRectangle( props.getXCoordinate(),
                                  props.getYCoordinate(),
                                  props.getXLength(),
                                  props.getYLength(),
                                  props.getMainColour() );
    }

    /**
     *  Add a new PolylineFigure to the canvas
     */
    protected Figure createPolyline( FigureProps props )
    {
        Polyline2D.Double d = new Polyline2D.Double();
        d.moveTo( props.getXCoordinate(), props.getYCoordinate() );
        d.lineTo( props.getXCoordinate()+50.0, props.getYCoordinate()+25.0 );
        return new PolylineFigure( d );
        //return new PolylineFigure( props.getXCoordinate(),
        //                           props.getYCoordinate(),
        //                           props.getMainColour() );
    }

    /**
     * Record the creation of a new figure.
     */
    protected void recordFigure( Figure newFigure )
    {
        figureLayer.add( newFigure );
        newFigure.setInteractor( selectionInteractor );
        figureList.add( newFigure );
    }

    /**
     * Return index of figure.
     */
    public int indexOf( Figure figure )
    {
        return figureList.indexOf( figure );
    }

    /**
     * Return the current properties of a figure.
     */
    public FigureProps getFigureProps( Figure figure )
    {
        Rectangle2D bounds = figure.getBounds();
        return new FigureProps( bounds.getX(), bounds.getY(),
                                bounds.getWidth(), bounds.getHeight() );
    }

    /**
     * Switch off selection using the drag box interactor.
     */
    public void disableFigureDraggerSelection()
    {
        //  Do this by the slight of hand that replaces the
        //  FigureLayer with one that has no figures.
        dragRegion.setFigureLayer( emptyFigureLayer );
    }
    private FigureLayer emptyFigureLayer = new FigureLayer();

    /**
     * Switch selection using the drag box interactor back on.
     */
    public void enableFigureDraggerSelection()
    {
        dragRegion.setFigureLayer( figureLayer );
    }

    /**
     *  Transform the positions of all figures from one graphics
     *  coordinate system to another. The first AST mapping should
     *  transform from old graphics coordinates to some intermediary
     *  system (like wavelength,counts) and the second back from this
     *  system to the new graphics coordinates.
     */
    //  TODO: could do all this using a single AffineTransform?
    public void astTransform( Mapping oldMapping, Mapping newMapping )
    {
        // Switch off figure resizing constraints
        new BasicPlotFigure().setTransformFreely( true );

        double[] oldCoords = new double[4];
        double[] tmpCoords = new double[4];
        double[][] neutralCoords = null;
        double[][] newCoords = null;
        for ( int i = 0; i < figureList.size(); i++ ) {

            Figure figure = (Figure) figureList.get( i );
            Rectangle2D rect = figure.getBounds();

            oldCoords[0] = rect.getX();
            oldCoords[1] = rect.getY();
            oldCoords[2] = rect.getX() + rect.getWidth();
            oldCoords[3] = rect.getY() + rect.getHeight();

            neutralCoords = AstUtilities.astTran2( oldMapping, oldCoords, true );

            tmpCoords[0] = neutralCoords[0][0];
            tmpCoords[1] = neutralCoords[1][0];
            tmpCoords[2] = neutralCoords[0][1];
            tmpCoords[3] = neutralCoords[1][1];

            newCoords = AstUtilities.astTran2( newMapping, tmpCoords, false );

            double xscale = ( newCoords[0][1] - newCoords[0][0] ) /
                            ( oldCoords[2] - oldCoords[0] );
            double yscale = ( newCoords[1][1] - newCoords[1][0] ) /
                            ( oldCoords[3] - oldCoords[1] );

            AffineTransform at = new AffineTransform();

            at.translate( newCoords[0][0], newCoords[1][0] );
            at.scale( xscale, yscale );
            at.translate( -oldCoords[0], -oldCoords[1] );

            figure.transform( at );
        }
        new BasicPlotFigure().setTransformFreely( false );
    }
}
