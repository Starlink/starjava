/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     21-OCT-2002 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.sog.photom;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableModel;

import jsky.coords.CoordinateConverter;
import jsky.coords.WorldCoordinateConverter;
import jsky.graphics.CanvasFigure;
import jsky.graphics.CanvasFigureEvent;
import jsky.graphics.CanvasFigureListener;
import jsky.graphics.CanvasGraphics;
import jsky.image.graphics.DivaImageGraphics;
import jsky.util.gui.DialogUtil;

import diva.canvas.Figure;
import diva.canvas.interactor.SelectionInteractor;

import uk.ac.starlink.sog.SOGNavigatorImageDisplay;
import uk.ac.starlink.sog.AstTransform;
import uk.ac.starlink.sog.ExceptionDialog;

/**
 * Provide a panel of controls for performing aperture photometry.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class AperturePhotometry
    extends JPanel
    implements CanvasFigureListener, ChangeListener
{
    /** The top level parent frame (or internal frame) used to close
        the window */
    private Component parent;

    /** The main image display window */
    private SOGNavigatorImageDisplay imageDisplay;

    /** List for storing references to the apertures */
    private PhotomList photomList = new PhotomList();

    /** Model for storing global parameters */
    private PhotometryGlobals globals = new PhotometryGlobals();

    /** The SOGCanvasDraw being used by the image */
    private SOGCanvasDraw sogCanvasDraw;

    /** The list of figures that we've created */
    private ArrayList figureList = new ArrayList();

    /** Set when the creation is started */
    private boolean creationStarted = false;

    /** Create an instance */
    public AperturePhotometry( Component parent,
                               SOGNavigatorImageDisplay imageDisplay )
    {
        super();
        this.parent = parent;
        this.imageDisplay = imageDisplay;

        setLayout( new BorderLayout() );

        add( makeViewPane(), BorderLayout.CENTER );
        add( makeButtonPanel(), BorderLayout.SOUTH );

        sogCanvasDraw = (SOGCanvasDraw) imageDisplay.getCanvasDraw();
        sogCanvasDraw.addFinishedListener( this );
    }

    /**
     * Make and return the "view" panel. This contains the details of
     * the current aperture.
     */
    private JTabbedPane makeViewPane()
    {
        JTabbedPane pane = new JTabbedPane();

        //  Add a page for the aperture details and add a controller
        //  for displaying and changing the current aperture.
        ApertureController controller = new ApertureController( photomList  );
        pane.add( controller, "Aperture" );

        //  Add a page for displaying and controlling the global parameters.
        PhotometryGlobalsView globalsView = 
            new PhotometryGlobalsView( globals );
        pane.add( globalsView, "Parameters" );

        //  Add a page to show the details of all apertures.
        JTable table = new JTable(new PhotomListTableModel( photomList ));
        table.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        JScrollPane scrollPane = new JScrollPane( table );
        pane.add( scrollPane, "Results" );

        //  We manage the Figures so we need to be informed of changes
        //  that should be displayed.
        photomList.addChangeListener( this );

        return pane;
    }

   /**
     * Make the button panel for issuing control directives.
     */
    private JPanel makeButtonPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout( new FlowLayout( FlowLayout.RIGHT ) );

        JButton addButton = new JButton( "Add Aperture" );
        panel.add( addButton );
        addButton.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent ev )
                {
                    addAperture();
                }
            });

        JButton measureButton = new JButton( "Calculate Results" );
        panel.add( measureButton );
        measureButton.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    doCalculations();
                }
            });

        JButton closeButton = new JButton( "Close" );
        panel.add( closeButton );
        closeButton.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent ev )
                {
                    close();
                }
            });
        return panel;
    }

    /**
     * Withdraw the parent window, and the related figures.
     */
    protected void close()
    {
        if ( parent != null ) {
            parent.setVisible( false );
        }
        //hideFigures();
    }

    /**
     * Get a reference to the PhotomList.
     */
    public PhotomList getPhotomList()
    {
        return photomList;
    }

    /**
     * Get a reference to the global parameters model.
     */
    public PhotometryGlobals getGlobals()
    {
        return globals;
    }

    /**
     * Get a reference to the list of figures.
     */
    public ArrayList getFigureList()
    {
        return figureList;
    }

    /**
     * Interactively add a new aperture.
     */
    public void addAperture()
    {
        // Grab the CanvasDraw object and switch it to ellipse
        // mode.
        sogCanvasDraw.setDrawingMode( SOGCanvasDraw.ANNULAR_CIRCLE );
        creationStarted = true;
    }

    //
    // Implement ChangeListener interface for receiving notification
    // when the ellipses are complete.
    //

    /**
     * Handle any sources of ChangeEvents. These can come from two
     * sources, the PhotomList and SOGCanvasDraw. Events come from the
     * PhotomList when is it changed and from the SOGCanvasDraw when
     * figures are being created.
     */
    public void stateChanged( ChangeEvent e )
    {
        Object source = e.getSource();
        if ( source instanceof PhotomList ) {
            if ( ! creationStarted ) {
                updateFigureList();
            }
        }
        else if ( source instanceof AnnulusFigure ) {
            CanvasFigure figure = (CanvasFigure) source;
            AnnulusPhotom annulusPhotom = null;
            if ( creationStarted ) {
                figure.addCanvasFigureListener( this );
                
                //  Create a AnnulusPhotom object as the model to the
                //  figure values.
                annulusPhotom = new AnnulusPhotom();
                photomList.add( annulusPhotom );
                
                //  Make Figure manageable.
                figureList.add( figure );
                figure.setClientData( annulusPhotom );
                creationStarted = false;
            }
            else {
                annulusPhotom = extractAperture( (AnnulusFigure) figure );
            }
            
            // Figure moved so re-size the model.
            matchModelToFigure( (AnnulusFigure) figure, annulusPhotom,
                                true );
        }
    }

    /**
     * bad: Make a figure managed by this instance.
     */
    protected void setupFigure( CanvasFigure figure, 
                                AnnulusPhotom annulusPhotom )
    {
        //  Respond to direct changes.
        figure.removeCanvasFigureListener( this );
        figure.addCanvasFigureListener( this );
        
        //  Remember it.
        //figureList.add( figure );

        // Retain a reference to the associated AnnulusPhotom 
        figure.setClientData( annulusPhotom );
    } 

    /**
     * Remove an aperture.
     */
    public void remove( int index )
    {
        removeFigure( index );
        photomList.remove( index );
    }

    /**
     * Remove a Figure from display by index. Doesn't effect the
     * PhotomList.
     */
    protected void removeFigure( int index )
    {
        AnnulusFigure figure = (AnnulusFigure) figureList.get( index );
        sogCanvasDraw.removeFigure( figure );
        figureList.remove( index );
    }

    //
    // Implement the CanvasFigureListener interface.
    //

    /**
     * Invoked when the figure is selected.
     */
    public void figureSelected( CanvasFigureEvent e )
    {
        AnnulusPhotom aperture = extractAperture( e );
        if ( aperture != null ) {
            makeCurrent( aperture );
        }
    }

    protected AnnulusPhotom extractAperture( CanvasFigureEvent e )
    {
        AnnulusFigure figure = extractFigure( e );
        if ( figure != null ) {
            return extractAperture( figure );
        }
        return null;
    }

    protected AnnulusPhotom extractAperture( AnnulusFigure figure )
    {
        if ( figure.getClientData() instanceof AnnulusPhotom ) {
            return (AnnulusPhotom) figure.getClientData();
        }
        return null;
    }

    protected AnnulusFigure extractFigure( CanvasFigureEvent e )
    {
        if ( e.getSource() instanceof AnnulusFigure ) {
            return (AnnulusFigure) e.getSource();
        }
        return null;
    }

    protected int makeCurrent( AnnulusPhotom aperture )
    {
        int index = photomList.indexOf( aperture );
        if ( index != -1 ) {
            photomList.setCurrent( index );
        }
        return index;
    }

    /**
     * Invoked when the figure is deselected.
     */
    public void figureDeselected( CanvasFigureEvent e )
    {
        // Nothing to do.
    }

    /**
     * Invoked when the figure's size changes.
     */
    public void figureResized( CanvasFigureEvent e )
    {
        AnnulusPhotom aperture = extractAperture( e );
        if ( aperture != null ) {
            // Match aperture details to model.
            matchModelToFigure( extractFigure( e ), aperture, true );
        }
    }

    /**
     * Match the details of an AnnulusPhotom object to those of the
     * related AnnulusFigure.
     */
    protected void matchModelToFigure( AnnulusFigure figure,
                                       AnnulusPhotom model,
                                       boolean makeCurrent )
    {
        if ( model != null && figure != null ) {

            // Get the coordinates. Need to transform from canvas to
            // image.
            Point2D.Double cxy = figure.getPosition();
            double cr = figure.getRadius();
            Point2D.Double rad = new Point2D.Double( cr, cr );
            double cinner = figure.getInnerscale();
            double couter = figure.getOuterscale();

            //  Convert to image coordinates.
            CoordinateConverter cc = imageDisplay.getCoordinateConverter();
            cc.screenToImageCoords( cxy, false );
            cc.screenToImageCoords( rad, true );

            // If possible convert to pixel coordinates.
            WorldCoordinateConverter wcc = imageDisplay.getWCS();
            if ( wcc instanceof AstTransform ) {
                AstTransform astWCS = (AstTransform) wcc;
                int current = astWCS.getCurrent();
                astWCS.setDomain( "PIXEL" );
                wcc.imageToWorldCoords( cxy, false );
                astWCS.setCurrent( current );
            }
            else {
                cxy.x = cxy.x - 1.5;
                cxy.y = cxy.y - 1.5;
            }

            model.setXcoord( cxy.x );
            model.setYcoord( cxy.y );
            model.setSemimajor( rad.x );
            model.setSemiminor( rad.y );
            model.setInnerscale( cinner );
            model.setOuterscale( couter );
            if ( makeCurrent ) {
                makeCurrent( model );
            }
        }
    }

    /**
     * Match the details of an AnnulusFigure to the related
     * AnnulusPhotom model.
     */
    protected void matchFigureToModel( AnnulusPhotom model,
                                       AnnulusFigure figure,
                                       boolean makeCurrent )
    {
        if ( model != null && figure != null ) {

            // Get the coordinates. Need to transform from image to
            // canvas.
            Point2D.Double xy =
                new Point2D.Double( model.getXcoord(), model.getYcoord() );
            double imrad = model.getSemimajor();
            Point2D.Double rad = new Point2D.Double( imrad, imrad );

            // If possible convert from pixel coordinates.
            WorldCoordinateConverter wcc = imageDisplay.getWCS();
            if ( wcc instanceof AstTransform ) {
                AstTransform astWCS = (AstTransform) wcc;
                int current = astWCS.getCurrent();
                astWCS.setDomain( "PIXEL" );  // Should check PIXEL exists?
                wcc.worldToImageCoords( xy, false );
                astWCS.setCurrent( current );
            }
            else {
                xy.x = xy.x + 1.5;
                xy.y = xy.y + 1.5;
            }

            CoordinateConverter cc = imageDisplay.getCoordinateConverter();
            cc.imageToScreenCoords( xy, false );
            cc.imageToScreenCoords( rad, true );

            figure.setPosition( xy );
            figure.setRadius( rad.x );
            figure.setInnerscale( model.getInnerscale() );
            figure.setOuterscale( model.getOuterscale() );

            //figure.setClientData( model );
            if ( makeCurrent ) {
                makeCurrent( model );
            }
        }
    }

    /**
     * Invoked when the figure's position changes.
     */
    public void figureMoved( CanvasFigureEvent e )
    {
        // Update configuration
        figureResized( e );
    }

    /**
     * Calculate the aperture photometry for all results
     */
    public void doCalculations()
    {
        if ( photomList.size() > 0 ) {
            try {
                PhotometryWorker worker = new PhotometryWorker();
                worker.calculate( imageDisplay.getCurrentNdx(),
                                  photomList, globals, this );
            }
            catch (Exception e) {
                new ExceptionDialog( this, e );
            }
        }
        else {
            DialogUtil.error( "No apertures are defined" );
        }
    }

    /**
     * Called by the PhotometryWorker when the calculations are
     * complete. This means that the photomList has been updated with
     * the calculations and we need to arrange for this to be shown in
     * the figures.
     */
    public void calculationsDone()
    {
        //  OK, update the figure list. There should still be a
        //  one-to-one correspondence, but all connections have been
        //  lost between the model and the figures.
        updateFigureList();
    }

    /**
     * Modify any displayed figures to match the values in the
     * PhotomList.
     */
    public void updateFigureList()
    {
        AnnulusFigure figure = null;
        AnnulusPhotom photom = null;
        Paint fill = sogCanvasDraw.getFill();
        Paint outline = sogCanvasDraw.getOutline();
        float lineWidth = sogCanvasDraw.getLineWidth();
        DivaImageGraphics canvasGraphics =
            (DivaImageGraphics) imageDisplay.getCanvasGraphics();
        SelectionInteractor interactor =
            canvasGraphics.getSelectionInteractor();

        for( int i = 0; i < photomList.size(); i++ ) {
            if ( figureList.size() <= i ) {
                figure = new AnnulusFigure( 10.0, fill, outline,
                                            lineWidth, interactor );
            }
            else {
                figure = (AnnulusFigure) figureList.get( i );
            }
            photom = (AnnulusPhotom) photomList.get( i );
            setupFigure( figure, photom );
            matchFigureToModel( photom, figure, false );
        }

        // Clear the selection and make it the current figure.
        canvasGraphics.clearSelection();
        int index = photomList.indexOf( photomList.getCurrent() );
        canvasGraphics.select( (AnnulusFigure) figureList.get( index ) );

        if ( figureList.size() > photomList.size() ) {
            // Trim excess, shouldn't happen really.
            System.out.println( "trimmed excess figures..." );
            for ( int i = figureList.size()-1; i >= photomList.size(); i-- ) {
                removeFigure( i );
            }
        }
    }
}
