package uk.ac.starlink.votable;

import java.io.IOException;
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
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.DOMUtils;

/**
 * A {@link uk.ac.starlink.table.StarTable} implementation based on a VOTable.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOStarTable extends AbstractStarTable {

    private TableElement votable;
    private TabularData tdata;
    private List params;
    private ColumnInfo[] colinfos;

    /* Auxiliary metadata. */
    private final static ValueInfo idInfo = new DefaultValueInfo(
        "ID", String.class, "VOTable ID attribute" );
    private final static ValueInfo datatypeInfo = new DefaultValueInfo(
        "Datatype", String.class, "VOTable data type name" );
    private final static ValueInfo nullInfo = Tables.NULL_VALUE_INFO;
    private final static ValueInfo widthInfo = new DefaultValueInfo(
        "Width", Integer.class, "VOTable width attribute" );
    private final static ValueInfo precisionInfo = new DefaultValueInfo(
        "Precision", Double.class, "VOTable precision attribute" );
    private final static ValueInfo typeInfo = new DefaultValueInfo(
        "Type", String.class, "VOTable type attribute" );
    private final static List auxDataInfos = Arrays.asList( new ValueInfo[] {
        idInfo, datatypeInfo, nullInfo, widthInfo, precisionInfo, typeInfo,
    } );

    /**
     * Construct a VOStarTable from a VOTable <tt>Table</tt> object.
     *
     * @param  votable  Table VOElement
     */
    public VOStarTable( TableElement votable ) throws IOException {
        this.votable = votable;
        this.tdata = votable.getData();
        int ncol = tdata.getColumnCount();
        setName( calculateName( votable ) );
    }

    public int getColumnCount() {
        return tdata.getColumnCount();
    }

    public long getRowCount() {
        return tdata.getRowCount();
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

                String id = field.getAttribute( "ID" );
                if ( id != null ) {
                    auxdata.add( new DescribedValue( idInfo, id ) );
                }

                String datatype = field.getAttribute( "datatype" );
                if ( datatype != null ) {
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

                String width = field.getAttribute( "width" );
                if ( width != null ) {
                    try {
                        int wv = Integer.parseInt( width );
                        auxdata.add( new DescribedValue( widthInfo,
                                                         new Integer( wv ) ) );
                    }
                    catch ( NumberFormatException e ) {
                    }
                }

                String precision = field.getAttribute( "precision" );
                if ( precision != null ) {
                    try {
                        double pv = Double.parseDouble( precision );
                        auxdata.add( new DescribedValue( precisionInfo,
                                                         new Double( pv ) ) );
                    }
                    catch ( NumberFormatException e ) {
                    }
                }

                String type = field.getAttribute( "type" );
                if ( type != null ) {
                    auxdata.add( new DescribedValue( typeInfo, type ) );
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
            String description = votable.getDescription();
            if ( description != null ) {
                DefaultValueInfo descInfo = 
                    new DefaultValueInfo( "Description", String.class );
                params.add( new DescribedValue( descInfo, description ) );
            }

            VOElement parent = votable.getParent();
            if ( parent != null && parent.getTagName().equals( "RESOURCE" ) ) {
                VOElement[] paramels = parent.getChildrenByName( "PARAM" );
                for ( int i = 0; i < paramels.length; i++ ) {
                    ParamElement pel = (ParamElement) paramels[ i ];
                    DescribedValue dval = 
                        new DescribedValue( getValueInfo( pel ),
                                            pel.getObject() );
                    params.add( dval );
                }
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

}
