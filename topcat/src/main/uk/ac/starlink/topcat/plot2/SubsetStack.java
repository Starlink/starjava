package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.CheckBoxList;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.util.gui.ConstrainedViewportLayout;

/**
 * Provides a panel with a list of subsets and a configuration panel
 * for each one.  The list is selectable and the configuration panel
 * for the currently selected subset is shown.  The list of subsets
 * is a CheckBoxList, so has selection boxes and drag handles.
 *
 * @author   Mark Taylor
 * @since    14 Mar 2013
 */
public class SubsetStack {

    private final PermutedListModel model_;
    private final SubsetConfigManager subManager_;
    private final SubsetList subList_;
    private final ActionForwarder forwarder_;
    private final JComponent panel_;

    /**
     * Constructor.
     *
     * @param   baseModel   list model containing RowSubset objects
     * @param   subManager  provides per-subset configuration components
     */
    public SubsetStack( ListModel<RowSubset> baseModel,
                        SubsetConfigManager subManager ) {
        model_ = new PermutedListModel( baseModel );
        subManager_ = subManager;
        subList_ = new SubsetList( model_ );
        forwarder_ = new ActionForwarder();

        /* Prepare scroll panels to hold both parts of the GUI. */
        JScrollPane listScroller = new JScrollPane( subList_ );
        listScroller.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
        listScroller.getViewport().setLayout( new ConstrainedViewportLayout() );
        final JComponent configHolder = new JPanel( new BorderLayout() );
        configHolder.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        JScrollPane configScroller = new JScrollPane( configHolder );
        configScroller.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
        configScroller.getVerticalScrollBar().setUnitIncrement( 32 );

        /* Prepare a small component with buttons to check or uncheck all
         * the subsets at once. */
        JComponent listButtonBox = new JPanel( new GridLayout( 1, 0 ) );
        listButtonBox.add( createCheckAllButton( subList_, true ) );
        listButtonBox.add( createCheckAllButton( subList_, false ) );

        /* When a subset is selected in the list, display its configuration
         * component in the other panel. */
        subList_.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        subList_.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                configHolder.removeAll();
                RowSubset rset = subList_.getSelectedValue();
                if ( rset != null ) {
                    configHolder.add( subManager_
                                     .getConfiggerComponent( rset.getKey() ) );
                }
                panel_.revalidate();
                panel_.repaint();
            }
        } );
        subList_.updateCheckedEntries();

        /* Place components. */
        panel_ = new JPanel( new BorderLayout() );
        JComponent listHolder = new JPanel( new BorderLayout() );
        listHolder.add( listScroller, BorderLayout.CENTER );
        listHolder.add( listButtonBox, BorderLayout.SOUTH );
        panel_.add( listHolder, BorderLayout.WEST );
        panel_.add( configScroller, BorderLayout.CENTER );
    }

    /**
     * Return an array of the subsets which are currently active.
     *
     * @return  subsets in list with checked checkboxes
     */
    public RowSubset[] getSelectedSubsets() {
        return subList_.getCheckedEntries();
    }

    /**
     * Sets the list of active subsets.
     *
     * @param   rsets  subsets in list for which checkboxes should be checked
     */
    public void setSelectedSubsets( RowSubset[] rsets ) {
        subList_.setCheckedEntries( rsets );

        /* Make sure one of the subsets is selected if possible. */
        if ( subList_.getSelectedValue() == null && rsets.length > 0 ) {
            RowSubset rset = rsets.length == 1
                           ? rsets[ 0 ]
                           : subList_.getModel().getElementAt( 0 );
            subList_.setSelectedValue( rset, true );
        }
    }

    /**
     * Sets the active status of a given subset.
     *
     * @param  rset   row subset
     * @param  isSel   true for selected, false for unselected
     */
    public void setSelected( RowSubset rset, boolean isSel ) {
        subList_.setChecked( rset, isSel );
    }

    /**
     * Returns the graphical component for this stack.
     *
     * @return  component
     */
    public JComponent getComponent() {
        return panel_;
    }

    /**
     * Adds a listener to be notified when the selection list changes
     * content or sequence.
     *
     * @param  listener  listener to add
     */
    public void addActionListener( ActionListener listener ) {
        forwarder_.addActionListener( listener );
    }

    /**
     * Removes a listener previously added.
     *
     * @param  listener  listener to remove
     */
    public void removeActionListener( ActionListener listener ) {
        forwarder_.removeActionListener( listener );
    }

    /**
     * Messages the registered listeners that the list sequence or
     * selection status has changed.
     */
    private void fireActionEvent() {
        forwarder_.actionPerformed( new ActionEvent( this, 0, "change" ) );
    }

    /**
     * Returns the content of a ListModel as list of RowSubsets.
     *
     * @param  model  list model
     * @return  typed list of <code>model</code> entries
     */
    private static List<RowSubset> getEntries( ListModel<RowSubset> model ) {
        int count = model.getSize();
        List<RowSubset> entries = new ArrayList<RowSubset>( count );
        for ( int i = 0; i < count; i++ ) {
            entries.add( model.getElementAt( i ) );
        }
        return entries;
    }

    /**
     * Prepare a button which will either check or uncheck all the
     * subsets at once.  The button is customised a bit, in particular
     * to make it as small as possible, since it's rather special
     * interest and does not need to be prominent.
     *
     * @param  subList  subset list
     * @param  isCheck   true to check all, false to uncheck all
     * @return   new button
     */
    private static JButton createCheckAllButton( SubsetList subList,
                                                 boolean isCheck ) {
        Action act = subList.createCheckAllAction( isCheck );
        act.putValue( Action.SMALL_ICON,
                      isCheck ? ResourceIcon.REVEAL_ALL_TINY
                              : ResourceIcon.HIDE_ALL_TINY );
        String actVerb = isCheck ? "Show" : "Hide";
        act.putValue( Action.NAME, actVerb + " All" );
        act.putValue( Action.SHORT_DESCRIPTION,
                      actVerb + " all listed subsets" );
        JButton butt = new JButton( act );
        butt.setHideActionText( true );
        butt.setMargin( new Insets( 2, 2, 2, 2 ) );
        return butt;
    }

    /**
     * Wrapper list model whose elements can be reordered.
     * It is backed by a base model, and any insertions or deletions to
     * that model are tracked.
     */
    private class PermutedListModel extends AbstractListModel<RowSubset> {

        final ListModel<RowSubset> baseModel_;
        final List<RowSubset> entries_;

        /**
         * Constructor.
         *
         * @param   baseModel  list model backing this one
         */
        PermutedListModel( ListModel<RowSubset> baseModel ) {
            baseModel_ = baseModel;
            entries_ = new ArrayList<RowSubset>();

            /* Ensure that changes to the base model are tracked by this one. */
            baseModel_.addListDataListener( new ListDataListener() {
                public void contentsChanged( ListDataEvent evt ) {
                    updateFromModel();
                }
                public void intervalAdded( ListDataEvent evt ) {
                    updateFromModel();
                }
                public void intervalRemoved( ListDataEvent evt ) {
                    updateFromModel();
                }
            } );
            updateFromModel();
        }

        public RowSubset getElementAt( int index ) {
            return entries_.get( index );
        }

        public int getSize() {
            return entries_.size();
        }

        /**
         * Reorders the list by moving an item from one position to another.
         *
         * @param  ifrom  source list index
         * @param  ito    destination list index
         */
        public void moveItem( int ifrom, int ito ) {
            if ( ifrom != ito ) {
                entries_.add( ito, entries_.remove( ifrom ) );
                fireContentsChanged( this, Math.min( ifrom, ito ),
                                           Math.max( ifrom, ito ) );
                fireActionEvent();
            }
        }

        /**
         * Updates the state of this model from scratch by reading the
         * content of the base model.  Should be called any time the
         * base model changes.  There are probably more efficient ways
         * of doing this than re-populating from scratch every time,
         * but this is fairly foolproof and the list will only contain
         * a few elements so it's not going to be a performance bottleneck.
         */
        private void updateFromModel() {

            /* Record the identity of the currently selected entry, if any. */
            RowSubset sel0 = subList_ == null ? null
                                              : subList_.getSelectedValue();

            /* Get the states of the old and new lists in a suitable form. */
            List<RowSubset.Key> keys0 =
                entries_.stream()
               .map( RowSubset::getKey )
               .collect( Collectors.toList() );
            Map<RowSubset.Key,RowSubset> map1 = new LinkedHashMap<>();
            for ( RowSubset rset : getEntries( baseModel_ ) ) {
                map1.put( rset.getKey(), rset );
            }

            /* Repopulate the list containing, in their original order,
             * those items from the old list that are also contained
             * in the new list. */
            entries_.clear();
            for ( RowSubset.Key key : keys0 ) {
                RowSubset rset1 = map1.remove( key );
                if ( rset1 != null ) {
                    entries_.add( rset1 );
                }
            }

            /* Then add at the end any items from the new list that we
             * haven't already added. */
            entries_.addAll( map1.values() );
            assert entries_.size() == baseModel_.getSize();

            /* Reinstate selection. */
            if ( sel0 != null ) {
                RowSubset selected =
                    entries_
                   .stream()
                   .filter( rset -> rset.getKey().equals( sel0.getKey() ) )
                   .findAny()
                   .orElse( entries_.size() > 0 ? entries_.get( 0 ) : null );
                subList_.clearSelection();
                if ( selected != null ) {
                    subList_.setSelectedValue( selected, false );
                }
            }

            /* Notify listeners to this list of a change. */
            fireContentsChanged( this, 0, entries_.size() - 1 );
        }
    }

    /**
     * JList component containing subsets.
     * It subclasses CheckBoxList so has the additional checkbox and
     * drag handle decorations.
     */
    private class SubsetList extends CheckBoxList<RowSubset> {

        private Set<RowSubset.Key> checked_;
        private PermutedListModel permModel_;

        /**
         * Constructor.
         *
         * @param  permModel  list model containing RowSubsets
         */
        public SubsetList( PermutedListModel permModel ) {
            super( permModel, true, createLabelRendering( RowSubset::getName ));
            permModel_ = permModel;
            checked_ = new HashSet<RowSubset.Key>();
            permModel_.addListDataListener( new ListDataListener() {
                public void contentsChanged( ListDataEvent evt ) {
                    updateCheckedEntries();
                }
                public void intervalAdded( ListDataEvent evt ) {
                    updateCheckedEntries();
                }
                public void intervalRemoved( ListDataEvent evt ) {
                    updateCheckedEntries();
                }
            } );
        }

        /**
         * Returns the active subsets (those whose checkboxes are checked).
         *
         * @return  list of checked entries
         */
        public RowSubset[] getCheckedEntries() {
            return permModel_.entries_
                  .stream()
                  .filter( rset -> checked_.contains( rset.getKey() ) )
                  .collect( Collectors.toList() )
                  .toArray( new RowSubset[ 0 ] );
        }

        /**
         * Sets the active subsets.
         *
         * @param  checked1  list of entries to be checked
         */
        public void setCheckedEntries( RowSubset[] checked1 ) {
            Set<RowSubset.Key> gotKeys =
                permModel_.entries_.stream()
                          .map( RowSubset::getKey )
                          .collect( Collectors.toSet() );
            checked_.clear();
            checked_.addAll( Arrays.stream( checked1 )
                            .map( RowSubset::getKey )
                            .filter( key -> gotKeys.contains( key ) )
                            .collect( Collectors.toList() ) );
            fireActionEvent();
            repaint();
        }

        /**
         * Overridden for efficiency.
         */
        @Override
        public void setCheckedAll( boolean isChecked ) {
            checked_.clear();
            if ( isChecked ) {
                checked_.addAll( permModel_.entries_.stream()
                                .map( RowSubset::getKey )
                                .collect( Collectors.toList() ) );
            }
            fireActionEvent();
            repaint();
        }

        /**
         * Called when the base model changes to ensure that this component's
         * internal state is still valid with respect to it.
         */
        void updateCheckedEntries() {
            if ( panel_ != null ) {
                panel_.revalidate();
            }

            /* Make sure that the display component associated with each 
             * subset exists and has its entries initialised.
             * The getConfiggerComponent method does some lazy
             * initialization. */
            List<RowSubset> entries = permModel_.entries_;
            for ( RowSubset rset : entries ) {
                subManager_.getConfiggerComponent( rset.getKey() );
            }

            /* Throw out any selected entries which are no longer in the
             * base model, and repaint if any are actually discarded. */
            if ( checked_.retainAll( entries.stream()
                                    .map( RowSubset::getKey )
                                    .collect( Collectors.toSet() ) ) ) { 
                revalidate();
                repaint();
                fireActionEvent();
            }
        }

        @Override
        public void setChecked( RowSubset rset, boolean isCheck ) {
            RowSubset.Key key = rset.getKey();
            if ( isCheck ^ checked_.contains( key ) ) {
                if ( isCheck ) {
                    checked_.add( key );
                }
                else {
                    checked_.remove( key );
                }
                repaint();
                fireActionEvent();
            }
        }

        @Override
        public boolean isChecked( RowSubset rset ) {
            return checked_.contains( rset.getKey() );
        }

        @Override
        public void moveItem( int ifrom, int ito ) {
            permModel_.moveItem( ifrom, ito );
        }
    }
}
