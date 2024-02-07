package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.vo.AdqlExample;
import uk.ac.starlink.vo.AdqlSyntax;
import uk.ac.starlink.vo.AdqlValidator;
import uk.ac.starlink.vo.AuxServiceFinder;
import uk.ac.starlink.vo.GlotsServiceFinder;
import uk.ac.starlink.vo.RegistryPanel;
import uk.ac.starlink.vo.TapCapability;
import uk.ac.starlink.vo.TapMetaPolicy;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.TapQueryPanel;
import uk.ac.starlink.vo.TapServiceFinder;
import uk.ac.starlink.vo.TapTableLoadDialog;
import uk.ac.starlink.vo.UwsJob;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableVersion;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * TapTableLoadDialog subclass customised for use with TOPCAT.
 *
 * @author   Mark Taylor
 * @since    18 Jan 2011
 */
public class TopcatTapTableLoadDialog extends TapTableLoadDialog {

    private final RegistryDialogAdjuster adjuster_;
    private AdqlExample[] uploadExamples_;
    private Consumer<URL> urlHandler_;
    private double[] skypos_;
    private volatile DeletionPolicy deletionPolicy_;

    /**
     * Constructor.
     */
    public TopcatTapTableLoadDialog() {
        adjuster_ = new RegistryDialogAdjuster( this, "tap", false );
    }

