//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class QuantityPanel
//
//--- Development History -----------------------------------------------------
//
//	6/1/99      S. Grosvenor
//		Original implementation. Combines Quantity text and labels field
//      into a single panel that automatically tracks changes to the
//      the default units value.
//  7/30/99     S. Grosvenor
//      Amended property change handling so that all change events are reflected
//      through the Quantity instance being editted (the fQuantity).. You no
//      longer need to listen to this Quantity Panel.. just do a setQuantity( myGuy)
//      and listen to myGuy for changes.
//  11/11/99    S. Grosvenor
//      Now puts text in red if quantity is not a valid number.  Also parses several
//      variants of invalid numbers. Such as MinVal, MaxVal, +Infinity, -Infinity and Invalid
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
//=== End File Prolog =========================================================

package jsky.util.gui;

import jsky.science.Quantity;
import jsky.util.ReplaceablePropertyChangeListener;
import jsky.util.ReplacementEvent;
import jsky.util.FormatUtilities;


import java.beans.PropertyChangeEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Color;

import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;

/**
 * A JPanel subclass that manages coordinated display and editing of Quantity values and units.
 * The JPanel contains both a JTextField for displaying/editing the Quantity's
 * value, and a JLabel for displaying the Quantity's current default units.
 * The panel listens to for relevant changes in the Quantity's list of default
 * units and updates the display accordingly.
 *
 * Changes to the Quantity instance edited by the QuantityPanel are passed on to
 * registered PropertyChangeListeners via PropertyChangeEvents have the propertyName
 * of QuantityPanel.QUANTITY.
 *
 * @version		1999.11.11
 * @author		S. Grosvenor
 **/
