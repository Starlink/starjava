/*
 * ESO Archive
 *
 * $Id: GridBagUtil.java,v 1.5 2002/07/09 13:30:38 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.util.gui;

import java.awt.*;
import javax.swing.*;

/**
 * Utility class for use with the GridBagLayout layout manager.
 */
public class GridBagUtil {

    private GridBagLayout layout;
    private Container container;

    public GridBagUtil(Container container) {
        this.container = container;
        layout = new GridBagLayout();
        container.setLayout(layout);
    }

    public GridBagUtil(Container container, GridBagLayout layout) {
        this.layout = layout;
        this.container = container;
    }

    /**
     * Add the given component to the given container with the given options.
     */
    public void add(Component component,
                    int gridx, int gridy,
                    int gridwidth, int gridheight,
                    double weightx, double weighty,
                    int fill, int anchor, Insets insets,
                    int ipadx, int ipady) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.gridwidth = gridwidth;
        gbc.gridheight = gridheight;
        gbc.fill = fill;
        gbc.anchor = anchor;
        gbc.weightx = weightx;
        gbc.weighty = weighty;
        gbc.insets = insets;
        gbc.ipadx = ipadx;
        gbc.ipady = ipady;
        layout.setConstraints(component, gbc);
        container.add(component);
    }

    /**
     * Add the given component to the given container with the given options.
     */
    public void add(Component component,
                    int gridx, int gridy,
                    int gridwidth, int gridheight,
                    double weightx, double weighty,
                    int fill, int anchor, Insets insets) {

        add(component, gridx, gridy, gridwidth, gridheight,
                weightx, weighty, fill, anchor, insets, 0, 0);
    }

    /**
     * Add the given component to the given container with the given options and
     * default insets.
     */
    public void add(Component component,
                    int gridx, int gridy,
                    int gridwidth, int gridheight,
                    double weightx, double weighty,
                    int fill, int anchor) {

        add(component, gridx, gridy, gridwidth, gridheight,
                weightx, weighty, fill, anchor,
                new Insets(1, 2, 1, 2));
    }
}