    public Component createQueryComponent() {
        Component comp = super.createQueryComponent();
        adjuster_.adjustComponent();

        /* Add menus for TAP-specific items. */
        List<JMenu> menuList =
            new ArrayList<JMenu>( Arrays.asList( super.getMenus() ) );
        int imenu = 0;
        JMenu tapMenu = new JMenu( "TAP" );
        tapMenu.setMnemonic( KeyEvent.VK_T );
        menuList.add( imenu++, tapMenu );
        JMenu authMenu = new JMenu( "Authentication" );
        authMenu.setMnemonic( KeyEvent.VK_A );
        menuList.add( imenu++, authMenu );
        JMenu regMenu = new JMenu( "Registry" );
        menuList.add( imenu++, regMenu );
        setMenus( menuList.toArray( new JMenu[ 0 ] ) );

        /* Add reload action. */
        tapMenu.add( getReloadAction() );

        /* Add sub-menu for job deletion. */
        JMenu delMenu = new JMenu( "Job Deletion" );
        ButtonGroup delButtGroup = new ButtonGroup();
        for ( DeletionPolicy p : DeletionPolicy.values() ) {
            final DeletionPolicy delPolicy = p;
            Action act = new AbstractAction( delPolicy.name_ ) {
                public void actionPerformed( ActionEvent evt ) {
                    deletionPolicy_ = delPolicy;
                }
            };
            act.putValue( Action.SHORT_DESCRIPTION, delPolicy.description_ );
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem( act );
            delButtGroup.add( menuItem );
            delMenu.add( menuItem );
            if ( delPolicy == DeletionPolicy.DEFAULT ) { // default
                menuItem.doClick();
            }
        }
        tapMenu.add( delMenu );

        /* Add sub-menu for TAP metadata acquisition policy. */
        JMenu metaMenu = new JMenu( "Metadata Acquisition" );
        ButtonGroup metaButtGroup = new ButtonGroup();
        for ( TapMetaPolicy p : TapMetaPolicy.getStandardInstances() ) {
            final TapMetaPolicy metaPolicy = p;
            Action act = new AbstractAction( metaPolicy.getName() ) {
                public void actionPerformed( ActionEvent evt ) {
                    setMetaPolicy( metaPolicy );
                }
            };
            act.putValue( Action.SHORT_DESCRIPTION,
                          metaPolicy.getDescription() );
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem( act );
            metaButtGroup.add( menuItem );
            metaMenu.add( menuItem );
            if ( metaPolicy == TapMetaPolicy.getDefaultInstance() ) {
                menuItem.doClick();
            }
        }
        tapMenu.add( metaMenu );

        /* Add sub-menu for output format preference. */
        JMenu ofmtMenu = new JMenu( "Response Format" );
        ButtonGroup ofmtButtGroup = new ButtonGroup();
        Map<String,String> ofmtMap = new LinkedHashMap<String,String>();
        ofmtMap.put( "Service Default", null );
        String tapregextStd = TapCapability.TAPREGEXT_STD_URI;
        ofmtMap.put( "TABLEDATA", tapregextStd + "#output-votable-td" );
        ofmtMap.put( "BINARY", tapregextStd + "#output-votable-binary" );
        ofmtMap.put( "BINARY2", tapregextStd + "#output-votable-binary2" );
        for ( Map.Entry<String,String> entry : ofmtMap.entrySet() ) {
            String optName = entry.getKey();
            final String ofmtId = entry.getValue();
            Action act = new AbstractAction( optName ) {
                public void actionPerformed( ActionEvent evt ) {
                    setPreferredOutputFormat( ofmtId );
                }
            };
            act.putValue( Action.SHORT_DESCRIPTION,
                          "Request TAP results in " + ofmtId
                        + " format if supported" );
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem( act );
            ofmtButtGroup.add( menuItem );
            ofmtMenu.add( menuItem );
            if ( optName.equals( "BINARY2" ) ) {
                menuItem.doClick();
            }
        }
        tapMenu.add( ofmtMenu );

        /* Add sub-menu for upload format preference. */
        JMenu ufmtMenu = new JMenu( "Upload Format" );
        ButtonGroup ufmtButtGroup = new ButtonGroup();
        boolean hasDflt = false;
        for ( DataFormat datfmt :
              new DataFormat[] { DataFormat.TABLEDATA,
                                 DataFormat.BINARY,
                                 DataFormat.BINARY2 } ) {
            String fname = datfmt.toString();
            VOTableVersion version = datfmt == DataFormat.BINARY2
                                   ? VOTableVersion.V13
                                   : VOTableVersion.V12;
            final VOTableWriter vowriter =
                new VOTableWriter( datfmt, true, version );
            Action act = new AbstractAction( fname ) {
                public void actionPerformed( ActionEvent evt ) {
                    setVOTableWriter( vowriter );
                }
            };
            act.putValue( Action.SHORT_DESCRIPTION,
                          "Upload tables using VOTable " + fname
                        + " serialization" );
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem( act );
            ufmtButtGroup.add( menuItem );
            ufmtMenu.add( menuItem );
            if ( datfmt == TapQuery.DFLT_UPLOAD_SER ) {
                menuItem.doClick();
                hasDflt = true;
            }
        }
        assert hasDflt;
        tapMenu.add( ufmtMenu );

        /* Add sub-menu for by-type service finder implementation. */
        JMenu finderMenu = new JMenu( "Service Discovery" );
        Map<String,TapServiceFinder> finderMap =
            new LinkedHashMap<String,TapServiceFinder>();
        finderMap.put( "GloTS", new GlotsServiceFinder() );
        finderMap.put( "Reg Prototype", new AuxServiceFinder() );
        ButtonGroup finderButtGroup = new ButtonGroup();
        for ( Map.Entry<String,TapServiceFinder> entry :
              finderMap.entrySet() ) {
            String optName = entry.getKey();
            final TapServiceFinder finder = entry.getValue();
            Action act = new AbstractAction( optName ) {
                public void actionPerformed( ActionEvent evt ) {
                    setServiceFinder( finder );
                }
            };
            act.putValue( Action.SHORT_DESCRIPTION,
                          "Locate services using " + optName );
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem( act );
            finderButtGroup.add( menuItem );
            finderMenu.add( menuItem );
            if ( optName.equals( finderMap.keySet().iterator().next() ) ) {
                menuItem.doClick();
            }
        }
        tapMenu.add( finderMenu );

        /* Add menu item for HTTP-level compression. */
        final JCheckBoxMenuItem codingButton =
            new JCheckBoxMenuItem( "HTTP gzip" );
        codingButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                setContentCoding( codingButton.isSelected()
                                      ? ContentCoding.GZIP
                                      : ContentCoding.NONE );
            }
        } );
        codingButton.setToolTipText( "Determines whether HTTP-level compression"
                                   + " is used for results of TAP and metadata"
                                   + " queries" );
        codingButton.setSelected( true );
        tapMenu.add( codingButton );

        /* Add items to authentication menu. */
        authMenu.add( getAuthenticateAction() );
        authMenu.add( ControlWindow.getInstance().getAuthResetAction() );

        /* Add items to registry menu. */
        RegistryPanel regPanel = getRegistryPanel();
        if ( regPanel != null ) {
            regMenu.add( regPanel.getRegistryUpdateAction() );
        }

        /* Prepare a handler for clickable URLs. */
        setUrlHandler( TopcatUtils.getDocUrlHandler() );

        return comp;
    }

    public boolean acceptResourceIdList( String[] ivoids, String msg ) {
        return adjuster_.acceptResourceIdLists()
            && super.acceptResourceIdList( ivoids, msg );
    }

    /**
     * Notifies this object of a preferred sky position to use for examples.
     * If this is done, then new ADQL examples should use the provided
     * sky position rather than some more or less arbitrary position.
     *
     * @param  raDegrees   RA in degrees
     * @param  decDegrees  Declination in degrees
     */
    public boolean acceptSkyPosition( double raDegrees, double decDegrees ) {
        skypos_ = new double[] { raDegrees, decDegrees };
        return true;
    }

    protected StarTable getUploadTable( String upLabel ) {

        /* Get list of loaded tables. */
        TopcatModel[] tcModels = getTopcatModels();

        /* Check the given label against the permitted upload aliases of
         * each one. */
        for ( int it = 0; it < tcModels.length; it++ ) {
            TopcatModel tcModel = tcModels[ it ];
            String[] aliases = getUploadAliases( tcModel );
            for ( int ia = 0; ia < aliases.length; ia++ ) {
                if ( upLabel.equalsIgnoreCase( aliases[ ia ] ) ) {
                    return TopcatUtils.getSaveTable( tcModel );
                }
            }
        }

        /* Otherwise, reject with a helpful error message. */
        StringBuffer sbuf = new StringBuffer()
            .append( "No upload table available under the name " )
            .append( upLabel )
            .append( ".\n" );
        if ( tcModels.length == 0 ) {
            sbuf.append( "No tables are currently loaded" );
        }
        else {
            TopcatModel tcModel = tcModels[ 0 ];
            sbuf.append( "Use either T<n> or <alphanumeric_name>" );
        }
        throw new IllegalArgumentException( sbuf.toString() );
    }

    @Override
    protected TableSequence createTableSequence( StarTableFactory tfact,
                                                 final UwsJob tapJob,
                                                 DescribedValue[] tapMetadata )
            throws IOException {

        /* This isn't quite right - the deletionPolicy is polled at table
         * creation time, but really it should be polled at job dispatch
         * time.  They are likely to be close together though. */
        DeletionPolicy deletionPolicy = deletionPolicy_;

        StoragePolicy storage = tfact.getStoragePolicy();
        if ( deletionPolicy.deleteOnExit_ ) {
            tapJob.setDeleteOnExit( true );
        }
        tapJob.start();
        try {

            /* Note it's essential to make sure that the table has been
             * read (is stored locally) before deleting the job, since
             * otherwise subsequent attempts might be made to read it from
             * its URL, which would no longer be there. */
            StarTable table =
                TapQuery.waitForResult( tapJob, getContentCoding(),
                                        storage, 4000 );
            if ( table != null ) {
                table = storage.randomTable( table );
            }
            table.getParameters().addAll( Arrays.asList( tapMetadata ) );
            return Tables.singleTableSequence( table );
        }
        catch ( InterruptedException e ) {
            throw (IOException) new IOException( "Interrupted" ).initCause( e );
        }
        finally {
            if ( deletionPolicy.deleteOnCompletion_ ) {
                Thread delThread = new Thread( "Delete TAP job" ) {
                    public void run() {
                        tapJob.attemptDelete();
                    }
                };
                delThread.setDaemon( true );
                delThread.start();
            }
        }
    }

    @Override
    protected TapQueryPanel createTapQueryPanel() {
        TapQueryPanel tqp = new TapQueryPanel( this ) {
            @Override
            public double[] getSkyPos() {
                return TopcatTapTableLoadDialog.this.skypos_;
            }
        };
        tqp.addCustomExamples( "Upload", getUploadExamples() );

        /* Make sure the panel is kept up to date with the list of
         * tables known by the application. */
        ControlWindow.getInstance().getTablesListModel()
                     .addListDataListener( new ListDataListener() {
            public void intervalAdded( ListDataEvent evt ) {
                updateTopcatTables();
            }
            public void intervalRemoved( ListDataEvent evt ) {
                updateTopcatTables();
            }
            public void contentsChanged( ListDataEvent evt ) {
                updateTopcatTables();
            }
            private void updateTopcatTables() {
                if ( tqp != null ) {
                    tqp.setExtraTables( createTopcatValidatorTables() );
                }
            }
        } );
        tqp.setExtraTables( createTopcatValidatorTables() );
        return tqp;
    }

    /**
     * Returns a list of tables in the TAP_UPLOAD schema which may
     * be used in ADQL as well as those declared by the service.
     *
     * @return   list of validation tables corresponding to topcat tables
     */
    private AdqlValidator.ValidatorTable[] createTopcatValidatorTables() {
        TopcatModel[] tcModels = getTopcatModels();
        List<AdqlValidator.ValidatorTable> vtList =
            new ArrayList<AdqlValidator.ValidatorTable>();
        String upSchemaName = "TAP_UPLOAD";
        for ( TopcatModel tcModel : getTopcatModels() ) {
            String[] aliases = getUploadAliases( tcModel );
            for ( int ia = 0; ia < aliases.length; ia++ ) {
                String tname = upSchemaName + "." + aliases[ ia ];
                vtList.add( toValidatorTable( tcModel, tname, upSchemaName ) );
            }
        }
        return vtList.toArray( new AdqlValidator.ValidatorTable[ 0 ] );
    }

    /**
     * Returns a list of table names within the TAP_UPLOAD schema
     * which can be used to refer to a given loaded TOPCAT table.
     *
     * @param  tcModel  topcat model referencing a loaded table
     * @return   array of alternative names for referencing the table
     *           in an ADQL upload query; the "TAP_UPLOAD." prefix is omitted
     */
    private String[] getUploadAliases( TopcatModel tcModel ) {
        List<String> aliasList = new ArrayList<String>();

        /* Use "T<n>", where <n> is table ID of a currently loaded table. */
        aliasList.add( "T" + tcModel.getID() );

        /* Use the table label if it is syntactically appropriate. */
        String tcLabel = tcModel.getLabel();
        if ( AdqlSyntax.getInstance().isIdentifier( tcLabel ) ) {
            aliasList.add( tcLabel );
        }
        return aliasList.toArray( new String[ 0 ] );
    }

    /**
     * Utility method providing the list of tables currently loaded into
     * the application.
     *
     * @return   array of currently loaded TopcatModels
     */
    private TopcatModel[] getTopcatModels() {
        ListModel<TopcatModel> tcList =
            ControlWindow.getInstance().getTablesListModel();
        TopcatModel[] tcModels = new TopcatModel[ tcList.getSize() ];
        for ( int it = 0; it < tcModels.length; it++ ) {
            tcModels[ it ] = tcList.getElementAt( it );
        }
        return tcModels;
    }

    /**
     * Returns a lazily constructed list of examples of ADQL queries involving
     * table upload suitable for this window.
     *
     * @return   example list
     */
    private AdqlExample[] getUploadExamples() {
        if ( uploadExamples_ == null ) {
            uploadExamples_ =
                UploadAdqlExample
               .createSomeExamples( ControlWindow.getInstance()
                                                 .getTablesList() );
        }
        return uploadExamples_;
    }

    /**
     * Adapts a TopcatModel for use as a table metadata object indicating to
     * the ADQL validator a permitted (upload) table.
     *
     * @param  tcModel  loaded table
     * @param  tname  schema-qualified table name
     * @param  sname  schema name
     * @return   table metadata object suitable for passing to validator
     */
    private static AdqlValidator.ValidatorTable
            toValidatorTable( TopcatModel tcModel, final String tname,
                              final String sname ) {
        final StarTable dataTable = tcModel.getDataModel();
        final List<String> colList = new AbstractList<String>() {
            public int size() {
                return dataTable.getColumnCount();
            }
            public String get( int index ) {
                return dataTable.getColumnInfo( index ).getName();
            }
        };
        return new AdqlValidator.ValidatorTable() {
            public String getTableName() {
                return tname;
            }
            public String getSchemaName() {
                return sname;
            }
            public Collection<String> getColumnNames() {
                return colList;
            }
        };
    }

    /**
     * Enumeration which codifies under what circumstances a UWS job should
     * be deleted.
     */
    private static enum DeletionPolicy {

        /** Delete when the job has finished. */
        FINISHED( "On Completion",
                  "Delete jobs on completion, either successful or failed",
                  true, false ),

        /** Delete on application exit. */
        EXIT( "On Exit", "Delete jobs on application exit", false, true ),

        /** Delete never. */
        NEVER( "Never", "Do not delete jobs", false, false );

        /** Default policy. */
        public static final DeletionPolicy DEFAULT = EXIT;

        private final String name_;
        private final String description_;
        private final boolean deleteOnCompletion_;
        private final boolean deleteOnExit_;

        /**
         * Constructor.
         *
         * @param  name  name, used for action name
         * @param  description  description, used for tooltip
         * @param  deleteOnCompletion  true to attempt job deletion when it
         *         has completed and its result table (if any) has been read
         * @param  deleteOnExit  true to attempt job deletion at application
         *         exit time
         */
        private DeletionPolicy( String name, String description,
                                boolean deleteOnCompletion,
                                boolean deleteOnExit ) {
            name_ = name;
            description_ = description;
            deleteOnCompletion_ = deleteOnCompletion;
            deleteOnExit_ = deleteOnExit;
        }
    }
}
