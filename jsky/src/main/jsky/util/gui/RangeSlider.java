//=== File Prolog===========================================================
//    This code was developed by NASA, Goddard Space Flight Center, Code 588
//    for the Scientist's Expert Assistant (SEA) project for Next Generation
//    Space Telescope (NGST).
//
//--- Notes-----------------------------------------------------------------
//
//--- Development History---------------------------------------------------
//    Date              Author          Reference
//    5/11/01      S. Grosvenor / 588 Booz-Allen
//      Initial implementation.
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

import javax.swing.JSlider;
import javax.swing.JComponent;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import java.awt.Dimension;

/**
 * Extension of JSlider that maintains the standard Java "Metal" Look-and-feel,
 * while providing some modified behavior.  Supports a range of values, with
 * thumbs to set the left and right edge of a range.  Also the scale, ticks, and
 * thumbs are condensed on top of each other to take up less space
 */

public class RangeSlider extends JSlider {

    /**
     * Returns the name of the L&F class that renders this component.
     *
     * @return "SliderUI"
     * @see JComponent#getUIClassID
     */
    public String getUIClassID() {
        return "RangeSliderUI";
    }

    /**
     * Notification from the UIFactory that the L&F has changed.
     * Called to replace the UI with the latest version from the
     * default UIFactory.
     *
     * @see JComponent#updateUI
     */
    public void updateUI() {
        updateLabelUIs();
        LookAndFeel laf = UIManager.getLookAndFeel();

        if (laf instanceof javax.swing.plaf.metal.MetalLookAndFeel)
            setUI(new MetalRangeSliderUI(this, showExtent));
        else if (laf instanceof javax.swing.plaf.multi.MultiLookAndFeel)
            setUI(new MultiRangeSliderUI());
        else
            setUI(new BasicRangeSliderUI(this, showExtent));
    }


    /**
     * Creates a horizontal slider with the range 0 to 100 and
     * an intitial value of 50.
     */
    public RangeSlider() {
        this(HORIZONTAL, 0, 100, 50, 0, false);
    }


    /**
     * Creates a slider using the specified orientation with the
     * range 0 to 100 and an intitial value of 50.
     */
    public RangeSlider(int orientation) {
        this(orientation, 0, 100, 50, 0, false);
    }


    /**
     * Creates a horizontal slider using the specified min and max
     * with an intitial value of 50.
     */
    public RangeSlider(int min, int max) {
        this(HORIZONTAL, min, max, 50, 0, false);
    }


    /**
     * Creates a horizontal slider using the specified min, max and value.
     */
    public RangeSlider(int min, int max, int value) {
        this(HORIZONTAL, min, max, value, 0, false);
    }


    public RangeSlider(int orientation, int min, int max, int value, int extent) {
        this(orientation, min, max, value, extent, (extent != 0));
    }

    public boolean thumbContains(int x, int y) {
        return ((RangeSliderUI) getUI()).thumbContains(x, y);
    }

    public boolean extentContains(int x, int y) {
        return ((RangeSliderUI) getUI()).extentContains(x, y);
    }

    /**
     * Creates a slider with the specified orientation and the
     * specified mimimum, maximum, and initial values.
     *
     * @exception IllegalArgumentException if orientation is not one of VERTICAL, HORIZONTAL
     *
     * @see #setOrientation
     * @see #setMinimum
     * @see #setMaximum
     * @see #setValue
     */
    public RangeSlider(int orientation, int min, int max, int value, int extent, boolean doExtent) {
        this(orientation, new DefaultBoundedRangeModel(value, extent, min, max), doExtent);
    }

    boolean showExtent;

    /**
     * Creates a horizontal slider using the specified
     * BoundedRangeModel.
     */
    public RangeSlider(BoundedRangeModel brm) {
        this(JSlider.HORIZONTAL, brm, (brm.getExtent() != 0));
    }

    /**
     * Creates a horizontal slider using the specified
     * BoundedRangeModel.
     */
    public RangeSlider(BoundedRangeModel brm, boolean doExtent) {
        this(JSlider.HORIZONTAL, brm, doExtent);
    }

    public RangeSlider(int orient, BoundedRangeModel brm, boolean doExtent) {
        this.orientation = orient;
        setModel(brm);
        sliderModel.addChangeListener(changeListener);
        showExtent = doExtent;
        updateUI();

        setPaintLabels(true);
        setPaintTicks(true);
        setPaintTrack(false);
    }


    /**
     * Notification from the UIFactory that the L&F has changed.
     * Called to replace the UI with the latest version from the
     * default UIFactory.
     *
     * @see JComponent#updateUI
     public void updateUI() {
     updateLabelUIs();
     setUI( new RangeSliderUI( showExtent));
     }
     */


    public static void main(String[] args) {
        int sz = UIManager.getInstalledLookAndFeels().length;
        for (int i = 0; i < 1; i++) {
            try {
                String laf = UIManager.getInstalledLookAndFeels()[i].getClassName();
                UIManager.setLookAndFeel(laf);
                System.out.println(laf);

                RangeSlider s = new RangeSlider(VERTICAL, 0, 100, 50, 20, true);
                s.setPaintLabels(true);
                s.setPaintTicks(true);
                s.setPaintTrack(true);

                javax.swing.JOptionPane.showMessageDialog(null, s);
            }
            catch (Exception e) {
                System.out.println("   exception=" + e);
            }
        }
        System.exit(0);
    }
}
