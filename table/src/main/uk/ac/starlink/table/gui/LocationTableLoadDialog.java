package uk.ac.starlink.table.gui;

import java.awt.Component;
import java.io.IOException;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.util.DataSource;

/**
 * Load dialogue which relies on the user typing the location into
 * a text field.
 *
 * @author   Mark Taylor
 * @since    13 Sept 2010
 */
public class LocationTableLoadDialog extends AbstractTableLoadDialog {

    private JTextField locField_;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public LocationTableLoadDialog() {
        super( "Location", "Loads from a filename or URL entered as text" );
        setIconUrl( StarTable.class.getResource( "gui/loctext.png" ) );
    }

    protected Component createQueryComponent() {
        JComponent qc = Box.createHorizontalBox();
        qc.add( new JLabel( "Location: " ) );
        locField_ = new JTextField( 25 );
        locField_.addCaretListener( new CaretListener() {
            public void caretUpdate( CaretEvent evt ) {
                updateReady();
            }
        } );
        updateReady();
        qc.add( locField_ );
        locField_.addActionListener( getSubmitAction() );
        qc.add( createFormatSelector() );
        return qc;
    }

    public boolean isReady() {
        String text = locField_.getText();
        return text != null && text.trim().length() > 0;
    }

    public TableLoader createTableLoader() {
        final String loc = locField_.getText();
        final String format = getSelectedFormat();
        if ( loc == null || loc.trim().length() == 0 ) {
            throw new IllegalStateException( "No location supplied" );
        }
        else {
            return new TableLoader() {
                public String getLabel() {
                    return loc;
                }
                public TableSequence loadTables( StarTableFactory tfact )
                        throws IOException {
                    return tfact.makeStarTables( loc, format );
                }
            };
        }
    }

    /**
     * Returns the text field into which the location is entered.
     *
     * @return   location text field
     */
    public JTextField getLocationField() {
        getQueryComponent();
        return locField_;
    }
}
