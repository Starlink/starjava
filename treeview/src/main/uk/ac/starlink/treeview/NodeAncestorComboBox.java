package uk.ac.starlink.treeview;

import java.awt.Component;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;


/**
 * Combo box which presents a given DataNode and its ancestors.
 * The parents are its natural parent objects, not its parents within
 * a given DataNodeTreeModel.
 * This is designed for use in selecting the root node of a node
 * browser type object.
 */
public class NodeAncestorComboBox extends JComboBox {

    private DataNodeFactory nodeMaker = new DataNodeFactory();

    /**
     * Construct a new combo box from a given data node.
     */
    public NodeAncestorComboBox() {

        /* Set a special renderer. */
        setRenderer( new NodeChooserRenderer() );
    }

    /**
     * Sets the node which from which the contents of this combo box will
     * be determined.  The box will contain <tt>node</tt> plus all
     * its ancestors.
     *
     * @param  node  bottom node in the hierarchy to be represented
     */
    public void setBottomNode( DataNode node ) {

        /* Assemble a list of the direct ancestors of this node. */
        List ancestorList = new ArrayList();
        DataNode next = node;
        ancestorList.add( next );
        for ( Object parentObj; 
              ( parentObj = next.getParentObject() ) != null; ) {
            try {
                next = nodeMaker.makeDataNode( null, parentObj );
                ancestorList.add( next );
            }
            catch ( NoSuchDataException e ) {
                break;
            }
        }

        /* Set the items in this combo box to the contents of the list. */
        Collections.reverse( ancestorList );
        setModel( new DefaultComboBoxModel( ancestorList.toArray() ) );

        /* Set the selected item to the bottom one. */
        setSelectedItem( node );
    }

    /**
     * Returns the node which represents the selected item.
     *
     * @return  the selected data node
     */
    public DataNode getSelectedNode() {
        return (DataNode) getSelectedNode();
    }

    /**
     * Class to do rendering in combo box.  This just re-uses the behaviour
     * of a 
     */
    private static class NodeChooserRenderer extends BasicComboBoxRenderer {

        public Component getListCellRendererComponent( JList list, Object value,
                                                       int index,
                                                       boolean isSelected,
                                                       boolean hasFocus ) {

            /* Re-use default behaviour. */
            JLabel rendered = 
                (JLabel)
                super.getListCellRendererComponent( list, value, index, 
                                                    isSelected, hasFocus );

            /* Customise the label by using the node name. */
            DataNode node = (DataNode) value;
            rendered.setText( node.getName() );

            /* Customise the label by using the node icon.  We add some 
             * space to the left of the icon to get the nodes indenting
             * hierarchically. */
            final Icon baseIcon = node.getIcon();
            if ( baseIcon != null ) {
                final int offset = 20 + index * 10;
                Icon icon = new Icon() {
                    public int getIconHeight() {
                        return baseIcon.getIconHeight();
                    }
                    public int getIconWidth() {
                        return baseIcon.getIconWidth() + offset;
                    }
                    public void paintIcon( Component c, Graphics g,
                                           int x, int y ) {
                        baseIcon.paintIcon( c, g, x + offset, y );
                    }
                };
                rendered.setIcon( icon );
            }

            /* Return the rendered label. */
            return rendered;
        }
    }
}
