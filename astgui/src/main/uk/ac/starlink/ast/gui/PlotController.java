/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     26-SEP-2002 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import java.awt.Color;
import uk.ac.starlink.ast.Plot; // for javadocs
import uk.ac.starlink.ast.Frame; // for javadocs
import uk.ac.starlink.ast.FrameSet; // for javadocs

/**
 * An interface for interacting with a PlotConfigurator. This defines
 * methods that make requests for the current PlotConfiguration to be
 * applied, for a optional colour to be used for the background and
 * for access to the current Frame used by the Plot (so that values
 * can be formatted correctly for the axes being used, RA/Dec etc.).
 *
 * @author Peter W. Draper
 * @version $Id$
 *
 * @see Plot
 * @see PlotConfigurator
 * @see PlotConfiguration
 */
public interface PlotController
{
    /**
     * Apply the current PlotConfiguration object state to the Plot.
     * The current configuration can be obtained as single String
     * using the {@link PlotConfiguration.getAst} method.
     */
    public void updatePlot();

    /**
     * Apply a colour to the Component that contains the Plot. Only
     * needed if any {@link ComponentColourControls} instances are used,
     * otherwise the implementation may do nothing.
     */
    public void setPlotColour( Color color );

    /**
     * Get the current colour of the Component that contains the Plot. Only
     * needed if any {@link ComponentColourControls} instances are used,
     * otherwise the implementation may do nothing.
     */
    public Color getPlotColour();

    /**
     * Return a reference to the current {@link Frame} that is used
     * when creating the controlled {@link Plot}s. This is used for
     * formatting and unformatting data values in a way that is
     * natural for the Plot axes (i.e. can be used to enter RA and Dec
     * values as dd:hh:ss.ss). A {@link FrameSet} can be returned
     * (which could be the Plot if it is guaranteed to be always
     * available), in which case the current Frame of that will be used.
     * <p>
     * Note this may never return a null, if this may be the case then
     * consider using a different Frame/FrameSet (i.e. the one read
     * from the original dataset).
     */
    public Frame getPlotCurrentFrame();
}
