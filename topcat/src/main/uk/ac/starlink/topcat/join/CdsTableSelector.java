package uk.ac.starlink.topcat.join;

import cds.moc.SMoc;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.cone.CdsUploadMatcher;
import uk.ac.starlink.ttools.cone.CdsUploadMatcher.VizierMeta;
import uk.ac.starlink.ttools.cone.Coverage;
import uk.ac.starlink.ttools.cone.MocCoverage;
import uk.ac.starlink.ttools.cone.UrlMocCoverage;
import uk.ac.starlink.util.gui.ComboBoxBumper;
import uk.ac.starlink.util.gui.Downloader;

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

    private final JComboBox<String> nameSelector_;
    private final VizierMetaDownloader metaDownloader_;
    private final MocDownloader mocDownloader_;
    private final JTextField nameField_;
    private final JTextField aliasField_;
    private final JTextField descField_;
    private final JTextField nrowField_;
    private final JTextField mocField_;
    private final CoverageView mocView_;
    private final ExecutorService metaExecutor_;
    private final ExecutorService mocExecutor_;
    private String tableName_;
    private Future<?> metaFuture_;
    private Future<?> mocFuture_;

    private static final Downloader<String[]> aliasDownloader_ =
        createAliasDownloader();

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public CdsTableSelector() {
        setLayout( new BorderLayout() );

        /* Add selector component. */
        nameSelector_ = new JComboBox<String>() {

            /* If the catalogue names when they are eventually downloaded
             * are too long for the width of the window, make sure that
             * the long entries, rather than the combo box itself,
             * get truncated. */
            @Override
            public Dimension getMinimumSize() {
                return new Dimension( 140, super.getMinimumSize().height );
            }

            /* No sense making the combo box longer than the longest entry. */
            @Override
            public Dimension getMaximumSize() {
                return super.getPreferredSize();
            }
        };
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

        /* Selector line. */
        JComponent selectorLine = Box.createHorizontalBox();
        selectorLine.add( new JLabel( "VizieR Table ID/Alias: " ) );
        selectorLine.add( nameSelector_ );
        selectorLine.add( Box.createHorizontalStrut( 5 ) );
        selectorLine.add( new ComboBoxBumper( nameSelector_ ) );
        selectorLine.add( Box.createHorizontalStrut( 5 ) );
        selectorLine.add( Box.createHorizontalGlue() );
        selectorLine.add( aliasDownloader_.createMonitorComponent() );

        /* Table metadata. */
        LabelledComponentStack stack = new LabelledComponentStack();
        nameField_ = createMetaField();
        aliasField_ = createMetaField();
        descField_ = createMetaField();
        nrowField_ = createMetaField();

        /* Coverage view. */
        mocDownloader_ = new MocDownloader();
        mocExecutor_ = Executors.newSingleThreadExecutor();
        metaDownloader_ = new VizierMetaDownloader();
        metaExecutor_ = Executors.newSingleThreadExecutor();

        mocField_ = createMetaField();
        mocView_ = new CoverageView();
        mocView_.setForeground( new Color( 0x0000ff ) );
        mocView_.setBackground( new Color( 0xc0c0ff ) );

        JComponent covBox = Box.createHorizontalBox();
        covBox.add( mocField_ );
        covBox.add( Box.createHorizontalStrut( 10 ) );
        covBox.add( mocView_ );
        covBox.add( Box.createHorizontalStrut( 10 ) );
        covBox.add( mocDownloader_.createMonitorComponent() );
        JComponent nameBox = Box.createHorizontalBox();
        nameBox.add( nameField_ );
        nameBox.add( Box.createHorizontalStrut( 10 ) );
        nameBox.add( metaDownloader_.createMonitorComponent() );

        stack.addLine( "Name", null, nameBox, true );
        stack.addLine( "Alias", aliasField_ );
        stack.addLine( "Description", descField_ );
        stack.addLine( "Row Count", nrowField_ );
        stack.addLine( "Coverage", null, covBox, true );

        /* Initialise and place components. */
        updateTableName();
        JComponent main = Box.createVerticalBox();
        add( main, BorderLayout.CENTER );
        main.add( selectorLine );
        main.add( Box.createVerticalStrut( 5 ) );
        main.add( stack );
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

    /**
     * Attempts to return the non-alias name of the selected table.
     * If not available, the selected (possibly alias) table name will be
     * returned instead.
     *
     * @return  vizier name of selected table
     */
    public String getCanonicalTableName() {
        String canonicalName = nameField_.getText();
        return canonicalName == null || canonicalName.trim().length() == 0
             ? tableName_ 
             : canonicalName;
    }

    /**
     * Returns the coverage object for the currently selected table,
     * if available.
     *
     * @return   coverage for current table
     */
    public MocCoverage getCoverage() {
        return mocDownloader_.getData();
    }

    /**
     * Returns the component which the user interacts with to select
     * the CDS table.
     *
     * @return  name selector
     */
    public JComboBox<String> getNameSelector() {
        return nameSelector_;
    }

    @Override
    public void setEnabled( boolean isEnabled ) {
        super.setEnabled( isEnabled );
        nameSelector_.setEnabled( isEnabled );
    }

    /**
     * Returns the object that manages metadata downloads for the
     * currently-selected table.
     *
     * @return   metadata downloader; this will not change over the
     *           lifetime of this object
     */
    public Downloader<VizierMeta> getMetadataDownloader() {
        return metaDownloader_;
    }

    /**
     * Invoked when the selected table name may have changed.
     * Updates the display accordingly.
     * Must be invoked from the Event Dispatch Thread.
     */
    private void updateTableName() {
        String tableName = (String) nameSelector_.getSelectedItem();
        tableName_ = tableName;
        setMetadata( null );
        setMoc( null );

        /* Reset the downloaded coverage information.  Action will be taken
         * by the setMetadata method to populate this with non-empty
         * coverage information when metadata is available. */
        mocDownloader_.setTableName( null );
        if ( mocFuture_ != null ) {
            mocFuture_.cancel( true );
        }

        /* If the new table is non-null, update the displayed metadata
         * as downloaded from the remote metadata service.
         * Do the update synchronously if the data is known to be available
         * immediately, otherwise asynchronously. */
        metaDownloader_.setTableName( tableName );
        if ( tableName != null ) {

            /* Update table metadata. */
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

            /* Display the alias only if it differs from the name. */
            if ( alias != null && alias.equals( name ) ) {
                alias = null;
            }

            /* Having got the metadata we can now get the MOC,
             * asynchronously if required.
             * It would be more straightforward and reliable to get the MOC
             * concurrently as soon as the alias-or-id entered by the user
             * is known, but Thomas Boch (CDS) advises/requests that by
             * preference the table name rather than alias should be used
             * for MOC retrieval (though by alias usually seems to work),
             * and we may not have the name until we have the metadata.
             * But if the name is not available for some reason, fall back
             * to what we have, which is whatever the user entered. */
            String mocName = name == null ? getTableName() : name;
            String dlName = mocDownloader_.tableName_;
            if ( mocName == null ? dlName != null
                                 : ! mocName.equals( dlName ) ) {
                mocDownloader_.setTableName( mocName );
                if ( mocDownloader_.isComplete() ) {
                    setMoc( mocDownloader_.getData() );
                }
                else {
                    if ( mocFuture_ != null ) {
                        mocFuture_.cancel( true );
                    }
                    mocFuture_ = mocExecutor_.submit( new Runnable() {
                        public final void run() {
                            final MocCoverage moc =
                                mocDownloader_.waitForData();
                            SwingUtilities.invokeLater( new Runnable() {
                                public void run() {
                                    setMoc( moc );
                                }
                            } );
                        }
                    } );
                }
            }
        }

        /* Populate the metadata fields. */
        setMetaField( nameField_, name );
        setMetaField( aliasField_, alias );
        setMetaField( descField_, description );
        setMetaField( nrowField_,
                      nrow == null
                          ? null
                          : TopcatUtils.formatLong( nrow.longValue() ) );
    }

    /**
     * Updates the display to show a given coverage object.
     *
     * @param  coverage  coverage item to display
     */
    private void setMoc( MocCoverage coverage ) {
        String txt = null;
        if ( coverage != null ) {
            SMoc smoc = coverage.getMoc();
            if ( smoc != null ) {
                txt = new StringBuffer()
                     .append( Float.toString( (float) smoc.getCoverage() ) )
                     .append( " (order " )
                     .append( smoc.getMocOrder() )
                     .append( ")" )
                     .toString();
            }
        }
        mocField_.setText( txt );
        mocView_.setCoverage( coverage );
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

    /**
     * Downloader for acquiring <b>initialised</b> MOC coverage objects.
     */
    private static class MocDownloader extends Downloader<MocCoverage> {

        private String tableName_;
        private MocCoverage moc_;

        /**
         * Constructor.
         */
        MocDownloader() {
            super( MocCoverage.class, "VizieR MOC" );
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
            moc_ = tableName == null
                 ? null
                 : UrlMocCoverage.getVizierMoc( tableName, -1 );
            tableName_ = tableName;
        }

        /**
         * Returns the table name or ID for which this dowloader is currently
         * configured.
         *
         * @return  table name or ID
         */
        public String getTableName() {
            return tableName_;
        }

        @Override
        public MocCoverage getData() {
            Coverage.Amount amount = moc_ == null ? null : moc_.getAmount();
            if ( amount != null && amount != Coverage.Amount.NO_DATA ) {
                return moc_;
            }
            else {
                return null; 
            }
        }

        @Override
        public boolean isComplete() {
            return super.isComplete()
                || ( moc_ != null && moc_.getAmount() != null );
        }

        public MocCoverage attemptReadData() throws IOException {
            if ( moc_ != null ) {
                moc_.initCoverage();
            }
            if ( moc_ == null || moc_.getAmount() == Coverage.Amount.NO_DATA ) {
                throw new IOException( "No MOC available" );
            }
            else {
                return moc_;
            }
        }
    }
}
