package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.BitSet;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.topcat.interop.SubsetActivity;
import uk.ac.starlink.topcat.interop.TopcatCommunicator;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Dialogue window which asks the user what to do with a newly created
 * row subset.
 *
 * <p>This component is used in a modal fashion despite the general
 * prejudice in TOPCAT against modal dialogues, since at present some of
 * the code which invokes it does so using data which may become out of
 * date if further user GUI activity takes place.
 *
 * @author   Mark Taylor
 * @since    11 Dec 2008
 */
public class SubsetConsumerDialog extends JPanel {

    private final JComboBox<String> nameSelector_;
    private JDialog dialog_;
    private SubsetConsumer consumer_;

    /**
     * Constructor.
     *
     * @param   tcModel   table for which table was generated
     * @param   communicator   interop abstraction object
     */
    @SuppressWarnings("this-escape")
    public SubsetConsumerDialog( TopcatModel tcModel,
                                 final TopcatCommunicator communicator ) {
        super( new BorderLayout() );
        JComponent box = Box.createVerticalBox();
        add( box, BorderLayout.CENTER );

        /* Set up layout. */
        JComponent nameLine = Box.createHorizontalBox();
        nameSelector_ = tcModel.createNewSubsetNameSelector();
        nameLine.add( new JLabel( "New Subset Name: " ) );
        nameLine.add( nameSelector_ );
        box.add( nameLine );

        /* Action for just adding the subset to the models subset list. */
        Action addAction =
                new ConsumerAction( "Add Subset", ResourceIcon.ADD,
                                    "Add the new subset to the table's "
                                  + "Subsets Window",
                                    true ) {
            public void consumeSubset( TopcatModel tcModel, RowSubset rset ) {
                tcModel.addSubset( rset );
            }
        };
        JComponent addLine = Box.createHorizontalBox();
        addLine.add( new JButton( addAction ) );
        addLine.add( Box.createHorizontalGlue() );
        box.add( Box.createVerticalStrut( 5 ) );
        box.add( addLine );

        /* Like add, but also sets to current subset. */
        Action applyAction =
                new ConsumerAction( "Add and Set Current Subset",
                                    ResourceIcon.ADD,
                                    "Add the new subset to the table's "
                                  + "Subsets Window and set it as the table's "
                                  + "Current Subset",
                                    true ) {
            public void consumeSubset( TopcatModel tcModel, RowSubset rset ) {
                tcModel.addSubset( rset );
                tcModel.applySubset( rset );
            }
        };
        JComponent applyLine = Box.createHorizontalBox();
        applyLine.add( new JButton( applyAction ) );
        applyLine.add( Box.createHorizontalGlue() );
        box.add( Box.createVerticalStrut( 5 ) );
        box.add( applyLine );

        /* Transmits the subset to listening clients. */
        if ( communicator != null ) {
            final SubsetActivity subsetActivity =
                communicator.createSubsetActivity();
            final Action transmitAction =
                    new ConsumerAction( "Transmit Subset",
                                        ResourceIcon.BROADCAST,
                                        "Send the subset to all "
                                      + "listening applications",
                                        false ) {
                public void consumeSubset( TopcatModel tcModel,
                                           RowSubset rset ) {
                    try {
                        subsetActivity.selectSubset( tcModel, rset );
                    }
                    catch ( IOException e ) {
                        ErrorDialog.showError( SubsetConsumerDialog.this,
                                               "Send Error", e,
                                               "Failed to transmit subset" );
                    }
                }
            };
            JComponent transmitLine = Box.createHorizontalBox();
            @SuppressWarnings("unchecked")
            ComboBoxModel<Object> targetModel =
                (ComboBoxModel<Object>) subsetActivity.getTargetSelector();
            final JComboBox<Object> targetSelector =
                new JComboBox<Object>( targetModel );
            if ( subsetActivity != null ) {
                transmitLine.add( new JButton( transmitAction ) );
            }
            transmitLine.add( Box.createHorizontalStrut( 5 ) );
            transmitLine.add( new JLabel( ResourceIcon.FORWARD ) );
            transmitLine.add( Box.createHorizontalStrut( 5 ) );
            transmitLine.add( targetSelector );
            communicator.addConnectionListener( new ChangeListener() {
                public void stateChanged( ChangeEvent evt ) {
                    boolean isConn = communicator.isConnected();
                    transmitAction.setEnabled( isConn );
                    targetSelector.setEnabled( isConn );
                }
            } );
            box.add( Box.createVerticalStrut( 5 ) );
            box.add( transmitLine );
        }
    }

    /**
     * Asks the user what should be done with a subset.
     * Blocks until a response is available.
     *
     * @param  parent  dialogue parent component
     * @return   consumer object, or null for no action
     */
    public SubsetConsumer enquireConsumer( Component parent ) {
        JOptionPane op = new JOptionPane( this, JOptionPane.QUESTION_MESSAGE );
        op.setOptions( new String[] { "Cancel", } );
        dialog_ = op.createDialog( parent, "New Subset" );
        consumer_ = null;
        dialog_.setVisible( true );
        return consumer_;
    }

    /**
     * Returns the name entered by a user in the subset name text field.
     *
     * @return  current name field content
     */
    private String getSubsetName() {
        Object item = nameSelector_.getSelectedItem();
        if ( item instanceof String ) {
            return (String) item;
        }
        else if ( item instanceof RowSubset ) {
            return ((RowSubset) item).getName();
        }
        else if ( item == null ) {
            return null;
        }
        else {
            assert false;
            return item.toString();
        }
    }

    /**
     * Action which represents one of the consumer possibilities.
     */
    private abstract class ConsumerAction extends BasicAction
                                          implements SubsetConsumer {
        private final boolean reqName_;
        private String name_;

        /**
         * Constructor.
         *
         * @param   name  action name
         * @param   icon  action icon
         * @param   descrip   action description
         * @param   reqName  true if this action requires a non-empty name
         *                   in order to make sense
         */
        ConsumerAction( String name, Icon icon, String descrip,
                        boolean reqName ) {
            super( name, null, descrip );
            reqName_ = reqName;
        }

        public void actionPerformed( ActionEvent evt ) {
            Object item = nameSelector_.getSelectedItem();
            name_ = getSubsetName();
            if ( reqName_ && ( name_ == null || name_.trim().length() == 0 ) ) {
                JOptionPane.showMessageDialog( SubsetConsumerDialog.this,
                                               "Name required to add subset",
                                               "Missing Name",
                                               JOptionPane.WARNING_MESSAGE );
            }
            else {
                consumer_ = this;
                dialog_.dispose();
                if ( reqName_ ) {
                    nameSelector_.setSelectedItem( null );
                }
            }
        }

        /**
         * Implement SubsetConsumer.
         */
        public void consumeSubset( TopcatModel tcModel, BitSet rowMask ) {
            consumeSubset( tcModel,
                           new BitsRowSubset( name_, rowMask ) );
        }

        /**
         * Does something with an actual RowSubset.
         *
         * @param   tcModel   table
         * @param   rset   row subset (name will be non-null if reqName is true)
         */
        abstract void consumeSubset( TopcatModel tcModel, RowSubset rset );
    }
}
