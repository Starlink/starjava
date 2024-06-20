package uk.ac.starlink.topcat;

import cds.tools.ExtApp;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.ListModel;
import javax.swing.table.TableColumn;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.votable.VOTableBuilder;

/**
 * ExtApp implementation for TOPCAT.  This is the interface via which
 * Aladin talks to external applications.
 * Aladin's model (i.e. the VOPlot model) of what an external application
 * is expected to look like is not quite the same as TOPCAT's, so 
 * the methods don't all do exactly what the interface says they should,
 * but it should be enough to implement useful communication.
 *
 * @author   Mark Taylor
 * @since    14 Oct 2005
 */
public class TopcatExtApp implements ExtApp {

    private final ControlWindow cwin_;
    private final Map<TopcatModel,String> importTables_;
    private int subsetCount_;

    /** Name defined by ExtApp documentation to mark ID column. */
    private static final String OID_COLNAME = "_OID";

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /**
     * Constructor.
     *
     * @param   cwin  control window to which this ExtApp relates
     */
    TopcatExtApp( ControlWindow cwin ) {
        cwin_ = cwin;
        importTables_ = new HashMap<TopcatModel,String>();
    }

    public void loadVOTable( ExtApp app, InputStream in ) {

        /* Try to work out the name of the app which is donating this table,
         * for documentation purposes. */
        String appName;
        if ( app != null ) {
            String appClass = app.getClass().getName();
            appName = app.toString();
            if ( appName.startsWith( appClass + "@" ) ) {
                appName = appClass.replaceFirst( ".*\\.", "" );
            }
        }
        else {
            appName = "ExtApp";
        }

        /* Create a StarTable object from the input stream. */
        StarTable table;
        try { 
            table = cwin_.getTableFactory()
                         .makeStarTable( in, new VOTableBuilder() );
        }
        catch ( IOException e ) {
            ErrorDialog.showError( cwin_,
                                   "Error accepting table from " + appName, e );
            return;
        }

        /* Add this table to the ControlWindow. */
        TopcatModel tcModel = cwin_.addTable( table, appName, true );
        importTables_.put( tcModel, appName );

        /* Mark any _OID columns as hidden in TOPCAT. */
        int[] oidCols = getOidColumns( tcModel );
        ColumnList colList = tcModel.getColumnList();
        for ( int i = 0; i < oidCols.length; i++ ) {
            colList.setActive( oidCols[ i ], false );
        }
    }

    public void setVisible( boolean flag ) {

        /* A set visible request brings the ControlWindow forward. */
        if ( flag ) {
            logger_.info( "ExtApp setVisible(true) received" );
            cwin_.toFront();
        }

        /* A set invisible request currently hides all the view windows. */
        else {
            logger_.info( "ExtApp setVisible(false) received" );
            ListModel<TopcatModel> tcList = cwin_.getTablesListModel();
            for ( int i = 0; i < tcList.getSize(); i++ ) {
                cwin_.setViewsVisible( tcList.getElementAt( i ), false );
            }
        }
    }

    public String execCommand( String cmd ) {

        /* Does nothing (TOPCAT currently has no command-line interface). */
        logger_.info( "ExtApp execCommand(\"" + cmd + 
                    "\") received (ignored)" );
        return "External commands currently unsupported";
    }

    public void showVOTableObject( String[] oids ) {
        addSubsets( oids, true );
    }

    public void selectVOTableObject( String[] oids ) {
        addSubsets( oids, false );
    }

    /**
     * Takes a list of object IDs and for every table which these IDs relate
     * to, creates and selects a subset corresponding to them.
     *
     * @param  oids  id list
     */
    private void addSubsets( String[] oids, boolean apply ) {
        subsetCount_++;
        logger_.info( "ExtApp showVOTableObject() received, " 
                    + oids.length + " items" );

        /* Make a Set from the array of IDs. */
        Set<String> oidSet = new HashSet<String>();
        for ( int i = 0; i < oids.length; i++ ) {
            oidSet.add( oids[ i ] );
        }

        /* Look at each of the known TopcatModels. */
        ListModel<TopcatModel> tcList = cwin_.getTablesListModel();
        for ( int i = 0; i < tcList.getSize(); i++ ) {
            TopcatModel tcModel = tcList.getElementAt( i );
            RowSubset rset = locateSubset( tcModel, oidSet );
            if ( rset != null && apply ) {
                // tcModel.getSubsetAction().getWindow( cwin_ ).toFront();
                tcModel.applySubset( rset );
            }
        }
    }

