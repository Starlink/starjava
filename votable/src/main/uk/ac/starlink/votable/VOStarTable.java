package uk.ac.starlink.votable;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.URLValueInfo;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.votable.datalink.ServiceDescriptor;
import uk.ac.starlink.votable.datalink.ServiceDescriptorFactory;
import uk.ac.starlink.votable.datalink.ServiceDescriptorInfo;

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
 *                               .getAuxDatumValue(VOStarTable.ID_INFO);
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
    private ColumnInfo[] colinfos;
    private boolean doneParams;
    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.votable" );

    /* Public column auxiliary metadata definitions. */

    /** ValueInfo for VOTable <tt>ID</tt> attribute. */
    public final static ValueInfo ID_INFO = new DefaultValueInfo(
        "VOTable ID", String.class, "VOTable ID attribute" );

    /** ValueInfo for VOTable <tt>ucd</tt> attribute. */
    public final static ValueInfo UCD_INFO = new DefaultValueInfo(
        "UCD", String.class, "Table UCD" );

    /** ValueInfo for VOTable <tt>utype</tt> attribute. */
    public final static ValueInfo UTYPE_INFO =
        new DefaultValueInfo( "utype", String.class,
                              "Usage-specific type"
                            + " (ties value to an external data model)" );

    /** ValueInfo for VOTable <tt>width</tt> attribute. */
    public final static ValueInfo WIDTH_INFO = new DefaultValueInfo(
        "VOTable width", Integer.class, "VOTable width attribute" );

    /** ValueInfo for VOTable <tt>precision</tt> attribute. */
    public final static ValueInfo PRECISION_INFO = new DefaultValueInfo(
        "VOTable precision", String.class, "VOTable precision attribute" );

    /** ValueInfo for VOTable <tt>ref</tt> attribute. */
    public final static ValueInfo REF_INFO = new DefaultValueInfo(
        "VOTable ref", String.class, "VOTable ref attribute" );

    /** ValueInfo for VOTable <tt>type</tt> attribute. */
    public final static ValueInfo TYPE_INFO = new DefaultValueInfo(
        "Type", String.class, "VOTable type attribute" );

    /** ValueInfo for VOTable <tt>datatype</tt> attribute. */
    public final static ValueInfo DATATYPE_INFO = new DefaultValueInfo(
        "Datatype", String.class, "VOTable data type name" );

    /** ValueInfo for COOSYS <tt>system</tt> attribute. */
    public final static ValueInfo COOSYS_SYSTEM_INFO = new DefaultValueInfo(
        "CoosysSystem", String.class, "Sky coordinate system name from COOSYS");
  
    /** ValueInfo for COOSYS <tt>epoch</tt> attribute. */
    public final static ValueInfo COOSYS_EPOCH_INFO = new DefaultValueInfo(
        "CoosysEpoch", String.class, "Sky epoch from COOSYS" );

    /** ValueInfo for COOSYS <tt>refposition</tt> attribute. */
    public final static ValueInfo COOSYS_REFPOSITION_INFO =
        new DefaultValueInfo( "CoosysRefposition", String.class,
                              "Reference position from COOSYS" );

    /** ValueInfo for COOSYS <tt>equinox</tt> attribute. */
    public final static ValueInfo COOSYS_EQUINOX_INFO = new DefaultValueInfo(
        "CoosysEquinox", String.class, "Sky equinox from COOSYS" );

    /** ValueInfo for TIMESYS <tt>timeorigin</tt> attribute. */
    public final static ValueInfo TIMESYS_TIMEORIGIN_INFO =
        new DefaultValueInfo( "TimesysTimeorigin", String.class,
                              "Time origin from TIMESYS" );

    /** ValueInfo for TIMESYS <tt>timescale</tt> attribute. */
    public final static ValueInfo TIMESYS_TIMESCALE_INFO =
        new DefaultValueInfo( "TimesysTimescale", String.class,
                              "Timescale from TIMESYS" );

    /** ValueInfo for TIMESYS <tt>refposition</tt> attribute. */
    public final static ValueInfo TIMESYS_REFPOSITION_INFO =
        new DefaultValueInfo( "TimesysRefposition", String.class,
                              "Ref position from TIMESYS" );

    private final static ValueInfo nullInfo = Tables.NULL_VALUE_INFO;
    private final static ValueInfo ubyteInfo = Tables.UBYTE_FLAG_INFO;

    private final static List<ValueInfo> auxDataInfos =
            Arrays.asList( new ValueInfo[] {
        DATATYPE_INFO, nullInfo,
        COOSYS_SYSTEM_INFO, COOSYS_EPOCH_INFO, COOSYS_REFPOSITION_INFO,
        COOSYS_EQUINOX_INFO,
        TIMESYS_TIMEORIGIN_INFO, TIMESYS_TIMESCALE_INFO,
        TIMESYS_REFPOSITION_INFO,
        ubyteInfo, WIDTH_INFO, PRECISION_INFO, ID_INFO, REF_INFO, TYPE_INFO,
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
                colinfos[ i ] = new ColumnInfo( getValueInfo( field ) );
            }
        }
        return colinfos[ icol ];
    }

    public List<DescribedValue> getParameters() {

        /* Lazily construct parameter list. */
        if ( ! doneParams ) {
            List<DescribedValue> params = new ArrayList<DescribedValue>();

            /* DESCRIPTION child. */
            String description = votable.getDescription();
            if ( description != null && description.trim().length() > 0 ) {
                DefaultValueInfo descInfo = 
                    new DefaultValueInfo( "Description", String.class );
                params.add( new DescribedValue( descInfo,
                                                description.trim() ) );
            }

            /* UCD attribute. */
            if ( votable.hasAttribute( "ucd" ) ) {
                DescribedValue dval =
                    new DescribedValue( UCD_INFO, 
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

            /* Track back through ancestor elements to pick up parameter-
             * like elements in this TABLE element and any ancestor 
             * RESOURCE elements. */
            List<VOElement> pelList = new ArrayList<VOElement>();
            for ( VOElement ancestor = votable; ancestor != null;
                  ancestor = ancestor.getParent() ) {
                addParamElements( ancestor, pelList );
            }

            /* Convert these elements into DescribedValue metadata objects. */
            for ( VOElement el : pelList ) {
                String tag = el.getVOTagName();
                if ( el instanceof ParamElement ) {
                    ParamElement pel = (ParamElement) el;
                    params.add( new DescribedValue( getValueInfo( pel ),
                                                    pel.getObject() ) );
                }
                else if ( el instanceof LinkElement ) {
                    LinkElement lel = (LinkElement) el;
                    params.add( getDescribedValue( lel ) );
                }
                else if ( "INFO".equals( tag ) ) {
                    String content = DOMUtils.getTextContent( el );
                    String descrip =
                        ( content != null && content.trim().length() > 0 )
                            ? content
                            : null;
                    ValueInfo info = new DefaultValueInfo( el.getHandle(),
                                                           String.class,
                                                           descrip );
                    DescribedValue dval =
                        new DescribedValue( info, el.getAttribute( "value" ) );
                    params.add( dval );
                }
                else {
                    assert false : el;
                }
            }

            /* Datalink-style Service Descriptors. */
            ServiceDescriptorFactory sdFact = new ServiceDescriptorFactory();
            ServiceDescriptor[] servDescrips =
                sdFact.readTableServiceDescriptors( votable );
            int nsd = servDescrips.length;
            for ( int isd = 0; isd < nsd; isd++ ) {
                ServiceDescriptor sd = servDescrips[ isd ];
                final String sdName;
                if ( sd.getName() != null ) {
                    sdName = "Service_" + sd.getName();
                }
                else {
                    sdName = nsd == 1 ? "ServiceDescriptor"
                                      : "ServiceDescriptor" + ( isd + 1 );
                }
                String sdDescrip = sd.getDescription() == null
                                 ? null
                                 : "Service Descriptor: " + sd.getDescription();
                ValueInfo sdInfo =
                    new ServiceDescriptorInfo( sdName, sdDescrip, this );
                params.add( new DescribedValue( sdInfo, sd ) );
            }

            /* Post-process parameter list. */
            adjustParams( params );

            /* Append this list to the superclass list. */
            synchronized ( this ) {
                if ( ! doneParams ) {
                    super.getParameters().addAll( params );
                    doneParams = true;
                }
            }
        }
        return super.getParameters();
    }

    public List<ValueInfo> getColumnAuxDataInfos() {
        return auxDataInfos;
    }

    public RowSequence getRowSequence() throws IOException {
        return tdata.getRowSequence();
    }

    public RowAccess getRowAccess() throws IOException {
        if ( isRandom() ) {
            return tdata.getRowAccess();
        }
        else {
            throw new UnsupportedOperationException();
        }
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

    public void close() throws IOException {
        tdata.close();
    }

    /**
     * Perform post-processing on the list of table parameters that
     * has been acquired from the input VOTable document.
     *
     * <p>At present this strips out potentially huge numbers of
     * uk.ac.starlink.topcat.plot2.TopcatLayer* INFO elements
     * that were erroneously added by TOPCAT v4.5 when a table
     * was plotted, thus trying to undo the mess that bug added.
     * It is <em>conceivable</em> that this is behaviour could be
     * unwanted, but (except for specific debugging purposes)
     * very unlikely, so just do it rather than make it configurable.
     *
     * @param  params  mutable metadata item list to adjust in place
     */
    private static void adjustParams( List<DescribedValue> params ) {
        int nTclayer = 0;
        String tclayerPrefix = "uk.ac.starlink.topcat.plot2.TopcatLayer";
        for ( Iterator<DescribedValue> it = params.iterator(); it.hasNext(); ) {
            DescribedValue dval = it.next();
            String dvname = dval.getInfo().getName();
            if ( dvname != null && dvname.startsWith( tclayerPrefix ) ) {
                nTclayer++;
                it.remove();
            }
        }
        if ( nTclayer > 0 ) {
            logger_.warning( "Discarded " + nTclayer + " " + tclayerPrefix + "*"
                           + " INFOs (added by TOPCAT v4.5 bug)" );
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
        Document doc = table.getOwnerDocument();
        boolean multiTable = (doc instanceof VODocument)
                           ? ((VODocument) doc).getElementCount( "TABLE" ) > 1
                           : true;
        if ( multiTable ) {
            return ( sysid == null ? "" : sysid )
                 + "#" + ( table.getElementSequence() + 1 );
        }
        else {
            return sysid == null ? "votable" : sysid;
        }
    }

    /**
     * Adds parameter-like children of a given element to a given list.
     * Recurses into GRUOP elements, but not into any other elements 
     * such as TABLE or RESOURCE.
     *
     * <p>Element types added to the list are
     * PARAM (including referents of PARAMrefs), LINK and INFO.
     *
     * @param  parent   element whose children are to be considered
     * @param  pelList  list to which parameter-like elements will be added
     */
    private static void addParamElements( VOElement parent,
                                          List<VOElement> pelList ) {
        VOElement[] children = parent.getChildren();
        for ( int i = 0; i < children.length; i++ ) { 
            VOElement child = children[ i ];
            if ( child instanceof ParamElement ) {
                if ( ! pelList.contains( child ) ) {
                    pelList.add( child );
                }
            }
            else if ( child instanceof ParamRefElement ) {
                ParamElement pel = ((ParamRefElement) child).getParam();
                if ( pel != null ) {
                    if ( ! pelList.contains( pel ) ) {
                        pelList.add( pel );
                    }
                }
                else {
                    String msg = new StringBuffer()
                        .append( "Ignoring PARAMref element with no referent " )
                        .append( '"' )
                        .append( child.getAttribute( "ref" ) )
                        .append( '"' )
                        .toString();
                    logger_.warning( msg );
                }
            }
            else if ( child instanceof LinkElement ) {
                if ( ! pelList.contains( child ) ) {
                    pelList.add( child );
                }
            }
            else if ( "INFO".equals( child.getVOTagName() ) ) {
                if ( ! pelList.contains( child ) ) {
                    pelList.add( child );
                }
            }
            else if ( child instanceof GroupElement ) {
                addParamElements( child, pelList );
            }
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

        /* Decoder. */
        Decoder decoder = field.getDecoder();

        /* Basic metadata. */
        Class<?> clazz = decoder.getContentClass();
        String name = field.getHandle();
        long[] shapel = decoder.getDecodedShape();
        DefaultValueInfo info = new DefaultValueInfo( name, clazz );
        info.setDescription( field.getDescription() );
        info.setUnitString( field.getUnit() );
        info.setUCD( field.getUcd() );
        info.setUtype( field.getUtype() );
        info.setXtype( field.getXtype() );
        info.setShape( ( shapel == null || shapel.length == 0 ) 
                            ? null
                            : Decoder.longsToInts( shapel ) );
        info.setElementSize( decoder.getElementSize() );

        /* Aux metadata. */
        List<DescribedValue> auxdata = info.getAuxData();
        if ( field.hasAttribute( "ID" ) ) {
            String id = field.getAttribute( "ID" );
            auxdata.add( new DescribedValue( ID_INFO, id ) );
        }
        if ( field.hasAttribute( "datatype" ) ) {
            String datatype = field.getAttribute( "datatype" );
            auxdata.add( new DescribedValue( DATATYPE_INFO, datatype ) );
            if ( "unsignedByte".equals( datatype ) ) {
                auxdata.add( new DescribedValue( ubyteInfo, Boolean.TRUE ) );
            }
        }
        String blankstr = field.getNull();
        if ( blankstr != null ) {
            Object blank = blankstr;
            try {
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
            auxdata.add( new DescribedValue( PRECISION_INFO, precision ) );
        }
        if ( field.hasAttribute( "type" ) ) {
            String type = field.getAttribute( "type" );
            auxdata.add( new DescribedValue( TYPE_INFO, type ) );
        }

        /* Coosys. */
        VOElement coosys = field.getCoosys();
        if ( coosys != null ) {
            if ( coosys.hasAttribute( "system" ) ) {
                String system = coosys.getAttribute( "system" );
                auxdata.add( new DescribedValue( COOSYS_SYSTEM_INFO,
                                                 system ) );
            }
            if ( coosys.hasAttribute( "epoch" ) ) {
                String epoch = coosys.getAttribute( "epoch" );
                auxdata.add( new DescribedValue( COOSYS_EPOCH_INFO,
                                                 epoch ) );
            }
            if ( coosys.hasAttribute( "refposition" ) ) {
                String refposition = coosys.getAttribute( "refposition" );
                auxdata.add( new DescribedValue( COOSYS_REFPOSITION_INFO,
                                                 refposition ) );
            }
            if ( coosys.hasAttribute( "equinox" ) ) {
                String equinox = coosys.getAttribute( "equinox" );
                auxdata.add( new DescribedValue( COOSYS_EQUINOX_INFO,
                                                 equinox ) );
            }
        }

        /* Timesys. */
        VOElement timesys = field.getTimesys();
        if ( timesys != null ) {
            if ( timesys.hasAttribute( "timeorigin" ) ) {
                String torigin = timesys.getAttribute( "timeorigin" );
                auxdata
               .add( new DescribedValue( TIMESYS_TIMEORIGIN_INFO,
                                         torigin ) );
            }
            if ( timesys.hasAttribute( "timescale" ) ) {
                String tscale = timesys.getAttribute( "timescale" );
                auxdata.add( new DescribedValue( TIMESYS_TIMESCALE_INFO,
                                                 tscale ) );
            }
            if ( timesys.hasAttribute( "refposition" ) ) {
                String refpos = timesys.getAttribute( "refposition" );
                auxdata
               .add( new DescribedValue( TIMESYS_REFPOSITION_INFO,
                                         refpos ) );
            }
        }

        /* Links. */
        VOElement[] links = field.getChildrenByName( "LINK" );
        for ( int j = 0; j < links.length; j++ ) {
            auxdata.add( getDescribedValue( (LinkElement) links[ j ] ) );
        }

        /* Domain mappers. */
        info.setDomainMappers( VOTableDomainMappers.getMappers( info ) );

        return info;
    }

    /**
     * Identifies the column that was labelled with a given ID attribute.
     *
     * @param  colRef    ID string
     * @param  table     table to interrogate; this will presumably be based
     *                   on a VOStarTable, but it may be some kind of
     *                   wrapped form of one
     * @return   index of the column in <code>table</code> whose FIELD
     *           element had an ID attribute of <code>colRef</code>,
     *           or -1 if none exists
     */
    public static int getRefColumnIndex( String colRef, StarTable table ) {
        if ( table != null ) {
            int ncol = table.getColumnCount();
            for ( int ic = 0; ic < ncol; ic++ ) {
                ColumnInfo info = table.getColumnInfo( ic );
                if ( colRef.equals( info.getAuxDatumValue( ID_INFO,
                                                           String.class ) ) ) {
                    return ic;
                }
            }
        }
        return -1;
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
