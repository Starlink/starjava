/*
 * Copyright (C) 2001-2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     15-NOV-2000 (Peter W. Draper):
 *        Original version.
 *     10-OCT-2002 (Peter W. Draper):
 *        Refactored from SPLAT into ast.gui;
 */
package uk.ac.starlink.ast.gui;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.starlink.ast.Plot; // for documentation links
import uk.ac.starlink.util.XMLEncodeAndDecode;

/**
 * PlotConfiguration is a repository for all the configuration information
 * related to a Plot (i.e. the AST graphics configuration, plus
 * addition information such as data limits and antialiasing state,
 * which may be added).
 * <p>
 * The specific configurations for classes of objects are controlled by
 * a series of container objects that can be accessed individually as
 * required, or queried as a whole (for instance to get the complete
 * AST description).
 * <p>
 * Each Plot should have one of these objects associated with it
 * (which can be viewed and changed using a PlotConfigurator window).
 * <p>
 * The total state of configuration can be saved and restored from an
 * XML snippet attached to a given XML Element.
 * <p>
 *
 * @author Peter W. Draper
 * @version $Id$
 *
 * @see PlotConfigurator
 * @see Plot
 * @see AstTitle
 * @see AstAxisLabels,
 * @see AstNumberLabels
 * @see AstGrid
 * @see AstAxes
 * @see AstBorder
 * @see AstTicks
 */
public class PlotConfiguration implements XMLEncodeAndDecode
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
     * AST model of the axes.
     */
    protected AstAxes astAxes = new AstAxes();

    /**
     * AST model of the border.
     */
    protected AstBorder astBorder = new AstBorder();

    /**
     * AST model of the plot ticks.
     */
    protected AstTicks astTicks = new AstTicks();

    /**
     * Array of AbstractPlotControlsModel objects that are storing
     * the configuration parts. Note we populate this with the default
     * set, which are always available.
     */
    protected ArrayList configObjects = new ArrayList( 10 );

    /**
     * Array of AbstractPlotControlsModel objects that are genuine Ast
     * related parts (i.e. can be passed to an Ast Plot as
     * configuration options, these are the default list and cannot be
     * extended).
     */
    protected ArrayList astConfigObjects = new ArrayList( 10 );

    /**
     * Create an instance.
     */
    public PlotConfiguration()
    {
        //  Create the default models.
        configObjects.add( astTitle );
        configObjects.add( astAxisLabels );
        configObjects.add( astNumberLabels );
        configObjects.add( astGrid );
        configObjects.add( astAxes );
        configObjects.add( astBorder );
        configObjects.add( astTicks );

        // These are the AST models (i.e. can be passed to an AST Plot
        // as an options string).
        astConfigObjects.add( astTitle );
        astConfigObjects.add( astAxisLabels );
        astConfigObjects.add( astNumberLabels );
        astConfigObjects.add( astGrid );
        astConfigObjects.add( astAxes );
        astConfigObjects.add( astBorder );
        astConfigObjects.add( astTicks );
    }

    /**
     * Get the complete AST description of all AST components.
     * This option string can be given to a {@link Plot}.
     */
    public String getAst()
    {
        StringBuffer result =
            new StringBuffer( astConfigObjects.get( 0 ).toString() );
        String description = null;
        for ( int i = 1; i < astConfigObjects.size(); i++ ) {
            description = astConfigObjects.get( i ).toString();
            if ( description != null && ! description.equals( "" ) ) {
                result.append( "," );
                result.append( description );
            }
        }
        return result.toString();
    }

    /**
     * Add an AbstractPlotControlsModel to the list.
     */
    public void add( AbstractPlotControlsModel model )
    {
        configObjects.add( model );
    }

    /**
     * Return an Iterator for the list of AbstractPlotControlsModels.
     */
    public Iterator iterator()
    {
        return configObjects.iterator();
    }

    /**
     * Get an AbstractPlotControlsModel by class
     */
    public AbstractPlotControlsModel getControlsModel( Class clazz )
    {
        Iterator i = configObjects.iterator();
        Object n = null;
        while ( i.hasNext() ) {
            n = i.next();
            if ( clazz.isInstance( n ) ) {
                return (AbstractPlotControlsModel) n;
            }
        }
        return null;
    }

//
// Implementation of the XMLEncodeAndDecode interface.
//
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
        Document parent = rootElement.getOwnerDocument();
        AbstractPlotControlsModel model;
        for ( int i = 0; i < configObjects.size(); i++ ) {
            model = (AbstractPlotControlsModel) configObjects.get( i );
            child = parent.createElement( model.getTagName() );
            rootElement.appendChild( child );
            model.encode( child );
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
        Element child;
        AbstractPlotControlsModel model;
        List children = ConfigurationStore.getChildElements(rootElement);

        for ( int i = 0; i < children.size(); i++ ) {
            child = (Element) children.get( i );
            String name = child.getTagName();
            for ( int j = 0; j < configObjects.size(); j++ ) {
                model = (AbstractPlotControlsModel) configObjects.get( j );
                if ( model.getTagName().equals( name ) ) {
                    model.decode( child );
                    break;
                }
            }
        }
    }

    /**
     * The name for the parent tag of all stored elements.
     */
    public String getTagName()
    {
        return "plot-config";
    }
}
