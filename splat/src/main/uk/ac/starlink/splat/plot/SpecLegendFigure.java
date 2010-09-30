/*
 * Copyright (C) 2009 Science and Technology Facilities Council
 *
 *  History:
 *     17-SEP-2009 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.plot;

import diva.canvas.event.MouseFilter;
import diva.canvas.interactor.BoundedDragInteractor;
import diva.canvas.interactor.Interactor;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import uk.ac.starlink.diva.DrawCompositeFigure;
import uk.ac.starlink.diva.DrawLabelFigure;
import uk.ac.starlink.diva.DrawRectangleFigure;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataComp;

/**
 * Create a composite figure that contains a labels and rendering of
 * a {@link SpecDataComp} instance to display a legend of the spectra
 * displayed in a {@link DivaPlot}.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see DrawCompositeFigure
 */
public class SpecLegendFigure
    extends DrawCompositeFigure
{
    /**
     * Reference to background Figure, just a box for all the legend.
     */
    private DrawRectangleFigure backFigure = null;

    /**
     * The {@link DivaPlot} instance we're working with.
     */
    private DivaPlot divaPlot = null;

    /**
     * Reference position for the legend.
     */
    private Point anchor = new Point( 100, 100 );

    /**
     * List of labels for spectra.
     */
    private ArrayList labels = new ArrayList();

    /**
     *  Create the legend, based on the given {@link SpecDataComp}.
     */
    public SpecLegendFigure( DivaPlot divaPlot )
    {
        super();
        this.divaPlot = divaPlot;

        //  Backing figure is a rectangle.
        backFigure = new DrawRectangleFigure( anchor.x, anchor.y, 
                                              200.0, 200.0 );
        setBackgroundFigure( backFigure );

        update();

    }

    protected void update()
    {
        //  Add the shortnames of the spectra.
        SpecData spectra[] = divaPlot.getSpecDataComp().get();
        double x[] = new double[2];
        double y[] = new double[2];
        y[0] = y[1] = anchor.y;
        x[0] = x[1] = anchor.x;
        x[1] += 50.0;
        for ( int i = 0; i < spectra.length; i++ ) {
            y[0] += 20.0;
            y[1] = y[0];
            DrawLabelFigure label =
                new DrawLabelFigure( spectra[i].getShortName() );
            label.translate( 55.0, y[0] );
            labels.add( label );
            add( label );

            //  And get the spectra to render themselves at our location.
            System.out.println( "drawLegendSpec: " + 
                                divaPlot.getGrf() + " " + 
                                x[0] + " " + y[0] + " -> " + 
                                x[1] + " " + y[1] );
            spectra[i].drawLegendSpec( divaPlot.getGrf(), x, y );
        }
    }

    /**
     * Set the local anchor position.
     */
    public void setLocalAnchor( Point anchor )
    {
        this.anchor = anchor.getLocation();
    }

    /**
     * Return a copy of the current local anchor position.
     */
    public Point getLocalAnchor()
    {
        return anchor.getLocation();
    }
}
