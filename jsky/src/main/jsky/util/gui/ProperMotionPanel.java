//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class ProperMotionPanel
//
//--- Description -------------------------------------------------------------
//	ProperMotionPanel is a user interface item that contains entry fields for
//	for Offsets to a Coordinate that include proper motion deviations
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	11/4/98     S. Grosvenor / Booz-Allen
//
//		Original implementation.
//
//--- DISCLAIMER---------------------------------------------------------------
//
//	This software is provided "as is" without any warranty of any kind, either
//	express, implied, or statutory, including, but not limited to, any
//	warranty that the software will conform to specification, any implied
//	warranties of merchantability, fitness for a particular purpose, and
//	freedom from infringement, and any warranty that the documentation will
//	conform to the program, or any warranty that the software will be error
//	free.
//
//	In no event shall NASA be liable for any damages, including, but not
//	limited to direct, indirect, special or consequential damages, arising out
//	of, resulting from, or in any way connected with this software, whether or
//	not based upon warranty, contract, tort or otherwise, whether or not
//	injury was sustained by persons or property or otherwise, and whether or
//	not loss was sustained from or arose out of the results of, or use of,
//	their software or services provided hereunder.
//
//=== End File Prolog =========================================================

package jsky.util.gui;

import java.awt.Insets;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;

import jsky.science.CoordinatesOffset;
import jsky.science.Coordinates;
import jsky.science.ProperMotion;
import jsky.util.ReplaceablePropertyChangeListener;
import jsky.util.ReplacementEvent;
import jsky.util.FormatUtilities;

/**
 * A component extends the CoordinatesOffsetPanel to include
 * proper motion information.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		1999.11.04
 * @author		S. Grosvenor
 **/
public class ProperMotionPanel extends JComponent implements ReplaceablePropertyChangeListener {

    private CoordinatesOffsetPanel fPanelOffset;
    private JLabel fRaPmLabel;
    private JLabel fDecPmLabel;
    private JLabel fRaPmUnits;
    private JLabel fDecPmUnits;
    private JTextField fRaPmField;
    private JTextField fDecPmField;
    protected int fOrientation;

    // Action commands
    protected static final String RAPROPERMOTION_PROPERTY = "RAProperMotion".intern();
    protected static final String DECPROPERMOTION_PROPERTY = "DECProperMotion".intern();

    public static final String PROPERMOTION_PROPERTY = "ProperMotion".intern();

    /** Orientation where RA is on top of DEC */
    public static final int VERTICAL = 1;

    /** Orientation where RA and DEC are side by side */
    public static final int HORIZONTAL = 0;

    /** Bound property name. */
    public static final String ORIENTATION_PROPERTY = "Orientation".intern();

    private ProperMotion fMotion;

