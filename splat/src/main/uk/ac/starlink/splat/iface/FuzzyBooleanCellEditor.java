package uk.ac.starlink.splat.iface;


import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;

import uk.ac.starlink.splat.util.FuzzyBoolean;

/**
 * This class defines a simple editor component for a three state
 * checkbox, representing a FuzzyBoolean. The three states are
 * TRUE, MAYBE and FALSE. The only edits that are allowed are to the
 * states TRUE (from FALSE) and FALSE (from MAYBE or TRUE). So the
 * state MAYBE is lost. If you don't like this define a new class.
 *
 * @since $Date$
 * @since 14-FEB-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see #FuzzyBoolean, #SplatPlotTable.
 */
public class FuzzyBooleanCellEditor extends DefaultCellEditor 
{
    /**
     * Create an instance.
     */
    public FuzzyBooleanCellEditor() 
    {
        super( new JCheckBox() );
        final JCheckBox checkBox = (JCheckBox) getComponent();
        editorComponent = checkBox;
        delegate = new EditorDelegate() {
                public void setValue( Object value ) { 
                    if ( value instanceof FuzzyBoolean ) {
                        FuzzyBoolean fb = (FuzzyBoolean) value;
                        if ( fb.isTrue() || fb.isMaybe() ) {
                            checkBox.setSelected( true );
                        }
                        else {
                            checkBox.setSelected( false );
                        }
                    }
                }
                public Object getCellEditorValue() {
                    return new FuzzyBoolean( checkBox.isSelected() );
                }
            };
        checkBox.addActionListener(delegate);
        checkBox.setHorizontalAlignment( JCheckBox.CENTER );
    }

    public Object getCellEditorValue() 
    {
        Object obj = super.getCellEditorValue();
        if ( obj instanceof FuzzyBoolean ) {
            FuzzyBoolean fb = (FuzzyBoolean) super.getCellEditorValue();
            return fb;
        }
        return new FuzzyBoolean( FuzzyBoolean.FALSE );
    }
}
