package uk.ac.starlink.ttools.plottask;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Executable;
import uk.ac.starlink.task.IntegerParameter;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot.AuxLegend;
import uk.ac.starlink.ttools.plot.GraphicExporter;
import uk.ac.starlink.ttools.plot.Legend;
import uk.ac.starlink.ttools.plot.PlotData;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot.TablePlot;
import uk.ac.starlink.ttools.task.InputTableSpec;

/**
 * Abstract superclass for tasks which generate plots from table data.
 *
 * @author   Mark Taylor
 * @since    22 Apr 2008
 */
public abstract class PlotTask implements Task {

    private final String purpose_;
    private final PlotStateFactory stateFactory_;
    private final TablePlot plot_;
    private final List paramList_;
    private final IntegerParameter xpixParam_;
    private final IntegerParameter ypixParam_;
    private final BooleanParameter legendParam_;
    private final FontParameter fontParam_;
    private final PaintModeParameter painterParam_;
    private final Parameter titleParam_;

    /**
     * Constructor.
     *
     * @param  purpose  short (one-line) task purpose
     * @param  stateFactory   PlotStateFactory for use with this plot
     * @param  plot   plot component for use with this plot
     */
    public PlotTask( String purpose, PlotStateFactory stateFactory,
                     TablePlot plot ) {
        purpose_ = purpose;
        stateFactory_ = stateFactory;
        plot_ = plot;
        paramList_ = new ArrayList();

        xpixParam_ = new IntegerParameter( "xpix" );
        xpixParam_.setMinimum( 1 );
        xpixParam_.setPrompt( "Width of plot in pixels" );
        xpixParam_.setDescription( new String[] {
            "<p>The width of the output graphic in pixels.",
            "</p>",
        } );
        xpixParam_.setDefault( "400" );
        paramList_.add( xpixParam_ );

        ypixParam_ = new IntegerParameter( "ypix" );
        ypixParam_.setMinimum( 1 );
        ypixParam_.setPrompt( "Height of plot in pixels" );
        ypixParam_.setDescription( new String[] {
            "<p>The height of the output graphic in pixels.",
            "</p>",
        } );
        ypixParam_.setDefault( "300" );
        paramList_.add( ypixParam_ );

        fontParam_ = new FontParameter( "font" );
        paramList_.add( fontParam_ );
        paramList_.addAll( Arrays
                          .asList( fontParam_.getAssociatedParameters() ) );

        legendParam_ = new BooleanParameter( "legend" );
        legendParam_.setPrompt( "Whether to include legend" );
        legendParam_.setDescription( new String[] {
            "<p>Determines whether a legend showing which plotting style is",
            "used for each data set.",
            "Defaults to true if there is more than one set, false otherwise.",
            "</p>",
        } );
        paramList_.add( legendParam_ );

        titleParam_ = new Parameter( "title" );
        titleParam_.setPrompt( "Plot title" );
        titleParam_.setNullPermitted( true );
        titleParam_.setDescription( new String[] {
            "<p>A one-line title to display at the top of the plot.",
            "</p>",
        } );
        paramList_.add( titleParam_ );

        painterParam_ = new PaintModeParameter( "omode" );
        paramList_.add( painterParam_ );
        paramList_.add( painterParam_.getOutputParameter() );
        paramList_.add( painterParam_.getFormatParameter() );

        paramList_.addAll( Arrays.asList( stateFactory_.getParameters() ) );
    }

    protected List getParameterList() {
        return paramList_;
    }

    public String getPurpose() {
        return purpose_;
    }

    public Parameter[] getParameters() {
        return (Parameter[]) paramList_.toArray( new Parameter[ 0 ] );
    }

    /**
     * Returns the parameter used for setting plot output X dimension.
     *
     * @return  xpix parameter
     */
    public Parameter getXpixParameter() {
        return xpixParam_;
    }

    /**
     * Returns the parameter used for setting plot output Y dimension.
     *
     * @return  ypix parameter
     */
    public Parameter getYpixParameter() {
        return ypixParam_;
    }

