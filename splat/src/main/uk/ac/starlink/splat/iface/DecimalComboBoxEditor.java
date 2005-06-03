/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     26-OCT-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

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
 *    ScientificFormat format = new ScientificFormat();
 *    DecimalComboBoxEditor editor = new DecimalComboBoxEditor(format);
 *    box.setEditor( editor );
 *    box.setEditable( true );
 * </pre>
 * Control of the exact formatting is done by the ScientificFormat object.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see BasicComboBoxEditor
 * @see DecimalField
 */
public class DecimalComboBoxEditor 
    extends BasicComboBoxEditor 
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
