/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     23-JAN-2004 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.diva;

import org.w3c.dom.Element;
import uk.ac.starlink.util.gui.StoreSource;
import uk.ac.starlink.util.gui.StoreControlFrame;
import uk.ac.starlink.util.PrimitiveXMLEncodeAndDecode;
import java.util.ListIterator;
import java.util.List;

/**
 * Implementation of a {@link StoreSource} as a {@link FigureStore} to save 
 * and restore figures from an application specific backing store file using 
 * a {@link StoreControlFrame}.
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
public class DrawFigureStore
    implements FigureStore, StoreSource
{
    private String application = null;
    private String storeFile = null;
    private String tagName = null;
    private StoreControlFrame store = null;
    private DrawActions drawActions = null;
    private DrawFigureFactory figureFactory = DrawFigureFactory.getReference();

    public DrawFigureStore( String application, String storeFile,
                            String tagName )
    {
        this.application = application;
        this.storeFile = storeFile;
        this.tagName = tagName;
    }

    public void saveState( Element rootElement )
    {
        if ( drawActions != null ) {
            DrawFigureFactory fact = DrawFigureFactory.getReference();
            ListIterator it = drawActions.getFigureList().listIterator(0);
            while ( it.hasNext() ) {
                DrawFigure fig = (DrawFigure) it.next();
                FigureProps props = fact.getFigureProps( fig );
                props.encode( PrimitiveXMLEncodeAndDecode
                              .addChildElement( rootElement, 
                                                props.getTagName() ) );
            }
        }
    }

    public void restoreState( Element rootElement )
    {
        List children =
            PrimitiveXMLEncodeAndDecode.getChildElements( rootElement );
        int size = children.size();
        Element element = null;
        String name = null;
        FigureProps props = null;
        DrawFigure figure = null;
        for ( int i = 0; i < size; i++ ) {
            props = new FigureProps();
            element = (Element) children.get( i );
            name = PrimitiveXMLEncodeAndDecode.getElementName( element );
            if ( props.getTagName().equals( name ) ) {
                props.decode( element );
                figure = figureFactory.create( props );
                drawActions.addDrawFigure( figure );
                figure.repaint();
            }
        }
    }

    public String getApplicationName()
    {
        return application;
    }

    public String getStoreName()
    {
        return storeFile;
    }

    public String getTagName()
    {
        return tagName;
    }

    public void setDrawActions( DrawActions drawActions )
    {
        this.drawActions = drawActions;
    }

    public void activate()
    {
        if ( store == null ) {
            store = new StoreControlFrame( this );
        }
        store.setVisible( true );
    }
}
