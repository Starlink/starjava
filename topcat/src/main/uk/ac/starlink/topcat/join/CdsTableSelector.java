package uk.ac.starlink.topcat.join;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.topcat.Downloader;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.cone.CdsUploadMatcher;
import uk.ac.starlink.ttools.cone.CdsUploadMatcher.VizierMeta;

/**
 * Component that allows the user to select table names for use with
 * the CDS X-Match service.
 *
 * <p>Currently rather basic.
 *
 * @author   Mark Taylor
 * @since    15 May 2014
 */
public class CdsTableSelector extends JPanel {

    private final JLabel selectorLabel_;
    private final JComboBox nameSelector_;
    private final VizierMetaDownloader metaDownloader_;
    private final JTextField nameField_;
    private final JTextField aliasField_;
    private final JTextField descField_;
    private final JTextField nrowField_;
    private final ExecutorService metaExecutor_;
    private String tableName_;
    private Future<?> metaFuture_;

    private static final Downloader<String[]> aliasDownloader_ =
        createAliasDownloader();

    /**
     * Constructor.
     */
    public CdsTableSelector() {
        setLayout( new BorderLayout() );

        /* Add selector component. */
        nameSelector_ = new JComboBox();
        nameSelector_.setEditable( true );
        nameSelector_.setSelectedItem( null );

        /* Populate with vizier aliases. */
        nameSelector_.addItem( CdsUploadMatcher.SIMBAD_NAME );
        if ( aliasDownloader_.isComplete() ) {
            addAliases( aliasDownloader_.getData() );
        }
        else {
            Thread aliasLoader = new Thread( "Vizier Aliases" ) {
                public void run() {
                    final String[] aliases = aliasDownloader_.waitForData();
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            addAliases( aliases );
                        }
                    } );
                }
            };
            aliasLoader.setDaemon( true );
            aliasLoader.start();
        }
        nameSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                updateTableName();
            }
        } );
        selectorLabel_ = new JLabel( "VizieR Table ID/Alias: " );

        /* Selector line. */
        JComponent selectorLine = Box.createHorizontalBox();
        selectorLine.add( selectorLabel_ );
        selectorLine.add( nameSelector_ );
        selectorLine.add( Box.createHorizontalStrut( 5 ) );
        selectorLine.add( aliasDownloader_.createMonitorComponent() );

        /* Table metadata. */
        LabelledComponentStack stack = new LabelledComponentStack();
        nameField_ = createMetaField();
        stack.addLine( "ID", nameField_ );
        aliasField_ = createMetaField();
        stack.addLine( "Alias", aliasField_ );
        descField_ = createMetaField();
        stack.addLine( "Description", descField_ );
        nrowField_ = createMetaField();
        stack.addLine( "Row Count", nrowField_ );
        JComponent infoLine = Box.createHorizontalBox();
        metaDownloader_ = new VizierMetaDownloader();
        metaExecutor_ = Executors.newSingleThreadExecutor();
        infoLine.add( stack );
        infoLine.add( Box.createHorizontalStrut( 5 ) );
        JComponent metaMonBox = Box.createVerticalBox();
        metaMonBox.add( metaDownloader_.createMonitorComponent() );
        metaMonBox.add( Box.createVerticalGlue() );
        infoLine.add( metaMonBox );

        /* Initialise and place components. */
        updateTableName();
        JComponent main = Box.createVerticalBox();
        add( main, BorderLayout.CENTER );
        main.add( selectorLine );
        main.add( infoLine );
        main.add( Box.createVerticalStrut( 5 ) );
    }

    /**
     * Returns the human-readable name of a selected table.
     *
     * @return  currently selected table name
     */
    public String getTableName() {
        return tableName_;
    }

    @Override
    public void setEnabled( boolean isEnabled ) {
        super.setEnabled( isEnabled );
        nameSelector_.setEnabled( isEnabled );
        selectorLabel_.setEnabled( isEnabled );
    }

    /**
     * Invoked when the selected table name may have changed.
     * Updates the display accordingly.
     * Must be invoked from the Event Dispatch Thread.
     */
    private void updateTableName() {
        String tableName = (String) nameSelector_.getSelectedItem();
        tableName_ = tableName;
        metaDownloader_.setTableName( tableName );
        if ( tableName != null ) {
            if ( metaDownloader_.isComplete() ) {
                setMetadata( metaDownloader_.getData() );
            }
            else {
                if ( metaFuture_ != null ) {
                    metaFuture_.cancel( true );
                }
                metaFuture_ = metaExecutor_.submit( new Runnable() {
                    public void run() {
                        final VizierMeta meta = metaDownloader_.waitForData();
                        SwingUtilities.invokeLater( new Runnable() {
                            public void run() {
                                setMetadata( meta );
                            }
                        } );
                    }
                } );
            }
        }
    }

    /**
     * Updates the display to show a given table metadata object.
     *
     * @param  meta  metadata to display
     */
    private void setMetadata( VizierMeta meta ) {
        String name = null;
        String alias = null;
        String description = null;
        Long nrow = null;
        if ( meta != null ) {
            name = meta.getName();
            alias = meta.getPrettyName();
            description = meta.getDescription();
            nrow = meta.getRowCount();
            if ( alias != null && alias.equals( name ) ) {
                alias = null;
            }
        }
        setMetaField( nameField_, name );
        setMetaField( aliasField_, alias );
        setMetaField( descField_, description );
        setMetaField( nrowField_,
                      nrow == null
                          ? null
                          : TopcatUtils.formatLong( nrow.longValue() ) );
    }

    /**
     * Adds a list of aliases to the existing list.
     *
     * @param  aliases  list of vizier table aliases
     */
    private void addAliases( String[] aliases ) {
        if ( aliases != null ) {
            for ( int i = 0; i < aliases.length; i++ ) {
                nameSelector_.addItem( aliases[ i ] );
            }
        }
    }

    /**
     * Returns a new component that can display a line of text.
     *
     * @return  text-bearing component
     */
    private static JTextField createMetaField() {
        JTextField textField = new JTextField();
        textField.setEditable( false );
        textField.setBorder( BorderFactory.createEmptyBorder() );
        return textField;
    }

    /**
     * Sets the text of a one-line text component.
     *
     * @param  field   field created by createMetaField
     * @param    text  text value (null to clear)
     */
    private void setMetaField( JTextField field, String text ) {
        field.setText( text );
        field.setCaretPosition( 0 );
    }

    /**
     * Returns a downloader for Xmatch service alias names.
     *
     * @return   alias downloader
     */
    private static Downloader<String[]> createAliasDownloader() {
        return new Downloader<String[]>( String[].class, "VizieR aliases" ) {
            public String[] attemptReadData() throws IOException {
                return CdsUploadMatcher.readAliases();
            }
        };
    }

    /**
     * Downloads metadata about a vizier table.
     * A single instance is used by CdsTableSelector, but the current table
     * can be reset.
     */
    private static class VizierMetaDownloader extends Downloader<VizierMeta> {

        private String tableName_;

        /**
         * Constructor.
         */
        VizierMetaDownloader() {
            super( VizierMeta.class, "VizieR table metadata" );
        }

        public VizierMeta attemptReadData() throws IOException {
            return CdsUploadMatcher.readVizierMetadata( tableName_ );
        }

        /**
         * Sets the table name or ID for which metadata is required.
         * Resetting this value causes the downloader to be cleared.
         *
         * @param  vizier table name or ID
         */
        public void setTableName( String tableName ) {
            if ( tableName == null || ! tableName.equals( tableName_ ) ) {
                clearData();
            }
            tableName_ = tableName;
        }
    }
}
