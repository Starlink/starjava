package uk.ac.starlink.topcat;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.MouseInputAdapter;

/**
 * JList subclass that adds a couple of features.
 * First, each entry has a checkbox associated with it that may be
 * selected or deselected by the user.
 * Second, the list entries can be reordered by the user dragging
 * them up or down in the list.  A distinctive handle is provided to
 * indicate this option visually.
 * Third, you can overpaint a message below the list items.
 *
 * <p>The selection model for the checkboxes and the list model giving
 * entry ordering are held externally to this implementation, so must be
 * explicitly managed by clients of this class,
 * and may be modified programmatically.
 *
 * @author   Mark Taylor
 * @since    13 Mar 2013
 */
public abstract class CheckBoxList<T> extends JList {

    private final Class<T> clazz_;
    private final boolean canSelect_;
    private final CheckBoxCellRenderer renderer_;
    private final DragListener dragger_;
    private final List<ListDataListener> listeners_;
    private String[] msgLines_;

    /**
     * Constructor.
     * A renderer component is supplied; when entries need to be displayed
     * the <code>configureEntryRenderer</code> method is called.
     *
     * @param   clazz  supertype for each entry in the list
     * @param   model  list model
     * @param   canSelect   true if list item selection is permitted
     * @param   entryRenderer   renderer for list entry contents
     *                          (excluding drag and checkbox decorations)
     */
    public CheckBoxList( Class<T> clazz, ListModel model, boolean canSelect,
                         JComponent entryRenderer ) {
        super( model );
        clazz_ = clazz;
        canSelect_ = canSelect;

        /* Arrange to forward ListDataEvents from the base list model to
         * listeners to this object.  Checkbox update events will also
         * be forwarded by separate arrangement. */
        listeners_ = new ArrayList<ListDataListener>();
        model.addListDataListener( new ListDataListener() {
            public void contentsChanged( ListDataEvent evt ) {
                for ( ListDataListener l : listeners_ ) {
                    l.contentsChanged( evt );
                }
            }
            public void intervalAdded( ListDataEvent evt ) {
                for ( ListDataListener l : listeners_ ) {
                    l.intervalAdded( evt );
                }
            }
            public void intervalRemoved( ListDataEvent evt ) {
                for ( ListDataListener l : listeners_ ) {
                    l.intervalRemoved( evt );
                }
            }
        } );

        /* Set up cell rendering. */
        renderer_ = new CheckBoxCellRenderer( entryRenderer );
        setCellRenderer( renderer_ );

        /* Remove any default mouse listeners and replace them with
         * custom ones.  We need to handle all mouse events explicitly
         * to make sure drag and checkbox selection works predictably. */
        MouseListener[] mListeners = getListeners( MouseListener.class );
        for ( int il = 0; il < mListeners.length; il++ ) {
            removeMouseListener( mListeners[ il ] );
        }
        MouseMotionListener[] mmListeners =
            getListeners( MouseMotionListener.class );
        for ( int il = 0; il < mmListeners.length; il++ ) {
            removeMouseMotionListener( mmListeners[ il ] );
        }
        addMouseListener( new FocusListener() );
        addMouseListener( new CheckBoxListener() );
        dragger_ = new DragListener();
        addMouseListener( dragger_ );
        addMouseMotionListener( dragger_ );
    }

    /**
     * This method is called whenever the list cell needs to be painted.
     *
     * @param  entryRenderer  renderer object supplied at construction time
     * @param  item   list entry
     * @param  index  index in list at which entry appears
     */
    protected abstract void configureEntryRenderer( JComponent entryRenderer,
                                                    T item, int index );

    /**
     * Indicates whether the checkbox for a given item is selected.
     *
     * @param  item   list entry
     * @return  true iff item is selected
     */
    public abstract boolean isChecked( T item );

    /**
     * Sets whether the checkbox for a given item is selected.
     * Called when the user interacts with the checkbox.
     * It is up to the concrete implementation to ensure that this is
     * reflected by the <code>isChecked</code> method.
     *
     * @param  item  list entry
     * @param  isChecked   whether item should be selected
     */
    public abstract void setChecked( T item, boolean isChecked );

    /**
     * Indicates that the user has requested a reordering of the list model.
     * It is up to the concrete implementation to ensure that this is
     * reflected in the list model.
     *
     * @param   ifrom  source list index
     * @param   ito  destination list index
     */
    public abstract void moveItem( int ifrom, int ito );

    /**
     * Returns a list cell entry cast to the entry type of this list,
     * or null if it can't be done.
     *
     * @param  value   list entry
     * @return  typed list entry, or null
     */
    public T getTypedValue( Object value ) {
        return clazz_.isInstance( value ) ? clazz_.cast( value ) : null;
    }

    @Override
    public T getSelectedValue() {
        return getTypedValue( super.getSelectedValue() );
    }

