/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-JAN-2004 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.ast.gui;

import java.util.List;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;

import org.w3c.dom.Element;

import uk.ac.starlink.ast.Mapping;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.ast.xml.XAstReader;
import uk.ac.starlink.ast.xml.XAstWriter;
import uk.ac.starlink.diva.DrawFigure;
import uk.ac.starlink.diva.DrawFigureStore;
import uk.ac.starlink.util.PrimitiveXMLEncodeDecode;
import uk.ac.starlink.util.SourceReader;

/**
 * A subclass of {@link DrawFigureStore} that adds functionality to store and
 * restore the current AST context along with the properties of a set of
 * figures. This is intended to make it possible to save figures and restore
 * them using world coordinates, not just graphics coordinates (so that
 * figures can be redrawn at the same wavelength, time, celestial coordinates
 * etc.).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AstFigureStore
    extends DrawFigureStore
{
    /** Source of the AST Plot currently being used to map from world
     * to graphics coordinates */
    protected AstPlotSource plotSource = null;

    /** Name of the tag used for the Plot serialisation */
    public static final String PLOT_TAG = "plotframeset";

    /**
     * Constructor.
     *
     * @param plotSource the source of the {@link Plot} currently
     *                   being used to map world coordinates to
     *                   graphics coordinates. It is expected that the
     *                   {@link Plot} will be re-created so access
     *                   needs to be provided in this form.
     * @param application the name of the application directory used for
     *                    backing store files (created in "home" directory).
     * @param storeFile file used for storing figures (in application specific
     *                  directory).
     * @param tagName the name of the tag (root element) that contains figures
     *                that can be restored by this object.
     */
    public AstFigureStore( AstPlotSource plotSource, String application,
                           String storeFile, String tagName )
    {
        super( application, storeFile, tagName );
        this.plotSource = plotSource;
    }

    // Writer for AST XML stream.
    private XAstWriter astWriter = null;

    // Reader for AST XML stream.
    private XAstReader astReader = null;

    public void saveState( Element rootElement )
    {
        savePlot( rootElement );
        super.saveState( rootElement );
    }

    public void restoreState( Element rootElement )
    {
        List children =
            PrimitiveXMLEncodeDecode.getChildElements( rootElement );
        int size = children.size();

        Element element = null;
        String name = null;
        AstFigureProps props = null;
        DrawFigure figure = null;
        Mapping oldMapping = null;
        Mapping newMapping = (Mapping) plotSource.getPlot();

        for ( int i = 0; i < size; i++ ) {
            props = new AstFigureProps();
            element = (Element) children.get( i );
            name = PrimitiveXMLEncodeDecode.getElementName( element );
            if ( PLOT_TAG.equals( name ) ) {
                oldMapping = restorePlot( element );
            }
            if ( props.getTagName().equals( name ) ) {
                props.decode( element, oldMapping, newMapping );
                figure = figureFactory.create( props );
                drawActions.addDrawFigure( figure );
                figure.repaint();
            }
        }
    }

    protected void savePlot( Element rootElement )
    {
        if ( plotSource != null ) {
            Plot plot = plotSource.getPlot();
            if ( plot != null ) {
                if ( astWriter == null ) {
                    astWriter = new XAstWriter();
                }
                Element plotElement =
                    PrimitiveXMLEncodeDecode.addChildElement( rootElement,
                                                              PLOT_TAG );
                plotElement.setAttribute( "encoding", "AST-XML" );
                Result result = new DOMResult( plotElement );
                Source source = astWriter.makeSource( plot );
                Transformer trans = new SourceReader().getTransformer();
                try {
                    trans.transform( source, result );
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected Plot restorePlot( Element rootElement )
    {
        if ( astReader == null ) {
            astReader = new XAstReader();
        }
        List children =
            PrimitiveXMLEncodeDecode.getChildElements( rootElement );
        Element plotElement = (Element) children.get( 0 );

        try {
            return (Plot) astReader.makeAst( plotElement );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
