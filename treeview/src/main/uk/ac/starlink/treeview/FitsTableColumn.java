package uk.ac.starlink.treeview;

public class FitsTableColumn {

    private String format;
    private String type;
    private String unit;
    private Integer blank;
    private double scale = 1.0;
    private double zero = 0.0;
    private String disp;

    public FitsTableColumn() {}

    public String getFormat() { return format; }
    public String getType() { return type; }
    public String getUnit() { return unit; }
    public Integer getBlank() { return blank; }
    public double getScale() { return scale; }
    public double getZero() { return zero; }
    public String getDisp() { return disp; }
    public boolean isScaled() { return scale != 1.0 || zero != 0.0; }

    public void setFormat( String fmt ) { this.format = normalize( fmt ); }
    public void setType( String type ) { this.type = normalize( type ); }
    public void setUnit( String unit ) { this.unit = normalize( unit ); }
    public void setBlank( Integer blank ) { this.blank = blank; }
    public void setScale( double scale ) { this.scale = scale; }
    public void setZero( double zero ) { this.zero = zero; }
    public void setDisp( String disp ) { this.disp = normalize( disp ); }

    public String toString() {
        return ( ( type == null ) ? "<column>" : type )
             + ( ( unit == null ) ? "" : ( " / " + unit ) );
    }

    private static String normalize( String str ) {
        if ( str == null ) {
            return null;
        }
        else {
            str = str.trim();
            return ( str.length() > 0 ) ? str : null;
        }
    }

}
