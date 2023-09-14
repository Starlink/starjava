package uk.ac.starlink.topcat;

import java.awt.Component;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.gui.StarTableColumn;

/**
 * List cell renderer which will render StarTableColumn objects sensibly.
 *
 * @author   Mark Taylor (Starlink)
 * @since    20 Feb 2004
 */
public class ColumnCellRenderer implements ListCellRenderer<TableColumn> {

    private final BasicComboBoxRenderer baseRenderer_;
    private Object nullRep_;
    private JComboBox<TableColumn> comboBox_;

    /**
     * Sets up a ColumnCellRenderer for a given combo box.
     * The only use of the combo box is that its tooltip text will be set
     * to that of the description of the selected column (if there is one).
     *
     * @param  comboBox  box to watch
     */
    public ColumnCellRenderer( JComboBox<TableColumn> comboBox ) {
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

    public Component
            getListCellRendererComponent( JList<? extends TableColumn> list,
                                          TableColumn col, int index,
                                          boolean isSelected,
                                          boolean hasFocus ) {
        ColumnInfo cinfo = col instanceof StarTableColumn
                         ? ((StarTableColumn) col).getColumnInfo()
                         : null;
        Object rep;
        if ( col == null ) {
            rep = nullRep_;
        }
        else {
            rep = cinfo == null ? col.getHeaderValue()
                                : cinfo.getName();
        }
        Component comp = baseRenderer_
                        .getListCellRendererComponent( list, rep, index, 
                                                       isSelected, hasFocus );
        String descrip = cinfo == null
                       ? null
                       : cinfo.getDescription();
        if ( comboBox_ != null ) {
            if ( descrip != null && descrip.trim().length() == 0 ) {
                descrip = null;
            }
            comboBox_.setToolTipText( descrip );
        }
        return comp;
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
