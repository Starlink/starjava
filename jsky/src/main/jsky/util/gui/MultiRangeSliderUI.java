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
//    5/23/01       S. Grosvenor / 588 Booz-Allen
//      Made thumbs partially transparent to not hide the scale values
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

import javax.swing.plaf.multi.*;
import javax.swing.plaf.ComponentUI;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.SwingUtilities;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.AlphaComposite;

import javax.swing.event.*;
import java.awt.event.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * The core repaints for our RangeSlider
 */

public class MultiRangeSliderUI extends MultiSliderUI
        implements RangeSliderUI {

    public boolean thumbContains(int x, int y) {
        return false; // NYI
    }

    public boolean extentContains(int x, int y) {
        return false; // NYI
    }


    public static ComponentUI createUI(JComponent c) {
        return new MultiRangeSliderUI();
    }
}