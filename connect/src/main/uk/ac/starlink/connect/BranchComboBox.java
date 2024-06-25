package uk.ac.starlink.connect;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.filechooser.FileView;

/**
 * JComboBox which allows selection of {@link Branch} objects.
 * For any branch in the box's model, all its ancestors are automatically
 * in the model too (this behaviour is inspired by widgets like
 * {@link javax.swing.JFileChooser}).
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Feb 2005
 */
public class BranchComboBox extends JComboBox<Branch> {

    private BranchComboBoxModel model_;
    private static FileView fileView_;

    /**
     * Constructor.
     */
    public BranchComboBox() {
        super( new BranchComboBoxModel() );
        assert model_ instanceof BranchComboBoxModel;
        setRenderer( new BranchCellRenderer() );
    }

    /**
     * Sets this combo box's model.  Will only accept a suitable model
     * (one acquired from another <code>BranchComboBox</code>).
     *
     * @param  model  model
     * @throws  ClassCastException  if it's the wrong type
     */
    public void setModel( ComboBoxModel<Branch> model ) {
        model_ = (BranchComboBoxModel) model;
        super.setModel( model );
        Object selected = model.getSelectedItem();
        int state = selected == null ? ItemEvent.DESELECTED
                                     : ItemEvent.SELECTED;
        fireItemStateChanged( new ItemEvent( this, 0, selected, state ) );
    }

    /**
     * Sets the selected branch.  This may or may not have the effect of
     * adding or subtracting branches from the box's model.
     *
     * @param   branch  branch to form the current selection
     */
    public void setSelectedBranch( Branch branch ) {
        model_.setSelectedBranch( branch );
    }

    /**
     * Returns the currently selected branch.
     *
     * @return  current branch
     */
    public Branch getSelectedBranch() {
        return model_.getSelectedBranch();
    }

    /**
     * Adds a new normal branch to the model.
     *
     * @param   branch  branch to add
     */
    public void addBranch( Branch branch ) {
        model_.addBranch( branch );
    }

    /**
     * Adds a new branch to the model which represents a 
     * <code>Connector</code>.
     *
     * @param  connAct  connector action to be represented by a new branch
     */
    public void addConnection( ConnectorAction connAct ) {
        model_.addBranch( new ConnectorBranch( connAct ) );
    }

    /**
     * Returns any connector action which is assocated with the currently
     * selected branch.  This will return null unless the current branch
     * represents a <code>Connector</code>.
     *
     * @return   connector action associated with current selection, if any
     * @see      #addConnection
     */
    public ConnectorAction getConnectorAction() {
        return model_.getConnectorAction();
    }

    /**
     * Returns a FileView object which can be used to interpret files on
     * this platform.
     *
     * @return   FileView
     */
    public static FileView getFileView() {
        if ( fileView_ == null ) {
            JFileChooser fc = new JFileChooser();
            fileView_ = fc.getUI().getFileView( fc );
        }
        return fileView_;
    }

    /**
     * Returns the connector, if any, associated with a branch.
     * It will be null unless the branch is a ConnectorBranch.
     */
    private static Connector getConnector( Branch branch ) {
        return branch instanceof ConnectorBranch
             ? ((ConnectorBranch) branch).getConnectorAction().getConnector()
             : null;
    }

