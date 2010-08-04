package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.table.gui.TableSaveChooser;

/**
 * SavePanel implementation for saving the current table.
 *
 * @author   Mark Taylor
 * @since    
 */
public class CurrentSavePanel extends SavePanel {

    private final JLabel nameField_;
    private final JLabel subsetField_;
    private final JLabel orderField_;
    private final TopcatListener tcListener_;
    private TableSaveChooser saveChooser_;
    private TopcatModel tcModel_;

    /**
     * Constructor.
     *
     * @param  sto   output marshaller
     */
    public CurrentSavePanel( StarTableOutput sto ) {
        super( "Current Table",
               TableSaveChooser.makeFormatBoxModel( sto, false ) );
        setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        final JList tablesList =
            ControlWindow.getInstance().getTablesList();

        /* Ensure displayed table is always the TOPCAT current table. */
        tablesList.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                setDisplayedTable( (TopcatModel)
                                   tablesList.getSelectedValue() );
            }
        } );

        /* Ensure that current table characteristics are always up to date. */
        tcListener_ = new TopcatListener() {
            public void modelChanged( TopcatEvent evt ) {
                handleTopcatEvent( evt.getModel(), evt.getCode() );
            }
        };

        /* Place components. */
        setLayout( new BorderLayout() );
        LabelledComponentStack stack = new LabelledComponentStack();
        Font inputFont = stack.getInputFont();
        add( stack, BorderLayout.NORTH );
        nameField_ = new JLabel();
        subsetField_ = new JLabel();
        orderField_ = new JLabel();
        nameField_.setFont( inputFont );
        subsetField_.setFont( inputFont );
        orderField_.setFont( inputFont );
        orderField_.setHorizontalTextPosition( SwingConstants.LEADING );
        stack.addLine( "Table", nameField_ );
        stack.addLine( "Subset", subsetField_ );
        stack.addLine( "Order", orderField_ );
        setDisplayedTable( (TopcatModel) tablesList.getSelectedValue() );
    }

    public StarTable[] getTables() {
        return new StarTable[] { tcModel_.getApparentStarTable() };
    }

    public void setActiveChooser( TableSaveChooser chooser ) {
        saveChooser_ = chooser;
        if ( saveChooser_ != null ) {
            saveChooser_.setEnabled( tcModel_ != null );
        }
    }

    /**
     * Sets the table which is displayed in this panel.
     *
     * @param  tcModel  table to display
     */
    private void setDisplayedTable( TopcatModel tcModel ) {
        if ( tcModel_ != null ) {
            tcModel_.removeTopcatListener( tcListener_ );
        }
        tcModel_ = tcModel;
        if ( saveChooser_ != null ) {
            saveChooser_.setEnabled( tcModel != null );
        }
        if ( tcModel == null ) {
            nameField_.setText( null );
            subsetField_.setText( null );
            orderField_.setText( null );
        }
        else {
            handleTopcatEvent( tcModel, TopcatEvent.LABEL );
            handleTopcatEvent( tcModel, TopcatEvent.CURRENT_SUBSET );
            handleTopcatEvent( tcModel, TopcatEvent.CURRENT_ORDER );
            tcModel.addTopcatListener( tcListener_ );
        }
    }

    /**
     * Update a field of the displayed table as instructed.
     * Does the work for the TopcatListener interface, but can be called
     * separately as well.
     *
     * @param   tcModel  table providing data
     * @param   code   event type
     */
    private void handleTopcatEvent( TopcatModel tcModel, int code ) {
        assert tcModel == tcModel_;
        if ( code == TopcatEvent.LABEL ) {
            nameField_.setText( tcModel.toString() );
        }
        else if ( code == TopcatEvent.CURRENT_SUBSET ) {
            RowSubset subset = tcModel.getSelectedSubset();
            subsetField_.setText( RowSubset.ALL.equals( subset )
                                      ? null
                                      : subset.toString() );
        }
        else if ( code == TopcatEvent.CURRENT_ORDER ) {
            orderField_.setText( tcModel.getSelectedSort().toString() );
        }
    }
}