public class QuantityPanel extends JPanel implements ReplaceablePropertyChangeListener,
        ActionListener,
        FocusListener {

    Quantity fQuantity = null;
    Class fClass = null;
    int fDecs = 2;

    JTextField fTextValue;
    JLabel fLabelUnits;
    boolean fAbbreviate = true;

    public static final String QUANTITY = "Quantity";

    private static final String NAN_STRING = "Invalid";
    private static final String NINFINITY_STRING = "-Infinity";
    private static final String PINFINITY_STRING = "+Infinity";
    private static final String MAXVAL_STRING = "MaxVal";
    private static final String MINVAL_STRING = "MinVal";

    private Color fNormalColor = Color.black;
    private Color fErrorColor = Color.red;

    /**
     * Creates a QuantityPanel providing for 8 columns in the quantity field, and
     * displaying the default units.
     * @param cl  The subclass of Quantity for which editting is to be performed
     **/
    public QuantityPanel(Class cl) {
        this(8, SwingConstants.EAST, cl);
    }

    /**
     * Creates a QuantityPanel providing for specified number of columns in
     * the quantity field, and
     * displaying the default units.
     * @param width  The width of the text field in the panel
     * @param cl  The subclass of Quantity for which editting is to be performed
     **/
    public QuantityPanel(int width, Class cl) {
        this(width, SwingConstants.EAST, cl);
    }

    /**
     * Creates a QuantityPanel providing for specified number of columns in
     * the quantity field, displaying the Quantity's default units, and
     * providing an option to display the unit beside or below the textfield.
     * @param width  The width of the text field in the panel
     * @param labelLocation The location for the units, use SwingConstants.EAST
     * to locate along side, any other string (recommend using SwingConstants.SOUTH),
     * will result in units displayed below the textfield
     * @param cl  The subclass of Quantity for which editting is to be performed
     **/
    public QuantityPanel(int width, int labelLocation, Class cl) {
        super();
        fClass = cl;

        if (labelLocation == SwingConstants.EAST) {
            setLayout(new FlowLayout());
        }
        else {
            setLayout(new GridLayout(2, 1));
        }

        fTextValue = new JTextField(width);
        fTextValue.setActionCommand(QUANTITY);
        add(fTextValue);

        fLabelUnits = new JLabel();
        setAbbreviate(fAbbreviate);

        add(fLabelUnits);

        updateText();

        Quantity.addDefaultUnitsChangeListener(fClass, this);
        addListeners();

    }

    /**
     * When set to True, the units will be displayed as abbreviations,
     * the default is True.
     */
    public void setAbbreviate(boolean abbrev) {
        fAbbreviate = abbrev;
        if (fAbbreviate) {
            fLabelUnits.setText(Quantity.getDefaultUnitsAbbrev(fClass));
        }
        else {
            fLabelUnits.setText(Quantity.getDefaultUnits(fClass));
        }
    }

    private boolean isListening = false;

    /**
     * internal method for handling listener events
     */
    private void addListeners() {
        if (!isListening) {
            isListening = true;
            fTextValue.addActionListener(this);
            fTextValue.addFocusListener(this);
        }
    }

    /**
     * internal method for handling listener events
     */
    private void removeListeners() {
        if (isListening) {
            isListening = false;
            fTextValue.removeActionListener(this);
            fTextValue.removeFocusListener(this);
        }
    }

    /**
     * Sets the Color for the foreground of "normal" text in the textfield
     */
    public void setNormalColor(Color c) {
        fNormalColor = c;
        updateText();
    }

    /**
     * Sets the Color for the foreground of "error" text in the textfield.
     * Values such as NaN
     */
    public void setErrorColor(Color c) {
        fErrorColor = c;
        updateText();
    }

    /**
     * internal method for updating component displays.
     */
    private void updateText() {
        double q = (fQuantity == null)? 0 : fQuantity.getValue();

        if (fQuantity == null) {
            fTextValue.setForeground(fNormalColor);
            fTextValue.setText("");
        }
        else if (Double.isNaN(q)) {
            fTextValue.setForeground(fErrorColor);
            fTextValue.setText(NAN_STRING);
        }
        else if (Double.isInfinite(q)) {
            fTextValue.setForeground(fErrorColor);
            fTextValue.setText((q < Double.POSITIVE_INFINITY) ?
                    NINFINITY_STRING : PINFINITY_STRING);
        }
        else if (q == Double.MAX_VALUE) {
            fTextValue.setForeground(fErrorColor);
            fTextValue.setText(MAXVAL_STRING);
        }
        else if (q == Double.MIN_VALUE) {
            fTextValue.setForeground(fErrorColor);
            fTextValue.setText(MINVAL_STRING);
        }
        else {
            fTextValue.setForeground(fNormalColor);
            fTextValue.setText(FormatUtilities.formatDouble(q, fDecs, fTextValue.getColumns()));
        }
    }

    /**
     * Sets the Quantity instance to be editted.
     * @param newQ Quantity object to be editted
     **/
    public void setQuantity(Quantity newQ) {
        Quantity oldW = fQuantity;

        if ((newQ != null) && !fClass.isInstance(newQ)) {
            throw new ClassCastException();
        }

        fQuantity = newQ;

        firePropertyChange(QUANTITY, oldW, newQ);

        updateText();
    }

    /**
     * Returns the Quantity object being editted
     **/
    public Quantity getQuantity() {
        return fQuantity;
    }

    /**
     * Sets the number of decimal places to be displayed in the TextField of the panel
     * @param inDecs the number of decimal places to be displayed
     **/
    public void setDecs(int inDecs) {
        fDecs = inDecs;
        updateText();
    }

    /**
     * returns the number of decimal places being displayed in the JTextField
     */
    public int getDecs() {
        return fDecs;
    }

    /**
     * Sets the font to be displayed in the JTextField
     */
    public void setFont(Font f) {
        if (fTextValue != null) fTextValue.setFont(f);
    }

    /**
     * returns the current font for the JTextField
     */
    public Font getFont() {
        return (fTextValue == null) ? null : fTextValue.getFont();
    }

    /**
     * handles inbound PropertyEvents (only expected to come from Quantity
     * in response to changes to the default units
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(Quantity.getDefaultUnitsProperty(fClass))) {
            fLabelUnits.setText((String) event.getNewValue());
            updateText();
        }
    }

    /**
     * handles requests to swap out the edited Quantity instance
     */
    public void replaceObject(ReplacementEvent event) {
        setQuantity((Quantity) event.getNewValue());
    }

    /**
     * handles ActionEvents from the JTextField
     */
    public void actionPerformed(ActionEvent event) {
        if (event != null && !event.getActionCommand().equals(QUANTITY)) return;
        if (fQuantity == null) return;
        String t = fTextValue.getText();
        if (t.equalsIgnoreCase("null") && t.equalsIgnoreCase(NAN_STRING)) {
            setQuantity(fQuantity.newInstance(Double.NaN));
        }
        else if (t.equalsIgnoreCase(NINFINITY_STRING)) {
            setQuantity(fQuantity.newInstance(Double.NEGATIVE_INFINITY));
        }
        else if (t.equalsIgnoreCase(PINFINITY_STRING)) {
            setQuantity(fQuantity.newInstance(Double.POSITIVE_INFINITY));
        }
        else if (t.equalsIgnoreCase(MAXVAL_STRING)) {
            setQuantity(fQuantity.newInstance(Double.MAX_VALUE));
        }
        else if (t.equalsIgnoreCase(MINVAL_STRING)) {
            setQuantity(fQuantity.newInstance(Double.MIN_VALUE));
        }
        else {
            try {
                setQuantity(fQuantity.newInstance(new Double(t).doubleValue()));
            }
            catch (NumberFormatException e) {
                errorPaneUp = true; // to prevent doubling up of this message
                javax.swing.JOptionPane.showMessageDialog(
                        this, "Decimal number expected, please re-enter",
                        "Invalid number",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                errorPaneUp = false;
            }
        }
    }

    private String oldText = null;

    public void focusGained(FocusEvent event) {
        // do nothing
        oldText = fTextValue.getText();
    }

    /**
     * set true just before Error message is displayed on invalid Quantities. Prevents
     * a double error message
     **/
    private boolean errorPaneUp = false;

    /**
     * Updates the quantity value when the JTextField loses focus
     */
    public void focusLost(FocusEvent event) {
        if (!oldText.equals(fTextValue.getText()) && !errorPaneUp)
            actionPerformed(null);
    }

    /**
     * turns on or off the displaying of the units label
     **/
    public void setUnitsVisible(boolean onOff) {
        fLabelUnits.setVisible(onOff);
    }

    /**
     * overrides setToolTip to pass the tool tip on to both the label and the
     * text field in the Panel
     */
    public void setToolTipText(String text) {
        super.setToolTipText(text);
        fTextValue.setToolTipText(text);
        fLabelUnits.setToolTipText(text);
    }

    /**
     * overrides parent setEnabled to pass the enabling onto the textfield
     * contained in the quantiypanel.
     */
    public void setEnabled(boolean b) {
        super.setEnabled(b);
        fTextValue.setEnabled(b);
    }

}