    /**
     * Model for the combo box.
     */
    private static class BranchComboBoxModel extends AbstractListModel<Branch>
                                             implements ComboBoxModel<Branch> {

        /* The basic data structure in which the model's data is held is
         * an array of N BranchHolder objects.
         * However this corresponds to >= N items, since the model
         * is considered to hold all the ancestors of each branch.
         * The selected_ member points to the currently selected 
         * BranchHolder; the currently selected branch is the terminal
         * node of the selected branch. */

        private BranchHolder[] holders_ = new BranchHolder[ 0 ];
        private int selected_ = -1;
        private static final FileView fView_ = getFileView();

        public int getSize() {
            int size = 0;
            for ( int i = 0; i < holders_.length; i++ ) {
                size += holders_[ i ].getDepth();
            }
            return size;
        }

        public Branch getElementAt( int index ) {
            for ( int i = 0; i < holders_.length; i++ ) {
                BranchHolder holder = holders_[ i ];
                int depth = holder.getDepth();
                if ( index < depth ) {
                    return holder.getAncestor( index );
                }
                else {
                    index -= depth;
                }
            }
            return null;
        }

        public Object getSelectedItem() {
            return getSelectedBranch();
        }

        public void setSelectedItem( Object branch ) {
            setSelectedBranch( (Branch) branch );
        }

        public Branch getSelectedBranch() {
            return selected_ >= 0 ? holders_[ selected_ ].getBranch()
                                  : null;
        }

 
        /**
         * Sets the selected branch to a given value.
         * If the given branch has the same root as one of the 
         * existing holders, that holder's branch is set to the one 
         * given and it is marked selected.  If the given branch is
         * not rooted at the same place as any of the existing ones,
         * a new holder is added containing it.
         *
         * @param  branch   new selection
         */
        public void setSelectedBranch( Branch branch ) {
            Branch root = new BranchHolder( branch ).getRoot();
            for ( int i = 0; i < holders_.length; i++ ) {
                if ( root.equals( holders_[ i ].getRoot() ) ) {
                    holders_[ i ].setBranch( branch );
                    selected_ = i;
                    fireContentsChanged( this, -1, -1 );
                    return;
                }
            }
            addBranch( branch );
            selected_ = holders_.length - 1;
            fireContentsChanged( this, -1, -1 );
        }

        public void addBranch( Branch branch ) {
            int oldSize = getSize();
            final BranchHolder holder = new BranchHolder( branch );
            List<BranchHolder> hlist =
                new ArrayList<BranchHolder>( Arrays.asList( holders_ ) );
            hlist.add( holder );
            holders_ = hlist.toArray( new BranchHolder[ 0 ] );
            fireIntervalAdded( this,
                               Math.max( 0, oldSize - 1 ), getSize() - 1 );

            /* If it's a connector branch, do some additional work.
             * Make sure that this model reacts properly to 
             * the connection going up or down. */
            if ( branch instanceof ConnectorBranch ) {
                final ConnectorBranch connBranch = (ConnectorBranch) branch;
                connBranch.getConnectorAction()
                          .addPropertyChangeListener(
                    new PropertyChangeListener() {
                        public void propertyChange( PropertyChangeEvent evt ) {
                            if ( evt.getPropertyName()
                                    .equals( ConnectorAction
                                            .CONNECTION_PROPERTY ) ) {
                                Connection conn = (Connection)
                                                  evt.getNewValue();
                                int oldSize = getSize();
                                if ( conn == null ) {
                                    holder.setBranch( connBranch );
                                }
                                else {
                                    holder.setBranch( conn.getRoot() );
                                }
                                int maxSize = Math.max( oldSize, getSize() );
                                fireContentsChanged( this, 0, maxSize );
                            }
                        }
                    } );
            }
        }

        public ConnectorAction getConnectorAction() {
            return selected_ >= 0 ? holders_[ selected_ ].getConnectorAction()
                                  : null;
        }

        Icon getCustomIcon( Branch branch ) {
            if ( branch instanceof FileBranch && fView_ != null ) {
                return fView_.getIcon( ((FileBranch) branch).getFile() );
            }
            Branch root = new BranchHolder( branch ).getRoot();
            for ( int i = 0; i < holders_.length; i++ ) {
                BranchHolder holder = holders_[ i ];
                if ( root.equals( holder.getRoot() ) ) {
                    ConnectorAction connAct = holder.getConnectorAction();
                    if ( connAct != null ) {
                        return connAct.getConnector().getIcon();
                    }
                }
            }
            return null;
        }
    }

    /**
     * Renderer used for a BranchComboBox.
     */
    private static class BranchCellRenderer
            implements ListCellRenderer<Object> {

        final DefaultListCellRenderer baseRenderer_;
        final static Icon FOLDER_ICON = UIManager.getIcon( "Tree.closedIcon" );
        final Icon ROOT_ICON =
            new ImageIcon( BranchComboBox.class.getResource( "disk.gif" ) );

        public BranchCellRenderer() {
            baseRenderer_ = new DefaultListCellRenderer();
        }

        public Component getListCellRendererComponent( JList<?> list,
                                                       Object value, int index,
                                                       boolean isSelected,
                                                       boolean hasFocus ) {
            int depth = 0;
            Icon baseIcon = FOLDER_ICON;
            if ( value instanceof Branch ) {
                Branch branch = (Branch) value;
                value = branch.getName();
                while ( branch.getParent() != null ) {
                    branch = branch.getParent();
                    depth++;
                }

                ListModel<?> model = list.getModel();
                if ( model instanceof BranchComboBoxModel ) {
                    Icon icon = ((BranchComboBoxModel) model)
                               .getCustomIcon( branch );
                    if ( icon != null ) {
                        baseIcon = icon;
                    }
                }
            }

            Component comp =
                baseRenderer_
               .getListCellRendererComponent( list, value, index,
                                              isSelected, hasFocus );
            if ( comp instanceof JLabel && baseIcon != null ) {
                final int offset = 2 + ( ( index >= 0 ) ? depth * 10
                                                        : 0 );
                final Icon baseIcon1 = baseIcon;
                Icon icon = new Icon() {
                    public int getIconHeight() {
                        return baseIcon1.getIconHeight();
                    }
                    public int getIconWidth() {
                        return baseIcon1.getIconWidth() + offset;
                    }
                    public void paintIcon( Component c, Graphics g,
                                           int x, int y ) {
                        baseIcon1.paintIcon( c, g, x + offset, y );
                    }
                };
                ((JLabel) comp).setIcon( icon );
            }

            return comp;
        }
    }


