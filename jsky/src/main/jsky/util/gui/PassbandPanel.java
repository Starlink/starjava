//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class PassbandPanel
//
//--- Development History -----------------------------------------------------
//
//	6/1/99      S. Grosvenor
//		Original implementation. Combines Quantity text and labels field
//      into a single panel that less users enter a range of values and
//      automatically tracks changes to the default units value.
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
import jsky.science.Wavelength;
import jsky.science.Passband;
import jsky.util.ReplacementEvent;
import jsky.util.FormatUtilities;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridLayout;

import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;

import java.util.List;

/**
 * A JPanel subclass for editing Passbands.  Contains two fields: text field
 * for the a Passband object and a label for the units.  Similar to QuantityPanel
 * except that this class supports special handling where the JTextField
 * contains a Passband object.
 *
 * @version		6.1.99
 * @author		S. Grosvenor
 **/
public class PassbandPanel extends JPanel implements PropertyChangeListener,
        ActionListener,
        FocusListener {

    Passband fBand = null;
    int fDecs = 2;

    JTextField fTextRange;
    JLabel fLabelUnits;

    public static final String PASSBAND = "Passband";

    /**
     * Creates a PassbandPanel providing for 12 columns in the text field, and
     * displaying the default units, placing the labels to the right of the
     * textfield and containing a null Passband
     **/
    public PassbandPanel() {
        this(null, 12, SwingConstants.EAST);
    }

    /**
     * Creates a PassbandPanel providing for 12 columns in the text field, and
     * displaying the default units
     * @param inP  The Passband to be editted
     **/
    public PassbandPanel(Passband inP) {
        this(inP, 12, SwingConstants.EAST);
    }

    /**
     * Creates a PassbandPanel providing for specified columns in the text field,
     * displaying the default units, a null passband, and labels to the east.
     * @param width  The width of the text field in the panel
     **/
    public PassbandPanel(int width) {
        this(null, width, SwingConstants.EAST);
    }

    /**
     * Creates a PassbandPanel providing for specified columns in the text field,
     * displaying the default units, a specified Passband, and specified location
     * for the labels.
     * @param inP The Passband to be displayed/edited
     * @param width  The width of the text field in the panel
     * @param labelLocation The location for the units, use SwingConstants.EAST
     * to locate along side, any other string (recommend using SwingConstants.SOUTH),
     * will result in units displayed below the textfield
     **/
    public PassbandPanel(Passband inP, int width, int labelLocation) {
        super();

        if (labelLocation == SwingConstants.EAST) {
            setLayout(new GridLayout(1, 2));
        }
        else {
            setLayout(new GridLayout(2, 1));
        }

        fTextRange = new JTextField(width);
        fTextRange.setActionCommand(PASSBAND);
        add(fTextRange);

        fLabelUnits = new JLabel(Quantity.getDefaultUnits(Wavelength.class));
        List allUnits = Quantity.getAllUnits(Wavelength.class);
        int maxWidth = 0;
        FontMetrics fm = getFontMetrics(getFont());
        for (int i = 0; i < allUnits.size(); i++) {
            maxWidth = Math.max(maxWidth, fm.stringWidth((String) allUnits.get(i)));
        }
        Dimension dim = getPreferredSize();
        dim.width = maxWidth + 10;
        fLabelUnits.setPreferredSize(dim);

        add(fLabelUnits);

        setPassband(inP);

        Quantity.addDefaultUnitsChangeListener(Wavelength.class, this);
        addListeners();
    }

    private boolean isListening = false;

    /** internal method for handling listeners */
    private void addListeners() {
        if (!isListening) {
            isListening = true;
            fTextRange.addActionListener(this);
            fTextRange.addFocusListener(this);
        }
    }

    /** internal method for handling listeners */
    private void removeListeners() {
        if (isListening) {
            isListening = false;
            fTextRange.removeActionListener(this);
            fTextRange.removeFocusListener(this);
        }
    }

    /** internal method that updates the component contents */
    private void updateText() {
        if (fBand == null) {
            fTextRange.setText("");
        }
        else if (fBand.getLowWavelength().equals(fBand.getHighWavelength())) {
            fTextRange.setText(
                    FormatUtilities.formatDouble(fBand.getLowWavelength().getValue(), fDecs, fTextRange.getColumns()));
        }
        else {
            fTextRange.setText(
                    FormatUtilities.formatDouble(fBand.getLowWavelength().getValue(), fDecs, fTextRange.getColumns()) +
                    "-" +
                    FormatUtilities.formatDouble(fBand.getHighWavelength().getValue(), fDecs, fTextRange.getColumns()));
        }
    }

    /**
     * Sets the Passband instance to be displayed/edited.
     **/
    public void setPassband(Passband newP) {
        Passband oldP = fBand;
        if (fBand != null) removePropertyChangeListener(this);
        fBand = newP;
        updateText();
        if (fBand != null) addPropertyChangeListener(this);
    }

    /** Returns the Passband being edited. */
    public Passband getPassband() {
        return fBand;
    }

    /**
     * Sets the number of decimal places to be displayed in the TextField of the panel.
     * @param inDecs the number of decimal places to be displayed
     **/
    public void setDecs(int inDecs) {
        fDecs = inDecs;
        updateText();
    }

    /**
     * Returns the number of decimal places currently being displayed.
     */
    public int getDecs() {
        return fDecs;
    }

    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(Wavelength.DEFAULTUNITS_PROPERTY)) {
            fLabelUnits.setText((String) event.getNewValue());
            updateText();
        }
        else if (event.getSource() == fBand) {
            updateText();
        }
    }

    public void replaceObject(ReplacementEvent event) {
        if (event.getOldValue() == fBand) {
            setPassband((Passband) event.getNewValue());
        }
    }

    public void actionPerformed(ActionEvent event) {
        if (event != null && event.getActionCommand().equals(PASSBAND)) {
            Passband tempP = new Passband(fTextRange.getText());
            fBand.setLowWavelength(tempP.getLowWavelength());
            fBand.setHighWavelength(tempP.getHighWavelength());
        }
    }

    public void focusGained(FocusEvent event) {
        // do nothing
    }

    public void focusLost(FocusEvent event) {
        actionPerformed(null);
    }

    /**
     * Turns on or off the displaying of the units label.
     **/
    public void setUnitsVisible(boolean onOff) {
        fLabelUnits.setVisible(onOff);
    }

}


