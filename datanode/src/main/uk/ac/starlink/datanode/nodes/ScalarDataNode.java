package uk.ac.starlink.datanode.nodes;

/**
 * Simple DataNode for representing scalar values.
 */
public class ScalarDataNode extends DefaultDataNode {

    private String desc;
    private String type;
    private String value;
    private static final String QUOTE = "\"";
    
    /**
     * Constructs a ScalarDataNode with a given name and value.
     */
    public ScalarDataNode( String name, String type, String value ) {
        super( name );
        
        if ( type.equalsIgnoreCase( "string" ) &&
             ! ( value.startsWith( QUOTE ) && value.endsWith( QUOTE ) ) ) {
            value = QUOTE + value + QUOTE;
        }
            
        desc = value;
        this.value = value;
        if ( type != null && type.trim().length() > 0 ) {
            this.type = type;
            desc = "<" + type + ">" + desc;
        }
        setIconID( IconFactory.SCALAR );
    }

    public String getDescription() {
        return desc;
    }

    public String getNodeTLA() {
        return "SCA";
    }

    public String nodeType() {
        return "Scalar";
    }

    public void configureDetail( DetailViewer dv ) {
        if ( type != null ) {
           dv.addKeyedItem( "Type", type );
        }
        dv.addKeyedItem( "Value", value );
    }
}