    public Executable createExecutable( Environment env ) throws TaskException {
        final int xpix = xpixParam_.intValue( env );
        final int ypix = ypixParam_.intValue( env );
        final PlotState state = stateFactory_.getPlotState( env );
        legendParam_.setDefault( state.getPlotData().getSetCount() > 1
                                      ? "true"
                                      : "false" );
        final boolean hasLegend = legendParam_.booleanValue( env );
        final Painter painter = painterParam_.painterValue( env );
        final Font font = fontParam_.fontValue( env );
        final String title = titleParam_.stringValue( env );
        return new Executable() {
            public void execute() throws TaskException, IOException {
                stateFactory_.configureFromData( state, plot_ );
                state.setValid( true );
                plot_.setState( state );
                plot_.setFont( font );

                /* Add legend if required. */
                JComponent box =
                    addDecoration( plot_, state, hasLegend, title );

                /* Set the size for the output image. */
                Dimension size = new Dimension( xpix, ypix );

                /* If we are actually displaying this to the screen, handle
                 * it specially.  As well as being more direct, this is
                 * necessary to make sure that window resizing works. */
                if ( painter instanceof SwingPainter ) {
                    box.setPreferredSize( size );
                    ((SwingPainter) painter).postComponent( box );
                }

                /* Otherwise generate graphics which is a copy of the window
                 * content and export it.  Som magic is required in this case
                 * to get a Swing component laid out and painted when it
                 * is not actually present on screen.
                 * See, e.g., Sun's Java bugs #4356383, #4639354, #4520228. */
                else {
                    box.setSize( size );
                    box.setDoubleBuffered( false );  // is this sensible??
                    box.addNotify();
                    box.validate();
                    painter.paintPicture( GraphicExporter.toPicture( box ) );
                }
            }
        };
    }

    /**
     * Adds legend-type material to a plot.
     *
     * @param   plot  table plot component
     * @param   state  plot state
     * @param   hasLegend  whether the plot should feature a normal legend
     * @param   title   plot title
     * @return   output component; may or may not be the same as 
     *           <code>plot</code>
     */
    private static JComponent addDecoration( final TablePlot plot,
                                             PlotState state, boolean hasLegend,
                                             String title ) {

        /* Return if no legend material is required. */
        int naux = state.getShaders().length;
        if ( naux == 0 && ! hasLegend && title == null ) {
            return plot;
        }

        /* Otherwise set up a new component containing the given plot. */
        JComponent box = new JPanel( new BorderLayout() );
        Font font = plot.getFont();
        box.setFont( font );
        box.setOpaque( false );
        box.add( plot, BorderLayout.CENTER );

        /* Add a title. */
        if ( title != null ) {
            JLabel tLabel = new JLabel( title, SwingConstants.CENTER );
            tLabel.setFont( font );
            tLabel.setOpaque( false );
            box.add( tLabel, BorderLayout.NORTH );
        }

        /* Prepare and place a component to host legend-type material. */
        JComponent legendBox = Box.createVerticalBox();
        legendBox.setFont( font );
        int topgap = plot.getPlotBounds().y;
        int auxgap = 10;
        int botgap = 37;  // hack hack hack - should come from plot component
        legendBox.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 5 ) );
        box.add( legendBox, BorderLayout.EAST );

        /* Add an actual legend if one is required. */
        if ( hasLegend ) {
            PlotData plotData = state.getPlotData();
            int nset = plotData.getSetCount();
            Style[] styles = new Style[ nset ];
            String[] labels = new String[ nset ];
            for ( int is = 0; is < nset; is++ ) {
                styles[ is ] = plotData.getSetStyle( is );
                labels[ is ] = plotData.getSetName( is );
            }
            Legend legend = new Legend();
            Border border =
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder( Color.BLACK ),
                    BorderFactory.createEmptyBorder( 5, 5, 5, 10 ) );
            legend.setBorder( border );
            legendBox.add( Box.createVerticalStrut( topgap ) );
            legendBox.add( legend );
            legend.setStyles( styles, labels );
        }

        /* Add auxiliary axis keys if required. */
        if ( naux > 0 ) {
            Box auxBox = Box.createHorizontalBox();
            legendBox.add( auxBox );
            for ( int iaux = 0; iaux < naux; iaux++ ) {
                AuxLegend auxLegend = new AuxLegend( false, 16 );
                auxLegend.setLengthPadding( hasLegend ? auxgap : topgap,
                                            botgap );
                if ( iaux > 0 ) {
                    auxBox.add( Box.createHorizontalStrut( 10 ) );
                }
                auxBox.add( auxLegend );
                auxLegend.configure( state, iaux );
            }
            auxBox.add( Box.createHorizontalGlue() );
        }

        /* Return the component containing plot and legend. */
        return box;
    }
}
