package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.ListModel;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.vo.AbstractAdqlExample;
import uk.ac.starlink.vo.AdqlExample;
import uk.ac.starlink.vo.AdqlSyntax;
import uk.ac.starlink.vo.AdqlValidator;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.TapQueryPanel;
import uk.ac.starlink.vo.TapTableLoadDialog;
import uk.ac.starlink.vo.UwsJob;

/**
 * TapTableLoadDialog subclass customised for use with TOPCAT.
 *
 * @author   Mark Taylor
 * @since    18 Jan 2011
 */
public class TopcatTapTableLoadDialog extends TapTableLoadDialog {
    private final RegistryDialogAdjuster adjuster_;
    private final AdqlExample[] examples_;
    private volatile DeletionPolicy deletionPolicy_;

    public TopcatTapTableLoadDialog() {
        adjuster_ = new RegistryDialogAdjuster( this, "tap", false );

        /* Prepare ADQL examples: basic and upload-based. */
        List<AdqlExample> exampleList = new ArrayList<AdqlExample>();
        exampleList.addAll( Arrays.asList( AbstractAdqlExample
                                          .createSomeExamples() ) );
        JList tcList = ControlWindow.getInstance().getTablesList();
        exampleList.addAll( Arrays.asList( UploadAdqlExample
                                          .createSomeExamples( tcList ) ) );
        examples_ = exampleList.toArray( new AdqlExample[ 0 ] );
    }

    public Component createQueryComponent() {
        Component comp = super.createQueryComponent();
        adjuster_.adjustComponent();

        /* Add menu for configurable job deletion. */
        List<JMenu> menuList =
            new ArrayList<JMenu>( Arrays.asList( super.getMenus() ) );
        JMenu delMenu = new JMenu( "Deletion" );
        delMenu.setMnemonic( KeyEvent.VK_D );
        ButtonGroup delButtGroup = new ButtonGroup();
        DeletionPolicy[] delPolicies = DeletionPolicy.values();
        for ( int i = 0; i < delPolicies.length; i++ ) {
            final DeletionPolicy policy = delPolicies[ i ];
            Action act = new AbstractAction( policy.name_ ) {
                public void actionPerformed( ActionEvent evt ) {
                    deletionPolicy_ = policy;
                }
            };
            act.putValue( Action.SHORT_DESCRIPTION, policy.description_ );
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem( act );
            delButtGroup.add( menuItem );
            delMenu.add( menuItem );
            if ( policy == DeletionPolicy.DEFAULT ) { // default
                menuItem.doClick();
            }
        }
        menuList.add( 0, delMenu );
        setMenus( menuList.toArray( new JMenu[ 0 ] ) );

        return comp;
    }

    public boolean acceptResourceIdList( String[] ivoids, String msg ) {
        return adjuster_.acceptResourceIdLists()
            && super.acceptResourceIdList( ivoids, msg );
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
                    return tcModel.getApparentStarTable();
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
            StarTable table = TapQuery.waitForResult( tapJob, storage, 4000 );
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
        return new TapQueryPanel( examples_ ) {
            @Override
            protected AdqlValidator.ValidatorTable[] getExtraTables() {

                /* Return a list of tables in the TAP_UPLOAD schema which may
                 * be used in ADQL as well as those declared by the service. */
                TopcatModel[] tcModels = getTopcatModels();
                List<AdqlValidator.ValidatorTable> vtList =
                    new ArrayList<AdqlValidator.ValidatorTable>();
                for ( int it = 0; it < tcModels.length; it++ ) {
                    TopcatModel tcModel = tcModels[ it ];
                    String[] aliases = getUploadAliases( tcModel );
                    for ( int ia = 0; ia < aliases.length; ia++ ) {
                        String tname = "TAP_UPLOAD." + aliases[ ia ];
                        vtList.add( toValidatorTable( tcModel, tname ) );
                    }
                }
                return vtList.toArray( new AdqlValidator.ValidatorTable[ 0 ] );
            }
        };
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
        ListModel tcList = ControlWindow.getInstance().getTablesListModel();
        TopcatModel[] tcModels = new TopcatModel[ tcList.getSize() ];
        for ( int it = 0; it < tcModels.length; it++ ) {
            tcModels[ it ] = (TopcatModel) tcList.getElementAt( it );
        }
        return tcModels;
    }

    /**
     * Adapts a TopcatModel for use as a table metadata object indicating to
     * the ADQL validator a permitted (upload) table.
     *
     * @param  tcModel  loaded table
     * @param  tname  schema-qualified table name
     * @return   table metadata object suitable for passing to validator
     */
    private static AdqlValidator.ValidatorTable
            toValidatorTable( TopcatModel tcModel, final String tname ) {
        StarTable dataTable = tcModel.getDataModel();
        int ncol = dataTable.getColumnCount();
        final AdqlValidator.ValidatorColumn[] vcols =
            new AdqlValidator.ValidatorColumn[ ncol ];
        final AdqlValidator.ValidatorTable vtable =
                new AdqlValidator.ValidatorTable() {
            public String getName() {
                return tname;
            }
            public AdqlValidator.ValidatorColumn[] getColumns() {
                return vcols;
            }
        };
        for ( int ic = 0; ic < ncol; ic++ ) {
            final String cname = dataTable.getColumnInfo( ic ).getName();
            vcols[ ic ] = new AdqlValidator.ValidatorColumn() {
                public String getName() {
                    return cname;
                }
                public AdqlValidator.ValidatorTable getTable() {
                    return vtable;
                }
            };
        }
        return vtable;
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
