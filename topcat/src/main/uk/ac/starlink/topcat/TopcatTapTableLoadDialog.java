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
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.vo.AdqlExample;
import uk.ac.starlink.vo.TapQuery;
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
    private volatile DeletionPolicy deletionPolicy_;

    public TopcatTapTableLoadDialog() {
        adjuster_ = new RegistryDialogAdjuster( this, "tap", false );
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

        /* Get the list of known TopcatModels, since these are the ones
         * that may be referred to as an upload table. */
        ListModel tcListModel =
            ControlWindow.getInstance().getTablesListModel();
        TopcatModel[] tcModels = new TopcatModel[ tcListModel.getSize() ];
        for ( int i = 0; i < tcModels.length; i++ ) {
            tcModels[ i ] = (TopcatModel) tcListModel.getElementAt( i );
        }

        /* Check for a match against the label of a known table. 
         * If found and syntactically legal, return the appropriate table.
         * If found and illegal, reject with a helpful message. */
        for ( int i = 0; i < tcModels.length; i++ ) {
            TopcatModel tcModel = tcModels[ i ];
            String tlabel = tcModel.getLabel();
            if ( upLabel.equalsIgnoreCase( tlabel ) ||
                 upLabel.equalsIgnoreCase( '"' + tlabel + '"' ) ) {
                if ( tlabel.matches( "[A-Za-z][A-Za-z0-9_]*" ) ) {
                    return tcModel.getApparentStarTable();
                }
                else {
                    String msg = "Illegal upload table name \"" + upLabel + "\""
                               + "\nMust be alphanumeric or of form T<n>;"
                               + " try T" + tcModel.getID();
                    throw new IllegalArgumentException( msg );
                }
            }
        }

        /* Check for a match of the form "T<n>", where <n> is table ID of
         * a currently loaded table. */
        for ( int i = 0; i < tcModels.length; i++ ) {
            TopcatModel tcModel = tcModels[ i ];
            if ( upLabel.equalsIgnoreCase( "T" + tcModel.getID() ) ) {
                return tcModel.getApparentStarTable();
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
    protected AdqlExample[] createAdqlExamples() {

        /* The text of the upload examples will be dependent on the
         * current content and selection of the application main table list.
         * So make sure they are reconfigured when that changes. */
        JList tablesList = ControlWindow.getInstance().getTablesList();
        tablesList.addListSelectionListener( new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                configureExamples();
            }
        } );
        tablesList.getModel().addListDataListener( new ListDataListener() {
            public void contentsChanged( ListDataEvent evt ) {
                configureExamples();
            }
            public void intervalAdded( ListDataEvent evt ) {
                configureExamples();
            }
            public void intervalRemoved( ListDataEvent evt ) {
                configureExamples();
            }
        } );

        /* Prepare the new examples and add them to the inherited list. */
        List<AdqlExample> exampleList =
            new ArrayList( Arrays.asList( super.createAdqlExamples( ) ) );
        exampleList.addAll( Arrays.asList( UploadAdqlExample
                                          .createSomeExamples( tablesList ) ) );
        return exampleList.toArray( new AdqlExample[ 0 ] );
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
