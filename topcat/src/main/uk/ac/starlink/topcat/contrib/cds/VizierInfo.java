package uk.ac.starlink.topcat.contrib.cds;

import java.awt.Component;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOStarTable;

/**
 * Can obtain information about VizieR service.
 *
 * @author   Mark Taylor
 * @since    3 Nov 2009
 */
public class VizierInfo {
    public static final String VIZIER_BASE_URL =
        "http://vizier.u-strasbg.fr/viz-bin/votable";
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.contrib.cds" );

    private final Component parent_;
    private InfoItem[] surveyItems_;
    private InfoItem[] archiveItems_;
    private boolean loaded_;

    /**
     * Constructor.
     *
     * @param   parent  parent component, used for placing error messages
     */
    VizierInfo( Component parent ) {
        parent_ = parent;
    }

    /**
     * Returns a list of the known survey resources.
     * May take time; do not call on the event dispatch thread.
     *
     * @return  survey array; may be empty, but not null, if there was an error
     */
    public synchronized InfoItem[] getSurveys() {
        if ( ! loaded_ ) {
            readTables();
        }
        assert loaded_;
        return surveyItems_;
    }

    /**
     * Returns a list of the known archive resources.
     * May take time; do not call on the event dispatch thread.
     *
     * @return  archive array; may be empty, but not null, if there was an error
     */
    public synchronized InfoItem[] getArchives() {
        if ( ! loaded_ ) {
            readTables();
        }
        assert loaded_;
        return archiveItems_;
    }

    /**
     * Reads the resource information from VizieR service.
     * If there is an error, a dialogue will be posted.
     * May take time; do not call on the event dispatch thread.
     */
    private void readTables() {
        try {
            attemptReadTables();
        }
        catch ( IOException e ) {
            ErrorDialog.showError( parent_, "VizieR Error", e,
                                   "Couldn't read metadata from VizieR" );
        }
        catch ( SAXException e ) {
            ErrorDialog.showError( parent_, "VizieR Error", e,
                                   "Couldn't read metadata from VizieR" );
        }
        loaded_ = true;
    }

    /**
     * Attempts to read the resource information from VizieR service.
     * May take time; do not call on the event dispatch thread.
     */
    private void attemptReadTables() throws IOException, SAXException {
        URL url = new URL( VIZIER_BASE_URL + "?-meta.aladin=all" );
        logger_.info( url.toString() );
        VOElementFactory vfact =
            new VOElementFactory( StoragePolicy.PREFER_MEMORY );
        VOElement top = vfact.makeVOElement( new URLDataSource( url ) );
        NodeList tableList = top.getElementsByVOTagName( "TABLE" );
        for ( int i = 0; i < tableList.getLength(); i++ ) {
            VOElement el = (VOElement) tableList.item( i );
            if ( el instanceof TableElement ) {
                TableElement tEl = (TableElement) el;
                String id = tEl.getID();
                if ( "AladinSurveys".equals( id ) ) {
                    surveyItems_ = getItems( new VOStarTable( tEl ) );
                }
                else if ( "AladinArchives".equals( id ) ) {
                    archiveItems_ = getItems( new VOStarTable( tEl ) );
                }
            }
        }
    }

    /**
     * Turns a StarTable obtained by querying VizieR into an array of
     * InfoItem objects.
     *
     * @param  table  table from VizieR
     * @return  array of row items
     */
    private static InfoItem[] getItems( StarTable table ) throws IOException {
        int icName = -1;
        int icTitle = -1;
        int icKrows = -1;
        for ( int icol = 0; icol < table.getColumnCount(); icol++ ) {
            ColumnInfo info = table.getColumnInfo( icol );
            String name = info.getName();
            Class clazz = info.getContentClass();
            if ( "name".equalsIgnoreCase( name ) &&
                 String.class.equals( clazz ) ) {
                icName = icol;
            }
            else if ( "title".equalsIgnoreCase( name ) &&
                      String.class.equals( clazz ) ) {
                icTitle = icol;
            }
            else if ( "krows".equalsIgnoreCase( name ) &&
                      Number.class.isAssignableFrom( clazz ) ) {
                icKrows = icol;
            }
        }
        List itemList = new ArrayList();
        RowSequence rseq = table.getRowSequence();
        while ( rseq.next() ) {
            Object[] row = rseq.getRow();
            String name = icName >= 0 ? (String) row[ icName ] : null;
            String title = icTitle >= 0 ? (String) row[ icTitle ] : null;
            Integer krows = icKrows >= 0 && row[ icKrows ] != null 
                          ? new Integer( ((Number) row[ icKrows ]).intValue() )
                          : null;
            itemList.add( new InfoItem( name, title, krows ) );
        }
        return (InfoItem[]) itemList.toArray( new InfoItem[ 0 ] );
    }
}
