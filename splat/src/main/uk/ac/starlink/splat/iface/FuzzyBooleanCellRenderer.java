/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-JUN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import uk.ac.starlink.splat.util.FuzzyBoolean;

/**
 * This class renders a FuzzyBoolean into a JTable cell. This uses a
 * JCheckBox that has different backgrounds to differentiate between
 * the TRUE and MAYBE states (which are both ticked).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class FuzzyBooleanCellRenderer 
    extends JCheckBox 
    implements TableCellRenderer
{
    public FuzzyBooleanCellRenderer() 
    {
        super();
        setHorizontalAlignment( JCheckBox.CENTER );
    }

    /**
     * Returns a renderer for a FuzzyBoolean. The MAYBE state is
     * represented by a different coloured background.
     *
     * @param table the JTable
     * @param value the value to control the JCheckBox colour.
     * @param isSelected true if cell is selected
     * @param hasFocus true if cell has focus
     * @param row  the row of the cell to render
     * @param column the column of the cell to render
     * @return the table cell renderer
     */
    public Component getTableCellRendererComponent( JTable table, 
                                                    Object value,
                                                    boolean isSelected, 
                                                    boolean hasFocus, 
                                                    int row, 
                                                    int column ) 
    {
        FuzzyBoolean fb = new FuzzyBoolean( FuzzyBoolean.FALSE );
        if ( value != null ) {
            fb = (FuzzyBoolean)value;
        }

        // Change colour of JCheckBox to match whether the JTable's
        // cell is selected or not. MAYBE values are always red.
        if ( fb.isMaybe() ) {
            super.setBackground( Color.red );
        }
        if ( isSelected ) {
            setForeground( table.getSelectionForeground() );
            if ( fb.isTrue() || fb.isFalse() ) {
                super.setBackground( table.getSelectionBackground() );
            }
        }
        else {
            setForeground( table.getForeground() );
            if ( fb.isTrue() || fb.isFalse() ) {
                setBackground( table.getBackground() );
            }
        }

        //  Set the state of the JCheckBox to show whether it is
        //  selected or not.
        setSelected( fb.isTrue() || fb.isMaybe() );
        return this;
    }
}
