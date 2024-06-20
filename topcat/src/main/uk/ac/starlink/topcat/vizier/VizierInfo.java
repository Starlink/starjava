package uk.ac.starlink.topcat.vizier;

import java.awt.Component;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.votable.ParamElement;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.ValuesElement;

/**
 * Can obtain information about VizieR service.
 * Most of the methods in this class may take time to interrogate the
 * VizieR server, and therefore ought not to be called on the event
 * dispatch thread.
 *
 * @author   Mark Taylor
 * @since    3 Nov 2009
 */
public class VizierInfo {
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.vizier" );

    private final Component parent_;
    private final URL vizierBaseUrl_;
    private final ContentCoding coding_;
    private InfoItem[] surveyItems_;
    private InfoItem[] archiveItems_;
    private String[] lambdaKws_;
    private String[] missionKws_;
    private String[] astroKws_;
    private boolean loaded_;

    /**
     * Constructor.
     *
     * @param   parent  parent component, used for placing error messages
     * @param   vizierBaseUrl   base URL for VizieR service
     * @param   coding   controls HTTP-level byte stream encoding
     */
    VizierInfo( Component parent, URL vizierBaseUrl, ContentCoding coding ) {
        parent_ = parent;
        vizierBaseUrl_ = vizierBaseUrl;
        coding_ = coding;
        surveyItems_ = new InfoItem[ 0 ];
        archiveItems_ = new InfoItem[ 0 ];
        lambdaKws_ = new String[ 0 ];
        missionKws_ = new String[ 0 ];
        astroKws_ = new String[ 0 ];
    }

    public URL getBaseUrl() {
        return vizierBaseUrl_;
    }

    /**
     * Returns a list of the known survey resources.
     *
     * @return  survey array; may be empty, but not null, if there was an error
     */
    public InfoItem[] getSurveys() {
        checkLoaded();
        return surveyItems_;
    }

    /**
     * Returns a list of the known archive resources.
     *
     * @return  archive array; may be empty, but not null, if there was an error
     */
    public InfoItem[] getArchives() {
        checkLoaded();
        return archiveItems_;
    }

    /**
     * Returns the list of keyword values permitted for the Wavelength
     * controlled vocabulary.
     *
     * @return  wavelength keywords
     */
    public String[] getWavelengthKws() {
        checkLoaded();
        return lambdaKws_;
    }

    /**
     * Returns the list of keyword values permitted for the Mission
     * controlled vocabulary.
     *
     * @return  mission keywords
     */
    public String[] getMissionKws() {
        checkLoaded();
        return missionKws_;
    }

    /**
     * Returns the list of keyword values permitted for the Astronomy
     * controlled vocabulary.
     *
     * @return  astronomy keywords
     */
    public String[] getAstronomyKws() {
        checkLoaded();
        return astroKws_;
    }
 
    /**
     * Returns the declared parent component used by this object for
     * placing warning messages.
     *
     * @return  parent component
     */
    public Component getParent() {
        return parent_;
    }

    /**
     * Checks that the the resource information has been read from the
     * VizieR service.
     * If a read error occurs, a dialogue will be posted.
     */
    private synchronized void checkLoaded() {
        if ( ! loaded_ ) {
            Throwable error = null;
            try {
                attemptReadTables();
            }
            catch ( IOException e ) {
                error = e;
            }
            catch ( SAXException e ) {
                error = e;
            }
            loaded_ = true;
            if ( error != null ) {
                final Throwable error1 = error;
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        ErrorDialog
                       .showError( parent_, "VizieR Error", error1,
                                   "Couldn't read metadata from VizieR" );
                    }
                } );
            }
        }
        assert loaded_;
    }

    /**
     * Attempts to read the resource information from VizieR service.
     */
    private void attemptReadTables() throws IOException, SAXException {
        URL url = new URL( vizierBaseUrl_ + "?-meta.aladin=all" );
        logger_.info( url.toString() );
        VOElement top = new VOElementFactory( StoragePolicy.PREFER_MEMORY )
                       .makeVOElement( new URLDataSource( url, coding_ ) );

        /* Get Survey and Archive lists. */
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

        /* Get controlled vocabulary keyword lists. */
        NodeList paramList = top.getElementsByVOTagName( "PARAM" );
        for ( int i = 0; i < paramList.getLength(); i++ ) {
            VOElement el = (VOElement) paramList.item( i );
            if ( el instanceof ParamElement ) {
                ParamElement pEl = (ParamElement) el;
                String id = pEl.getID();
                if ( "Wavelength".equals( id ) ) {
                    lambdaKws_ = readOptions( pEl );
                }
                else if ( "Mission".equals( id ) ) {
                    missionKws_ = readOptions( pEl );
                }
                else if ( "Astronomy".equals( id ) ) {
                    astroKws_ = readOptions( pEl );
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
            Class<?> clazz = info.getContentClass();
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
        List<InfoItem> itemList = new ArrayList<InfoItem>();
        RowSequence rseq = table.getRowSequence();
        while ( rseq.next() ) {
            Object[] row = rseq.getRow();
            String name = icName >= 0 ? (String) row[ icName ] : null;
            String title = icTitle >= 0 ? (String) row[ icTitle ] : null;
            if ( name != null ) {
                name = name.trim();
            }
            if ( title != null ) {
                title = title.trim();
            }
            Integer krows =
                  icKrows >= 0 && row[ icKrows ] != null 
                ? Integer.valueOf( ((Number) row[ icKrows ]).intValue() )
                : null;
            itemList.add( new InfoItem( name, title, krows ) );
        }
        return itemList.toArray( new InfoItem[ 0 ] );
    }

    /**
     * Reads a list of legal value options from a parameter element.
     *
     * @param  pEl  parameter element
     * @return   option list
     */
    private static String[] readOptions( ParamElement pEl ) {
        ValuesElement valsEl = pEl.getLegalValues();
        return valsEl == null ? null
                              : valsEl.getOptions();
    }
}
