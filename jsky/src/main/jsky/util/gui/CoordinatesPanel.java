//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class CoordinatesPanel
//
//--- Description -------------------------------------------------------------
//	CoordinatesPanel is a user interface item that contains entry fields for
//	astronomical coordinates.  It includes entry fields for an RA and DEC,
//	provides validation, and includes methods for setting and getting
//	a Coordinates object.
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	02/09/98	J. Jones / 588
//
//		Original implementation.
//
//	07/24/98	J. Jones / 588
//
//		Added check for NumberFormatException on field validation,
//		presents error dialog if exception occurs.
//
//	09/21/98	J. Jones / 588
//
//		Added equinox.
//
//	09/23/98	J. Jones / 588
//
//		Now responds to changes in coordinates format.  Also displays
//      format in tooltips.
//
//  05/24/00 	F. Tanner / STScI
// 		Added the Equinox ComboBox to those items which are changed to
// 		editable/non-editable (in this case enabled/disabled) in the
// 		setReadOnly method.  This was done in support of OPR 41742.
//
// 	06/29/00 	F. Tanner / STScI
// 		Updated in support of OPR 41875.  Added a specialized FocusAdapter
//		called focusAdapterThatPostsActionEventsToJTextFields that does
//		just that.  This enables a pair of JTextFields to affect changes
// 		to the VTT after losing focus.  That way, the user doesn't have to
// 		hit "Enter" in each JTextField in order for the combined pair
// 		to take effect.
//
// 		I also made the object name VERY long so
// 		future developers wouldn't accidentally use it for some other object
// 		without recording the focusLost method.  Right now, the focusLost
// 		method casts the source object to a JTextField without catching
// 		possible ClassCastExceptions.  I didn't catch the exception on purpose
// 		because I didn't see a need to do so.  No one should use the
// 		focusAdapterThatPostsActionEventsToJTextFields on an object that is
// 		not a JTextField and if they do, then they deserve to throw exceptions.
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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;

import jsky.science.Coordinates;
import jsky.util.FormatUtilities;

/**
 * A component that contains entry fields for astronomical coordinates.
 * It includes entry fields for an RA and DEC, provides validation,
 * and includes methods for getting and setting	a Coordinates object.
 *
 * <P>This code was orginally developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project. Subsequently adapted
 * by STScI as part of the APT project
 *
 * @version		06.29.2000
 * @author		J. Jones / 588
 **/
