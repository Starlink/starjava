package uk.ac.starlink.treeview;

import javax.swing.Icon;
import javax.swing.JComponent;
import org.jdom.EntityRef;

/**
 * A {@link DataNode} representing and XML entity reference.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class XMLEntityRefDataNode extends DefaultDataNode {

    private EntityRef eref;
    private Icon icon;
    private JComponent fullView;
    private String name;

    /**
     * Initialises a new XMLEntityRefDataNode from an EntityRef object.
     *
     * @param  eref  the EntityRef object
     */
    public XMLEntityRefDataNode( EntityRef eref ) {
        this.eref = eref;
        name = eref.getName();
        setLabel( name );
    }

    public boolean allowsChildren() {
        return false;
    }

    public String getName() {
        return name;
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = iconMaker.getIcon( IconFactory.XML_EREF );
        }
        return icon;
    }

    public String getNodeTLA() {
        return "XER";
    }

    public String getNodeType() {
        return "XML entity reference node";
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            String sysID = eref.getSystemID();
            String pubID = eref.getPublicID();
            if ( sysID != null || pubID != null ) {
                dv.addSeparator();
            }
            if ( sysID != null ) {
                dv.addKeyedItem( "System ID", sysID );
            }
            if ( pubID != null ) {
                dv.addKeyedItem( "Public ID", pubID );
            }
        }
        return fullView;
    }
}
