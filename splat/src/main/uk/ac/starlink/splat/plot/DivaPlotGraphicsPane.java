/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     11-JAN-2001 (Peter W. Draper):
 *        Original version.
 *     19-DEC-2003 (Peter W. Draper):
 *        Major re-write to a more generic form (to work with
 *        DivaPlotCanvasDraw).
 */
package uk.ac.starlink.splat.plot;

import diva.canvas.Figure;
import diva.canvas.FigureDecorator;
import diva.canvas.FigureLayer;
import diva.canvas.GraphicsPane;
import diva.canvas.event.MouseFilter;
import diva.canvas.interactor.SelectionInteractor;
import diva.canvas.interactor.SelectionListener;
import diva.canvas.interactor.Interactor;
import diva.canvas.interactor.SelectionModel;

import java.awt.event.InputEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.CmpMap;

import uk.ac.starlink.diva.DrawActions;    // for javadocs
import uk.ac.starlink.diva.DrawGraphicsPane;
import uk.ac.starlink.diva.DrawFigure;
import uk.ac.starlink.diva.DrawBasicFigure;
import uk.ac.starlink.diva.DrawFigureFactory;
import uk.ac.starlink.diva.FigureProps;

/**
 * This class extends {@link DrawGraphicsPane} to resize Figures so
 * that they keep their aspect ratio to the DivaPlot.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see DivaPlot
 * @see DrawGraphicsPane
 * @see DrawActions
 */
public class DivaPlotGraphicsPane
    extends DrawGraphicsPane
{
    /**
     *  Constructor accepts a FigureDecorator. Normally this will be tuned
     *  to interact with all the types of figures that will be offered
     *  (see {@link DrawActions} and the TypedDecorator it creates).
     */
    public DivaPlotGraphicsPane( FigureDecorator decorator )
    {
        super( decorator );
    }

    /**
     * Return the current properties of a figure.
     */
    public FigureProps getFigureProps( DrawFigure figure )
    {
        return DrawFigureFactory.getReference().getFigureProps( figure );
    }

    /**
     * Dummy Figure for setting TransformFreely state.
     */
    private DrawBasicFigure dummyFigure = new DrawBasicFigure();

    /**
     *  Transform the positions of all figures from one graphics
     *  coordinate system to another. The first AST mapping should
     *  transform from old graphics coordinates to some intermediary
     *  system (like wavelength,counts) and the second back from this
     *  system to the new graphics coordinates.
     */
    public void astTransform( Mapping oldMapping, Mapping newMapping )
    {
        // Switch off figure resizing constraints
        dummyFigure.setTransformFreely( true );

        // Create a simplified series map of the two mappings. Note
        // newMapping is inverted temporarily.
        boolean invert = newMapping.getInvert();
        newMapping.setInvert( true );
        CmpMap cmpmap = new CmpMap( oldMapping, newMapping, true );
        Mapping smap = cmpmap.simplify();

        AffineTransform at;
        Figure figure;
        Rectangle2D rect;
        double xscale;
        double yscale;
        double[] oldCoords = new double[4];
        double[][] newCoords;

        Iterator it = getFigureLayer().figures();
        while ( it.hasNext() ) {

            figure = (Figure) it.next();
            rect = figure.getBounds();

            oldCoords[0] = rect.getX();
            oldCoords[1] = rect.getY();
            oldCoords[2] = oldCoords[0] + rect.getWidth();
            oldCoords[3] = oldCoords[1] + rect.getHeight();

            newCoords = ASTJ.astTran2( smap, oldCoords, true );

            xscale = ( newCoords[0][1] - newCoords[0][0] ) /
                        ( oldCoords[2] - oldCoords[0] );
            yscale = ( newCoords[1][1] - newCoords[1][0] ) /
                        ( oldCoords[3] - oldCoords[1] );

            at = new AffineTransform();
            at.translate( newCoords[0][0], newCoords[1][0] );
            at.scale( xscale, yscale );
            at.translate( -oldCoords[0], -oldCoords[1] );

            figure.transform( at );
        }
        dummyFigure.setTransformFreely( false );
        newMapping.setInvert( invert );
    }
}
