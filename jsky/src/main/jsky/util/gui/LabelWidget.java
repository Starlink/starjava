/*
 * ESO Archive
 *
 * $Id: LabelWidget.java,v 1.2 2002/07/09 13:30:38 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.util.gui;

import java.awt.*;
import javax.swing.*;


/**
 * Base class for labeled widgets.
 */
public class LabelWidget extends JPanel {

    /** Label for teh widget */
    protected JLabel label;

    /** layout helper object */
    protected GridBagUtil layoutUtil;

    /**
     * Constructor.
     * @param text The text to display in the label.
     * @param anchor anchor position for the label: one of the GridBagConstraints constants (EAST, WEST, ...)
     */
    public LabelWidget(String text, int anchor) {
        setLayout(new GridBagLayout());
        layoutUtil = new GridBagUtil(this, (GridBagLayout) getLayout());

        label = new JLabel(text + ": ");

        //label.setForeground(Color.black);

        Font font = label.getFont();
        label.setFont(new Font(font.getName(), font.BOLD, font.getSize()));

        //             comp   x  y  w  h  wtx  wty  fill                           anchor
        layoutUtil.add(label, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.HORIZONTAL, anchor);
    }

    /**
     * Constructor.
     * @param text The text to display in the label.
     */
    public LabelWidget(String text) {
        this(text, GridBagConstraints.EAST);
    }


    /** Return the widget used to display the label. */
    public JLabel getLabel() {
        return label;
    }
}

