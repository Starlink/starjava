package uk.ac.starlink.frog.iface;

import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboBoxEditor;

import uk.ac.starlink.ast.gui.DecimalField;
import uk.ac.starlink.ast.gui.ScientificFormat;

/**
 * DecimalComboBoxEditor extends BasicComboBoxEditor to provide a
 * mechanism for making the editable values of a JComboBox decimal
 * numbers of somekind (either floating point or integers). 
 * <p>
 * For instance to get a floating point editor combobox you would do
 * something like:
 * <pre>
 *    JComboBox box = new JComboBox();
 *    DecimalFormat format = new DecimalFormat();
 *    DecimalComboBoxEditor editor = new DecimalComboBoxEditor(format);
 *    box.setEditor( editor );
 *    box.setEditable( true );
 * </pre>
 * Control of the exact formatting (and locale) is done by the 
 * DecimalFormat object.
 *
 * @since $Date$
 * @since 26-OCT-2000
 * @author Peter W. Draper
 * @version $Id$
 * @see BasicComboBoxEditor, DecimalField
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 */
public class DecimalComboBoxEditor extends BasicComboBoxEditor 
{
    public DecimalComboBoxEditor( ScientificFormat format ) {
        super();
        editor = new BorderlessDecimalField( 0, 9, format );
    }

    //  Local class to stop DecimalField from getting a border.
    static class BorderlessDecimalField extends DecimalField 
    {
        public BorderlessDecimalField( double value , int width,
                                       ScientificFormat format ) 
        {
            super(value, width, format );
        }
        
        public void setBorder( Border b ) {}
    }
}