    /**
     * Sets a message which is overpainted on the blank part of this
     * component.  If null or empty, no message is painted.
     *
     * @param  msgLines  lines of a message to paint
     */
    public void setListMessage( String[] msgLines ) {
        msgLines_ = msgLines;
        repaint();
    }

    /**
     * Adds a listener for list events.  As well as changes to the
     * underlying ListModel, this will also be messaged when any of
     * the elements becomes checked or unchecked.
     *
     * @param  l  listener to add
     */
    public void addListDataListener( ListDataListener l ) {
        listeners_.add( l );
    }

    /**
     * Removes a listener previously added.
     *
     * @param  l  listener to remove
     */
    public void removeListDataListener( ListDataListener l ) {
        listeners_.remove( l );
    }

    @Override
    protected void paintComponent( Graphics g ) {
        super.paintComponent( g );

        /* Handle rendering of list rows being dragged up and down to
         * reorder the list. */
        T dragItem = dragger_.dragItem_;
        Point itemBase = indexToLocation( dragger_.fromIndex_ );
        if ( dragItem != null ) {
            int fromIndex = dragger_.fromIndex_;
            CheckBoxCellRenderer dragComp =
                (CheckBoxCellRenderer)
                renderer_.getListCellRendererComponent(
                    this, dragItem, fromIndex, isSelectedIndex( fromIndex ),
                    hasFocus() );
            int dy = dragger_.dragPoint_.y - dragger_.fromPoint_.y;
            Rectangle bounds = getCellBounds( fromIndex, fromIndex );
            bounds.y += dy;
            dragComp.setBounds( bounds );
            dragComp.stamp( g );
        }

        /* Overpaint the message text if there is any. */
        if ( msgLines_ != null && msgLines_.length > 0 ) {
            int nItem = getModel().getSize();
            Rectangle space = getBounds( null );
            Rectangle itemBounds = getCellBounds( 0, getModel().getSize() - 1 );
            if ( itemBounds != null ) {
                space.y = itemBounds.y + itemBounds.height;
                space.height -= itemBounds.height;
            }
            Color color0 = g.getColor();
            g.setColor( Color.LIGHT_GRAY );
            FontMetrics fm = g.getFontMetrics();
            int wmax = 0;
            for ( String line : msgLines_ ) {
                int width = (int) fm.getStringBounds( line, g ).getWidth();
                wmax = Math.max( wmax, width );
            }
            int x = Math.max( 0, ( space.width - wmax ) / 2 );
            int y = space.y + fm.getLeading() + fm.getAscent();
            int dy = fm.getAscent() + fm.getDescent() + fm.getLeading();
            for ( String line : msgLines_ ) {
                y += dy;
                g.drawString( line, x, y );
            }
            g.setColor( color0 );
        }
    }

    /**
     * Mouse listener that looks out for clicks on the checkbox part of
     * list cells and behaves as if the checkbox is working.
     */
    private class CheckBoxListener extends MouseAdapter {

        @Override
        public void mousePressed( MouseEvent evt ) {
            Point point = evt.getPoint();
            if ( canSelect_ && ! isCheckbox( point ) && ! isHandle( point ) ) {
                int index = locationToIndex( point );
                Object value = index >= 0 ? getModel().getElementAt( index )
                                          : null;
                setSelectedValue( value, false );
            }
        }

        @Override
        public void mouseClicked( MouseEvent evt ) {
            Point point = evt.getPoint();
            if ( isCheckbox( point ) ) {
                int index = locationToIndex( point );
                T item = getTypedValue( getModel().getElementAt( index ) );
                if ( item != null ) {
                    setChecked( item, ! isChecked( item ) );
                    ListDataEvent devt =
                        new ListDataEvent( this, ListDataEvent.CONTENTS_CHANGED,
                                           index, index );
                    for ( ListDataListener l : listeners_ ) {
                        l.contentsChanged( devt );
                    }
                }
            }
        }

        /**
         * Returns the relative position within a list cell of a given
         * graphics position.
         *
         * @param  point  graphics position
         * @return   relative position within list cell
         */
        private Point getRelativePoint( Point point ) {
            Point basePoint = indexToLocation( locationToIndex( point ) );
            return basePoint == null ? null
                                     : new Point( point.x - basePoint.x,
                                                  point.y - basePoint.y );
        }

        /**
         * Indicates whether a graphics position is positioned over
         * a checkbox for one of the list entries.
         *
         * @param  point  graphics position
         * @return  true iff point is on a checkbox
         */
        private boolean isCheckbox( Point point ) {
            Point rp = getRelativePoint( point );
            return rp != null && renderer_.isCheckBox( rp );
        }

        /**
         * Indicates whether a graphics position is positioned over
         * an up/down drag handles for one of the list entries.
         *
         * @param  point  graphics position
         * @return  true iff point is on a drag handle
         */
        private boolean isHandle( Point point ) {
            Point rp = getRelativePoint( point );
            return rp != null && renderer_.isHandle( rp );
        }
    }

