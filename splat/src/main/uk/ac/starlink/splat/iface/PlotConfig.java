package uk.ac.starlink.splat.iface;

import java.awt.Graphics2D;

import org.jdom.Element;

import uk.ac.starlink.splat.ast.AstAxisLabels;
import uk.ac.starlink.splat.ast.AstBorder;
import uk.ac.starlink.splat.ast.AstGrid;
import uk.ac.starlink.splat.ast.AstNumberLabels;
import uk.ac.starlink.splat.ast.AstTicks;
import uk.ac.starlink.splat.ast.AstTitle;
import uk.ac.starlink.splat.data.DataLimits;
import uk.ac.starlink.splat.util.AbstractStorableConfig;
import uk.ac.starlink.splat.util.XMLEncodeAndDecode;

/**
 * PlotConfig is a repository for all the configuration information
 * related to a Plot (i.e. the AST graphics configuration, data limits
 * and antialiasing state).
 * <p>
 * The specific configurations for classes of objects are controlled by
 * a series of container objects that can be accessed individually as
 * required, or queried as a whole (for instance to get the complete
 * AST description).
 * <p>
 * Each Plot should have one of these objects associated with it
 * (which can be viewed and changed using a PlotConfigFrame window).
 * <p>
 * The total state of configuration can be saved and restored from an 
 * XML snippet attached to a given XML Element.
 * <p>
 *
 * @since $Date$
 * @since 15-NOV-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research
 * Councils
 * @see PlotConfigFrame, Plot, AstTitle, AstAxisLabels,
 * AstNumberLabels, AstGrid, AstBorder, AstTicks 
 */
public class PlotConfig implements XMLEncodeAndDecode
{
    /**
     * AST model of the title.
     */
    protected AstTitle astTitle = new AstTitle();

    /**
     * AST model of the axis labels.
     */
    protected AstAxisLabels astAxisLabels = new AstAxisLabels();

    /**
     * AST model of the number labels.
     */
    protected AstNumberLabels astNumberLabels = new AstNumberLabels();

    /**
     * AST model of the grid.
     */
    protected AstGrid astGrid = new AstGrid();

    /**
     * AST model of the border.
     */
    protected AstBorder astBorder = new AstBorder();

    /**
     * AST model of the plot ticks.
     */
    protected AstTicks astTicks = new AstTicks();

    /**
     * Model of the limits of the plot. The default is autoscaled in
     * both axes.
     */
    protected DataLimits dataLimits = new DataLimits();

    /**
     * Model of the graphics rendering options.
     */
    protected GraphicsHints graphicsHints = new GraphicsHints();

    /**
     * Model of the graphics edge options.
     */
    protected GraphicsEdges graphicsEdges = new GraphicsEdges();

    /**
     * Model of the background colour.
     */
    protected ColourStore backgroundColour = new ColourStore();

    /**
     * Array of objects that are storing the configuration parts.
     */
    protected AbstractStorableConfig[] configObjects = 
    { astTitle, astAxisLabels, astNumberLabels, astGrid, astBorder,
      astTicks, dataLimits, graphicsHints, graphicsEdges, 
      backgroundColour };

    /**
     * Create an instance.
     */
    public PlotConfig() 
    {
        //  Do nothing.
    }

    /**
     * Get reference to the AST object describing the title state.
     */
    public AstTitle getAstTitle() {
        return astTitle;
    }

    /**
     * Get reference to the AST object describing the axis labels state.
     */
    public AstAxisLabels getAstAxisLabels() 
    {
        return astAxisLabels;
    }

    /**
     * Get reference to the AST object describing the axis number
     * labels state.
     */
    public AstNumberLabels getAstNumberLabels() 
    {
        return astNumberLabels;
    }

    /**
     * Get reference to the AST object describing the grid state.
     */
    public AstGrid getAstGrid() 
    {
        return astGrid;
    }

    /**
     * Get reference to the AST object describing the border state.
     */
    public AstBorder getAstBorder() 
    {
        return astBorder;
    }

    /**
     * Get reference to the AST object describing the tick marks state.
     */
    public AstTicks getAstTicks() 
    {
        return astTicks;
    }

    /**
     * Get the complete AST description of all components.
     */
    public String getAst() 
    {
        return
            astTitle + "," +
            astAxisLabels + "," +
            astNumberLabels + "," +
            astGrid + "," +
            astBorder + "," +
            astTicks;
    }

    /**
     * Get the DataLimits object.
     */
    public DataLimits getDataLimits() 
    {
        return dataLimits;
    }

    /**
     * Get the GraphicsHints object.
     */
    public GraphicsHints getGraphicsHints() 
    {
        return graphicsHints;
    }

    /**
     * Apply the rendering hints to a graphics object.
     */
    public void applyRenderingHints( Graphics2D g2 )
    {
        graphicsHints.applyRenderingHints( g2 );
    }

    /**
     * Get the GraphicsEdges object.
     */
    public GraphicsEdges getGraphicsEdges() 
    {
        return graphicsEdges;
    }

    /**
     * Get the background colour.
     */
    public ColourStore getBackgroundColour()
    {
        return backgroundColour;
    }

//
// Implementation of the XMLEncodeAndDecode interface.
//

    /**
     * Symbolic names for the configuration parts.
     */
    protected String[] configNames = 
    { "title", "axislabels", "numberlabels", "grid", "border",
      "ticks", "datalimits", "graphicshints", "graphicsedges",
      "backgroundcolour" };
    
    /**
     * Encode the internal state of this object into an XML snippet
     * rooted in an Element.
     *
     * @param rootElement the Element within which the object should
     *                    store its configuration.
     */
    public void encode( Element rootElement )
    {
        //  Create children nodes for each type of configuration
        //  object that we're handling and get them to encode
        //  themselves.
        Element child;
        for ( int i = 0; i < configObjects.length; i++ ) {
            child = new Element( configNames[i] );
            rootElement.addContent( child );
            configObjects[i].encode( child );
        }
    }

    /**
     * Decode (i.e. restore) the internal state of this object from an
     * XML Element.
     *
     * @param rootElement the element to which a previous object this
     *                    this type has attached its configuration.
     */
    public void decode( Element rootElement )
    {
        // Visit each child of the root element and look for a known
        // string. If located pass that child to the related
        // configuration object.
        java.util.List children = rootElement.getChildren();
        Element child;
        for ( int i = 0; i < children.size(); i++ ) {
            child = (Element) children.get( i );
            String name = child.getName();
            for ( int j = 0; j < configNames.length; j++ ) {
                if ( configNames[j].equals( name ) ) {
                    configObjects[j].decode( child );
                    break;
                }
            }
        }
    }
}
