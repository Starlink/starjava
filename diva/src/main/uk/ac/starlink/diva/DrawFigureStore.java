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
import uk.ac.starlink.util.PrimitiveXMLEncodeDecode;
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
    /**
     * Name of the application (used to generate directory for backing
     * store).
     */
    private String application = null;

    /**
     * Name of the backing store file (lives in application specific
     * directory).
     */
    private String storeFile = null;

    /**
     * Name for the tag used to parent all the stored XML.
     */
    private String tagName = null;

    /**
     * UI used for storing and restoring XML configurations.
     */
    private StoreControlFrame store = null;

    /**
     * The object that controls the graphical figures.
     */
    protected DrawActions drawActions = null;

    /**
     * Factory for creating figures.
     */
    protected DrawFigureFactory figureFactory = 
        DrawFigureFactory.getReference();

    /**
     * Create an instance.
     */
    public DrawFigureStore( String application, String storeFile,
                            String tagName )
    {
        this.application = application;
        this.storeFile = storeFile;
        this.tagName = tagName;
    }

    /**
     * Save all the figures currently held by the {@link DrawActions} to the
     * given root element.
     */
    public void saveState( Element rootElement )
    {
        if ( drawActions != null ) {
            ListIterator it = drawActions.getListIterator( false );
            while ( it.hasPrevious() ) {
                DrawFigure fig = (DrawFigure) it.previous();
                FigureProps props = figureFactory.getFigureProps( fig );
                props.encode( PrimitiveXMLEncodeDecode
                              .addChildElement( rootElement,
                                                props.getTagName() ) );
            }
        }
    }

    /**
     * Restore a previous saved state to the {@link DrawActions}
     * object. These are then repainted by any GraphicsPanes that are
     * associated with the {@link DrawActions} instance.
     */
    public void restoreState( Element rootElement )
    {
        List children =
            PrimitiveXMLEncodeDecode.getChildElements( rootElement );
        int size = children.size();
        Element element = null;
        String name = null;
        FigureProps props = null;
        DrawFigure figure = null;
        for ( int i = 0; i < size; i++ ) {
            props = new FigureProps();
            element = (Element) children.get( i );
            name = PrimitiveXMLEncodeDecode.getElementName( element );
            if ( props.getTagName().equals( name ) ) {
                props.decode( element );
                figure = figureFactory.create( props );
                drawActions.addDrawFigure( figure );
                figure.repaint();
            }
        }
    }

    /** Return the application name associated with this store */
    public String getApplicationName()
    {
        return application;
    }

    /** Return the file store name associated with this store */
    public String getStoreName()
    {
        return storeFile;
    }

    /** Return the root element tag associated with this store */
    public String getTagName()
    {
        return tagName;
    }

    /** Set the instance of {@link DrawActions} to use with this store */
    public void setDrawActions( DrawActions drawActions )
    {
        this.drawActions = drawActions;
    }

    /** Make the store control UI active */
    public void activate()
    {
        if ( store == null ) {
            store = new StoreControlFrame( this );
        }
        store.setVisible( true );
    }
}
