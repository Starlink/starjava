package uk.ac.starlink.treeview.votable;

import java.lang.reflect.Array;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Field extends GenericElement {

    private final long[] arraysize;
    private final Datatype datatype;
    private String blank;
    private String unit;
    private String ucd;
    private boolean isVariable;
    private long sliceSize;
    private Element el;

    public Field( Element el ) {
        super( el );
        this.el = el;

        /* Get array size. */
        sliceSize = 1;
        isVariable = false;
        if ( el.hasAttribute( "arraysize" ) ) {
            String as = el.getAttribute( "arraysize" );
            String[] dimtxt = as.split( "x" );
            int ndim = dimtxt.length;
            arraysize = new long[ ndim ];
            for ( int i = 0; i < ndim; i++ ) {
                if ( i == ndim - 1 && dimtxt[ i ].trim().endsWith( "*" ) ) {
                    arraysize[ i ] = -1;
                    isVariable = true;
                }
                else {
                    try {
                        arraysize[ i ] = Long.parseLong( dimtxt[ i ] );
                        sliceSize *= arraysize[ i ];
                    }
                    catch ( NumberFormatException e ) {
                        throw new VOTableFormatException( e );
                    }
                    if ( arraysize[ i ] <= 0 ) {
                        throw new VOTableFormatException( 
                           "Negative dimensions illegal: " + as );
                    }
                }
            }
        }
        else {
            arraysize = new long[ 0 ];
        }

        /* Get blank value. */
        String blank = null;
        NodeList vals = el.getElementsByTagName( "VALUES" );
        for ( int i = 0; i < vals.getLength(); i++ ) {
            Element values = (Element) vals.item( i );
            if ( values.hasAttribute( "null" ) ) {
                blank = values.getAttribute( "null" );
            }
        }

        /* Get data type. */
        String dt = el.getAttribute( "datatype" );
        datatype = Datatype.makeDatatype( dt, arraysize, blank );

        /* Get simple attributes. */
        if ( el.hasAttribute( "unit" ) ) {
            this.unit = el.getAttribute( "unit" );
        }
        if ( el.hasAttribute( "ucd" ) ) {
            this.ucd = el.getAttribute( "ucd" );
        }
    }

    /**
     * Returns the array size.  The returned value is an array of 
     * <tt>long</tt>, with one element for each dimension. 
     * The final dimension only may have the value -1, which indicates
     * that this dimension is unknown.  All other elements will be positive.
     *
     * @return   array giving dimensions of data in this field.
     */
    public long[] getArraysize() {
        return (long[]) arraysize.clone();
    }

    public Datatype getDatatype() {
        return datatype;
    }

    public String getNull() {
        return blank;
    }

    public String getUnit() {
        return unit;
    }

    public String getUcd() {
        return ucd;
    }

    public String toString() {
        String str = getHandle();
        StringBuffer sbuf = new StringBuffer( str );
        if ( el.hasAttribute( "datatype" ) ) {
            sbuf.append( ' ' )
                .append( el.getAttribute( "datatype" ) );
        }
        if ( el.hasAttribute( "arraysize" ) ) {
            sbuf.append( " (" )
                .append( el.getAttribute( "arraysize" ) )
                .append( ")" );
        }
        if ( el.hasAttribute( "units" ) ) {
            sbuf.append( " / " + el.getAttribute( "units" ) );
        }
        return sbuf.toString();
    }
 

}

