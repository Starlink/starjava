//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class CoordinatesOffsetPanel
//
//--- Description -------------------------------------------------------------
//	CoordinatesOffsetPanel is a user interface item that contains entry fields for
//	for Offsets to a Coordinate.
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
import jsky.util.FormatUtilities;

/**
 * A component that contains entry fields for coordinate offset.
 * Currently only supports offsets in arcsec, but allows setting RA/Dec offsets
 * and supports the global preferences on RA/DEC formatting
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		1999.11.04
 * @author		S. Grosvenor
 **/
public class CoordinatesOffsetPanel extends JComponent
        implements ActionListener, PropertyChangeListener {

    protected JLabel fRaLabel;
    protected JLabel fDecLabel;
    protected JLabel fRaUnits;
    protected JLabel fDecUnits;
    protected JTextField fRaField;
    protected JTextField fDecField;
    protected CoordinatesOffset fOldOffset;
    protected String fFormatString;
    protected int fOrientation;

    // Action commands
    protected static final String RAOFFSET_PROPERTY = "RAOffset".intern();
    protected static final String DECOFFSET_PROPERTY = "DECOffset".intern();

    /** Orientation where RA and DEC are side by side */
    public static final int HORIZONTAL = 0;

    /** Orientation where RA is on top of DEC */
    public static final int VERTICAL = 1;

    /** Bound property name. */
    public static final String COORDINATES_OFFSET_PROPERTY = "CoordinatesOffset".intern();

    /** Bound property name. */
    public static final String READ_ONLY_PROPERTY = "ReadOnly".intern();

    /** Bound property name. */
    public static final String ORIENTATION_PROPERTY = "Orientation".intern();

    public CoordinatesOffsetPanel() {
        super();

        setLayout(new GridBagLayout());

        fRaLabel = new JLabel("RA:");
        fRaField = new JTextField(6 + CoordinatesOffset.NUM_DECIMAL);
        String tooltip = "Offset formats should be: +/-##.##, units are arcsec";
        fRaLabel.setToolTipText(tooltip);
        fRaField.setToolTipText(tooltip);
        fRaUnits = new JLabel("arcsec");

        fDecLabel = new JLabel("Dec:");
        fDecField = new JTextField(6 + CoordinatesOffset.NUM_DECIMAL);
        fDecLabel.setToolTipText(tooltip);
        fDecField.setToolTipText(tooltip);
        fDecUnits = new JLabel("arcsec");

        fRaField.setActionCommand(RAOFFSET_PROPERTY);
        fRaField.addActionListener(this);
        fDecField.setActionCommand(DECOFFSET_PROPERTY);
        fDecField.addActionListener(this);

        add(fRaLabel);
        add(fRaField);
        add(fRaUnits);
        add(fDecLabel);
        add(fDecField);
        add(fDecUnits);

        fOldOffset = null;

        setOrientation(HORIZONTAL);
    }

    public CoordinatesOffset getCoordinatesOffset() {
        try {
            fOldOffset = textFieldToOffset();
            return fOldOffset;
        }
        catch (NumberFormatException ex) {
            return null;
        }
        catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public void setCoordinatesOffset(CoordinatesOffset offset) {
        CoordinatesOffset old = getCoordinatesOffset();
        fOldOffset = offset;

        String raString = "";
        String decString = "";
        if (offset != null) {
            raString = FormatUtilities.formatDouble(offset.getRa(Coordinates.ARCSEC), CoordinatesOffset.NUM_DECIMAL);
            decString = FormatUtilities.formatDouble(offset.getDec(Coordinates.ARCSEC), CoordinatesOffset.NUM_DECIMAL);
        }

        if (!fRaField.getText().equals(raString)) {
            fRaField.setText(raString);
        }

        if (!fDecField.getText().equals(decString)) {
            fDecField.setText(decString);
        }

        firePropertyChange(COORDINATES_OFFSET_PROPERTY, old, offset);
    }

    public void setCoordinatesOffset(String ra, String dec) {
        try {
            CoordinatesOffset hold = fOldOffset;
            CoordinatesOffset newPosition = stringToOffset(ra, dec);

            fOldOffset = newPosition;

            if (!fRaField.getText().equals(ra)) {
                fRaField.setText(ra);
            }

            if (!fDecField.getText().equals(dec)) {
                fDecField.setText(dec);
            }

            firePropertyChange(COORDINATES_OFFSET_PROPERTY, hold, newPosition);
        }
        catch (NumberFormatException ex) {
            FormatUtilities.writeError(this, "Unable to set coordinate offset: " + ex.toString());
        }
        catch (IllegalArgumentException ex) {
            FormatUtilities.writeError(this, "Unable to set coordinate offset: " + ex.toString());
        }

    }

    public boolean isReadOnly() {
        return (!fRaField.isEditable());
    }

    public void setReadOnly(boolean readOnly) {
        firePropertyChange(READ_ONLY_PROPERTY, !fRaField.isEditable(), readOnly);

        fRaField.setEditable(!readOnly);
        fDecField.setEditable(!readOnly);
    }

    public void setEnabled(boolean b) {
        super.setEnabled(b);
        setReadOnly(!b);
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
        firePropertyChange(ORIENTATION_PROPERTY, fOrientation, orient);
        fOrientation = orient;

        if (fOrientation == VERTICAL) {
            // * RA Panel *
            setConstraints(fRaLabel, 0, 0, 1, 1, GridBagConstraints.EAST);
            setConstraints(fRaField, 1, 0, 1, 1, GridBagConstraints.WEST);
            setConstraints(fRaUnits, 2, 0, 1, 1, GridBagConstraints.WEST);
            fRaUnits.setVisible(true);

            // * DEC Panel *
            setConstraints(fDecLabel, 0, 1, 1, 1, GridBagConstraints.EAST);
            setConstraints(fDecField, 1, 1, 1, 1, GridBagConstraints.WEST);
            setConstraints(fDecUnits, 2, 1, 1, 1, GridBagConstraints.WEST);
        }
        else // Horizontal 0 all one row
        {
            // * RA Panel *
            setConstraints(fRaLabel, 0, 0, 1, 1, GridBagConstraints.EAST);
            setConstraints(fRaField, 1, 0, 1, 1, GridBagConstraints.WEST);
            setConstraints(fRaUnits, 2, 0, 1, 1, GridBagConstraints.WEST);
            fRaUnits.setVisible(false);

            // * DEC Panel *
            setConstraints(fDecLabel, 3, 0, 1, 1, GridBagConstraints.EAST);
            setConstraints(fDecField, 4, 0, 1, 1, GridBagConstraints.WEST);
            setConstraints(fDecUnits, 5, 0, 1, 1, GridBagConstraints.WEST);
        }
    }

    protected CoordinatesOffset textFieldToOffset()
            throws NumberFormatException, IllegalArgumentException {
        return stringToOffset(fRaField.getText(), fDecField.getText());
    }

    protected CoordinatesOffset stringToOffset(String raString, String decString)
            throws NumberFormatException, IllegalArgumentException {
        double ra = new Double(raString).doubleValue();
        double dec = new Double(decString).doubleValue();
        return new CoordinatesOffset(ra, dec, Coordinates.ARCSEC);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == RAOFFSET_PROPERTY
                || e.getActionCommand() == DECOFFSET_PROPERTY) {
            try {
                CoordinatesOffset newOffset = textFieldToOffset();
                firePropertyChange(COORDINATES_OFFSET_PROPERTY, fOldOffset, newOffset);
                fOldOffset = newOffset;
            }
            catch (NumberFormatException ex) {
                showErrorDialog();
                if (fOldOffset != null) {
                    // Restore the old offset text since the new one is invalid
                    setCoordinatesOffset(fOldOffset);
                }
            }
            catch (IllegalArgumentException ex) {
                showErrorDialog();
                if (fOldOffset != null) {
                    // Restore the old offset text since the new one is invalid
                    setCoordinatesOffset(fOldOffset);
                }
            }
        }
    }

    public void propertyChange(PropertyChangeEvent event) {
        //System.out.println( " //CoordinatesPanel.pc: this=" +
        //    getObjectIdString(this) +
        //    ", prop=" + event.getPropertyName() +
        //    ", src=" + getObjectIdString( event.getSource()) +
        //    ", oldV=" + event.getOldValue() + ", newV=" + event.getNewValue());

        // Recalculate field contents to display new format
        if (fOldOffset != null) {
            setCoordinatesOffset(fOldOffset);
        }
    }

    /**
     * Shows an error dialog to the user which informs them that the
     * current input is not a valid set of coordinates.
     **/
    public void showErrorDialog() {
        String message = "Invalid format.  CoordinatesOffset should be"
                + "\nentered in the following format: ##.## arcsec";

        JOptionPane.showMessageDialog(this,
                message,
                "Invalid CoordinatesOffset",
                JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        CoordinatesOffset offset = new CoordinatesOffset(5., 5.,
                Coordinates.ARCSEC);
        CoordinatesOffsetPanel panel = new CoordinatesOffsetPanel();
        panel.setCoordinatesOffset(offset);
        //panel.setOrientation(VERTICAL);
        JOptionPane.showConfirmDialog(null, panel);
    }
}
