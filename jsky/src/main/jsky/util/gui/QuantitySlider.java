//=== File Prolog===========================================================
//    This code was developed by NASA, Goddard Space Flight Center, Code 588
//    for the Scientist's Expert Assistant (SEA) project for Next Generation
//    Space Telescope (NGST).
//
//--- Notes-----------------------------------------------------------------
//
//--- Development History---------------------------------------------------
//    Date              Author          Reference
//    12/11/00      S. Grosvenor / 588 Booz-Allen
//      Initial implementation.
//    05/16/01      S. Grosvenor
//      "Slimmed" down space the slider takes up, expanded it to handle
//      a range of values
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

import jsky.util.FileUtil;

import java.awt.event.*;
import javax.swing.event.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;

import javax.swing.JComponent;
import java.awt.Insets;
import java.awt.MediaTracker;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;
import javax.swing.JButton;
import java.awt.Component;
import java.awt.Image;
import javax.swing.ImageIcon;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.Timer;
import javax.swing.SwingConstants;

import jsky.science.Quantity;
import jsky.science.Time;


/**
 * A Panel that blends editing a quantity or range of quantities by slider or
 * straight data entry.
 * Encorporates the default units display, and adds options change the max or
 * Min value of the slider range
 * <P>
 * Behavior of the properties is modified. A propertyChanged is fired
 * every time a statechange is recieved from the slider, OR a new value is
 * enter in the text box. Since whenever the slider is "active" its valueIsChanging
 * property is true.  This component also fires of a last (Possibly redundant)
 * propertyChange event after the slider stops moving (when valueIsChanging is no longer
 * true.
 * <P>
 * Minor HACK is doing things this way, to force a propertyChange after the sliders
 * stops, the PropertyChangeEvent has getOldValue() of NULL... the receiving
 * listener needs to be aware
 * that the value may not have really changed
 * <P>
 * NOTE currently works only with WHOLE numbers
 */
