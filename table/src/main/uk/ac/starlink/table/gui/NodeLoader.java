package uk.ac.starlink.table.gui;

import java.awt.Component;
import javax.swing.ComboBoxModel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

public class NodeLoader implements TableLoadDialog {

    private final StarTableNodeChooser nodeChooser_;

    public NodeLoader() {
        nodeChooser_ = StarTableNodeChooser.newInstance();
    }

    public String getName() {
        return "Browse Hierarchy";
    }

    public String getDescription() {
        return "Load table using treeview-type browser";
    }

    public boolean isEnabled() {
        return nodeChooser_ != null;
    }

    public StarTable loadTableDialog( Component parent,
                                      StarTableFactory factory,
                                      ComboBoxModel formatModel ) {
        return nodeChooser_.chooseStarTable( parent );
    }
}
