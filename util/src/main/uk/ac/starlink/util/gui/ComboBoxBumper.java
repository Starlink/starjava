package uk.ac.starlink.util.gui;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import uk.ac.starlink.util.images.ImageHolder;

/**
 * Icon providing arrows to cycle the value of a JComboBox up or down.
 * This allows you to cycle through the combo box elements using only
 * one click at a time.
 *
 * @author   Mark Taylor
 * @since    10 Nov 2005
 */
public class ComboBoxBumper extends JPanel {

    private final JComboBox<?> comboBox_;
    private final Action[] bumpActions_;

    /** Icon for decrement button. */
    public static Icon DEC_ICON =
        new ImageIcon( ImageHolder.class.getResource( "dec.gif" ) );

    /** Icon for increment button. */
    public static Icon INC_ICON =
        new ImageIcon( ImageHolder.class.getResource( "inc.gif" ) );

    /**
     * Constructs a new bumper based on a given combo box.
     *
     * @param   comboBox  the combo box this will operate on
     */
    @SuppressWarnings("this-escape")
    public ComboBoxBumper( JComboBox<?> comboBox ) {
        comboBox_ = comboBox;
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );

        /* Construct and place buttons. */
        bumpActions_ = new Action[] {
            new BumpAction( "Prev", DEC_ICON, "Select previous column", -1 ),
            new BumpAction( "Next", INC_ICON, "Select next column", 1 ),
        };
        for ( int i = 0; i < bumpActions_.length; i++ ) {
            JButton butt = new JButton( bumpActions_[ i ] );
            butt.setText( null );
            butt.setMargin( new Insets( 1, 1, 1, 1 ) );
            butt.setMaximumSize( new Dimension( butt.getMaximumSize().width,
                                                Integer.MAX_VALUE ) );
            add( butt );
        }

        /* Ensure that the buttons in this component track the enabled
         * status of the combo box itself. */
        comboBox_.addPropertyChangeListener( new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent evt ) {
                if ( "enabled".equals( evt.getPropertyName() ) ) {
                    setEnabled( comboBox_.isEnabled() );
                }
            }
        } );
    }

    public Dimension getPreferredSize() {
        return new Dimension( super.getPreferredSize().width,
                              comboBox_.getPreferredSize().height );
    }

    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        for ( int i = 0; i < bumpActions_.length; i++ ) {
            bumpActions_[ i ].setEnabled( enabled );
        }
    }

    /**
     * Action which cycles the associated combo box up or down.
     */
    private class BumpAction extends AbstractAction {

        private final int inc_;

        /**
         * Constructs a new BumpAction.
         *
         * @param  name  action name
         * @param  icon  action icon
         * @param  desc  short description (tool tip)
         * @param  inc   amount by which to increase the selection index
         *               of the combo box when this action is invoked
         */
        public BumpAction( String name, Icon icon, String desc, int inc ) {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, desc );
            inc_ = inc;
        }

        public void actionPerformed( ActionEvent evt ) {
            int nItem = comboBox_.getItemCount();

            /* Locate the index of the currently selected item.  Don't use
             * JComboBox.getSelectedIndex() - it doesn't cope properly
             * with nulls. */
            int isel = -1;
            Object sel = comboBox_.getSelectedItem();
            for ( int i = 0; i < nItem && isel < 0; i++ ) {
                Object item = comboBox_.getItemAt( i );
                if ( item == null ? sel == null
                                  : item.equals( sel ) ) {
                    isel = i;
                }
            }

            /* If we have a selection which is in the combo box, bump it
             * from the current selection in the requested direction. */
            if ( isel >= 0 ) {
                isel = ( isel + inc_ + nItem ) % nItem;
            }

            /* Otherwise, bump it to position zero. */
            else {
                isel = 0;
            }
            comboBox_.setSelectedIndex( isel );
            comboBox_.repaint();
        }
    }
}
