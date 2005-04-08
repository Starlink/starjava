/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     08-APR-2005 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.ast.gui.GraphicsEdges;
import uk.ac.starlink.splat.plot.DivaPlot;

/**
 * PlotGraphicsClipMenuItem is a simple view of a {@link GraphicsEdges} 
 * object, offering only the ability to switch the graphics clip on and off.
 * It extends a JCheckBoxMenuItem so can only be used in a menu.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see GraphicsEdges
 */
public class PlotGraphicsClipMenuItem
     extends JCheckBoxMenuItem
     implements ChangeListener
{
    /**
     * GraphicsEdges model for current state.
     */
    protected GraphicsEdges graphicsEdges = null;

    /**
     * The related DivaPlot object.
     */
    protected DivaPlot plot = null;

    /**
     * Create an instance.
     *
     * @param plot displays the spectra to be clipped or not.
     */
    public PlotGraphicsClipMenuItem( DivaPlot plot, String text )
    {
        super( text );
        setPlot( plot );
    }

    /**
     * Set the DivaPlot.
     *
     * @param control The new plot value
     */
    public void setPlot( DivaPlot plot )
    {
        this.plot = plot;
        setGraphicsEdges( plot.getGraphicsEdges() );
    }

    /**
     * Get reference to current DivaPlot.
     *
     * @return The plot value
     */
    public DivaPlot getPlot()
    {
        return plot;
    }

    /**
     * Set the GraphicsEdges object.
     *
     * @param graphicsEdges The new GraphicsEdges.
     */
    public void setGraphicsEdges( GraphicsEdges graphicsEdges )
    {
        this.graphicsEdges = graphicsEdges;
        graphicsEdges.addChangeListener( this );
        updateFromGraphicsEdges();
    }

    /**
     * Get copy of reference to current GraphicsEdges.
     *
     * @return The graphicsEdges value
     */
    public GraphicsEdges getGraphicsEdges()
    {
        return graphicsEdges;
    }

    /**
     * Update interface to reflect values of the current GraphicsEdges.
     */
    protected void updateFromGraphicsEdges()
    {
        //  Take care with the ChangeListener, we don't want to get into a
        //  loop.
        graphicsEdges.removeChangeListener( this );
        setState( graphicsEdges.isClipped() );
        graphicsEdges.addChangeListener( this );
    }

//
// Implement the ChangeListener interface
//
    /**
     * If the DataLimits object changes then we need to update the interface.
     */
    public void stateChanged( ChangeEvent e )
    {
        updateFromGraphicsEdges();
    }
}
