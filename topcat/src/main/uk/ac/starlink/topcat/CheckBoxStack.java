package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.JCheckBox;
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
 * Like a <tt>JList</tt>, it has an associated {@link javax.swing.ListModel}
 * and {@link javax.swing.ListSelectionModel}, 
 * which may be shared with other views in the usual way.
 *
 * @author   Mark Taylor (Starlink)
 */
public class CheckBoxStack extends JPanel
                           implements ListSelectionListener,
                                      ListDataListener,
                                      Scrollable {

    private ListSelectionModel selModel;
    private ListModel listModel;
    private List entries;

    /**
     * Constructs a new CheckBoxStack from a ListModel.
     *
     * @param  listModel the model
     */
    public CheckBoxStack( ListModel listModel ) {
        setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
        setListModel( listModel );
        setSelectionModel( new DefaultListSelectionModel() );
    }

    /**
     * Constructs a new CheckBoxStack from a default list model.
     */
    public CheckBoxStack() {
        this( new DefaultListModel() );
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
        final int pos = entries.size();
        if ( selModel != null ) {
            cbox.setSelected( selModel.isSelectedIndex( pos ) );
        }

        /* Make sure that changes in the selection status of the new 
         * checkbox are transmitted to the selection model. */
        cbox.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                if ( cbox.isSelected() ) {
                    selModel.addSelectionInterval( pos, pos );
                }
                else {
                    selModel.removeSelectionInterval( pos, pos );
                }
            }
        } );

        /* Keep a record of the checkbox. */
        entries.add( cbox );

        /* Add it to the panel. */
        add( cbox );
    }

    /**
     * Reconstruct the contents of this component from scratch.
     */
    private void redoAllItems() {
        removeAll();
        entries = new ArrayList();
        for ( int i = 0; i < listModel.getSize(); i++ ) {
            addItem( listModel.getElementAt( i ) );
        }
    }

    public ListSelectionModel getSelectionModel() {
        return selModel;
    }

    public void setSelectionModel( ListSelectionModel selModel ) {
        if ( this.selModel != null ) {
            this.selModel.removeListSelectionListener( this );
        }
        this.selModel = selModel;
        selModel.addListSelectionListener( this );
    }

    public ListModel getListModel() {
        return listModel;
    }

    public void setListModel( ListModel listModel ) {
        if ( this.listModel != null ) {
            this.listModel.removeListDataListener( this );
        }
        this.listModel = listModel;
        redoAllItems();
        listModel.addListDataListener( this );
    }

    public void valueChanged( ListSelectionEvent evt ) {
        for ( int i = evt.getFirstIndex(); i <= evt.getLastIndex(); i++ ) {
            ((JCheckBox) entries.get( i ))
           .setSelected( selModel.isSelectedIndex( i ) );
        }
    }

    public void intervalAdded( ListDataEvent evt ) {
        int index0 = evt.getIndex0();
        int index1 = evt.getIndex1();
        if ( index0 == entries.size() ) {
            for ( int i = index0; i <= index1; i++ ) {
                addItem( listModel.getElementAt( i ) );
            }
        }
        else {
            redoAllItems();
        }
        revalidate();
        repaint();
    }

    public void intervalRemoved( ListDataEvent evt ) {
        redoAllItems();
        revalidate();
        repaint();
    }

    public void contentsChanged( ListDataEvent evt ) {
        redoAllItems();
        revalidate();
        repaint();
    }

    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension( Math.max( getPreferredSize().width, 100 ),
                              getLineHeight() * 4 );
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
        return entries.size() > 0 ? ((Component) entries.get( 0 )).getHeight()
                                  : 24;
    }

}
