package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.gui.TableLoader;

/**
 * Load dialogue for TAP services.
 *
 * @author   Mark Taylor
 * @since    18 Jan 2011
 * @see <a href="http://www.ivoa.net/Documents/TAP/">IVOA TAP Recommendation</a>
 */
public class TapTableLoadDialog extends DalTableLoadDialog {

    private JEditorPane adqlPanel_;

    /**
     * Constructor.
     */
    public TapTableLoadDialog() {
        super( "TAP", "Query remote databases using SQL-like language",
               Capability.TAP, false, false );
        setIconUrl( TapTableLoadDialog.class.getResource( "tap.gif" ) );
    }

    protected Component createQueryComponent() {
        final Component superPanel = super.createQueryComponent();
        JComponent queryPanel = new JPanel( new BorderLayout() ) {
            public void setEnabled( boolean enabled ) {
                super.setEnabled( enabled );
                superPanel.setEnabled( enabled );
                adqlPanel_.setEnabled( enabled );
            }
        };
        queryPanel.add( superPanel, BorderLayout.NORTH );
        adqlPanel_ = new JEditorPane();
        adqlPanel_.setEditable( true );
        JComponent adqlScroller = new JScrollPane( adqlPanel_ );
        adqlScroller.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder( Color.BLACK ),
                "ADQL Text" ) );
        queryPanel.add( adqlScroller, BorderLayout.CENTER );
        return queryPanel;
    }

    public TableLoader createTableLoader() {
        String serviceUrl = getServiceUrl();
        checkUrl( serviceUrl );
        String adql = adqlPanel_.getText();
        final TapQuery query = TapQuery.createAdqlQuery( serviceUrl, adql );
        final List metadata = new ArrayList();
        metadata.addAll( Arrays.asList( query.getQueryMetadata() ) );
        metadata.addAll( Arrays.asList( getResourceMetadata( serviceUrl ) ) );
        final UwsJob job = query.getJob();
        return new TableLoader() {
            public TableSequence loadTables( StarTableFactory tfact )
                    throws IOException {
                StarTable st;
                try {
                    st = query.execute( tfact, 4000 );
                }
                catch ( InterruptedException e ) {
                    throw (IOException)
                          new InterruptedIOException( "Interrupted" )
                         .initCause( e );
                }
                st.getParameters().addAll( metadata );
                return Tables.singleTableSequence( st );
            }
            public String getLabel() {
                return query.getSummary();
            }
        };
    }
}
