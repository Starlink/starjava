package uk.ac.starlink.datanode.nodes;

import java.awt.Component;
import java.io.IOException;
import javax.swing.*;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Element;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.SpecFrame;
import uk.ac.starlink.ast.SkyFrame;
import uk.ac.starlink.ast.xml.XAstWriter;
import uk.ac.starlink.datanode.viewers.AstTextShower;
import uk.ac.starlink.datanode.viewers.TextViewer;

/**
 * A {@link DataNode} representing an AST {@link uk.ac.starlink.ast.Frame}
 * object.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class FrameDataNode extends DefaultDataNode {
    private Frame frame;
    private String description;
    private String name;
    private boolean sky;
    private boolean spec;

    /**
     * Creates a FrameDataNode from an AST Frame object.
     *
     * @param  frame  the AST Frame on which to base this DataNode
     */
    public FrameDataNode( Frame frame ) throws NoSuchDataException {
        this.frame = frame;
        name = frame.getDomain().trim();
        if ( name.equals( "" ) ) {
            name = "(no domain)";
        }
        setLabel( name );
        sky = frame instanceof SkyFrame;
        spec = frame instanceof SpecFrame;
        description = "(" + frame.getNaxes() + " axes) \"" 
                          + frame.getTitle() + "\"";

        /* Set the icon according to the type of frame it is. */
        short iconid;
        if ( sky ) {
            iconid = IconFactory.SKYFRAME;
        }
        else if ( spec ) {
            iconid = IconFactory.SPECFRAME;
        }
        else {
            iconid = IconFactory.FRAME;
        }
        setIconID( iconid );
    }

    /**
     * Returns a short string describing the node type.
     * 
     * @return  the string "FRM" or "SKY" depending on the type of frame
     */
    public String getNodeTLA() {
        return sky ? "SKY" : "FRM";
    }

    public String getNodeType() {
        return sky ? "AST Sky coordinate frame" : "AST coordinate frame";
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public void configureDetail( DetailViewer dv ) {
        int naxes = frame.getNaxes();
        addItem( dv, "Naxes" );
        addItem( dv, "Domain" );
        addItem( dv, "Title" );
        if ( sky ) {
            addItem( dv, "Epoch" );
            addItem( dv, "Equinox" );
            addItem( dv, "Projection" );
            addItem( dv, "System" );
        }
        for ( int i = 1; i <= naxes; i++ ) {
            dv.addSubHead( "Axis " + i );
            addAxisItem( dv, "Label", i );
            addAxisItem( dv, "Symbol", i );
            addAxisItem( dv, "Unit", i );
            addAxisItem( dv, "Digits", i );
            addAxisItem( dv, "Direction", i );
            addAxisItem( dv, "Format", i );
        }
        dv.addPane( "Text view",
                    new ComponentMaker() {
                        public JComponent getComponent() {
                            return new AstTextShower( frame );
                        }
                    } );
        dv.addPane( "XML view",
                    new ComponentMaker() {
                        public JComponent getComponent() 
                                throws TransformerException {
                            Element el = new XAstWriter()
                                        .makeElement( frame );
                            return new TextViewer( new DOMSource( el ) );
                        }
                    } );
    }

    private void addItem( DetailViewer dv, String attrib ) {
        if ( true || frame.test( attrib ) ) {
            dv.addKeyedItem( attrib, frame.getC( attrib ) );
        }
    }

    private void addAxisItem( DetailViewer dv, String attrib, int axis ) {
        String att = attrib + "(" + axis + ")";
        if ( true || frame.test( att ) ) {
            dv.addKeyedItem( attrib, frame.getC( att ) );
        }
    }

}
