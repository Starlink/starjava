package uk.ac.starlink.treeview;

import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * Simple DataNode for representing scalar values.
 */
public class ScalarDataNode extends DefaultDataNode {

    private String desc;
    private String type;
    private String value;
    private Icon icon;
    private JComponent fullView;
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

    public Icon getIcon() {
        if ( icon == null ) {
            icon = IconFactory.getIcon( IconFactory.SCALAR );
        }
        return icon;
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            if ( type != null ) {
               dv.addKeyedItem( "Type", type );
            }
            dv.addKeyedItem( "Value", value );
        }
        return fullView;
    }
}
