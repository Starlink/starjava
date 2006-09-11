package uk.ac.starlink.topcat;

import java.awt.Component;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import uk.ac.starlink.table.gui.StarTableColumn;

/**
 * List cell renderer which will render StarTableColumn objects sensibly.
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 Feb 2004
 */
public class ColumnCellRenderer implements ListCellRenderer {

    private final ListCellRenderer baseRenderer_;
    private Object nullRep_;
    private JComboBox comboBox_;

    /**
     * Sets up a ColumnCellRenderer for a given combo box.
     * The only use of the combo box is that its tooltip text will be set
     * to that of the description of the selected column (if there is one).
     *
     * @param  comboBox  box to watch
     */
    public ColumnCellRenderer( JComboBox comboBox ) {
        comboBox_ = comboBox;

        /* Should I be getting this from the PLAF somehow? */
        baseRenderer_ = new BasicComboBoxRenderer();
    }

    /**
     * Constructs a default renderer.
     */
    public ColumnCellRenderer() {
        this( null );
    }

    public Component getListCellRendererComponent( JList list, Object value,
                                                   int index,
                                                   boolean isSelected,
                                                   boolean hasFocus ) {
        Object rep;
        if ( value == null && nullRep_ != null ) {
            rep = nullRep_;
        }
        else {
            rep = mapValue( value );
        }
        Component comp = baseRenderer_
                        .getListCellRendererComponent( list, rep, index, 
                                                       isSelected, hasFocus );
        String descrip = value instanceof StarTableColumn
                       ? ((StarTableColumn) value).getColumnInfo()
                                                  .getDescription()
                       : null;
        if ( comboBox_ != null ) {
            if ( descrip != null && descrip.trim().length() == 0 ) {
                descrip = null;
            }
            comboBox_.setToolTipText( descrip );
        }
        return comp;
    }

    /**
     * Provides the representation (to be displayed in the combo box) 
     * for an object in the box's model.
     *
     * @param  value  input value
     * @return to which <tt>value</tt> is mapped
     */
    public Object mapValue( Object value ) {
        return value instanceof StarTableColumn 
             ? ((StarTableColumn) value).getColumnInfo().getName()
             : value;
    }

    /**
     * Sets the representation for the null item.
     *
     * @param  nullRep  null representation object to appear in combo box
     */
    public void setNullRepresentation( Object nullRep ) {
        nullRep_ = nullRep;
    }

}
