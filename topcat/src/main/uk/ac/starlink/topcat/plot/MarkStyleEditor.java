package uk.ac.starlink.topcat.plot;

import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.ttools.gui.ColorComboBox;
import uk.ac.starlink.ttools.gui.DashComboBox;
import uk.ac.starlink.ttools.gui.MarkStyleSelectors;
import uk.ac.starlink.ttools.gui.ThicknessComboBox;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.ErrorModeSelection;
import uk.ac.starlink.ttools.plot.ErrorRenderer;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot.XYStats;
import uk.ac.starlink.util.gui.ComboBoxBumper;
import uk.ac.starlink.util.gui.ShrinkWrapper;
import uk.ac.starlink.util.gui.ValueButtonGroup;

/**
 * StyleEditor implementation for editing {@link MarkStyle} objects.
 *
 * @author   Mark Taylor
 * @since    10 Jan 2005
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class MarkStyleEditor extends StyleEditor {

    private final JCheckBox markFlagger_;
    private final JComboBox shapeSelector_;
    private final JComboBox sizeSelector_;
    private final ColorComboBox colorSelector_;
    private final LogSlider opaqueSlider_;
    private final ThicknessComboBox thickSelector_;
    private final DashComboBox dashSelector_;
    private final ValueButtonGroup<MarkStyle.Line> lineSelector_;
    private final JComboBox errorSelector_;
    private final ErrorModeSelectionModel[] errorModeModels_;
    private final JComponent corrBox_;
    private final Map<SetId,XYStats> statMap_;
    private final String helpId_;

    private static final int MAX_SIZE = 5;
    private static final int MAX_THICK = 10;
    private static final MarkShape[] SHAPES = new MarkShape[] {
        MarkShape.FILLED_CIRCLE,
        MarkShape.OPEN_CIRCLE,
        MarkShape.CROSS,
        MarkShape.CROXX,
        MarkShape.OPEN_SQUARE,
        MarkShape.OPEN_DIAMOND,
        MarkShape.OPEN_TRIANGLE_UP,
        MarkShape.OPEN_TRIANGLE_DOWN,
        MarkShape.FILLED_SQUARE,
        MarkShape.FILLED_DIAMOND,
        MarkShape.FILLED_TRIANGLE_UP,
        MarkShape.FILLED_TRIANGLE_DOWN,
    };

    /**
     * Constructs a style editor with optional error style selection.
     *
     * @param   withLines  whether to show a panel for selecting line styles
     * @param   withTransparency  whether to show a control for selecting
     *          marker opacity
     * @param   errorRenderers  array of error renderers to choose from
     * @param   defaultRenderer  default error renderer to use if no other
     *          is known
     * @param   errorModeModels  array of error mode selectors,
     *          one per dimension of the error bars
     */
    @SuppressWarnings("this-escape")
    public MarkStyleEditor( boolean withLines, boolean withTransparency,
                            ErrorRenderer[] errorRenderers,
                            ErrorRenderer defaultRenderer,
                            ErrorModeSelectionModel[] errorModeModels ) {
        super();
        statMap_ = new HashMap<SetId,XYStats>();
        boolean withErrors = errorModeModels.length > 0;
        helpId_ = withLines ? "MarkStyleEditor" : "MarkStyleEditorNoLines";

        /* Shape selector. */
        shapeSelector_ = MarkStyleSelectors.createShapeSelector();
        shapeSelector_.addActionListener( this );

        /* Size selector. */
        sizeSelector_ = MarkStyleSelectors.createSizeSelector();
        sizeSelector_.addActionListener( this );

        /* Colour selector. */
        colorSelector_ = new ColorComboBox();
        colorSelector_.addActionListener( this );

        /* Opacity limit slider. */
        opaqueSlider_ = new LogSlider( 1000 );
        final JLabel opaqueLabel = new JLabel( "", SwingConstants.RIGHT );
        opaqueSlider_.addChangeListener( new ChangeListener() {
            Dimension size;
            public void stateChanged( ChangeEvent evt ) {
                if ( size == null ) {
                    opaqueLabel.setText( "9999" );
                    size = opaqueLabel.getPreferredSize();
                }
                opaqueLabel.setText( Integer.toString( opaqueSlider_
                                                      .getValue1() ) );
                opaqueLabel.setPreferredSize( size );
                if ( ! opaqueSlider_.getValueIsAdjusting() ) {
                    MarkStyleEditor.this.stateChanged( evt );
                }
            }
        } );

        /* Error renderer selector. */
        errorModeModels_ = errorModeModels;
        errorSelector_ = MarkStyleSelectors
                        .createErrorSelector( errorRenderers, defaultRenderer,
                                              errorModeModels );
        for ( int idim = 0; idim < errorModeModels_.length; idim++ ) {
            errorModeModels_[ idim ].addActionListener( this );
        }
        errorSelector_.addActionListener( this );

        /* Marker hiding selector. */
        markFlagger_ = new JCheckBox( "Hide Markers" );
        markFlagger_.setSelected( false );
        markFlagger_.addActionListener( this );

        /* Line thickness selector. */
        thickSelector_ = new ThicknessComboBox( MAX_THICK );
        thickSelector_.addActionListener( this );

        /* Line dash selector. */
        dashSelector_ = new DashComboBox();
        dashSelector_.addActionListener( this );

        /* Line type selector. */
        JRadioButton noneButton = new JRadioButton( "None", true );
        JRadioButton dotsButton = new JRadioButton( "Dot to Dot" );
        JRadioButton corrButton = new JRadioButton( "Linear Correlation" );
        lineSelector_ = new ValueButtonGroup<MarkStyle.Line>();
        lineSelector_.add( noneButton, null );
        lineSelector_.add( dotsButton, MarkStyle.DOT_TO_DOT );
        lineSelector_.add( corrButton, MarkStyle.LINEAR );
        lineSelector_.addChangeListener( this );
        corrBox_ = Box.createHorizontalBox();

        /* Place marker selection components. */
        JComponent formBox = Box.createHorizontalBox();
        formBox.add( new JLabel( "Shape: " ) );
        formBox.add( new ShrinkWrapper( shapeSelector_ ) );
        formBox.add( Box.createHorizontalStrut( 5 ) );
        formBox.add( new ComboBoxBumper( shapeSelector_ ) );
        formBox.add( Box.createHorizontalStrut( 10 ) );
        formBox.add( new JLabel( "Size: " ) );
        formBox.add( new ShrinkWrapper( sizeSelector_ ) );
        formBox.add( Box.createHorizontalStrut( 5 ) );
        formBox.add( new ShrinkWrapper( new ComboBoxBumper( sizeSelector_ ) ) );
        formBox.add( Box.createHorizontalStrut( 5 ) );
        formBox.add( Box.createHorizontalGlue() );

        JComponent colorBox = Box.createHorizontalBox();
        colorBox.add( new JLabel( "Colour: " ) );
        colorBox.add( new ShrinkWrapper( colorSelector_ ) );
        colorBox.add( Box.createHorizontalStrut( 5 ) );
        colorBox.add( new ComboBoxBumper( colorSelector_ ) );
        colorBox.add( Box.createHorizontalStrut( 5 ) );
        colorBox.add( Box.createHorizontalGlue() );

        JComponent opaqueBox = Box.createHorizontalBox();
        opaqueBox.add( new JLabel( "Transparency: " ) );
        opaqueBox.add( opaqueSlider_ );
        opaqueBox.add( Box.createHorizontalStrut( 5 ) );
        opaqueBox.add( opaqueLabel );
        opaqueBox.add( Box.createHorizontalStrut( 5 ) );
        opaqueBox.add( Box.createHorizontalGlue() );

        JComponent errorBox = Box.createHorizontalBox();
        errorBox.add( new JLabel( "Error Bars: " ) );
        errorBox.add( new ShrinkWrapper( errorSelector_ ) );
        errorBox.add( Box.createHorizontalStrut( 5 ) );
        errorBox.add( new ComboBoxBumper( errorSelector_ ) );
        errorBox.add( Box.createHorizontalStrut( 5 ) );
        errorBox.add( Box.createHorizontalGlue() );

        JComponent hideBox = Box.createHorizontalBox();
        hideBox.add( markFlagger_ );
        hideBox.add( Box.createHorizontalGlue() );

        JComponent markBox = Box.createVerticalBox();
        markBox.add( formBox );
        markBox.add( Box.createVerticalStrut( 5 ) );
        markBox.add( colorBox );
        if ( withTransparency ) {
            markBox.add( Box.createVerticalStrut( 5 ) );
            markBox.add( opaqueBox );
        }
        if ( withErrors ) {
            markBox.add( Box.createVerticalStrut( 5 ) );
            markBox.add( errorBox );
        }
        markBox.add( Box.createVerticalStrut( 5 ) );
        markBox.add( hideBox );
        markBox.setBorder( AuxWindow.makeTitledBorder( "Marker" ) );
        add( markBox );

        /* Place line selection components if required. */
        if ( withLines ) {
            Box lineStyleBox = Box.createHorizontalBox();
            lineStyleBox.add( new JLabel( "Thickness: " ) );
            lineStyleBox.add( new ShrinkWrapper( thickSelector_ ) );
            lineStyleBox.add( Box.createHorizontalStrut( 5 ) );
            lineStyleBox.add( new ComboBoxBumper( thickSelector_ ) );
            lineStyleBox.add( Box.createHorizontalStrut( 10 ) );
            lineStyleBox.add( new JLabel( "Dash: " ) );
            lineStyleBox.add( new ShrinkWrapper( dashSelector_ ) );
            lineStyleBox.add( Box.createHorizontalStrut( 5 ) );
            lineStyleBox.add( new ComboBoxBumper( dashSelector_ ) );
            lineStyleBox.add( Box.createHorizontalStrut( 5 ) );
            lineStyleBox.add( Box.createHorizontalGlue() );

            Box noneLineBox = Box.createHorizontalBox();
            noneLineBox.add( noneButton );
            noneLineBox.add( Box.createHorizontalGlue() );
            Box dotsLineBox = Box.createHorizontalBox();
            dotsLineBox.add( dotsButton );
            dotsLineBox.add( Box.createHorizontalGlue() );
            Box corrLineBox = Box.createHorizontalBox();
            corrLineBox.add( corrButton );
            corrLineBox.add( corrBox_ );
            corrLineBox.add( Box.createHorizontalGlue() );

            Box lineBox = Box.createVerticalBox();
            lineBox.add( lineStyleBox );
            lineBox.add( noneLineBox );
            lineBox.add( dotsLineBox );
            lineBox.add( corrLineBox );
            lineBox.setBorder( AuxWindow.makeTitledBorder( "Line" ) );
            add( lineBox );
        }
    }

    public void setStyle( Style style ) {
        MarkStyle mstyle = (MarkStyle) style;
        shapeSelector_.setSelectedItem( mstyle.getShapeId() );
        sizeSelector_.setSelectedIndex( mstyle.getSize() );
        colorSelector_.setSelectedItem( mstyle.getColor() );
        int opaqueLimit = mstyle.getOpaqueLimit();
        opaqueSlider_.setValue1( opaqueLimit );
        thickSelector_.setSelectedThickness( mstyle.getLineWidth() );
        dashSelector_.setSelectedDash( mstyle.getDash() );
        lineSelector_.setValue( mstyle.getLine() );
        markFlagger_.setSelected( mstyle.getHidePoints() );
        errorSelector_.setSelectedItem( mstyle.getErrorRenderer() );
    }

    public Style getStyle() {
        return MarkStyleSelectors
              .getStyle( (MarkShape) shapeSelector_.getSelectedItem(),
                         sizeSelector_.getSelectedIndex(),
                         colorSelector_.getSelectedColor(),
                         opaqueSlider_.getValue1(),
                         markFlagger_.isEnabled() && markFlagger_.isSelected(),
                         (ErrorRenderer) errorSelector_.getSelectedItem(),
                         lineSelector_.getValue(),
                         thickSelector_.getSelectedThickness(),
                         dashSelector_.getSelectedDash(),
                         errorModeModels_ );
    }

    public String getHelpID() {
        return helpId_;
    }

    /**
     * Returns a MarkStyle described by its attributes.
     *
     * @param  shape  marker shape
     * @param  size   marker size
     * @param  color  marker colour
     * @param  hidePoints  whether markers are invisible
     * @param  errorRenderer   error bar rendering style
     * @param  line   line type
     * @param  thick  line thickness
     * @param  dash   line dash pattern
     * @return  marker
     */
    private static MarkStyle getStyle( MarkShape shape, int size, Color color,
                                       int opaqueLimit, boolean hidePoints,
                                       ErrorRenderer errorRenderer,
                                       MarkStyle.Line line,
                                       int thick, float[] dash,
                                       ErrorModeSelection[] errModels ) {
        MarkStyle style = size == 0 ? MarkShape.POINT.getStyle( color, 0 )
                                    : shape.getStyle( color, size );
        style.setOpaqueLimit( opaqueLimit );
        style.setLine( line );
        style.setHidePoints( hidePoints );
        style.setErrorRenderer( errorRenderer );
        style.setLineWidth( thick );
        style.setDash( dash );
        style.setErrorModeModels( errModels );
        return style;
    }

    /**
     * Sets the known statistical information about a list of plottable sets.
     * This represents information about calculated linear regression
     * coefficients.
     * 
     * @param  setIds  set identifiers for the statistics objects provided
     * @param  stats   statistics objects themselves, one per element of 
     *                 <code>setIds</code>
     */
    public void setStats( SetId[] setIds, XYStats[] stats ) {
        statMap_.clear();
        if ( setIds.length != stats.length ) {
            throw new IllegalArgumentException();
        }
        for ( int i = 0; i < setIds.length; i++ ) {
            statMap_.put( setIds[ i ], stats[ i ] );
        }
        refreshState();
    }

    protected void refreshState() {
        super.refreshState();
        SetId setId = getSetId();

        /* Ensure that information about linear correlations is up to date. */
        String statText;
        XYStats stats = statMap_.get( setId );
        if ( stats != null ) {
            statText = new StringBuffer()
                .append( "m=" )
                .append( (float) stats.getLinearCoefficients()[ 1 ] )
                .append( "; c=" )
                .append( (float) stats.getLinearCoefficients()[ 0 ] )
                .append( "; r=" )
                .append( (float) stats.getCorrelation() )
                .toString();
        }
        else {
            statText = "";
        }
        corrBox_.removeAll();
        if ( statText != null && statText.trim().length() > 0 ) {
            JTextField corrField = new JTextField();
            corrField.setText( statText );
            corrField.setCaretPosition( 0 );
            corrField.setEditable( false );
            corrBox_.add( corrField );
            corrBox_.revalidate();
        }

        /* See if we are capable of plotting errors. */
        boolean permitErrors = ! ErrorMode.allBlank( getErrorModes() );

        /* Make sure that the error bar selector is only enabled if 
         * error bars will be plotted. */
        errorSelector_.setEnabled( permitErrors );

        /* Make sure that marker presence control is only enabled if 
         * a line or error bars are being plotted. */
        boolean visibleErrors =
             permitErrors && 
             ! ErrorRenderer.NONE.equals( ((ErrorRenderer)
                                           errorSelector_.getSelectedItem()) );
        markFlagger_.setEnabled( lineSelector_.getValue() != null ||
                                 visibleErrors );
    }

    /**
     * Returns the array of error modes associated with this editor.
     *
     * @return   array of error modes, one per error dimension
     */
    private ErrorMode[] getErrorModes() {
        int ndim = errorModeModels_.length;
        ErrorMode[] modes = new ErrorMode[ ndim ];
        for ( int idim = 0; idim < ndim; idim++ ) {
            modes[ idim ] = errorModeModels_[ idim ].getErrorMode();
        }
        return modes;
    }

    public Icon getLegendIcon() {
        return ((MarkStyle) getStyle()).getLegendIcon( getErrorModes() );
    }
}
