package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A component containing a column of checkboxes representing a set of
 * choices.  
 * This component does pretty much the same job as a JList, but 
 * is easier to use for individually selecting and deselecting individual
 * list items.
 * Like a <code>JList</code>, it has an associated {@link javax.swing.ListModel}
 * and {@link javax.swing.ListSelectionModel}, 
 * which may be shared with other views in the usual way.
 *
 * @author   Mark Taylor (Starlink)
 */
public class CheckBoxStack<T> extends JPanel
                              implements ListSelectionListener,
                                         ListDataListener,
                                         Scrollable {

    private DefaultListSelectionModel selModel_;
    private ListModel<T> listModel_;
    private List<JCheckBox> entries_;
    private final Annotator annotator_;

    /**
     * Constructs a new CheckBoxStack from a ListModel with an optional
     * annotator for labelling the boxes.
     *
     * @param  listModel the model
     * @param  annotator  object to generate annotations for each check box
     */
    public CheckBoxStack( ListModel<T> listModel, Annotator annotator ) {
        super( new GridBagLayout() );
        annotator_ = annotator;
        setListModel( listModel );
        setSelectionModel( new DefaultListSelectionModel() );
    }

    /**
     * Constructs a new CheckBoxStack with no annotations from a ListModel.
     *
     * @param  listModel the model
     */
    public CheckBoxStack( ListModel<T> listModel ) {
        this( listModel, null );
    }

    /**
     * Constructs a new CheckBoxStack from a default list model.
     */
    public CheckBoxStack() {
        this( new DefaultListModel<T>() );
        revalidate();
        repaint();
    }

    /**
     * Adds a new item to the stack.  Its stringified form will be used
     * for the label of a new JCheckBox which will be added to the bottom
     * of the list.
     *
     * @param  item  the new item to add
     */
    private void addItem( Object item ) {

        /* Create a new check box. */
        final JCheckBox cbox = new JCheckBox( item.toString() );

        /* Set its selection status in accordance with the selection model. */
        final int pos = entries_.size();
        if ( selModel_ != null ) {
            cbox.setSelected( selModel_.isSelectedIndex( pos ) );
        }

        /* Make sure that changes in the selection status of the new 
         * checkbox are transmitted to the selection model. */
        cbox.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                if ( cbox.isSelected() ) {
                    selModel_.addSelectionInterval( pos, pos );
                }
                else {
                    selModel_.removeSelectionInterval( pos, pos );
                }
            }
        } );

        /* Keep a record of the checkbox. */
        entries_.add( cbox );

        /* Add it to the panel. */
        addLine( cbox, annotator_ == null
                           ? null
                           : annotator_.createAnnotation( item ) );
    }

    /**
     * Adds a line consisting of one or two components to this panel.
     *
     * @param   c1  first component
     * @param   c2  second component (may be null)
     */
    private void addLine( Component c1, Component c2 ) {
        GridBagLayout layer = (GridBagLayout) getLayout();
        GridBagConstraints cons = new GridBagConstraints();
        cons.gridy = entries_.size();
        cons.gridx = 0;
        cons.weightx = 1.0;
        cons.weighty = 1.0;
        cons.anchor = GridBagConstraints.WEST;
        layer.setConstraints( c1, cons );
        add( c1 );
        cons.gridx++;
        if ( c2 != null ) {
            layer.setConstraints( c2, cons );
            add( c2 );
        }
        cons.gridx++;
    }

    /**
     * Reconstruct the contents of this component from scratch.
     */
    private void redoAllItems() {
        removeAll();
        entries_ = new ArrayList<JCheckBox>();
        for ( int i = 0; i < listModel_.getSize(); i++ ) {
            addItem( listModel_.getElementAt( i ) );
        }
    }

    public ListSelectionModel getSelectionModel() {
        return selModel_;
    }

    public void setSelectionModel( DefaultListSelectionModel selModel ) {
        if ( selModel_ != null ) {
            selModel_.removeListSelectionListener( this );
        }
        selModel_ = selModel;
        selModel_.addListSelectionListener( this );
    }

    public ListModel<T> getListModel() {
        return listModel_;
    }

    public void setListModel( ListModel<T> listModel ) {
        if ( listModel_ != null ) {
            listModel_.removeListDataListener( this );
        }
        listModel_ = listModel;
        redoAllItems();
        listModel_.addListDataListener( this );
    }

    public void valueChanged( ListSelectionEvent evt ) {
        for ( int i = evt.getFirstIndex(); i <= evt.getLastIndex(); i++ ) {
            if ( i < entries_.size() ) {
                entries_.get( i ).setSelected( selModel_.isSelectedIndex( i ) );
            }
        }
    }

    public void intervalAdded( ListDataEvent evt ) {
        int index0 = evt.getIndex0();
        int index1 = evt.getIndex1();
        if ( index0 == entries_.size() ) {
            for ( int i = index0; i <= index1; i++ ) {
                addItem( listModel_.getElementAt( i ) );
            }
        }
        else {
            redoAllItems();
            int start = Math.min( index0, index1 );
            int len = Math.abs( index1 - index0 ) + 1;
            selModel_.insertIndexInterval( start, len, false );
            selModel_.removeSelectionInterval( index0, index1 );
        }
        revalidate();
        repaint();
    }

    public void intervalRemoved( ListDataEvent evt ) {
        redoAllItems();
        selModel_.removeIndexInterval( evt.getIndex0(), evt.getIndex1() );
        revalidate();
        repaint();
    }

    public void contentsChanged( ListDataEvent evt ) {
        redoAllItems();
        revalidate();
        repaint();
    }

    public Dimension getPreferredScrollableViewportSize() {

        /* The addition of (about) 20 here appears to be required to 
         * prevent the layout going wrong (not enough width available)
         * Possibly to do with being in a JScrollPane; possibly not.
         * Edit at your own risk. */
        int width = getPreferredSize().width + 20;
        return new Dimension( Math.max( width, 120 ), getLineHeight() * 4 );
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public int getScrollableUnitIncrement( Rectangle visibleRect,
                                           int orientation, int direction ) {
        return ( orientation == SwingConstants.HORIZONTAL )
                     ? visibleRect.width
                     : getLineHeight();
    }

    public int getScrollableBlockIncrement( Rectangle visibleRect,
                                            int orientation, int direction ) {
        return ( orientation == SwingConstants.HORIZONTAL )
                     ? visibleRect.width
                     : visibleRect.height;
    }

    private int getLineHeight() {
        return entries_.size() > 0 ? entries_.get( 0 ).getHeight()
                                   : 0;
    }

    /**
     * Defines how to get annotations for check box items.
     */
    public interface Annotator {

        /**
         * Returns a new annotation for the given item.
         *
         * @param   item   item to be annotated
         * @return  new annotation component
         */
        Component createAnnotation( Object item );
    }
}
