/*
 * ESO Archive
 *
 * $Id: ImageProperties.java,v 1.8 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/05/03  Created
 */

package jsky.image.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeSet;

import javax.media.jai.PlanarImage;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;

import jsky.image.ImageChangeEvent;
import jsky.util.gui.SortedJTable;


/**
 * Dialog to view the image properties.
 *
 * @version $Revision: 1.8 $
 * @author Allan Brighton
 */
public class ImageProperties extends JPanel {

    // The top level parent frame (or internal frame) used to close the window
    protected Component parent;

    // The image being displayed
    protected MainImageDisplay imageDisplay;

    // The table used to display the properties
    protected SortedJTable table;

    /**
     * Make a window to display the image properties for the image being displayed
     * in the given ImageDisplay window.
     *
     * @param parent The top level parent frame (or internal frame) used to close the window
     * @param imageDisplay The image display window
     */
    public ImageProperties(Component parent, MainImageDisplay imageDisplay) {
        this.parent = parent;
        this.imageDisplay = imageDisplay;

        setLayout(new BorderLayout());

        add(makeTable(), BorderLayout.CENTER);
        add(makeButtonPanel(), BorderLayout.SOUTH);

        // register to receive notification when the image changes
        imageDisplay.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent ce) {
                ImageChangeEvent e = (ImageChangeEvent) ce;
                if (e.isNewImage() && !e.isBefore())
                    updateDisplay();
            }
        });


        // initialize the display
        updateDisplay();
    }


    /**
     * Make and return the handle for the table window
     */
    JScrollPane makeTable() {
        table = new SortedJTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane propertyScrollPane = new JScrollPane(table);
        return propertyScrollPane;
    }


    /**
     * Make the dialog button panel
     */
    JPanel makeButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JButton closeButton = new JButton("Close");
        panel.add(closeButton);
        closeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                close();
            }
        });

        return panel;
    }


    /**
     * Update the display to show the current values
     */
    public void updateDisplay() {
        PlanarImage image = imageDisplay.getImageProcessor().getSourceImage();
        if (image == null)
            return;
        String[] columnNames = {"Property Name", "Value"};
        String[] propertyNames = image.getPropertyNames();
        if (propertyNames == null)
            return;
        int numProperties = propertyNames.length;

        // Sort the property keyword and remove the ones that can't be displayed
        // (they might not all be strings...)
        TreeSet treeSet = new TreeSet();
        int n = 0;
        for (int i = 0; i < numProperties; i++) {
            String name = propertyNames[i];
            // note: special properties may start with "#" and are not listed
            if (name == null || name.length() == 0 || name.startsWith("#")) {
                continue;
            }
            // XXX who is converting the keywords to lower case?
            treeSet.add(name.toUpperCase());
            n++;
        }

        Object[] keys = treeSet.toArray();
        numProperties = keys.length;
        String[][] values = new String[numProperties][2];
        for (int i = 0; i < numProperties; i++) {
            String name = (String) (keys[i]);
            values[i][0] = name;
            Object property = image.getProperty(name);
            if (property == null)
                continue;
            values[i][1] = property.toString();
        }
        table.setModel(new DefaultTableModel(values, columnNames));
        table.sizeColumnsToFit(-1);  // XXX workaround for Java bugID# 4226181
    }


    /**
     * Close the window
     */
    void close() {
        if (parent != null)
            parent.setVisible(false);
    }
}

