/*
 * Copyright (C) 2001 Central Laboratory of the Research Councils
 *
 *  History:
 *     26_JUN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import java.awt.Component;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;

/**
 * Cell editor for table fields containing AstDouble objects.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see AstDouble
 */
public class AstCellEditor extends DefaultCellEditor
{
    /** 
     * The AstDouble object that currently resides in the field (when
     * editing starts) and that should be used to update the field
     * (when editing stops) 
     */
    private AstDouble value;

    /**
     * Create an instance.
     */
    public AstCellEditor()
    {
        super(new JTextField());
        ((JTextField)getComponent()).setHorizontalAlignment(JTextField.RIGHT);
    }

    /**
     * Respond to editing being stopped. Updates value to a new
     * AstDouble, if a valid field has been entered.
     */
    public boolean stopCellEditing()
    {
        String s = (String) super.getCellEditorValue();
        AstDouble newValue = null;
        newValue = value.valueOf( s );
        if ( newValue.isBad() ) {
            super.stopCellEditing();
            return false;
        }
        value = newValue;
        return super.stopCellEditing();
    }

    /**
     * Start editing by returning the component to edit and by
     * recording the current state of the field that is being edited.
     */
    public Component getTableCellEditorComponent( JTable table, 
                                                  Object value,
                                                  boolean isSelected,
                                                  int row, int column )
    {
        //  Record the current AstDouble object. This is used for the
        //  formatting context of any new objects created.
        this.value = (AstDouble) value;
        return super.getTableCellEditorComponent( table, value,
                                                  isSelected, 
                                                  row, column);
    }

    /**
     * Get the new value after editing completes.
     */
    public Object getCellEditorValue()
    {
        return value;
    }

}