    /**
     * Container object for a Branch.  The selection model contains
     * an expandable (but not otherwise changeable) list of BranchHolders
     * to define its internal state.
     * Each branchholder represents a branch; the current branch can 
     * change, but it's always notionally rooted at the same place,
     * which is either a branch representing a node in a virtual filesystem
     * or a connector action to a remote virtual filesystem.
     */
    private static class BranchHolder {
        private final Branch root_;
        private final ConnectorAction connAct_;
        private Branch[] chain_;

        /**
         * Constructor.
         *
         * @branch  branch initially represented by this holder
         */
        BranchHolder( Branch branch ) {
            setBranch( branch );
            root_ = chain_[ 0 ];
            connAct_ = root_ instanceof ConnectorBranch
                     ? ((ConnectorBranch) root_).getConnectorAction()
                     : null;
        }

        /**
         * Sets the branch represented by this holder.  When called
         * explicitly (other than in the constructor) it must always
         * be to set a branch consistent with
         * the existing root of this holder.
         *
         * @param  branch  new branch
         */
        void setBranch( Branch branch ) {
            List<Branch> ancestors = new ArrayList<Branch>();
            for ( Branch ancestor = branch; ancestor != null;
                  ancestor = ancestor.getParent() ) {
                ancestors.add( ancestor );
            }
            Collections.reverse( ancestors );
            chain_ = ancestors.toArray( new Branch[ 0 ] );
        }

        /**
         * Returns the root of this holder.  The root of a holder associated
         * with a connector branch changes; when the connection is open
         * it's the root branch of the connection itself, otherwise it's
         * the ConnectorBranch.
         */
        Branch getRoot() {
            if ( connAct_ != null ) {
                Connection conn = connAct_.getConnection();
                if ( conn != null ) {
                    Branch r = conn.getRoot();
                    if ( r != null ) {
                        return r;
                    }
                }
            }
            return root_;
        }

        /**
         * Returns the depth (number of nodes) of this branch.
         * A root holder has a depth of 1.
         *
         * @return   dept
         */
        int getDepth() {
            return chain_.length;
        }

        /**
         * Returns the terminal node of the branch held by this holder.
         *
         * @return  branch
         */
        Branch getBranch() {
            return chain_[ chain_.length - 1 ];
        }

        /**
         * Returns the N'th ancestor of this branch.  Level 0 is the root,
         * level 1 is the root's child, etc.
         *
         * @param   level  level
         * @return  ancestor
         */
        Branch getAncestor( int level ) {
            return chain_[ level ];
        }

        /**
         * Returns the connector action, if any, associated with this holder.
         *
         * @return   connector action, or null
         */
        ConnectorAction getConnectorAction() {
            return connAct_;
        }

    }

    /**
     * Special Branch implementation which represents a ConnectorAction.
     * The branch keeps track of a single connection at any one time, 
     * which may be open or closed.
     */
    private static class ConnectorBranch implements Branch {

        final ConnectorAction connAct_;

        ConnectorBranch( ConnectorAction connAct ) {
            connAct_ = connAct;
        }

        public String getName() {
            Connection conn = getConnection();
            return connAct_.getConnector().getName();
        }

        public Branch getParent() {
            return null;
        }

        public Node[] getChildren() {
            Connection conn = getConnection();
            return conn == null ? new Node[ 0 ]
                                : conn.getRoot().getChildren();
        }

        public Node createNode( String name ) {
            Connection conn = getConnection();
            return conn == null ? null
                                : conn.getRoot().createNode( name );
        }

        /**
         * Returns the connector action associated with this branch;
         *
         * @return  connector action
         */
        public ConnectorAction getConnectorAction() {
            return connAct_;
        }

        Connection getConnection() {
            return connAct_.getConnection();
        }

        public boolean equals( Object o ) {
            return o instanceof ConnectorBranch && 
                   ((ConnectorBranch) o).connAct_.equals( connAct_ );
        }

        public int hashCode() {
            return connAct_.hashCode();
        }

        public String toString() {
            return getName();
        }
    }
}