    /**
     * Grabs focus on click.  The component would normally have a listener
     * that does this anyway, but we chuck out all of the default mouse
     * listeners, so need to re-implement this functionality.  It's possible
     * that the behaviour won't be exactly right for all LAFs.  Too bad.
     */
    private class FocusListener extends MouseAdapter {
        @Override
        public void mouseClicked( MouseEvent evt ) {
            requestFocusInWindow();
        }
    }

    /**
     * Mouse listener that handles drag gestures to reorder list entries.
     */
    private class DragListener extends MouseInputAdapter {

        private Point fromPoint_;
        private int fromIndex_ = -1;
        private T dragItem_;
        private Point dragPoint_;

        @Override
        public void mousePressed( MouseEvent evt ) {
            fromPoint_ = evt.getPoint();
            fromIndex_ = locationToIndex( fromPoint_ );
        }

        @Override
        public void mouseReleased( MouseEvent evt ) {
            T selItem = getSelectedValue();
            int toIndex = locationToIndex( evt.getPoint() );
            if ( toIndex >= 0 && fromIndex_ >= 0 && toIndex != fromIndex_ ) {
                moveItem( fromIndex_, toIndex );
            }
            fromIndex_ = -1;
            fromPoint_ = null;
            dragItem_ = null;
            dragPoint_ = null;
            if ( selItem != null ) {
                setSelectedValue( selItem, true );
            }
        }

        @Override
        public void mouseDragged( MouseEvent evt ) {
            dragItem_ = getTypedValue( getModel().getElementAt( fromIndex_ ) );
            dragPoint_ = evt.getPoint();
            CheckBoxList.this.repaint();
        }
    }

    /**
     * Custom ListCellRenderer implementation for a CheckBoxList.
     */
    private class CheckBoxCellRenderer extends JPanel
                                       implements ListCellRenderer {

        private final JComponent entryRenderer_;
        private final JCheckBox checkBox_;
        private final JLabel handle_;
        private final DefaultListCellRenderer dfltRenderer_;

        /**
         * Constructor.
         *
         * @param   entryRenderer   renderer for list entry contents
         *                          (excluding drag and checkbox decorations)
         */
        CheckBoxCellRenderer( JComponent entryRenderer) {
            entryRenderer_ = entryRenderer;
            setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
            checkBox_ = new JCheckBox();
            checkBox_.setOpaque( false );
            handle_ = new JLabel( ResourceIcon.UP_DOWN );
            dfltRenderer_ = new DefaultListCellRenderer();
            setOpaque( true );
            add( handle_ );
            add( checkBox_ );
            add( entryRenderer_ );
        }

        public Component getListCellRendererComponent( JList list, Object value,
                                                       int index, boolean isSel,
                                                       boolean hasFocus ) {
            dfltRenderer_.getListCellRendererComponent( list, value, index,
                                                        isSel, hasFocus );
            setBackground( dfltRenderer_.getBackground() );
            setForeground( dfltRenderer_.getForeground() );
            setBorder( dfltRenderer_.getBorder() );
            T item = getTypedValue( value );
            final int itemWidth;
            if ( item != null ) {
                checkBox_.setSelected( isChecked( item ) );
                configureEntryRenderer( entryRenderer_, item, index );
                entryRenderer_.validate();
                itemWidth = entryRenderer_.getPreferredSize().width;
            }
            else {
                itemWidth = 0;
            }

            /* Desperate measures to get the sizing of this component right.
             * For reasons I've tried very hard, and failed, to understand,
             * if this preferred width is not set explicitly here, the
             * preferred size of the JList is sometimes reported wrong
             * (the size of the first entry). */
            Insets insets = getInsets();
            int totWidth = handle_.getPreferredSize().width
                         + checkBox_.getPreferredSize().width
                         + itemWidth
                         + insets.left + insets.right;
            int height = super.getPreferredSize().height;
            setPreferredSize( new Dimension( totWidth, height ) );
            return this;
        }

        /**
         * Indicates whether a given point within this component is
         * positioned over the checkbox part.
         *
         * @param  point  graphics position
         * @return  true iff point is over the checkbox
         */
        boolean isCheckBox( Point p ) {
            return checkBox_.getBounds().contains( p );
        }

        /**
         * Indicates whether a given point within this component is
         * positioned over the drag handle part.
         *
         * @param  point  graphics position
         * @return  true iff point is over the drag handle
         */
        boolean isHandle( Point p ) {
            return handle_.getBounds().contains( p );
        }

        /**
         * From what it says about list cell renderers in the JList javadocs,
         * I thought that this was what
         * {@link javax.swing.JComponent#paint(java.awt.Graphics)} did,
         * but it seems not.
         */
        public void stamp( Graphics g ) {
            Rectangle bounds = getBounds();
            g.translate( bounds.x, bounds.y );
            paintComponent( g );
            paintBorder( g );
            paintChildren( g );
            g.translate( -bounds.x, -bounds.y );
        }
    }
}
