//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class JTextFieldDouble
//
//--- Development History -----------------------------------------------------
//
// 11/19/99      S. Grosvenor
//		Original implementation. Converts standard double special codes to
//     and from strings and changes their color
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

//package GOV.nasa.gsfc.sea.util.gui;

package jsky.util.gui;

import java.beans.PropertyChangeEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Font;
import java.awt.Color;

import javax.swing.JTextField;


/**
 * An extension of JTextField to process the special non-number
 * doubles: NaN, MinVal, MaxVal, Infinities.  Turns them to special
 * color when they occur.  Also adds a setText( Double) and setText( double)
 * convenience methods
 *
 * @version		1999.11.11
 * @author		S. Grosvenor
 **/
public class JTextFieldDouble extends JTextField {

    private static JTextFieldDouble jt = new JTextFieldDouble(15);
    private static final String INVALID_STRING = "Invalid";
    private static final String NAN_STRING = "NaN";
    private static final String NINFINITY_STRING = "-Infinity";
    private static final String PINFINITY_STRING = "+Infinity";
    private static final String MAXVAL_STRING = "MaxVal";
    private static final String MINVAL_STRING = "MinVal";

    private Color fNormalColor;
    private Color fErrorColor;

    /**
     * Passthru to parent
     **/
    public JTextFieldDouble() {
        this(null, null, 0);
    }

    /**
     * Constructs a new TextField initialized with the specified text.
     * A default model is created and the number of columns is 0.
     *
     * @param text the text to be displayed, or null
     */
    public JTextFieldDouble(String text) {
        this(null, text, 0);
    }

    /**
     * Constructs a new empty TextField with the specified number of columns.
     * A default model is created and the initial string is set to null.
     *
     * @param columns  the number of columns to use to calculate
     *   the preferred width.  If columns is set to zero, the
     *   preferred width will be whatever naturally results from
     *   the component implementation.
     */
    public JTextFieldDouble(int columns) {
        this(null, null, columns);
    }

    /**
     * Constructs a new TextField initialized with the specified text
     * and columns.  A default model is created.
     *
     * @param text the text to be displayed, or null
     * @param columns  the number of columns to use to calculate
     *   the preferred width.  If columns is set to zero, the
     *   preferred width will be whatever naturally results from
     *   the component implementation.
     */
    public JTextFieldDouble(String text, int columns) {
        this(null, text, columns);
    }

    /**
     * Constructs a new JTextField that uses the given text storage
     * model and the given number of columns.  This is the constructor
     * through which the other constructors feed.  If the document is null,
     * a default model is created.
     *
     * @param doc  the text storage to use.  If this is null, a default
     *   will be provided by calling the createDefaultModel method.
     * @param text  the initial string to display, or null
     * @param columns  the number of columns to use to calculate
     *   the preferred width >= 0.  If columns is set to zero, the
     *   preferred width will be whatever naturally results from
     *   the component implementation.
     * @exception IllegalArgumentException if columns < 0
     **/
    public JTextFieldDouble(javax.swing.text.Document doc, String text, int columns) {
        super(doc, text, columns);

        fNormalColor = getForeground();
        fErrorColor = Color.red;
        setText(getText());
    }

    /**
     * sets the color font to be used for normal numbers, default is the
     * JTextField default color.
     **/
    public void setNormalColor(Color c) {
        fNormalColor = c;
        setText(getText());
    }

    public void setForeground(Color c) {
        if (fNormalColor == null) fNormalColor = c;
        super.setForeground(fNormalColor);
    }

    /**
     * sets the color font to be used for the special number. Default is red
     **/
    public void setErrorColor(Color c) {
        fErrorColor = c;
        setText(getText());
    }

    /**
     * checks for special texts, sets string and color accordingly
     *
     * @param Double a Double object to be entered
     **/
    public void setText(Double dObject) {
        setText(dObject.doubleValue());
    }

    /**
     * checks for special texts, sets string and color accordingly
     * @param double a double value to be entered, uses Double.toString()
     **/
    public void setText(double q) {
        if (Double.isNaN(q)) {
            setText(INVALID_STRING);
        }
        else if (Double.isInfinite(q)) {
            setText((q < Double.POSITIVE_INFINITY) ?
                    NINFINITY_STRING : PINFINITY_STRING);
        }
        else if (q == Double.MAX_VALUE) {
            setText(MAXVAL_STRING);
        }
        else if (q == Double.MIN_VALUE) {
            setText(MINVAL_STRING);
        }
        else {
            setText(Double.toString(q));
        }
    }

    /**
     * checks for special texts, sets string and color accordingly
     **/
    public void setText(String s) {
        super.setText(s);
        String sl = s.toLowerCase();

        if (s == null) {
            setForeground(fNormalColor);
        }
        else if (sl.startsWith(NAN_STRING.toLowerCase()) ||
                sl.startsWith(INVALID_STRING.toLowerCase()) ||
                sl.startsWith(PINFINITY_STRING.toLowerCase()) ||
                sl.startsWith(NINFINITY_STRING.toLowerCase()) ||
                sl.startsWith(MAXVAL_STRING.toLowerCase()) ||
                sl.startsWith(MINVAL_STRING.toLowerCase())) {
            setForeground(fErrorColor);
        }
        else {
            setForeground(fNormalColor);
        }
    }

    /** Test main. */
    public static void main(String[] args) {
        javax.swing.JDialog d = new javax.swing.JDialog();
        d.setSize(300, 150);
        d.getContentPane().setLayout(new java.awt.FlowLayout());
        d.getContentPane().add(jt);

        jt.setText(44.0);

        d.setVisible(true);

        jt.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                String s = (String) event.getActionCommand();
                try {
                    double dd = Double.parseDouble(s);
                    jt.setText(dd);
                }
                catch (Exception e) {
                    jt.setText(s);
                }
            }
        });
    }
}


