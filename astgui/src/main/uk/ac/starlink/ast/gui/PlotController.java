/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     26-SEP-2002 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import java.awt.Color;

/**
 * An interface for interacting with a PlotConfigurator. This defines
 * methods that make requests for the current PlotConfiguration to be
 * applied, plus interactions for setting the background colour of the
 * object hosting the Plot, plus setting the visible data limits of
 * the Plot.
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
     * Apply a colour to the Component that contains the Plot.
     */
    public void setPlotColour( Color color );
}
