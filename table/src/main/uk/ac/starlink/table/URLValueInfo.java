package uk.ac.starlink.table;

import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.table.TableCellRenderer;

/**
 * ValueInfo for URL values.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Sep 2004
 */
public class URLValueInfo implements ValueInfo {

    private String ucd_;
    private String utype_;
    private String name_;
    private String description_;

    /**
     * Constructs a new URLValueInfo.
     * 
     * @param  name  info name
     * @param  description  info description
     */
    public URLValueInfo( String name, String description ) {
        name_ = name;
        description_ = description;
    }

    public String getName() {
        return name_;
    }

    public String getDescription() {
        return description_;
    }

    public String getUnitString() {
        return null;
    }

    public String getUCD() {
        return ucd_;
    }

    public String getUtype() {
        return utype_;
    }

    public Class getContentClass() {
        return URL.class;
    }

    public boolean isArray() {
        return false;
    }

    public int[] getShape() {
        return null;
    }

    public int getElementSize() {
        return -1;
    }

    public boolean isNullable() {
        return true;
    }

    public String formatValue( Object value, int maxLength ) {
        if ( value == null ) {
            return "";
        }
        else {
            String txt = value.toString();
            int leng = txt.length();
            return leng <= maxLength ? txt : txt.substring( 0, maxLength );
        }
    }

    public Object unformatString( String rep ) {
        try {
            return new URL( rep );
        }
        catch ( MalformedURLException e ) {
            return null;
        }
    }

    public TableCellRenderer getCellRenderer() {
        return null;
    }
}