public class CoordinatesPanel extends JComponent
        implements ActionListener, PropertyChangeListener {

    private JLabel fRaLabel;
    private JLabel fDecLabel;
    private JPanel fRaPanel;
    private JPanel fDecPanel;
    private JTextField fRaField;
    private JTextField fDecField;
    private int fOrientation;
    private Coordinates fOldPosition;
    private JLabel fEquinoxLabel;
    private JComboBox fEquinoxField;
    private boolean fEquinoxShown;
    private int fOldEquinox;
    private String fFormatString;

    private FocusAdapter focusAdapterThatPostsActionEventsToJTextFields;

    // Action commands
    protected static final String RA_NOTIFY = "RA Notify".intern();
    protected static final String DEC_NOTIFY = "DEC Notify".intern();
    protected static final String EQUINOX_NOTIFY = "Equinox Notify".intern();

    /** Orientation where RA and DEC are side by side */
    public static final int HORIZONTAL = 0;

    /** Orientation where RA is on top of DEC */
    public static final int VERTICAL = 1;

    /** Bound property name. */
    public static final String COORDINATES_PROPERTY = "Coordinates".intern();

    /** Bound property name. */
    public static final String READ_ONLY_PROPERTY = "ReadOnly".intern();

    /** Bound property name. */
    public static final String ORIENTATION_PROPERTY = "Orientation".intern();

    /** Bound property name. */
    public static final String EQUINOX_SHOWN_PROPERTY = "EquinoxShown".intern();

    /** Bound property name. */
    public static final String EQUINOX_PROPERTY = "Equinox".intern();

    public CoordinatesPanel() {
        super();

        setLayout(new GridBagLayout());

        focusAdapterThatPostsActionEventsToJTextFields
                = new FocusAdapter() {

                    public void focusLost(FocusEvent e) {
                        ((JTextField) e.getSource()).postActionEvent();
                    } // focusLost
                }; // focusAdapterThatPostsActionEventsToJTextFields

        fRaLabel = new JLabel("RA:");
        fRaField = new JTextField(6 + Coordinates.NUM_DECIMAL);
        fDecLabel = new JLabel("Dec:");
        fDecField = new JTextField(6 + Coordinates.NUM_DECIMAL);

        fRaField.setActionCommand(RA_NOTIFY);
        fRaField.addActionListener(this);
        fDecField.setActionCommand(DEC_NOTIFY);
        fDecField.addActionListener(this);

        Coordinates.addSeparatorStyleChangeListener(this);
        updateFromFormatChange();

        add(fRaLabel);
        add(fRaField);
        add(fDecLabel);
        add(fDecField);

        fOldPosition = null;
        fEquinoxShown = false;
        fEquinoxLabel = null;
        fEquinoxField = null;
        fOldEquinox = -1;

        setOrientation(HORIZONTAL);
    }

    public Coordinates getCoordinates() {
        try {
            if (fEquinoxShown) {
                String eqstring = (String) fEquinoxField.getSelectedItem();
                int equinox = Coordinates.equinoxStringToInt(eqstring);
                return Coordinates.valueOf(fRaField.getText(), fDecField.getText(), equinox);
            }
            else {
                return Coordinates.valueOf(fRaField.getText(), fDecField.getText());
            }
        }
        catch (NumberFormatException ex) {
            return null;
        }
        catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public void setCoordinates(Coordinates position) {
        Coordinates old = getCoordinates();

        firePropertyChange(COORDINATES_PROPERTY, old, position);

        fOldPosition = position;

        String raString = "";
        String decString = "";
        if (position != null) {
            raString = position.raToString();
            decString = position.decToString();
        }

        if (!fRaField.getText().equals(raString)) {
            fRaField.setText(raString);
        }

        if (!fDecField.getText().equals(decString)) {
            fDecField.setText(decString);
        }
    }

    public void setCoordinates(String ra, String dec) {
        try {
            Coordinates newPosition = Coordinates.valueOf(ra, dec);

            firePropertyChange(COORDINATES_PROPERTY, fOldPosition, newPosition);

            fOldPosition = newPosition;

            if (!fRaField.getText().equals(ra)) {
                fRaField.setText(ra);
            }

            if (!fDecField.getText().equals(dec)) {
                fDecField.setText(dec);
            }
        }
        catch (NumberFormatException ex) {
            FormatUtilities.writeError(this, "Unable to set coordinates: " + ex.toString());
        }
        catch (IllegalArgumentException ex) {
            FormatUtilities.writeError(this, "Unable to set coordinates: " + ex.toString());
        }
    }

    /**
     * Registers weather the RA and DEC fields of the CoordinatesPanel should
     * postActionEvents when they lose focus.  This method is in place so
     * that the user will not have to press "Enter" when going from
     * one of the text fields to another in order for both
     * modifications to take effect.
     *
     * @param postFlag Determines if the RA and DEC fields should
     * 		post ActionEvents when losing focus.
     */
    public void setPostActionEventOnFocusLost(boolean postFlag) {
        if (postFlag) {
            fRaField.addFocusListener(focusAdapterThatPostsActionEventsToJTextFields);
            fDecField.addFocusListener(focusAdapterThatPostsActionEventsToJTextFields);
        } // if
        else {
            fRaField.removeFocusListener(focusAdapterThatPostsActionEventsToJTextFields);
            fDecField.removeFocusListener(focusAdapterThatPostsActionEventsToJTextFields);
        } // else
    } // setPostActionEventOnFocusLost

    public boolean isReadOnly() {
        return (!fRaField.isEditable());
    }

    /**
     * Sets the fields on this panel as read-only depending on the value
     * of readOnly.  The equinox ComboBox is also enabled/disabled along
     * with this.
     *
     * @param readOnly If <code>true</code> then the input widgets of this
     * 		class are set as readonly.  If <code>false</code> then the
     * 		input widgets of this class are editable.
     */
    public void setReadOnly(boolean readOnly) {
        firePropertyChange(READ_ONLY_PROPERTY, !fRaField.isEditable(), readOnly);

        fRaField.setEditable(!readOnly);
        fDecField.setEditable(!readOnly);

        fEquinoxField.setEnabled(!readOnly);
    }

    public boolean isEquinoxShown() {
        return fEquinoxShown;
    }

    public void setEquinoxShown(boolean show) {
        boolean old = fEquinoxShown;
        fEquinoxShown = show;

        firePropertyChange(EQUINOX_SHOWN_PROPERTY, old, fEquinoxShown);

        setOrientation(fOrientation); // rebuild GUI
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

        if (fEquinoxShown && fEquinoxField == null) {
            // Create and add the equinox fields

            fEquinoxLabel = new JLabel("Equinox:");
            fEquinoxField = new JComboBox();

            String[] eqs = Coordinates.getAllEquinoxes();
            for (int i = 0; i < eqs.length; ++i) {
                fEquinoxField.addItem(eqs[i]);
            }

            fEquinoxField.setActionCommand(EQUINOX_NOTIFY);
            fEquinoxField.addActionListener(this);
            add(fEquinoxLabel);
            add(fEquinoxField);
        }
        else if (!fEquinoxShown && fEquinoxField != null) {
            // remove the fields
            fEquinoxField.removeActionListener(this);
            remove(fEquinoxLabel);
            remove(fEquinoxField);
            fEquinoxLabel = null;
            fEquinoxField = null;
        }

        if (fOrientation == VERTICAL) {
            // * RA Panel *
            setConstraints(fRaLabel, 0, 0, 1, 1, GridBagConstraints.EAST);
            setConstraints(fRaField, 1, 0, 1, 1, GridBagConstraints.WEST);

            // * DEC Panel *
            setConstraints(fDecLabel, 0, 1, 1, 1, GridBagConstraints.EAST);
            setConstraints(fDecField, 1, 1, 1, 1, GridBagConstraints.WEST);

            // * Equinox Panel *
            if (fEquinoxShown) {
                setConstraints(fEquinoxLabel, 0, 2, 1, 1, GridBagConstraints.EAST);
                setConstraints(fEquinoxField, 1, 2, 1, 1, GridBagConstraints.WEST);
            }
        }
        else // Horizontal
        {
            // * RA Panel *
            setConstraints(fRaLabel, 0, 0, 1, 1, GridBagConstraints.EAST);
            setConstraints(fRaField, 1, 0, 1, 1, GridBagConstraints.WEST);

            // * DEC Panel *
            setConstraints(fDecLabel, 2, 0, 1, 1, GridBagConstraints.EAST);
            setConstraints(fDecField, 3, 0, 1, 1, GridBagConstraints.WEST);

            // * Equinox Panel *
            if (fEquinoxShown) {
                setConstraints(fEquinoxLabel, 4, 0, 1, 1, GridBagConstraints.EAST);
                setConstraints(fEquinoxField, 5, 0, 1, 1, GridBagConstraints.WEST);
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == RA_NOTIFY
                || e.getActionCommand() == DEC_NOTIFY) {
            Coordinates newPosition = null;
            try {
                newPosition = Coordinates.valueOf(fRaField.getText(), fDecField.getText());

                firePropertyChange(COORDINATES_PROPERTY, fOldPosition, newPosition);

                fOldPosition = newPosition;
            }
            catch (NumberFormatException ex) {
                showErrorDialog();

                if (fOldPosition != null) {
                    // Restore the old position text since the new one is invalid
                    setCoordinates(fOldPosition);
                }
            }
            catch (IllegalArgumentException ex) {
                showErrorDialog();

                if (fOldPosition != null) {
                    // Restore the old position text since the new one is invalid
                    setCoordinates(fOldPosition);
                }
            }
        }
        else if (e.getActionCommand() == EQUINOX_NOTIFY) {
            String eqstring = (String) fEquinoxField.getSelectedItem();
            int equinox = Coordinates.equinoxStringToInt(eqstring);

            firePropertyChange(EQUINOX_PROPERTY, fOldEquinox, equinox);
        }
    }

    public void propertyChange(PropertyChangeEvent event) {
        //System.out.println( " // CoordinatesPanel.pc: this=" +
        //    getObjectIdString(this) +
        //    ", prop=" + event.getPropertyName() +
        //    ", src=" + getObjectIdString( event.getSource()) +
        //    ", oldV=" + event.getOldValue() + ", newV=" + event.getNewValue());

        updateFromFormatChange();
    }

    /**
     * Shows an error dialog to the user which informs them that the
     * current input is not a valid set of coordinates.
     **/
    public void showErrorDialog() {
        String message;

        switch (Coordinates.getSeparatorStyle()) {
        case Coordinates.SPACE_SEPARATOR_STYLE:
        default:
            message = "Invalid coordinates format.  Coordinates should be"
                    + "\nentered in the following format: ## ## ##.##";
            break;

        case Coordinates.COLON_SEPARATOR_STYLE:
            message = "Invalid coordinates format.  Coordinates should be"
                    + "\nentered in the following format: ##:##:##.##";
            break;

        case Coordinates.LETTER_SEPARATOR_STYLE:
            message = "Invalid coordinates format.  Coordinates should be"
                    + "\nentered in the following format:"
                    + "\nRight-Ascension: ##h##m##.##s"
                    + "\nDeclination: ##d##m##.##s";
            break;
        }

        message += "\nValid ranges are 0 to 24 hours for RA,\n-90 to 90 degrees for DEC.";

        JOptionPane.showMessageDialog(this,
                message,
                "Invalid Coordinates",
                JOptionPane.ERROR_MESSAGE);
    }

    protected void updateFromFormatChange() {
        fFormatString = Coordinates.separatorStyleIntToString(Coordinates.getSeparatorStyle());
        fRaField.setToolTipText("Format: " + fFormatString);
        if (Coordinates.getSeparatorStyle() == Coordinates.LETTER_SEPARATOR_STYLE) {
            fDecField.setToolTipText("Format: ##d##m##.##s"); // for the 'd'
        }
        else {
            fDecField.setToolTipText("Format: " + fFormatString);
        }

        // Recalculate field contents to display new format
        if (fOldPosition != null) {
            setCoordinates(fOldPosition);
        }
    }

    public static void main(String[] args) {
        Coordinates offset = new Coordinates(5., 5.);
        CoordinatesPanel panel = new CoordinatesPanel();
        panel.setCoordinates(offset);
        panel.setEquinoxShown(true);
        panel.setOrientation(VERTICAL);
        JOptionPane.showConfirmDialog(null, panel);
    }
}
