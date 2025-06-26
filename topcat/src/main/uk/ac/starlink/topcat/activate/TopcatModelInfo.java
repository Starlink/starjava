package uk.ac.starlink.topcat.activate;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.calc.WebMapper;

/**
 * Summarises information about a TopcatModel that may be useful for
 * determining whether and how to configure activation actions for it.
 *
 * <p>An instance of this class doesn't tell you anthing that you
 * can't find out from the TopcatModel itself, but the process of
 * obtaining the summary information required to construct it may be
 * somewhat time-consuming, so constructing an instance of this class
 * and passing it to all the known ActivationType instances allows
 * that summarisation work to be done only once.
 *
 * @author   Mark Taylor
 * @since    23 Mar 2018
 */
public class TopcatModelInfo {

    private final TopcatModel tcModel_;
    private final int[] colMasks_;
    private final boolean hasSkyCoords_;
    private static final WebMapper WEBREF_MAPPER =
            WebMapper.createMultiMapper( "Webref", new WebMapper[] {
        WebMapper.DOI,
        WebMapper.BIBCODE,
        WebMapper.ARXIV,
    } );

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.activate" );

    /**
     * Constructor.
     *
     * @param  tcModel   topcat model
     * @param  colMasks   flag information about each table column
     * @param  hasSkyCoords  indicates whether the table has known
     *                       sky coordinate columns
     */
    private TopcatModelInfo( TopcatModel tcModel, int[] colMasks,
                             boolean hasSkyCoords ) {
        tcModel_ = tcModel;
        colMasks_ = colMasks;
        hasSkyCoords_ = hasSkyCoords;
    }

    /**
     * Returns the TopcatModel which this object is describing.
     *
     * @return  topcat model
     */
    public TopcatModel getTopcatModel() {
        return tcModel_;
    }

    /**
     * Indicates whether a given column has been marked as having
     * a particular characteristic.
     *
     * @param   icol  column index; refers to the TableColumnModel at
     *                the construction time of this object
     * @param   flag   characteristic type
     * @return  true iff column is marked with flag
     */
    public boolean columnHasFlag( int icol, ColFlag flag ) {
        return flag.isSet( colMasks_[ icol ] );
    }

