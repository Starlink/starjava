package uk.ac.starlink.votable;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.xml.transform.Source;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.IteratorRowSequence;
import uk.ac.starlink.table.RandomRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.ValueInfo;

/**
 * A {@link uk.ac.starlink.table.StarTable} implementation based on a VOTable.
 *
 * @author   Mark Taylor (Starlink)
 */
public class VOStarTable extends AbstractStarTable {

    private Table votable;
    private VOStarValueAdapter[] adapters;
    private List params;
    private Source tabsrc;
    private int ncol;

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
     * @param  tabsrc  an XML Source containing the TABLE element
     */
    public VOStarTable( Source tabsrc ) {
        this.tabsrc = tabsrc;

        /* Make a table object.  This will be used for the metadata.
         * If it implements the RandomTable interface it will also be
         * used for random access methods, otherwise the data content
         * will never be read (getRowSequence will create new Table
         * objects). */
        votable = Table.makeTable( tabsrc );
        ncol = votable.getColumnCount();
        adapters = new VOStarValueAdapter[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            adapters[ i ] = VOStarValueAdapter
                           .makeAdapter( votable.getField( i ) );
        }
    }

    public int getColumnCount() {
        return ncol;
    }

    public long getRowCount() {
        return votable.getRowCount();
    }

    public boolean isRandom() {
        return votable instanceof RandomTable;
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
                    Object pval = pel.getObject();
                    Object val = pval == null 
                               ? null
                               : VOStarValueAdapter.makeAdapter( pel )
                                                   .adapt( pval );
                    dval.setValue( val );
                    params.add( dval );
                }
            }
        }
        return params;
    }

    public String getName() {
        return votable.getName();
    }

    public List getColumnAuxDataInfos() {
        return auxDataInfos;
    }

    public RowSequence getRowSequence() {
        if ( isRandom() ) {
            return new RandomRowSequence( this );
        }
        else {
            final Table vtab = Table.makeTable( tabsrc );
            return new IteratorRowSequence(
                new Iterator() {
                    public boolean hasNext() {
                        return vtab.hasNextRow();
                    }
                    public Object next() {
                        try {
                            Object[] row = vtab.nextRow();
                            for ( int i = 0; i < row.length; i++ ) {
                                Object val = row[ i ];
                                row[ i ] = val == null 
                                         ? null 
                                         : adapters[ i ].adapt( val );
                            }
                            return row;
                        }
                        catch ( IOException e ) {
                            throw new IteratorRowSequence
                                     .PackagedIOException( e );
                        }
                    }
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                }
            );
        }
    }

    public Object[] getRow( long lrow ) throws IOException {
        if ( isRandom() ) {
            Object[] row = ((RandomTable) votable)
                          .getRow( checkedLongToInt( lrow ) );
            for ( int i = 0; i < row.length; i++ ) {
                Object val = row[ i ];
                row[ i ] = val == null ? null
                                       : adapters[ i ].adapt( row[ i ] );
            }
            return row;
        }
        else {
            throw new UnsupportedOperationException();
        }
    }

    public Object getCell( long lrow, int icol ) throws IOException {
        if ( isRandom() ) {
            Object val = ((RandomTable) votable)
                        .getCell( checkedLongToInt( lrow ), icol );
            return val == null ? null 
                               : adapters[ icol ].adapt( val );
        }
        else {
            throw new UnsupportedOperationException();
        }
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
        info.setElementSize( field.getDecoder().getElementSize() );
        return info;
    }

}