    public ProperMotionPanel() {
        super();

        setLayout(new GridBagLayout());
        fMotion = new ProperMotion();

        fPanelOffset = new CoordinatesOffsetPanel();

        fRaPmLabel = new JLabel("+/-");
        fRaPmField = new JTextField(3 + CoordinatesOffset.NUM_DECIMAL);
        fRaPmUnits = new JLabel("/yr");

        fDecPmLabel = new JLabel("+/-");
        fDecPmField = new JTextField(3 + CoordinatesOffset.NUM_DECIMAL);
        fDecPmUnits = new JLabel("/yr");

        add(fPanelOffset);
        add(fRaPmLabel);
        add(fRaPmField);
        add(fRaPmUnits);
        add(fDecPmLabel);
        add(fDecPmField);
        add(fDecPmUnits);

        String tooltip = "Errors should formatted, +/-##.##, units are arcsec/yr";
        fRaPmField.setToolTipText(tooltip);
        fDecPmField.setToolTipText(tooltip);

        setProperMotion(new ProperMotion());
        setOrientation(HORIZONTAL);

        fPanelOffset.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {
                if (fMotion != null && event.getPropertyName().equals(CoordinatesOffsetPanel.COORDINATES_OFFSET_PROPERTY)) {
                    fMotion.setOffset((CoordinatesOffset) event.getNewValue());
                }
            }
        });

        fRaPmField.setActionCommand(RAPROPERMOTION_PROPERTY);
        fRaPmField.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                if (fMotion != null) {
                    double rapm = 0;
                    try {
                        rapm = Coordinates.convert(
                                new Double(fRaPmField.getText()).doubleValue(),
                                Coordinates.ARCSEC,
                                Coordinates.DEGREE);
                    }
                    catch (Exception e) {
                    } // degrees
                    fMotion.setRaError(rapm);
                }
            }
        });
        fDecPmField.setActionCommand(DECPROPERMOTION_PROPERTY);
        fDecPmField.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                if (fMotion != null) {
                    double Decpm = 0;
                    try {
                        Decpm = Coordinates.convert(
                                new Double(fDecPmField.getText()).doubleValue(),
                                Coordinates.ARCSEC,
                                Coordinates.DEGREE);
                    }
                    catch (Exception e) {
                    } // degrees
                    fMotion.setDecError(Decpm);
                }
            }
        });

    }

    public ProperMotion getProperMotion() {
        return fMotion;
    }

    public void setProperMotion(ProperMotion pm) {
        if (fMotion != null) fMotion.removePropertyChangeListener(this);
        fMotion = pm;
        if (fMotion != null) {
            fMotion.addPropertyChangeListener(this);
        }
        updateFields();
    }

    public void propertyChange(PropertyChangeEvent event) {
        updateFields();
    }

    public void replaceObject(ReplacementEvent event) {
        setProperMotion((ProperMotion) event.getNewValue());
    }

    private void updateFields() {
        if (fMotion != null) {
            fPanelOffset.setCoordinatesOffset(fMotion.getOffset());
            fRaPmField.setText(FormatUtilities.formatDouble(
                    Coordinates.convert(fMotion.getRaError(), Coordinates.DEGREE, Coordinates.ARCSEC),
                    CoordinatesOffset.NUM_DECIMAL));
            fDecPmField.setText(FormatUtilities.formatDouble(
                    Coordinates.convert(fMotion.getDecError(), Coordinates.DEGREE, Coordinates.ARCSEC),
                    CoordinatesOffset.NUM_DECIMAL));
        }
        else {
            fPanelOffset.setCoordinatesOffset(null);
            fRaPmField.setText("");
            fDecPmField.setText("");
        }
    }

    public void setReadOnly(boolean readOnly) {
        fPanelOffset.setReadOnly(readOnly);
        fRaPmField.setEnabled(!readOnly);
        fDecPmField.setEnabled(!readOnly);
    }

    public int getOrientation() {
        return fOrientation;
    }

    GridBagConstraints constraint = new GridBagConstraints();

    private void setConstraints(JComponent comp, int x, int y, int w, int h, int a) {
        constraint.weightx = 1.0;
        constraint.insets = new Insets(2, 2, 2, 2);

        constraint.gridx = x;
        constraint.gridy = y;
        constraint.gridheight = h;
        constraint.gridwidth = w;
        constraint.anchor = a;

        ((GridBagLayout) getLayout()).setConstraints(comp, constraint);
    }

    public void setOrientation(int orient) {
        if (fRaPmUnits == null) return; // too early

        firePropertyChange(ORIENTATION_PROPERTY, fOrientation, orient);
        fOrientation = orient;
        fPanelOffset.setOrientation(fOrientation);

        if (fOrientation == VERTICAL) {
            // offsets panel
            setConstraints(fPanelOffset, 0, 0, 1, 2, GridBagConstraints.WEST);

            setConstraints(fRaPmLabel, 1, 0, 1, 1, GridBagConstraints.EAST);
            setConstraints(fRaPmField, 2, 0, 1, 1, GridBagConstraints.WEST);
            setConstraints(fRaPmUnits, 3, 0, 1, 1, GridBagConstraints.WEST);

            fDecPmLabel.setText(fRaPmLabel.getText());
            fRaPmUnits.setVisible(true);

            setConstraints(fDecPmLabel, 1, 1, 1, 1, GridBagConstraints.EAST);
            setConstraints(fDecPmField, 2, 1, 1, 1, GridBagConstraints.WEST);
            setConstraints(fDecPmUnits, 3, 1, 1, 1, GridBagConstraints.WEST);
        }
        else // Horizontal
        {
            // offsets panel
            setConstraints(fPanelOffset, 0, 0, 1, 1, GridBagConstraints.WEST);

            setConstraints(fRaPmLabel, 1, 0, 1, 1, GridBagConstraints.WEST);
            setConstraints(fRaPmField, 2, 0, 1, 1, GridBagConstraints.WEST);
            setConstraints(fRaPmUnits, 3, 0, 1, 1, GridBagConstraints.WEST);

            fRaPmUnits.setVisible(false);
            fDecPmLabel.setText(", ");

            setConstraints(fDecPmLabel, 4, 0, 1, 1, GridBagConstraints.WEST);
            setConstraints(fDecPmField, 5, 0, 1, 1, GridBagConstraints.WEST);
            setConstraints(fDecPmUnits, 6, 0, 1, 1, GridBagConstraints.WEST);
        }
    }

    public static void main(String[] args) {
        ProperMotion offset = new ProperMotion(5., 5., Coordinates.ARCSEC);
        ProperMotionPanel panel = new ProperMotionPanel();
        panel.setProperMotion(offset);
        panel.setOrientation(HORIZONTAL);
        JOptionPane.showConfirmDialog(null, panel);
    }

}
