package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.io.IOException;
import javax.swing.ComboBoxModel;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;

/**
 * Table load dialogue based on a Treeview-like node chooser.
 *
 * @author   Mark Taylor (Starlink)
 * @since    1 Dec 2004
 */
public class NodeLoader implements TableLoadDialog {

    private final StarTableNodeChooser nodeChooser_;

    /**
     * Constructor. 
     */
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

    public boolean showLoadDialog( Component parent,
                                   final StarTableFactory factory,
                                   ComboBoxModel formatModel,
                                   TableConsumer eater ) {
        final StarTable table = nodeChooser_.chooseStarTable( parent );
        if ( table != null ) {
            String id = null;
            if ( id == null && table.getURL() != null ) {
                id = table.getURL().toString();
            }
            if ( id == null ) {
                id = table.getName();
            }
            if ( id == null ) {
                id = "Table";
            }
            if ( ! factory.requireRandom() || table.isRandom() ) {
                eater.loadStarted( id );
                eater.loadSucceeded( table );
                return true;
            }
            else {
                new LoadWorker( eater, id ) {
                    public StarTable attemptLoad() throws IOException {
                        return factory.randomTable( table );
                    }
                }.invoke();
                return true;
            }
        }
        else {
            return false;
        }
    }

    /**
     * Returns the node chooser used by this dialogue.
     */
    public StarTableNodeChooser getNodeChooser() {
        return nodeChooser_;
    }
}
