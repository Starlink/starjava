package uk.ac.starlink.votable;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.SequentialStarTable;
import uk.ac.starlink.table.ValueInfo;

/**
 * A {@link uk.ac.starlink.table.StarTable} implementation based on a VOTable.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOStarTable extends SequentialStarTable {

    private Table votable;
    private VOStarValueAdapter[] adapters;
    private List params;

    /* Auxiliary metadata. */
    private final static ValueInfo idInfo = new DefaultValueInfo(
        "ID", String.class, "VOTable ID attribute" );
    private final static ValueInfo datatypeInfo = new DefaultValueInfo(
        "Datatype", String.class, "VOTable data type name" );
    private final static ValueInfo widthInfo = new DefaultValueInfo(
        "Width", Integer.class, "VOTable width attribute" );
    private final static ValueInfo precisionInfo = new DefaultValueInfo(
        "Precision", Double.class, "VOTable precision attribute" );
    private final static ValueInfo typeInfo = new DefaultValueInfo(
        "Type", String.class, "VOTable type attribute" );
    private final static List auxDataInfos = Arrays.asList( new ValueInfo[] {
        idInfo, datatypeInfo, widthInfo, precisionInfo, typeInfo,
    } );

    /**
     * Construct a VOStarTable from a VOTable <tt>Table</tt> object.
     *
     * @param  votable  the table object
     */
    public VOStarTable( Table votable ) {
        this.votable = votable;
        adapters = new VOStarValueAdapter[ votable.getColumnCount() ];
        for ( int i = 0; i < adapters.length; i++ ) {
            adapters[ i ] = VOStarValueAdapter
                           .makeAdapter( votable.getField( i ) );
        }
    }

    public int getColumnCount() {
        return votable.getColumnCount();
    }

    public long getRowCount() {
        return (long) votable.getRowCount();
    }

    protected Object[] getNextRow() {
        Object[] row = votable.nextRow();
        for ( int i = 0; i < row.length; i++ ) {
            row[ i ] = adapters[ i ].adapt( row[ i ] );
        }
        return row;
    }

    protected boolean hasNextRow() {
        return votable.hasNextRow();
    }

    public ColumnInfo getColumnInfo( int icol ) {
        Field field = votable.getField( icol );
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

        String width = field.getAttribute( "width" );
        if ( width != null ) {
            try {
                int widthval = Integer.parseInt( width );
                auxdata.add( new DescribedValue( widthInfo,
                                                 new Integer( widthval ) ) );
            }
            catch ( NumberFormatException e ) {
            }
        }

        String precision = field.getAttribute( "precision" );
        if ( precision != null ) {
            try {
                double precval = Double.parseDouble( precision );
                auxdata.add( new DescribedValue( precisionInfo,
                                                 new Double( precval ) ) );
            }
            catch ( NumberFormatException e ) {
            }
        }

        String type = field.getAttribute( "type" );
        if ( type != null ) {
            auxdata.add( new DescribedValue( typeInfo, type ) );
        }

        return cinfo;
    }

    public List getParameters() {
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
                    Param pel = (Param) paramels[ i ];
                    DescribedValue dval = 
                        new DescribedValue( getValueInfo( pel ) );
                    Object val = VOStarValueAdapter.makeAdapter( pel )
                                                   .adapt( pel.getObject() );
                    dval.setValue( val );
                    params.add( dval );
                }
            }
        }
        return params;
    }

    public List getColumnAuxDataInfos() {
        return auxDataInfos;
    }

    /**
     * Returns a ValueInfo object suitable for holding the values in a
     * VOTable Field (or Param) object.  The datatype, array shape and
     * other metadata in the returned object are taken from the 
     * relevant bits of the supplied field.
     *
     * @param   field  the Field object for which the ValueInfo is to be
     *          constructed
     * @return  a ValueInfo suitable for <tt>field</tt>
     */
    private static ValueInfo getValueInfo( Field field ) {
        Class clazz = VOStarValueAdapter.makeAdapter( field )
                     .getContentClass();
        String name = field.getHandle();
        DefaultValueInfo info = new DefaultValueInfo( name, clazz );
        info.setDescription( field.getDescription() );
        info.setUnitString( field.getUnit() );
        info.setUCD( field.getUcd() );
        info.setShape( Decoder.longsToInts( field.getDecoder()
                                                 .getDecodedShape() ) );
        return info;
    }

}
