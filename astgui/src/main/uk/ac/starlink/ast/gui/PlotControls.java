/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-SEP-2002 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import javax.swing.JComponent;

/**
 * Interface for a set of controls that may be added to a
 * PlotConfigurator tabbed pane. The class that implements this
 * interface should be a sub-class of a JComponent, or return a
 * reference to a sub-class so that it can be added and realized in a
 * JPanel (the tabbed pane parent). 
 * <p>
 * It is expected that such a pane of controls relate to the
 * configuration of an AST {@link Plot}.
 *
 * @author Peter W. Draper
 * @version $Id$
 *
 * @see PlotConfigurator
 */
public interface PlotControls
{
    /**
     * Return a title for these controls (for the border).
     */
    public String getControlsTitle();

    /**
     * Return a short name for these controls (for the tab).
     */
    public String getControlsName();

    /**
     * Reset controls to the defaults.
     */
    public void reset();

    /**
     * Return a reference to the JComponent sub-class that will be
     * displayed (normally a reference to this).
     */
    public JComponent getControlsComponent();

    /**
     * Return reference to the PlotControlsModel. This defines the
     * actual state of the controls and stores the current values.
     */
    public AbstractPlotControlsModel getControlsModel();
}
