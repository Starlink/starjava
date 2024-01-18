package uk.ac.starlink.topcat.plot2;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.ResourceIcon;

/**
 * Coordinate stack implementation that can hold a variable number of
 * coordinates, which can be rearranged in the GUI.
 * Lines may be added as required, and at any time the first N lines
 * may be displayed without discarding the hidden ones.
 *
 * <p>The implementation of this and its interaction with the
 * MatrixPositionCoordPanel is a bit scrappy.
 * A rewrite including better thought out coordinate GUI pluggability
 * in CoordPanel would not be a bad idea.
 *
 * @author   Mark Taylor
 * @since    23 Jun 2023
 */
public class VariableCoordStack extends JPanel
                                implements BasicCoordPanel.CoordStack {

    private final List<Item> items_;
    private SimplePositionCoordPanel coordPanel_;
    private int nVisible_;
    private Item dragItem_;
    private int dragY_;

    private static final int ONLY_IU = 0; // not suitable for multipart coords

    /**
     * Constructor.
     */
    public VariableCoordStack() {
        items_ = new ArrayList<Item>();
    }

    public JLabel addCoordLine( String labelTxt, JComponent line ) {
        JLabel label = new JLabel( labelTxt + ": " );
        Item item = new Item( label, line );
        DragListener dragger = new DragListener( item );
        item.handle_.addMouseListener( dragger );
        item.handle_.addMouseMotionListener( dragger );
        item.deleteButton_.addActionListener( evt -> deleteCoord( item ) );
        items_.add( item );
        return label;
    }

    public JComponent getPanel() {
        return this;
    }

    /**
     * Sets the coord panel with which this stack will be working.
     * Should be called before it is used.
     *
     * @param  coordPanel  owner
     */
    public void setCoordPanel( SimplePositionCoordPanel coordPanel ) {
        coordPanel_ = coordPanel;
    }

    /**
     * Configures the display of this component so that only the first
     * <code>nline</code> lines are visible.
     *
     * @param   nline   number of items from the top of the list to display,
     *                  or -1 for all items
     */
    public void showItems( int nline ) {
        removeAll();
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints cons = new GridBagConstraints();
        setLayout( layout );
        cons.gridy = 0;
        int nitem = Math.min( items_.size(),
                              nline >= 0 ? nline : Integer.MAX_VALUE );

        /* Add each item in turn. */
        nVisible_ = Math.min( items_.size(), nitem );
        for ( int i = 0; i < nVisible_; i++ ) {
            Item item = items_.get( i );
            JLabel label = item.label_;
            Component body = item.body_;
            JLabel handle = item.handle_;
            JButton deleteButton = item.deleteButton_;

            /* Add some vertical padding except for the first added line. */
            if ( cons.gridy > 0 ) {
                cons.gridx = 0;
                Component strut = Box.createVerticalStrut( 4 );
                layout.setConstraints( strut, cons );
                add( strut );
                cons.gridy++;
            }
            int gx = 0;

            /* Add the label. */
            GridBagConstraints consL = (GridBagConstraints) cons.clone();
            consL.gridx = gx++;
            consL.anchor = GridBagConstraints.EAST;
            layout.setConstraints( label, consL );
            add( label );

            /* Add the handle. */
            GridBagConstraints consH = (GridBagConstraints) cons.clone();
            consH.gridx = gx++;
            consH.anchor = GridBagConstraints.WEST;
            layout.setConstraints( handle, consH );
            add( handle );

            /* Add the deletion button. */
            GridBagConstraints consB = (GridBagConstraints) cons.clone();
            consB.gridx = gx++;
            consB.anchor = GridBagConstraints.WEST;
            consB.insets = new Insets( 0, 3, 0, 5 );
            layout.setConstraints( deleteButton, consB );
            add( deleteButton );

            /* Add the query component. */
            GridBagConstraints consQ = (GridBagConstraints) cons.clone();
            consQ.gridx = gx++;
            consQ.anchor = GridBagConstraints.WEST;
            consQ.weightx = 1.0;
            consQ.fill = GridBagConstraints.HORIZONTAL;
            consQ.gridwidth = GridBagConstraints.REMAINDER;
            layout.setConstraints( body, consQ );
            add( body );

            /* Bump line index. */
            cons.gridy++;
        }
        revalidate();
        repaint();
    }

    @Override
    public void paintComponent( Graphics g ) {
        super.paintComponent( g );

        /* Provide visual feedback on coordinate drag operations
         * currently in progress.  This probably could be done better
         * (would be nice to have visual consistency with the JList
         * item drag), but I explored various other options and they
         * all looked like a lot of work.  I think this is good enough
         * for users to understand what's going on. */
        if ( dragItem_ != null ) {
            JLabel handle = dragItem_.handle_;
            ResourceIcon.UP_DOWN.paintIcon( this, g, handle.getX() - 3,
                                            handle.getY() + dragY_ );
            int icTo = getInsertPosition( dragItem_, dragY_ );
            if ( icTo >= 0 && icTo < nVisible_ ) {
                JLabel labelTo = items_.get( icTo ).label_;
                Rectangle box = labelTo.getBounds();
                Color color0 = g.getColor();
                g.setColor( Color.DARK_GRAY );
                g.drawRect( box.x, box.y, box.width, box.height );
                g.setColor( color0 );
            }
        }
    }

    /**
     * Invoked if the little delete button by one of the coordinates
     * is clicked.  It has the effect of removing the coordinate
     * selected in that row and moving all the others up to fill the gap.
     *
     * @param  item  item whose content is to be deleted
     */
    private void deleteCoord( Item item ) {
        int icDel = items_.indexOf( item );

        /* What we actually do is swap the "deleted" model with that of the
         * list visible row and reset selection to null for the one that
         * ends up at the bottom. */
        ColumnDataComboBoxModel delModel = getCoordSelector( icDel );
        delModel.setSelectedItem( null );
        for ( int ic = icDel; ic < nVisible_ - 1; ic++ ) {
            setCoordSelector( ic, getCoordSelector( ic + 1 ) );
        }
        setCoordSelector( nVisible_ - 1, delModel );
        coordPanel_.getActionForwarder()
                   .actionPerformed( new ActionEvent( this, 0, null ) );
    }

    /**
     * Invoked at the end of a suitable drag action.
     * It moves the selection of one of the coordinates to a different
     * position.
     *
     * @param  icFrom  starting coordinate index
     * @param  icTo   destination coordinate index
     */
    private void moveCoord( int icFrom, int icTo ) {
        ColumnDataComboBoxModel moveModel = getCoordSelector( icFrom );
        if ( icTo > icFrom ) {
            for ( int ic = icFrom; ic < icTo; ic++ ) {
                setCoordSelector( ic, getCoordSelector( ic + 1 ) );
            }
        }
        else {
            for ( int ic = icFrom; ic > icTo; ic-- ) {
                setCoordSelector( ic, getCoordSelector( ic - 1 ) );
            }
        }
        setCoordSelector( icTo, moveModel );
        coordPanel_.getActionForwarder()
                   .actionPerformed( new ActionEvent( this, 0, null ) );
    }

    /**
     * Gets the coordinate selection model for a given coordinate index.
     *
     * @param  ic  coordinate index
     * @return   selection model
     */
    private ColumnDataComboBoxModel getCoordSelector( int ic ) {
        return coordPanel_.getColumnSelector( ic, ONLY_IU );
    }

    /**
     * Sets the coordinate selection model for a given coordinate index.
     *
     * @param  ic  coordinate index
     * @param  model   model to install at position ic
     */
    private void setCoordSelector( int ic, ColumnDataComboBoxModel model ) {
        int iu = 0;
        coordPanel_.setColumnSelector( ic, ONLY_IU, model );
    }

    /**
     * Returns the coordinate index at which a given drag operation is
     * currently going to dump the dragged item.
     *
     * @param  dragItem   coordinate item being dragged
     * @param  dragY   Y coordinate of current drag position
     */
    private int getInsertPosition( Item dragItem, int dragY ) {
        int nitem = items_.size();
        if ( nitem < 2 || dragItem == null ) {
            return 0;
        }
        int lineHeight = items_.get( 1 ).handle_.getY()
                       - items_.get( 0 ).handle_.getY();
        JLabel handle = dragItem.handle_;
        int p0 = dragItem.getCenterY() + dragY;
        for ( int i = 0; i < nVisible_; i++ ) {
            if ( p0 < items_.get( i ).getCenterY() + lineHeight / 2 ) {
                return i;
            }
        }
        return nVisible_;
    }

    /**
     * Mouse listener that deals with dragging coordinates up and down.
     */
    private class DragListener extends MouseInputAdapter {

        final Item item_;
        Point fromPoint_;
        Point dragPoint_;

        /**
         * Constructor.
         *
         * @param  item  item being dragged
         */
        DragListener( Item item ) {
            item_ = item;
        }

        @Override
        public void mousePressed( MouseEvent evt ) {
            fromPoint_ = evt.getPoint();
        }

        @Override
        public void mouseReleased( MouseEvent evt ) {
            if ( dragItem_ != null ) {
                int icFrom = items_.indexOf( dragItem_ );
                int icTo = getInsertPosition( dragItem_, dragY_ );
                if ( icFrom >= 0 && icFrom < nVisible_ &&
                     icTo >= 0 && icTo < nVisible_ &&
                     icTo != icFrom ) {
                    moveCoord( icFrom, icTo );
                }
            }
            fromPoint_ = null;
            dragItem_ = null;
            dragY_ = 0;
            VariableCoordStack.this.repaint();
        }

        @Override
        public void mouseDragged( MouseEvent evt ) {
            if ( fromPoint_ != null ) {
                Point dragPoint = evt.getPoint();
                dragItem_ = item_;
                dragY_ = dragPoint.y - fromPoint_.y;
            }
            VariableCoordStack.this.repaint();
        }
    }

    /**
     * Utility class aggregating a label and query component.
     */
    private static class Item {
        final JLabel label_;
        final JLabel handle_;
        final JButton deleteButton_;
        final Component body_;

        /**
         * Constructor.
         *
         * @param  label   label component
         * @parm   body    body component
         */
        Item( JLabel label, Component body ) {
            label_ = label;
            body_ = body;
            deleteButton_ = new JButton( ResourceIcon.SMALL_CLOSE );
            deleteButton_.setMargin( new Insets( 0, 0, 0, 0 ) );
            handle_ = new JLabel( ResourceIcon.UP_DOWN );
            handle_.setBorder( BorderFactory.createEmptyBorder( 0, 5, 0, 5 ) );
        }

        /**
         * Returns the vertical position of this item.
         *
         * @return   current central Y coordinate
         */
        int getCenterY() {
            return handle_.getY() + handle_.getHeight() / 2;
        }
    }
}
