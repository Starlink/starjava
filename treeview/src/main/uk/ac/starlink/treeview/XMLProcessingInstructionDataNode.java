package uk.ac.starlink.treeview;

import javax.swing.Icon;
import javax.swing.JComponent;
import org.jdom.ProcessingInstruction;

/**
 * A {@link DataNode} representing an XML processing instruction.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class XMLProcessingInstructionDataNode extends DefaultDataNode {

    private ProcessingInstruction pi;
    private Icon icon;
    private JComponent fullView;
    private String target;

    /**
     * Initialises an XMLProcessingInstructionDataNode from a 
     * ProcessingInstruction object.
     *
     * @param   pi   the ProcessingInstruction
     */
    public XMLProcessingInstructionDataNode( ProcessingInstruction pi ) {
        this.pi = pi;
        target = pi.getTarget();
        setLabel( target );
    }

    public boolean allowsChildren() {
        return false;
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = iconMaker.getIcon( IconFactory.XML_PI );
        }
        return icon;
    }

    public String getName() {
       return target;
    }

    public String getNodeTLA() {
        return "XPI";
    }

    public String getNodeType() {
        return "XML processing instruction node";
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            String text = pi.getData();
            dv.addSeparator();
            dv.addKeyedItem( "Length", text.length() + " characters" );
            dv.addSubHead( "Content" );
            dv.addText( text );
        }
        return fullView;
    }
}