    /**
     * Indicates whether a particular characteristic has been marked
     * on at least one of the columns in this table.
     *
     * @param   flag   characteristic type
     * @return  true iff any column is marked with flag
     */
    public boolean tableHasFlag( ColFlag flag ) {
        for ( int mask : colMasks_ ) {
            if ( flag.isSet( mask ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indicates whether the table has been determined to contain sky
     * coordinates.
     *
     * @return   true iff table is known to have sky coordinates
     */
    public boolean tableHasSkyCoords() {
        return hasSkyCoords_;
    }

    /**
     * Returns a standard suitability type for activation types
     * that require only or mainly a URL column.
     *
     * @return  suitability
     */
    public Suitability getUrlSuitability() {
        return tableHasFlag( ColFlag.URL ) ? Suitability.PRESENT
                                           : Suitability.AVAILABLE;
    }

    /**
     * Returns a standard suitability type for activation types
     * that require only or mainly sky coordinates.
     *
     * @return  suitability
     */
    public Suitability getSkySuitability() {
        return hasSkyCoords_ ? Suitability.PRESENT
                             : Suitability.PRESENT;
    }

    /**
     * Constructs a TopcatModelInfo instance from a TopcatModel.
     * Indices refer to the column index in the TopcatModel's
     * TableColumnModel (tcModel.getColumnModel()).
     *
     * <p>Note that the 'apparent' table is used for assessment.
     * That means firstly that, since the table rows and columns
     * may change, the information should be used right away and
     * not cached for later use, and secondly that this method should
     * be invoked on the Event Dispatch Thread.
     *
     * @param  tcModel  topcat model
     * @return  TopcatModelInfo representing topcat model table snapshot
     */
    public static TopcatModelInfo createInfo( TopcatModel tcModel ) {
        TableColumnModel colModel = tcModel.getColumnModel();
        StarTable table = tcModel.getViewModel().getSnapshot();
        int nExamine = Math.min( (int) table.getRowCount(), 24 );
        int ncol = colModel.getColumnCount();
        int[] colMasks = new int[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo info = ((StarTableColumn) colModel.getColumn( icol ))
                             .getColumnInfo();
            String ucd = info.getUCD();
            String name = info.getName();
            String utype = info.getUtype();
            Class<?> clazz = info.getContentClass();
            boolean isString = String.class.equals( clazz );
            boolean isDatalink = false;
            boolean isUrl = false;
            boolean isHtml = false;
            boolean isImage = false;
            boolean isVotable = false;
            boolean isSpectrum = false;
            boolean isWebref = false;
            boolean isMime = false;
            if ( isString ) {
                if ( ucd != null &&
                     ucd.toLowerCase().startsWith( "meta.ref.url" ) ) {
                    isUrl = true;
                }
                if ( ucd != null &&
                     ucd.equalsIgnoreCase( "VOX:Image_AccessReference" ) ) {
                    isUrl = true;
                    isImage = true;
                }
                if ( ucd != null &&
                     ucd.toLowerCase().equalsIgnoreCase( "meta.code.mime" ) ) {
                    isMime = true;
                }
                if ( utype != null &&
                     utype.equalsIgnoreCase( "Access.Reference" ) ) {
                    isUrl = true;
                    isSpectrum = true;
                }
                if ( name.toLowerCase().indexOf( "url" ) >= 0 ) {
                    isUrl = true;
                }
                if ( name.toLowerCase().indexOf( "datalink" ) >= 0 ) {
                    isUrl = true;
                    isDatalink = true;
                }
                if ( name.equalsIgnoreCase( "access_format" ) ||
                     name.equalsIgnoreCase( "content_type" ) ||
                     name.equalsIgnoreCase( "mime_type" ) ) {
                    isMime = true;
                }
                String sval1 = null;
                try {
                    for ( long irow = 0; irow < nExamine && sval1 == null;
                          irow++ ) {
                        Object val = table.getCell( irow, icol );
                        if ( val instanceof String ) {
                            String sval = ((String) val);
                            if ( sval.trim().length() > 0 ) {
                                sval1 = sval;
                            }
                        }
                    }
                }
                catch ( IOException e ) {
                    logger_.log( Level.WARNING, "Data read error: " + e, e );
                }
                if ( sval1 != null ) {
                    if ( WEBREF_MAPPER.toUrl( sval1 ) != null ) {
                        isWebref = true;
                    }
                    sval1 = sval1.toLowerCase();
                    if ( sval1.startsWith( "http://" ) ||
                         sval1.startsWith( "https://" ) ||
                         sval1.startsWith( "file://" ) ||
                         sval1.startsWith( "ftp://" ) ) {
                             isUrl = true;
                    }
                    if ( isUrl ) {
                        if ( sval1.endsWith( ".html" ) ||
                             sval1.endsWith( ".htm" ) ) {
                            isHtml = true;
                        }
                        else if ( sval1.endsWith( ".vot" ) ) {
                            isVotable = true;
                        }
                    }
                }
                if ( isUrl && name.toLowerCase().indexOf( "image" ) >= 0 ) {
                    isImage = true;
                }
                colMasks[ icol ] = ColFlag.STRING.toMask( isString )
                                 | ColFlag.DATALINK.toMask( isDatalink )
                                 | ColFlag.URL.toMask( isUrl )
                                 | ColFlag.HTML.toMask( isHtml )
                                 | ColFlag.IMAGE.toMask( isImage )
                                 | ColFlag.VOTABLE.toMask( isVotable )
                                 | ColFlag.SPECTRUM.toMask( isSpectrum )
                                 | ColFlag.WEBREF.toMask( isWebref )
                                 | ColFlag.MIME.toMask( isMime );
            }
        }
        boolean hasSkyCoords = hasColumn( tcModel, Tables.RA_INFO ) 
                            && hasColumn( tcModel, Tables.DEC_INFO );
        return new TopcatModelInfo( tcModel, colMasks, hasSkyCoords );
    }

    /**
     * Tries to determine whether a topcat model has a column matching
     * a given info.
     *
     * @param  info  template metadata
     * @return  true iff column looks like info
     */
    private static boolean hasColumn( TopcatModel tcModel, ValueInfo info ) {
        return tcModel.getColumnSelectorModel( info ).getColumnModel()
              .getSelectedItem() != null;
    }
}
