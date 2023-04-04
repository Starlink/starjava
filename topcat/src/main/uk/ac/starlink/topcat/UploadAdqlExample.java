package uk.ac.starlink.topcat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.vo.AbstractAdqlExample;
import uk.ac.starlink.vo.AdqlExample;
import uk.ac.starlink.vo.AdqlSyntax;
import uk.ac.starlink.vo.TableMeta;
import uk.ac.starlink.vo.TapCapability;
import uk.ac.starlink.vo.TapCapabilityPanel;
import uk.ac.starlink.vo.VersionedLanguage;

/**
 * Provides some ADQL examples showing how TOPCAT TAP uploads work.
 *
 * @author   Mark Taylor
 * @since    9 May 2011
 */
public abstract class UploadAdqlExample extends AbstractAdqlExample {

    private final JList<TopcatModel> tcList_;

    private static final Pattern[] RADEC_UCD_REGEXES = new Pattern[] {
        Pattern.compile( "^pos.eq.ra[_;.]?(.*)", Pattern.CASE_INSENSITIVE ),
        Pattern.compile( "^pos.eq.dec[_;.]?(.*)", Pattern.CASE_INSENSITIVE ),
    };
    private static final Pattern[] RADEC_NAME_REGEXES = new Pattern[] {
        Pattern.compile( "RA_?J?(2000)?", Pattern.CASE_INSENSITIVE ),
        Pattern.compile( "DEC?L?_?J?(2000)?", Pattern.CASE_INSENSITIVE )
    };
  
    /**
     * Constructor.
     *
     * @param   name  example name
     * @param   description  example description
     * @param   tcList  JList of known TopcatModels
     */
    public UploadAdqlExample( String name, String description,
                              JList<TopcatModel> tcList ) {
        super( name, description );
        tcList_ = tcList;
    }

    /**
     * Creates and returns a selection of examples for display in the TAP
     * load dialogue which illustrate how to use table uploads from TOPCAT.
     *
     * @param   tcList  JList of known TopcatModels
     */
    public static AdqlExample[]
            createSomeExamples( final JList<TopcatModel> tcList ) {
        return new AdqlExample[] {
            new UploadAdqlExample( "Trivial Upload",
                                   "Upload a table and query all its columns; "
                                 + "not very useful",
                                   tcList ) {
                public String getAdqlText( boolean lineBreaks,
                                           VersionedLanguage lang,
                                           TapCapability tcap,
                                           TableMeta[] tables, TableMeta table,
                                           double[] skypos ) {
                    if ( ! TapCapabilityPanel.canUpload( tcap ) ||
                         tcList.getModel().getSize() < 1 ) {
                        return null;
                    } 
                    return getTrivialText( lineBreaks, lang, tables, table,
                                           tcList );
                }
            },
            new UploadAdqlExample( "Upload Join",
                                   "Upload a local table and join a remote "
                                 + "table with it",
                                   tcList ) {
                public String getAdqlText( boolean lineBreaks,
                                           VersionedLanguage lang,
                                           TapCapability tcap,
                                           TableMeta[] tables, TableMeta table,
                                           double[] skypos ) {
                    if ( ! TapCapabilityPanel.canUpload( tcap ) ) {
                        return null;
                    } 
                    return getJoinText( lineBreaks, tcap, lang, tables, table,
                                        tcList );
                }
            },
        };
    }

    /**
     * Returns text for a trivial upload query.
     *
     * @param  lineBreaks  whether output ADQL should include multiline
     *                     formatting
     * @param  lang  ADQL language variant
     * @param  tables  table metadata set
     * @param  table  currently selected table
     * @param  tcList  JList of known TopcatModels
     */
    private static String getTrivialText( boolean lineBreaks,
                                          VersionedLanguage lang,
                                          TableMeta[] tables, TableMeta table,
                                          JList<TopcatModel> tcList ) {
        TopcatModel tcModel = tcList.getSelectedValue();
        if ( tcModel == null ) {
            return null;
        }
        int tcid = tcModel.getID();
        StringBuffer sbuf = new StringBuffer();
        Breaker breaker = createBreaker( lineBreaks );
        sbuf.append( "SELECT " )
            .append( "TOP " )
            .append( 1000 )
            .append( " *" )
            .append( breaker.space( 0 ) )
            .append( "FROM " )
            .append( "TAP_UPLOAD.t" )
            .append( tcid );
        return sbuf.toString();
    }