public class QuantitySlider extends JPanel implements PropertyChangeListener,
        ChangeListener, ActionListener, MouseListener {

    public static final String MINQUANTITY_PROPERTY = "MinQ";
    public static final String MAXQUANTITY_PROPERTY = "MaxQ";
    public static final String QUANTITY_PROPERTY = MINQUANTITY_PROPERTY;

    Quantity fScaleMin;
    Quantity fScaleMax;
    Quantity fMinValue;
    Quantity fMaxValue;

    boolean fMinChangeable = false;
    boolean fMaxChangeable = false;

    //QuantityPanel fPanelValue;
    RangeSlider fSlider;
    JButton fButtonNewMin;
    JButton fButtonNewMax;
    Timer fTimer;

    /**
     * Build a default bordered Quantity Slider editing a single value
     */
    public QuantitySlider() {
        this(JSlider.HORIZONTAL, new Time(50), new Time(0), new Time(100), "Exposure Time", true);
    }

    /**
     * Create a bordered Quantity Slider, with one edited value and a specified min/max to the scale
     *
     * @param inQ the current Quantity value
     * @param scaleMin the minimum allowable Quantity value
     * @param scaleMax the maximum allowable Quantity value
     * @param title the optional title to be displayed, can be null for no title
     * @param borderOn When true, a labeled border will be displayed. Requires more vertical space
     */
    public QuantitySlider(Quantity inQ, Quantity scaleMin, Quantity scaleMax, String title) {
        this(JSlider.HORIZONTAL, inQ, inQ, scaleMin, scaleMax, title, true, false);          // Default is to have a border
    }

    /**
     * Create a bordered Quantity Slider, with one edited value and a specified min/max to the scale
     * This constructor lets you set VERTICAL or HORIZONTAL for the orientation
     *
     * @param orient the orientation of the slider (JSlider.VERTICAL or HORIZONTAL)
     * @param inQ the current Quantity value
     * @param scaleMin the minimum allowable Quantity value
     * @param scaleMax the maximum allowable Quantity value
     * @param title the optional title to be displayed, can be null for no title
     * @param borderOn When true, a labeled border will be displayed. Requires more vertical space
     */
    public QuantitySlider(int orient, Quantity inQ, Quantity scaleMin, Quantity scaleMax, String title, boolean borderOn) {
        this(orient, inQ, inQ, scaleMin, scaleMax, title, borderOn, false);
    }

    /**
     * Create a bordered Quantity Slider, editing a range and a specified min/max to the scale
     * This constructor lets you set VERTICAL or HORIZONTAL for the orientation
     *
     * @param orient the orientation of the slider (JSlider.VERTICAL or HORIZONTAL)
     * @param minQ the "left" edge of the editing range
     * @param maxQ the "right" edge of the editing range
     * @param scaleMin the minimum allowable Quantity value
     * @param scaleMin the maximum allowable Quantity value
     * @param title the optional title to be displayed, can be null for no title
     * @param borderOn When true, a labeled border will be displayed. Requires more vertical space
     */
    public QuantitySlider(int orient, Quantity minQ, Quantity maxQ, Quantity scaleMin, Quantity scaleMax, String title, boolean borderOn) {
        this(orient, minQ, maxQ, scaleMin, scaleMax, title, borderOn, true);
    }

    /**
     * Fully qualified constructor
     *
     * @param orient the orientation of the slider (JSlider.VERTICAL or HORIZONTAL)
     * @param minQ the "left" edge of the editing range
     * @param maxQ the "right" edge of the editing range
     * @param scaleMin the minimum allowable Quantity value
     * @param scaleMin the maximum allowable Quantity value
     * @param title the optional title to be displayed, can be null for no title
     * @param borderOn When true, a labeled border will be displayed. Requires more vertical space
     * @param showExtent When true, will display and manage the 2nd right hand pointer marking the right edge of the range
     */
    public QuantitySlider(int orient, Quantity inMinQ, Quantity inMaxQ, Quantity scaleMin, Quantity scaleMax, String title, boolean borderOn, boolean showExtent) {
        super();
        fScaleMin = scaleMin;
        fScaleMax = scaleMax;
        fMinValue = inMinQ;
        fMaxValue = inMaxQ;
        fTimer = new Timer(100, this);
        fTimer.setRepeats(false);

        fTitle = title;
        fIsBordered = borderOn;
        fShowExtent = showExtent;

        addFields(orient, showExtent);
        updateTitle();
    }

    protected boolean fIsBordered;
    protected String fTitle;
    protected JLabel fLabelTitle;
    protected TitledBorder fBorder;
    protected boolean fShowExtent = false;

    protected void updateTitle() {
        StringBuffer sb = new StringBuffer();
        if (fTitle != null) {
            sb.append(fTitle);
            sb.append(": ");
        }
        sb.append(fMinValue.toString());
        if (fShowExtent) {
            sb.append(" - ");
            sb.append(fMaxValue.toString());
        }

        String newtitle = sb.toString();

        if (fIsBordered) {
            setBorder(BorderFactory.createTitledBorder(newtitle));
        }
        else {
            fLabelTitle.setText(newtitle);
        }
    }

    /**
     * Populate the Quantity Slider with the proper components
     *
     * @param the optional title to be displayed, can be null
     * @param when borderOn is true, then a labeled border will be displayed. Requires more vertical space
     */
    private void addFields(int orient, boolean showExtent) {
        setLayout(new BorderLayout(0, 0));
        // The Value panel with optional label on the Quantity field
        //fPanelValue = new QuantityPanel( fMinValue.getClass());
        //fPanelValue.setQuantity( fMinValue);

        JPanel panelNorth = new JPanel(new BorderLayout(0, 0));
        //GridBagConstraints gbc  = new GridBagConstraints();
        //gbc.insets = new Insets( 1,1,1,1);

        if (!fIsBordered) {
            panelNorth.setLayout(new GridLayout(1, 2, 0, 0));
            fLabelTitle = new JLabel("", SwingConstants.LEFT);
            panelNorth.add(fLabelTitle);
        }
        //panelNorth.add( fPanelValue);
        add(panelNorth, BorderLayout.NORTH);

        Image icon = findImage(this, "JumpLeft16.gif");
        if (icon == null) {
            fButtonNewMin = new JButton("|<");
            int fontsize = fButtonNewMin.getFont().getSize() - 1;
            fButtonNewMin.setFont(new java.awt.Font(
                    fButtonNewMin.getFont().getName(),
                    fButtonNewMin.getFont().getStyle(),
                    fontsize));
        }
        else {
            fButtonNewMin = new JButton(new ImageIcon(icon));
            fButtonNewMin.setPreferredSize(new Dimension(18, 18));
            fButtonNewMin.setMinimumSize(new Dimension(18, 18));
            fButtonNewMin.setMaximumSize(new Dimension(18, 18));
            fButtonNewMin.setBorderPainted(false);    // don't want a border around the icon. It looks silly
        }

        icon = findImage(this, "JumpRight16.gif");
        if (icon == null) {
            fButtonNewMax = new JButton(">|");
            int fontsize = fButtonNewMax.getFont().getSize() - 1;
            fButtonNewMax.setFont(new java.awt.Font(
                    fButtonNewMax.getFont().getName(),
                    fButtonNewMax.getFont().getStyle(),
                    fontsize));
        }
        else {
            fButtonNewMax = new JButton(new ImageIcon(icon));
            fButtonNewMax.setPreferredSize(new Dimension(18, 18));
            fButtonNewMax.setMinimumSize(new Dimension(18, 18));
            fButtonNewMax.setMaximumSize(new Dimension(18, 18));
            fButtonNewMax.setBorderPainted(false);    // don't want a border around the icon. It looks silly
        }

        // Create the Slider in between the min and max adjustment buttons
        fSlider = new RangeSlider(orient, (int) fScaleMin.getValue(), (int) fScaleMax.getValue(),
                (int) fMinValue.getValue(), (int) (fMaxValue.getValue() - fMinValue.getValue()), showExtent);
        updateTicks();

        // Now the slider portion with buttons for changing the min and max values
        if (orient == javax.swing.JSlider.HORIZONTAL) {
            add(fButtonNewMin, BorderLayout.WEST);
            add(fButtonNewMax, BorderLayout.EAST);
        }
        else {
            add(fButtonNewMin, BorderLayout.SOUTH);
            add(fButtonNewMax, BorderLayout.NORTH);
        }

        JPanel cPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        add(cPanel, BorderLayout.CENTER);
        cPanel.add(fSlider);

        //fPanelValue.addPropertyChangeListener( this);
        fButtonNewMax.addActionListener(this);
        fSlider.addChangeListener(this);
        fButtonNewMin.addActionListener(this);

        fSlider.addMouseListener(this);

        fSlider.setToolTipText("Drag slider to change value, or double click to type a new value");
        fButtonNewMin.setToolTipText("Click to change minimum scale value");
        fButtonNewMax.setToolTipText("Click to change maximum scale value");
        //fSlider.requestDefaultFocus();
        fSlider.requestFocus();
    }

    /**
     * sets the length (in milleseconds) of time after the user stops adjusting
     * the slider that a "final" propertyChange event is fired.  Additional
     * propertyChange events will be fired while the sliders is being moved, but
     * the getValueIsAdjusting() method will return true for these events.
     */
    public void setSliderDelay(int delay) {
        fTimer.setInitialDelay(delay);
    }

    /**
     * returns true if end-user can change the minimum value on the slider
     */
    public boolean isMinimumChangeable() {
        return fMinChangeable;
    }

    /**
     * returns true if end-user can change the maximum value on the slider
     */
    public boolean isMaximumChangeable() {
        return fMaxChangeable;
    }

    /**
     * sets whether or not the user can change the minimum slider value. When true,
     * a button with a "|<" icon will appear to the left of the slider.  When the user clicks
     * that button a popup window will prompt user for new minimum value
     */
    public void setMinimumChangeable(boolean b) {
        fMinChangeable = b;
        fButtonNewMin.setVisible(b);
    }

    /**
     * sets whether or not the user can change the maximum slider value. When true,
     * a button with a ">|" icon will appear to the right of the slider.  When the user clicks
     * that button a popup window will prompt user for new maximum value
     */
    public void setMaximumChangeable(boolean b) {
        fMaxChangeable = b;
        fButtonNewMax.setVisible(b);
    }

    /**
     * Update the tickmarks on the slider scale
     */
    private void updateTicks() {
        // 4 tick marks
        int ticks = (int) ((fScaleMax.getValue() - fScaleMin.getValue()) / 4);
        fSlider.setLabelTable(null);
        fSlider.setMajorTickSpacing(ticks);
        fSlider.setMinorTickSpacing((int) ticks / 5);
    }

    public Quantity getQuantity() {
        return fMinValue;
    }

    public Quantity getExtent() {
        return fMaxValue;
    }

    /**
     * passes through the valueIsAdjusting property of the slide, let
     * event recipients decide whether or not they want to deal with these
     * events
     */
    public boolean getValueIsAdjusting() {
        return fSlider.getValueIsAdjusting();
    }

    public void stateChanged(ChangeEvent event) {
        // should be inbound from fSlider
        if (event.getSource() == fSlider) {
            fTimer.restart();
            int newV = fSlider.getValue();
            setMinQuantity(fMinValue.newInstance((double) newV));

            newV = newV + fSlider.getExtent();
            setMaxQuantity(fMaxValue.newInstance((double) newV));
        }
    }


    public void propertyChange(PropertyChangeEvent event) {
//        // should come from QuantityPanel
//        if (event.getSource() == fPanelValue)
//        {
//            fTimer.stop();
//            Quantity newQ = fPanelValue.getQuantity();
//            setQuantity( newQ);
//        }
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == fTimer) {
            firePropertyChange(MINQUANTITY_PROPERTY, null, fMinValue);
            firePropertyChange(MAXQUANTITY_PROPERTY, null, fMaxValue);
        }
        else if (event.getSource() == fButtonNewMin) {
            StringBuffer sb = new StringBuffer("Enter new minimum value (");
            sb.append(fMinValue.getDefaultUnits());
            sb.append("):");
            double val = popupQuantityRequest(
                    "New Minimum Value",
                    sb.toString());
            if (!Double.isNaN(val)) setScaleMinimum((int) val);
        }
        else if (event.getSource() == fButtonNewMax) {
            StringBuffer sb = new StringBuffer("Enter new maximum value (");
            sb.append(fMinValue.getDefaultUnits());
            sb.append("):");
            double val = popupQuantityRequest(
                    "New Maximum Value",
                    sb.toString());
            if (!Double.isNaN(val)) setScaleMaximum((int) val);
        }
    }

    public double popupQuantityRequest(String title, String message) {
        double returnValue = Double.NaN;

        String reply = JOptionPane.showInputDialog(this,
                message,
                title,
                JOptionPane.QUESTION_MESSAGE);

        if (reply != null) {
            try {
                returnValue = Double.parseDouble(reply);
            }
            catch (Exception e) {
                // do nothing
            }
        }

        return returnValue;
    }

    /**
     * sets the minimum range scale
     */
    public void setScaleMinimum(int m) {
        fScaleMin = fScaleMin.newInstance(m);
        updateTicks();
        fSlider.setMinimum((int) Math.floor(m));
    }

    /**
     * sets the maximum range scale
     */
    public void setScaleMaximum(int m) {
        fScaleMax = fScaleMax.newInstance(m);
        updateTicks();
        fSlider.setMaximum((int) Math.ceil(m));
    }

    /**
     * @deprecated use setMinQuantity instead
     */
    public void setQuantity(Quantity inQ) {
        setMinQuantity(inQ);
    }

    /**
     * sets the current min quantity of the range
     */
    public void setMinQuantity(Quantity inQ) {
        Quantity oldQ = fMinValue;
        fMinValue = inQ;
        updateTitle();
        updateScaleMinMax((int) fMinValue.getValue());
        fSlider.setValue((int) inQ.getValue());
        firePropertyChange(MINQUANTITY_PROPERTY, oldQ, inQ);
    }

    /**
     * sets the current Max quantity of the range
     */
    public void setMaxQuantity(Quantity inQ) {
        Quantity oldQ = fMaxValue;
        fMaxValue = inQ;
        updateTitle();
        updateScaleMinMax((int) fMaxValue.getValue());
        fSlider.setExtent((int) (fMaxValue.getValue() - fMinValue.getValue()));
        firePropertyChange(MAXQUANTITY_PROPERTY, oldQ, inQ);
    }

    protected void updateScaleMinMax(int newvalue) {
        if (newvalue > fScaleMax.getValue()) setScaleMaximum(newvalue);
        if (newvalue < fScaleMin.getValue()) setScaleMinimum(newvalue);
    }

    public static void main(String[] args) {
        QuantitySlider timeSlider1 = new QuantitySlider(JSlider.HORIZONTAL,
                new Time(20), new Time(50), new Time(0), new Time(100), "Exposure Time", false);
        javax.swing.JOptionPane.showMessageDialog(null, timeSlider1);

        System.exit(0);
    }

    /**
     * Finds and loads an image based on a string input name.  Looks first in JAR file, if any, then
     * on local disk,  then local file
     **/
    private static Image findImage(Component inComp, String inName) {
        ImageIcon icon = (ImageIcon) jsky.util.Resources.getIcon(inName);
        if (icon == null)
            return null;
        return icon.getImage();

        /* XXX Allan: use the Resources class
        boolean haveLocalAccess = true; // (isApplet() ? false : SeaUtilities.checkLocalReadAccess());
        URL targetUrl = null;
        Image retImg = null;

        // first try the Jar file:
        try
        {
            URL u = null;
            try { u = inComp.getClass().getResource( inName);} catch (NullPointerException e) {}
            if (u != null) retImg = loadImage( inComp, u);
        }
        catch (Exception e) {}

        if ( (retImg == null) && (haveLocalAccess)) // not found in jar, try local disk
        {
            try
            {
                String fName = null;
                if (inName.startsWith("/")) fName = "." + inName;
                else fName = inName;
                retImg = loadImage( inComp, ClassLoader.getSystemResource( fName));
            }
            catch (Exception e) {}
        }

        return retImg;
	XXX */
    } // of findImage

    /**
     * tries to load image from Url. Returns null if input Url is null, or image cannot
     * be loaded
     *
     * @exception	IllegalArgumentException	if inComp is null
     **/
    private static Image loadImage(Component inComp, URL inUrl) {
        if (inComp == null) {
            // Must have valid Component for loading
            throw new IllegalArgumentException("loadImage called with null Component");
        }

        if (inUrl == null) {
            // No URL, so no Image
            return null;
        }

        Image loadedImage = null;

        try {
            Image tempImg = null;

            MediaTracker mt = new MediaTracker(inComp);
            tempImg = java.awt.Toolkit.getDefaultToolkit().getImage(inUrl);
            mt.addImage(tempImg, 0);
            mt.waitForID(0);

            loadedImage = tempImg;
        }
        catch (Exception e) {
            loadedImage = null;
        }

        return loadedImage;
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
            // have double click
            if (fSlider.thumbContains(e.getX(), e.getY())) {
                StringBuffer sb = new StringBuffer("Enter new min value (");
                sb.append(fMinValue.getDefaultUnits());
                sb.append("):");
                double val = popupQuantityRequest(
                        "Min Value",
                        sb.toString());

                if (!Double.isNaN(val)) {
                    Quantity newQ = fMinValue.newInstance(val);
                    setMinQuantity(newQ);
                }
            }
            else if (fSlider.extentContains(e.getX(), e.getY())) {
                StringBuffer sb = new StringBuffer("Enter new max value (");
                sb.append(fMaxValue.getDefaultUnits());
                sb.append("):");
                double val = popupQuantityRequest(
                        "Max Value",
                        sb.toString());

                if (!Double.isNaN(val)) {
                    Quantity newQ = fMaxValue.newInstance(val);
                    setMaxQuantity(newQ);
                }
            }
        }
    }

    public void mousePressed(MouseEvent e) {
        // do nothing
    }

    public void mouseReleased(MouseEvent e) {
        // do nothing
    }

    public void mouseEntered(MouseEvent e) {
        // do nothing
    }

    public void mouseExited(MouseEvent e) {
        // do nothing
    }


}
