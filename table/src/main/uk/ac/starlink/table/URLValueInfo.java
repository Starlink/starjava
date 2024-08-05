package uk.ac.starlink.table;

import java.util.ArrayList;
import java.util.List;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.table.TableCellRenderer;
import uk.ac.starlink.util.URLUtils;

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
    private List<DescribedValue> auxData_;

    /**
     * Constructs a new URLValueInfo.
     * 
     * @param  name  info name
     * @param  description  info description
     */
    public URLValueInfo( String name, String description ) {
        name_ = name;
        description_ = description;
        auxData_ = new ArrayList<DescribedValue>();
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

    public String getXtype() {
        return null;
    }

    public Class<?> getContentClass() {
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

    public DomainMapper[] getDomainMappers() {
        return new DomainMapper[ 0 ];
    }

    public List<DescribedValue> getAuxData() {
        return auxData_;
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
            return URLUtils.newURL( rep );
        }
        catch ( MalformedURLException e ) {
            return null;
        }
    }

    public TableCellRenderer getCellRenderer() {
        return null;
    }
}