    /**
     * Returns text for an upload query involving a join with a remote table.
     *
     * @param  lineBreaks  whether output ADQL should include multiline
     *                     formatting
     * @param  tcap  table capabilities for service
     * @param  lang  ADQL language variant
     * @param  tables  table metadata set
     * @param  table  currently selected table
     * @param  tcList  JList of known TopcatModels
     */
    private static String getJoinText( boolean lineBreaks, TapCapability tcap,
                                       VersionedLanguage lang,
                                       TableMeta[] tables, TableMeta table,
                                       JList<TopcatModel> tcJlist ) {
        AdqlSyntax syntax = AdqlSyntax.getInstance();
        TableWithCols[] rdRemotes =
            getRaDecTables( toTables( table, tables ), 1 );
        if ( rdRemotes.length == 0 ) {
            return null;
        }
        TableWithCols rdRemote = rdRemotes[ 0 ];
        ListModel<TopcatModel> tcListModel = tcJlist.getModel();
        List<TopcatModel> tcList = new ArrayList<>();
        TopcatModel selItem = tcJlist.getSelectedValue();
        if ( selItem != null ) {
            tcList.add( selItem );
        }
        for ( int i = 0; i < tcListModel.getSize(); i++ ) {
            TopcatModel item = tcListModel.getElementAt( i );
            if ( item != null && item != selItem ) {
                tcList.add( item );
            }
        }
        TopcatModel[] tcs = tcList.toArray( new TopcatModel[ 0 ] );
        TopcatModel localRd = null;
        String[] localCoords = null;
        for ( int i = 0; i < tcs.length; i++ ) {
            String[] crds = getRaDecDegreesNames( tcs[ i ], syntax );
            if ( crds != null ) {
                localRd = tcs[ i ];
                localCoords = crds;
                break;
            }
        }
        if ( localRd == null ) {
            return null;
        }
        Breaker breaker = createBreaker( lineBreaks );
        String localAlias = "tc";
        String remoteAlias = "db";
        String radiusTxt = "5./3600.";
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "SELECT " )
            .append( "TOP " )
            .append( 1000 )
            .append( breaker.space( 7 ) )
            .append( "*" )
            .append( breaker.space( 0 ) )
            .append( "FROM " )
            .append( rdRemote.getTable().getName() )
            .append( " AS " )
            .append( remoteAlias )
            .append( breaker.space( 0 ) )
            .append( "JOIN " )
            .append( "TAP_UPLOAD.t" )
            .append( localRd.getID() )
            .append( " AS " )
            .append( localAlias )
            .append( breaker.space( 2 ) );
        String raRemote = remoteAlias + "." + rdRemote.getColumns()[ 0 ];
        String deRemote = remoteAlias + "." + rdRemote.getColumns()[ 1 ];
        String raLocal = localAlias + "." + localCoords[ 0 ];
        String deLocal = localAlias + "." + localCoords[ 1 ];
        if ( isAdql21( lang ) ) {
            sbuf.append( "ON DISTANCE(" )
                .append( raRemote )
                .append( ", " )
                .append( deRemote )
                .append( ", " )
                .append( raLocal )
                .append( ", " )
                .append( deLocal )
                .append( ") < " )
                .append( radiusTxt );
        }
        else {
            sbuf.append( "ON 1=CONTAINS(POINT('ICRS', " )
                .append( raRemote )
                .append( ", " )
                .append( deRemote )
                .append( ")," )
                .append( breaker.space( 16 ) )
                .append( "CIRCLE('ICRS', " )
                .append( raLocal )
                .append( ", " )
                .append( deLocal )
                .append( ", " )
                .append( radiusTxt )
                .append( "))" );
        }
        return sbuf.toString();
    }

    /**
     * Returns the names for suitable RA/Dec columns in degrees from a table.
     * If no such column pair can be found, null is returned.
     * The column names are suitable for insertion into ADQL,
     * that is they must not be further quoted.
     *
     * @param  tcModel   topcat table to be investigated
     * @param  syntax    query language syntax
     * @return   2-element array with column names for RA, Dec respectively,
     *           or null if nothing suitable
     */
    private static String[] getRaDecDegreesNames( TopcatModel tcModel,
                                                  AdqlSyntax syntax ) {
        TableColumnModel colModel = tcModel.getColumnModel();
        String[] coords = new String[ 2 ];
        int[] scores = new int[ 2 ];
        int ncol = colModel.getColumnCount();
        for ( int ic = 0; ic < ncol; ic++ ) {
            ColumnInfo info = ((StarTableColumn) colModel.getColumn( ic ))
                             .getColumnInfo();
            String ucd = info.getUCD();
            String unit = info.getUnitString();
            String name = info.getName();
            if ( unit == null || unit.length() == 0
                              || unit.toLowerCase().startsWith( "deg" ) ) {
                for ( int id = 0; id < 2; id++ ) {
                    int score = 0;
                    if ( ucd != null && ucd.trim().length() > 0 ) { 
                        Matcher matcher =
                            RADEC_UCD_REGEXES[ id ].matcher( ucd );
                        if ( matcher.matches() ) {
                            score = 2;
                            String trailer = matcher.group( 1 );
                            if ( trailer == null ||
                                 trailer.trim().length() == 0 ) {
                                score = 3;
                            }
                            else if ( trailer.toLowerCase().equals( "main" ) ) {
                                score = 5;
                            }
                            else if ( trailer.toLowerCase()
                                             .startsWith( "main" ) ) {
                                score = 4;
                            }
                        }
                    }
                    else if ( name != null && name.trim().length() > 0 ) {
                        Matcher matcher =
                            RADEC_NAME_REGEXES[ id ].matcher( name );
                        if ( matcher.matches() ) {
                            score = 1;
                        }
                    }
                    if ( score > scores[ id ] ) {
                        scores[ id ] = score;
                        coords[ id ] = syntax.quoteIfNecessary( name );
                    }
                }
            }
        }
        return scores[ 0 ] > 0 && scores[ 1 ] > 0 ? coords : null;
    }
}
