package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.table.gui.TableSaveChooser;

/**
 * SavePanel implementation for saving the current table.
 *
 * @author   Mark Taylor
 */
public class CurrentSavePanel extends SavePanel {

    private final JLabel nameField_;
    private final JLabel subsetField_;
    private final JLabel orderField_;
    private final JLabel colsField_;
    private final JLabel rowsField_;
    private final TopcatListener tcListener_;
    private final TableColumnModelListener colListener_;
    private TableSaveChooser saveChooser_;
    private TopcatModel tcModel_;

    /**
     * Constructor.
     *
     * @param  sto   output marshaller
     */
    @SuppressWarnings("this-escape")
    public CurrentSavePanel( StarTableOutput sto ) {
        super( "Current Table",
               TableSaveChooser.makeFormatBoxModel( sto, false ) );
        setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        final JList<TopcatModel> tablesList =
            ControlWindow.getInstance().getTablesList();

        /* Ensure displayed table is always the TOPCAT current table. */
        tablesList.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                setDisplayedTable( tablesList.getSelectedValue() );
            }
        } );

        /* Ensure that current table characteristics are always up to date. */
        tcListener_ = new TopcatListener() {
            public void modelChanged( TopcatEvent evt ) {
                TopcatModel tcModel = evt.getModel();
                assert tcModel == tcModel_;
                int code = evt.getCode();
                if ( code == TopcatEvent.LABEL ) {
                    updateNameField( tcModel );
                }
                else if ( code == TopcatEvent.CURRENT_SUBSET ) {
                    updateRowsField( tcModel );
                    updateSubsetField( tcModel );
                }
                else if ( code == TopcatEvent.CURRENT_ORDER ) {
                    updateOrderField( tcModel );
                }
            }
        };
        colListener_ = new TableColumnModelListener() {
            public void columnAdded( TableColumnModelEvent evt ) {
                updateColsField( tcModel_ );
            }
            public void columnRemoved( TableColumnModelEvent evt ) {
                updateColsField( tcModel_ );
            }
            public void columnMoved( TableColumnModelEvent evt ) {
            }
            public void columnMarginChanged( ChangeEvent evt ) {
            }
            public void columnSelectionChanged( ListSelectionEvent evt ) {
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
        colsField_ = new JLabel();
        rowsField_ = new JLabel();
        nameField_.setFont( inputFont );
        subsetField_.setFont( inputFont );
        orderField_.setFont( inputFont );
        colsField_.setFont( inputFont );
        rowsField_.setFont( inputFont );
        stack.addLine( "Table", nameField_ );
        stack.addLine( "Current Subset", subsetField_ );
        stack.addLine( "Sort Order", orderField_ );

        /* I thought this might be a good idea, but probably it's too much
         * information. */
        if ( false ) {
            stack.addLine( "Columns", colsField_ );
            stack.addLine( "Rows", rowsField_ );
        }
        setDisplayedTable( tablesList.getSelectedValue() );
    }

    public StarTable[] getTables() {
        return new StarTable[] { TopcatUtils.getSaveTable( tcModel_ ) };
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
            tcModel_.getColumnModel().removeColumnModelListener( colListener_ );
        }
        tcModel_ = tcModel;
        if ( saveChooser_ != null ) {
            saveChooser_.setEnabled( tcModel != null );
        }
        updateNameField( tcModel );
        updateSubsetField( tcModel );
        updateOrderField( tcModel );
        updateColsField( tcModel );
        updateRowsField( tcModel );
        if ( tcModel != null ) {
            tcModel.addTopcatListener( tcListener_ );
            tcModel.getColumnModel().addColumnModelListener( colListener_ );
        }
    }

    /**
     * Updates the field containing the table name.
     *
     * @param   tcModel   model
     */
    private void updateNameField( TopcatModel tcModel ) {
        nameField_.setText( tcModel == null ? null : tcModel.toString() );
    }

    /**
     * Updates the field containing the current row subset.
     *
     * @param   tcModel   model
     */
    private void updateSubsetField( TopcatModel tcModel ) {
        final String text;
        if ( tcModel == null ) {
            text = null;
        }
        else {
            RowSubset subset = tcModel.getSelectedSubset();
            text = RowSubset.ALL.equals( subset ) ? null : subset.toString();
        }
        subsetField_.setText( text );
    }

    /**
     * Updates the field containing the current sort order.
     *
     * @param   tcModel   model
     */
    private void updateOrderField( TopcatModel tcModel ) {
        orderField_.setText( tcModel == null
                           ? null
                           : tcModel.getSelectedSort().toString() );
    }

    /**
     * Updates the field containing the table column count.
     *
     * @param  tcModel  model
     */
    private void updateColsField( TopcatModel tcModel ) {
        final String text;
        if ( tcModel == null ) {
            text = null;
        }
        else {
            int nvis = tcModel.getColumnModel().getColumnCount();
            int ntot = tcModel.getDataModel().getColumnCount();
            text = nvis == ntot
                 ? Integer.toString( nvis )
                 : Integer.toString( nvis ) + " / " + Integer.toString( ntot );
        }
        colsField_.setText( text );
    }

    /**
     * Updates the field containing the table row count.
     *
     * @param   tcModel   model
     */
    private void updateRowsField( TopcatModel tcModel ) {
        final String text;
        if ( tcModel == null ) {
            text = null;
        }
        else {
            long nvis = tcModel.getViewModel().getRowCount();
            long ntot = tcModel.getDataModel().getRowCount();
            text = nvis == ntot
                 ? TopcatUtils.formatLong( nvis )
                 : TopcatUtils.formatLong( nvis ) + " / " +
                   TopcatUtils.formatLong( ntot );
        }
        rowsField_.setText( text );
    }
}
