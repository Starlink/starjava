package uk.ac.starlink.votable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.ReaderRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.URLValueInfo;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.DOMUtils;

/**
 * A {@link uk.ac.starlink.table.StarTable} implementation based on a VOTable.
 *
 * <p>Some of the FIELD attributes defined by the VOTable format 
 * correspond to standard information in the corresponding ColumnInfo 
 * object, and some do not.  Those that do are accessed using the 
 * relevant ColumnInfo getter/setter methods directly, for instance
 * <pre>
 *     String ucd = table.getColumnInfo(0).getUCD();
 * </pre>
 * The ones that don't are stored in the ColumnInfo's auxiliary metadata
 * keyed using the various <tt>*_INFO</tt> public static variables defined
 * in this class.  These are accessed using the
 * {@link uk.ac.starlink.table.ColumnInfo#getAuxDatum} method, for instance:
 * <pre>
 *     String id = (String) table.getColumnInfo(0)
                                 .getAuxDatumValue(VOStarTable.ID_INFO);
 * </pre>
 * In the same way, if you set an auxiliary metadata item under one of
 * these keys, like this:
 * <pre>
 *     DescribedValue idVal = new DescribedValue(VOStarTable.ID_INFO, "COL0");
 *     table.getColumnInfo(0).setAuxDatum(idVal);
 * </pre>
 * then if the result is written to a VOTable the relevant attribute
 * will be attached to the corresponding FIELD element.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOStarTable extends AbstractStarTable {

    private TableElement votable;
    private TabularData tdata;
    private List params;
    private ColumnInfo[] colinfos;

    /* Table parameters. */
    private final static ValueInfo ucdInfo = new DefaultValueInfo(
        "UCD", String.class, "Table UCD" );

    /* Public column auxiliary metadata definitions. */

    /** ValueInfo for VOTable <tt>ID</tt> attribute. */
    public final static ValueInfo ID_INFO = new DefaultValueInfo(
        "VOTable ID", String.class, "VOTable ID attribute" );

    /** ValueInfo for VOTable <tt>utype</tt> attribute. */
    public final static ValueInfo UTYPE_INFO = new DefaultValueInfo(
        "utype", String.class, 
        "Usage-specific type (ties value to an external data model)" );

    /** ValueInfo for VOTable <tt>width</tt> attribute. */
    public final static ValueInfo WIDTH_INFO = new DefaultValueInfo(
        "VOTable width", Integer.class, "VOTable width attribute" );

    /** ValueInfo for VOTable <tt>precision</tt> attribute. */
    public final static ValueInfo PRECISION_INFO = new DefaultValueInfo(
        "VOTable precision", String.class, "VOTable precision attribute" );

    /** ValueInfo for VOTable <tt>type</tt> attribute. */
    public final static ValueInfo TYPE_INFO = new DefaultValueInfo(
        "Type", String.class, "VOTable type attribute" );

    private final static ValueInfo datatypeInfo = new DefaultValueInfo(
        "Datatype", String.class, "VOTable data type name" );
    private final static ValueInfo nullInfo = Tables.NULL_VALUE_INFO;

    private final static List auxDataInfos = Arrays.asList( new ValueInfo[] {
        ID_INFO, datatypeInfo, nullInfo, UTYPE_INFO,
        WIDTH_INFO, PRECISION_INFO, TYPE_INFO,
    } );

    /**
     * Construct a VOStarTable from a TABLE element.
     * The data itself is inferred or constructed from the state and
     * content of the element.
     *
     * @param  votable  Table VOElement
     */
    public VOStarTable( TableElement votable ) throws IOException {
        this( votable, votable.getData() );
    }

    /**
     * Construct a VOStarTable from a TABLE element forcing a particular
     * data implementation.
     *
     * @param  votable  Table VOElement, which supplies the table's metadata
     * @param  tdata   object supplying the table's data
     */
    VOStarTable( TableElement votable, TabularData tdata ) {
        this.votable = votable;
        this.tdata = tdata;
        setName( calculateName( votable ) );
    }

    public int getColumnCount() {
        return tdata.getColumnCount();
    }

    public long getRowCount() {
        return votable.getNrows();
    }

    public boolean isRandom() {
        return tdata.isRandom();
    }

    public ColumnInfo getColumnInfo( int icol ) {

        /* Lazily construct the columninfo list. */
        if ( colinfos == null ) {
            FieldElement[] fields = votable.getFields();
            int ncol = fields.length;
            colinfos = new ColumnInfo[ ncol ];
            for ( int i = 0; i < ncol; i++ ) {
                FieldElement field = fields[ i ];
                ColumnInfo cinfo = new ColumnInfo( getValueInfo( field ) );

                /* Set up auxiliary metadata for this column according to the
                 * attributes that the FIELD element has. */
                List auxdata = cinfo.getAuxData();

                if ( field.hasAttribute( "ID" ) ) {
                    String id = field.getAttribute( "ID" );
                    auxdata.add( new DescribedValue( ID_INFO, id ) );
                }

                if ( field.hasAttribute( "datatype" ) ) {
                    String datatype = field.getAttribute( "datatype" );
                    auxdata.add( new DescribedValue( datatypeInfo, datatype ) );
                }

                String blankstr = field.getNull();
                if ( blankstr != null ) {
                    Object blank = blankstr;
                    try {
                        Class clazz = cinfo.getContentClass();
                        if ( clazz == Byte.class ) {
                            blank = Byte.valueOf( blankstr );
                        }
                        else if ( clazz == Short.class ) {
                            blank = Short.valueOf( blankstr );
                        }
                        else if ( clazz == Integer.class ) {
                            blank = Integer.valueOf( blankstr );
                        }
                        else if ( clazz == Long.class ) {
                            blank = Long.valueOf( blankstr );
                        }
                    }
                    catch ( NumberFormatException e ) {
                        blank = blankstr;
                    }
                    auxdata.add( new DescribedValue( nullInfo, blank ) );
                }

                if ( field.hasAttribute( "width" ) ) {
                    String width = field.getAttribute( "width" );
                    try {
                        int wv = Integer.parseInt( width );
                        auxdata.add( new DescribedValue( WIDTH_INFO,
                                                         new Integer( wv ) ) );
                    }
                    catch ( NumberFormatException e ) {
                    }
                }

                if ( field.hasAttribute( "precision" ) ) {
                    String precision = field.getAttribute( "precision" );
                    auxdata.add( new DescribedValue( PRECISION_INFO,
                                                     precision ) );
                }

                if ( field.hasAttribute( "type" ) ) {
                    String type = field.getAttribute( "type" );
                    auxdata.add( new DescribedValue( TYPE_INFO, type ) );
                }

                if ( field.hasAttribute( "utype" ) ) {
                    String utype = field.getAttribute( "utype" );
                    auxdata.add( new DescribedValue( UTYPE_INFO, utype ) );
                }

                VOElement[] links = field.getChildrenByName( "LINK" );
                for ( int j = 0; j < links.length; j++ ) {
                    auxdata.add( getDescribedValue( (LinkElement) 
                                                    links[ j ] ) );
                }

                colinfos[ i ] = cinfo;
            }
        }
        return colinfos[ icol ];
    }

    public List getParameters() {

        /* Lazily construct parameter list. */
        if ( params == null ) {
            params = new ArrayList();

            /* DESCRIPTION child. */
            String description = votable.getDescription();
            if ( description != null ) {
                DefaultValueInfo descInfo = 
                    new DefaultValueInfo( "Description", String.class );
                params.add( new DescribedValue( descInfo, description ) );
            }

            /* UCD attribute. */
            if ( votable.hasAttribute( "ucd" ) ) {
                DescribedValue dval =
                    new DescribedValue( ucdInfo, 
                                        votable.getAttribute( "ucd" ) );
                params.add( dval );
            }

            /* Utype attribute. */
            if ( votable.hasAttribute( "utype" ) ) {
                DescribedValue dval =
                    new DescribedValue( UTYPE_INFO,
                                        votable.getAttribute( "utype" ) );
                params.add( dval );
            }

            /* Parameter-like elements in parent. */
            VOElement parent = votable.getParent();
            if ( parent != null && parent.getTagName().equals( "RESOURCE" ) ) {

                /* PARAM elements. */
                VOElement[] paramels = parent.getChildrenByName( "PARAM" );
                for ( int i = 0; i < paramels.length; i++ ) {
                    ParamElement pel = (ParamElement) paramels[ i ];
                    DescribedValue dval = 
                        new DescribedValue( getValueInfo( pel ),
                                            pel.getObject() );
                    params.add( dval );
                }

                /* LINK elements. */
                VOElement[] linkels = parent.getChildrenByName( "LINK" );
                for ( int i = 0; i < linkels.length; i++ ) {
                    params.add( getDescribedValue( (LinkElement) 
                                                   linkels[ i ] ) );
                }

                /* INFO elements. */
                VOElement[] infoels = parent.getChildrenByName( "INFO" );
                for ( int i = 0; i < infoels.length; i++ ) {
                    VOElement iel = infoels[ i ];
                    String content = DOMUtils.getTextContent( iel );
                    String descrip =
                         content != null && content.trim().length() > 0
                               ? content : null;
                    ValueInfo info = new DefaultValueInfo( iel.getHandle(), 
                                                           String.class,
                                                           descrip );
                    DescribedValue dval =
                        new DescribedValue( info, iel.getAttribute( "value" ) );
                    params.add( dval );
                }
            }

            /* Parameter-like children. */
            ParamElement[] pels = votable.getParams();
            for ( int i = 0; i < pels.length; i++ ) {
                params.add( new DescribedValue( getValueInfo( pels[ i ] ),
                                                pels[ i ].getObject() ) );
            }
        }
        return params;
    }

    public List getColumnAuxDataInfos() {
        return auxDataInfos;
    }

    public RowSequence getRowSequence() throws IOException {
        final RowStepper rstep = tdata.getRowStepper();
        return new ReaderRowSequence() {
            protected Object[] readRow() throws IOException {
                return rstep.nextRow();
            }
        };
    }

    public Object[] getRow( long lrow ) throws IOException {
        if ( isRandom() ) {
            return tdata.getRow( lrow );
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    public Object getCell( long lrow, int icol ) throws IOException {
        if ( isRandom() ) {
            return tdata.getCell( lrow, icol );
        }
        else {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * Works out a suitable name for a given table element.
     *
     * @param  table element
     * @return  label string
     */
    private static String calculateName( TableElement table ) {

        /* If there is a name attribute, use that. */
        if ( table.getName() != null ) {
            return table.getName();
        }

        /* Otherwise, try to get a system ID (document base name). */
        String sysid = ((VODocument) table.getOwnerDocument()).getSystemId();

        /* Shorten the system ID to a reasonable length. */
        if ( sysid != null ) {
            int sindex = sysid.lastIndexOf( '/' );
            if ( sindex < 0 || sindex == sysid.length() - 1 ) {
                sindex = sysid.lastIndexOf( '\\' );
            }
            if ( sindex > 0 && sindex < sysid.length() - 1 ) {
                sysid = sysid.substring( sindex + 1 );
            }
        }

        /* Work out how many TABLE elements there are in the document. */
        Element top = table.getOwnerDocument().getDocumentElement();
        NodeList tables = top.getElementsByTagName( "TABLE" );
        int index = 0;
        int ntab = tables.getLength();
        if ( ntab > 1 ) {
            for ( int i = 0; i < ntab; i++ ) {
                if ( tables.item( i ) == table ) {
                    index = i + 1;
                    break;
                }
            }
        }
        if ( index == 0 ) {
            return sysid == null ? "votable" : sysid;
        }
        else {
            return ( sysid == null ? "" : sysid )
                 + "#" + index;
        }
    }

    /**
     * Returns a ValueInfo object suitable for holding the values in a
     * VOTable Field (or Param) object.  The datatype, array shape and
     * other metadata in the returned object are taken from the 
     * relevant bits of the supplied field.
     *
     * @param   field  the FieldElement object for which the ValueInfo is to be
     *          constructed
     * @return  a ValueInfo suitable for <tt>field</tt>
     */
    public static ValueInfo getValueInfo( FieldElement field ) {
        Decoder decoder = field.getDecoder();
        Class clazz = decoder.getContentClass();
        String name = field.getHandle();
        long[] shapel = decoder.getDecodedShape();
        DefaultValueInfo info = new DefaultValueInfo( name, clazz );
        info.setDescription( field.getDescription() );
        info.setUnitString( field.getUnit() );
        info.setUCD( field.getUcd() );
        info.setShape( ( shapel == null || shapel.length == 0 ) 
                            ? null
                            : Decoder.longsToInts( shapel ) );
        info.setElementSize( decoder.getElementSize() );
        return info;
    }

    /**
     * Returns a DescribedValue representing a LINK element.
     *
     * @param  link link element
     * @return value describing <tt>link</tt>
     */
    static DescribedValue getDescribedValue( LinkElement link ) {
        try {
            URL url = link.getHref();
            ValueInfo vinfo = new URLValueInfo( link.getHandle(),
                                                link.getDescription() );
            return new DescribedValue( vinfo, url );
        }
        catch ( MalformedURLException e ) {
            String href = link.getAttribute( "href" );
            ValueInfo vinfo =
                new DefaultValueInfo( link.getHandle(), String.class,
                                      link.getDescription() );
            return new DescribedValue( vinfo, href );
        }
    }
}
