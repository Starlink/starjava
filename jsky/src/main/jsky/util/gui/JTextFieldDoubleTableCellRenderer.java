//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class JTextFieldDoubleTableCellRenderer
//
//--- Description -------------------------------------------------------------
//	A TableCellRenderer to support inserting JTextFieldDoubles in jtables
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	11/21/99    S Grosvenor
//
//		Original implementation.
//
//--- Warning -----------------------------------------------------------------
//	This software is property of the National Aeronautics and Space
//	Administration.  Unauthorized use or duplication of this software is
//	strictly prohibited.  Authorized users are subject to the following
//	restrictions:
//	*	Neither the author, their corporation, nor NASA is responsible for
//		any consequence of the use of this software.
//	*	The origin of this software must not be misrepresented either by
//		explicit claim or by omission.
//	*	Altered versions of this software must be plainly marked as such.
//	*	This notice may not be removed or altered.
//
//=== End File Prolog =========================================================

//package GOV.nasa.gsfc.sea.util.gui;

package jsky.util.gui;


import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.io.Serializable;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.TableCellRenderer;

/**
 * A TableCellRenderer that wraps a JTextFieldDouble into a table make for
 * easy highlighting of NaN's etc.
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version	1999.11.19
 * @author	S. Grosvenor
 **/
public class JTextFieldDoubleTableCellRenderer extends JTextFieldDouble
        implements TableCellRenderer, Serializable {

    protected static Border noFocusBorder;

    // We need a place to store the color the JLabel should be returned
    // to after its foreground and background colors have been set
    // to the selection background color.
    // These ivars will be made protected when their names are finalized.
    private Color unselectedForeground;
    private Color unselectedBackground;

    public JTextFieldDoubleTableCellRenderer() {
        super();
        noFocusBorder = new EmptyBorder(1, 2, 1, 2);
        setBorder(noFocusBorder);
        setHorizontalAlignment(SwingConstants.RIGHT);
        setNormalColor(Color.black);
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        if (isSelected) {
            setBackground(getSelectionColor());
        }
        else {
            setBackground(table.getBackground());
        }

        if (hasFocus) {
            setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            if (table.isCellEditable(row, column)) {
                setForeground(UIManager.getColor("Table.focusCellForeground"));
                setBackground(UIManager.getColor("Table.focusCellBackground"));
            }
        }
        else {
            setBorder(noFocusBorder);
        }

        setValue(value);
        return this;
    }

    protected void setValue(Object value) {
        if (value instanceof Double) {
            setText((Double) value);
        }
        else if (value != null) { // allan: added check for null
            setText(value.toString());
        }
    }

    // The default table renderers define validate, revalidate, repaint,
    // and  firePropertyChange to be no-ops
    public void validate() {
    }

    public void revalidate() {
    }

    public void repaint(Rectangle r) {
    }

    public void repaint(long tm, int x, int y, int width, int height) {
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    }
}