    public String toString() {
        return "TOPCAT";
    }

    /**
     * Returns column indices in a TopcatModel's DataModel which identify 
     * any special OID-type columns inserted by an ExtApp client.
     *
     * @param   tcModel  topcat model
     * @return  array of zero or more _OID-type columns
     */
    private static int[] getOidColumns( TopcatModel tcModel ) {
        List<Integer> oidColList = new ArrayList<Integer>();
        ColumnList colList = tcModel.getColumnList();
        for ( int icol = 0; icol < colList.size(); icol++ ) {
            TableColumn col = colList.getColumn( icol );
            if ( OID_COLNAME.equals( col.getHeaderValue() ) ) {
                oidColList.add( Integer.valueOf( icol ) );
            }
        }
        int[] oidCols = new int[ oidColList.size() ];
        for ( int i = 0; i < oidColList.size(); i++ ) {
            oidCols[ i ] = oidColList.get( i ).intValue();
        }
        return oidCols;
    }

    /**
     * Returns a RowSubset of rows within a given TopcatModel corresponding
     * to a given set of object IDs.  If there are no rows, null is returned
     * (not an empty RowSubset).
     *
     * @param  tcModel   topcat model
     * @param  oidSet    set of ID strings
     * @return  row subset identifynig rows in <code>tcModel</code> with
     *          IDs <code>oidSet</code>
     */
    private RowSubset locateSubset( TopcatModel tcModel, Set<String> oidSet ) {

        /* Get a bit vector representing the requested rows. */
        BitSet included = identifyIncluded( tcModel, oidSet );

        /* If it's empty, return null. */
        if ( included == null || included.cardinality() == 0 ) {
            return null;
        }

        /* If this is identical to any existing row subset, return that. */
        for ( RowSubset subset : tcModel.getSubsets() ) {
            if ( subset instanceof BitsRowSubset ) {
                BitSet cmpBits = ((BitsRowSubset) subset).getBitSet();
                if ( cmpBits.equals( included ) ) {
                    return subset;
                }
            }
        }

        /* Otherwise, create, add and return a new one. */
        RowSubset rset = new BitsRowSubset( "imported" + subsetCount_,
                                            included );
        tcModel.getSubsets().add( rset );
        logger_.info( "New subset " + rset.getName() + " in table " + tcModel );
        return rset;
    }

    /**
     * Returns a BitSet which identifies rows included in a given 
     * table according to a set of OID values.
     *
     * @param   tcModel  topcat model representing the table
     * @param   oidSet  set of OID identifiers
     * @return  bit vector with a true element for any row in
     *          <code>tcModel</code>'s dataModel with an OID in
     *          <code>oidSet</code>
     */
    private BitSet identifyIncluded( TopcatModel tcModel, Set<String> oidSet ) {
        int[] oidCols = getOidColumns( tcModel );
        StarTable dataModel = tcModel.getDataModel();
        BitSet included = new BitSet();
        RowSequence rseq = null;
        long irow = 0L;
        try {
            rseq = dataModel.getRowSequence();
            while ( rseq.next() ) {
                for ( int i = 0; i < oidCols.length; i++ ) {
                    int icol = oidCols[ i ];
                    Object str = rseq.getCell( icol );
                    if ( oidSet.contains( str ) ) {
                        included.set( Tables.checkedLongToInt( irow ) );
                    }
                }
                irow++;
            }
        }
        catch ( IOException e ) {
            logger_.warning( "ExtApp locate subset failed " + e );
            return null;
        }
        finally {
            if ( rseq != null ) {
                try {
                    rseq.close();
                }
                catch ( IOException e ) {
                    // never mind
                }
            }
        }
        return included;
    }
}
