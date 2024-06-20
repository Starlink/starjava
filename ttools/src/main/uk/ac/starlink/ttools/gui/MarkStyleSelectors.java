package uk.ac.starlink.ttools.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.ErrorModeSelection;
import uk.ac.starlink.ttools.plot.ErrorRenderer;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot2.layer.MarkerShape;
import uk.ac.starlink.ttools.plot2.layer.MarkerStyle;
import uk.ac.starlink.util.IconUtils;

/**
 * Contains some utility methods for generating selectors for plot
 * style elements.
 *
 * @author   Mark Taylor
 * @since    6 Mar 2013
 */
public class MarkStyleSelectors {

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
     * Private constructor prevents instantiation.
     */
    private MarkStyleSelectors() {
    }

    /**
     * Returns a new JComboBox for marker shape selection
     * with a default list of shapes.
     *
     * @return  new shape selection combo box
     */
    public static JComboBox<MarkShape> createShapeSelector() {
        return createShapeSelector( SHAPES );
    }

    /**
     * Returns a new JComboBox for marker shape selection
     * with specified list of shapes.
     *
     * @param  shapes  shape options
     * @return  new shape selection combo box
     */
    public static JComboBox<MarkShape>
            createShapeSelector( MarkShape[] shapes ) {
        final JComboBox<MarkShape> selector = new JComboBox<>( shapes );
        selector.setRenderer( new MarkRenderer<MarkShape>() {
            public MarkShape getMarkShape( int index ) {
                return selector.getItemAt( index );
            }
            public MarkShape getMarkShape() {
                return selector.getItemAt( selector.getSelectedIndex() );
            }
            public Color getMarkColor() {
                return Color.BLACK;
            }
            public int getMarkSize() {
                return 5;
            }
        } );
        return selector;
    }

    /**
     * Returns a new JComboBox for marker shape selection
     * with specified list of shapes.
     *
     * @param  shapes  shape options
     * @return  new shape selection combo box
     */
    public static JComboBox<MarkerShape>
            createMarkerShapeSelector( MarkerShape[] shapes ) {
        final JComboBox<MarkerShape> selector = new JComboBox<>( shapes );
        selector.setRenderer( new MarkerRenderer<MarkerShape>() {
            public MarkerShape getMarkerShape( int index ) {
                return selector.getItemAt( index );
            }
            public MarkerShape getMarkerShape() {
                return selector.getItemAt( selector.getSelectedIndex() );
            }
            public Color getMarkerColor() {
                return Color.BLACK;
            }
            public int getMarkerSize() {
                return 5;
            }
        } );
        return selector;
    }

    /**
     * Returns a new JComboBox for selecting symbol sizes,
     * using the default maximum size ({@link #MAX_SIZE}).
     *
     * @return  new size selection combo box
     */
    public static JComboBox<Integer> createSizeSelector() {
        return createSizeSelector( MAX_SIZE );
    }

    /**
     * Returns a new JComboBox for selecting symbol sizes,
     * using a specified maximum size.
     *
     * @param  maxSize  maximum size
     * @return  new size selection combo box
     */
    public static JComboBox<Integer> createSizeSelector( int maxSize ) {
        final JComboBox<Integer> selector =
            new JComboBox<>( createNumberedModel( maxSize + 1 ) );
        selector.setRenderer( new MarkRenderer<Integer>( true ) {
            public int getMarkSize( int index ) {
                return index;
            }
            public int getMarkSize() {
                return selector.getSelectedIndex();
            }
            public Color getMarkColor() {
                return Color.BLACK;
            }
            public MarkShape getMarkShape() {
                return MarkShape.OPEN_SQUARE;
            }
        } );
        return selector;
    }

    /**
     * Returns a new JComboBox which will contain ErrorRenderer objects.
     *
     * @param    errorRenderers  full list of renderers to select from
     *           (may be subsetted according to current ErrorMode selections)
     * @param   defaultRenderer  default error renderer to use if no other
     *          is known
     * @param    errorModeSelections error mode selection models, one per axis
     * @return   new error renderer combo box
     */
    public static JComboBox<ErrorRenderer> createErrorSelector(
            ErrorRenderer[] errorRenderers,
            ErrorRenderer defaultRenderer,
            ErrorModeSelection[] errorModeSelections ) {
        ComboBoxModel<ErrorRenderer> model =
            new ErrorRendererComboBoxModel( errorRenderers, defaultRenderer,
                                            errorModeSelections );
        ListCellRenderer<ErrorRenderer> renderer =
            new ErrorRendererRenderer( errorModeSelections );
        JComboBox<ErrorRenderer> errorSelector = new JComboBox<>( model );
        errorSelector.setRenderer( renderer );
        return errorSelector;
    }

    /**
     * ComboBoxModel suitable for selecting {@link ErrorRenderer} objects.
     * The contents of the model may change according to the {@link ErrorMode}
     * values currently in force.
     */
    private static class ErrorRendererComboBoxModel
            extends AbstractListModel<ErrorRenderer>
            implements ComboBoxModel<ErrorRenderer>, ActionListener {

        private final ErrorRenderer[] allRenderers_;
        private final ErrorRenderer defaultRenderer_;
        private final ErrorModeSelection[] modeSelections_;
        private List<ErrorRenderer> activeRendererList_;
        private ErrorRenderer selected_;

        /**
         * Constructor.
         *
         * @param   renderers  list of all the renderers that may be used
         * @param   defaultRenderer  default error renderer to use if no other
         *          is known
         * @param   modeSelections  selection models for the ErrorMode values
         *          in force
         */
        ErrorRendererComboBoxModel( ErrorRenderer[] renderers,
                                    ErrorRenderer defaultRenderer,
                                    ErrorModeSelection[] modeSelections ) {
            allRenderers_ = renderers;
            defaultRenderer_ = defaultRenderer;
            modeSelections_ = modeSelections;
            selected_ = defaultRenderer;
            updateState();

            /* Listen out for changes in the ErrorMode selectors, since they
             * may trigger changes in this model. */
            for ( int idim = 0; idim < modeSelections.length; idim++ ) {
                modeSelections[ idim ].addActionListener( this );
            }
        }

        public ErrorRenderer getElementAt( int index ) {
            return activeRendererList_.get( index );
        }

        public int getSize() {
            return activeRendererList_.size();
        }

        public Object getSelectedItem() {
            return selected_;
        }

        public void setSelectedItem( Object item ) {
            if ( activeRendererList_.contains( item ) &&
                 item instanceof ErrorRenderer ) {
                selected_ = (ErrorRenderer) item;
            }
            else {
                throw new IllegalArgumentException( "No such selection "
                                                  + item );
            }
        }

        public void actionPerformed( ActionEvent evt ) {
            updateState();
        }

        /**
         * Called when external influences may require that this model's
         * contents are changed.
         */
        private void updateState() {

            /* Count the number of dimensions in which errors are being
             * represented. */
            int ndim = 0;
            for ( int idim = 0; idim < modeSelections_.length; idim++ ) {
                if ( ! ErrorMode.NONE
                      .equals( modeSelections_[ idim ].getErrorMode() ) ) {
                    ndim++;
                }
            }

            /* Assemble a list of the renderers which know how to render
             * error bars in this dimensionality. */
            List<ErrorRenderer> rendererList = new ArrayList<ErrorRenderer>();
            for ( int ir = 0; ir < allRenderers_.length; ir++ ) {
                ErrorRenderer renderer = allRenderers_[ ir ];
                if ( renderer.supportsDimensionality( ndim ) ) {
                    rendererList.add( renderer );
                }
            }

            /* If the current selection does not exist in the new list,
             * use the default one. */
            if ( ! rendererList.contains( selected_ ) ) {
                selected_ = defaultRenderer_;
            }

            /* Install the new list into this model and inform listeners. */
            activeRendererList_ = rendererList;
            fireContentsChanged( this, 0, activeRendererList_.size() - 1 );
        }
    }

    /**
     * Class which performs rendering of ErrorRenderer objects in a JComboBox.
     */
    private static class ErrorRendererRenderer
            implements ListCellRenderer<ErrorRenderer> {
        private final ErrorModeSelection[] errModeSelections_;
        private final BasicComboBoxRenderer baseRenderer_;
        ErrorRendererRenderer( ErrorModeSelection[] errorModeSelections ) {
            errModeSelections_ = errorModeSelections;
            baseRenderer_ = new BasicComboBoxRenderer();
        }
        public Component getListCellRendererComponent(
                JList<? extends ErrorRenderer> list, ErrorRenderer er,
                int index, boolean isSelected, boolean hasFocus ) {
            Component c =
                baseRenderer_
               .getListCellRendererComponent( list, er, index,
                                              isSelected, hasFocus );
            if ( c instanceof JLabel ) {
                JLabel label = (JLabel) c;
                Icon icon = null;
                ErrorMode[] modes = new ErrorMode[ errModeSelections_.length ];
                for ( int imode = 0; imode < modes.length; imode++ ) {
                    modes[ imode ] = errModeSelections_[ imode ]
                                    .getErrorMode();
                }
                icon = er.getLegendIcon( modes, 40, 15, 5, 1 );
                icon = IconUtils.colorIcon( icon, c.getForeground() );
                label.setText( icon == null ? "??" : null );
                label.setIcon( icon );
            }
            return c;
        }
    }

    /**
     * Convenience method to construct a new ComboBoxModel which
     * contains Integers numbered from 0 to <code>count-1</code>.
     *
     * @param   count  number of entries in the model
     * @return  new ComboBoxModel filled with Integers
     */
    public static ComboBoxModel<Integer> createNumberedModel( int count ) {
        Integer[] items = new Integer[ count ];
        for ( int i = 0; i < count; i++ ) {
            items[ i ] = Integer.valueOf( i );
        }
        return new DefaultComboBoxModel<Integer>( items );
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
    public static MarkStyle getStyle( MarkShape shape, int size, Color color,
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
     * ComboBoxRenderer class suitable for rendering MarkStyles.
     */
    private static abstract class MarkRenderer<E>
            implements ListCellRenderer<E> {
        private boolean useText_;
        private final BasicComboBoxRenderer baseRenderer_;
        MarkRenderer() {
            this( false );
        }
        MarkRenderer( boolean useText ) {
            useText_ = useText;
            baseRenderer_ = new BasicComboBoxRenderer();
        }
        MarkShape getMarkShape( int itemIndex ) {
            return getMarkShape();
        }
        abstract MarkShape getMarkShape();
        int getMarkSize( int itemIndex ) {
            return getMarkSize();
        }
        abstract int getMarkSize();
        Color getMarkColor( int itemIndex ) {
            return getMarkColor();
        }
        abstract Color getMarkColor();
        public Component getListCellRendererComponent( JList<? extends E> list,
                                                       E value, int index,
                                                       boolean isSelected,
                                                       boolean hasFocus ) {
            Component c = baseRenderer_
                         .getListCellRendererComponent( list, value, index,
                                                        isSelected, hasFocus );
            if ( c instanceof JLabel ) {
                JLabel label = (JLabel) c;
                if ( ! useText_ ) {
                    label.setText( null );
                }
                MarkStyle style = index >= 0
                                ? getStyle( getMarkShape( index ),
                                            getMarkSize( index ),
                                            getMarkColor( index ),
                                            1, false, ErrorRenderer.NONE,
                                            null, 1, null,
                                            new ErrorModeSelection[ 0 ] )
                                : getStyle( getMarkShape(),
                                            getMarkSize(),
                                            getMarkColor(),
                                            1, false, ErrorRenderer.NONE,
                                            null, 1, null,
                                            new ErrorModeSelection[ 0 ] );
                label.setIcon( style.getLegendIcon() );
            }
            return c;
        }
    }

    /**
     * ComboBoxRenderer class suitable for rendering MarkerStyles.
     */
    private static abstract class MarkerRenderer<E>
            implements ListCellRenderer<E> {
        private boolean useText_;
        private final BasicComboBoxRenderer baseRenderer_;
        MarkerRenderer() {
            this( false );
        }
        MarkerRenderer( boolean useText ) {
            useText_ = useText;
            baseRenderer_ = new BasicComboBoxRenderer();
        }
        MarkerShape getMarkerShape( int itemIndex ) {
            return getMarkerShape();
        }
        abstract MarkerShape getMarkerShape();
        int getMarkerSize( int itemIndex ) {
            return getMarkerSize();
        }
        abstract int getMarkerSize();
        Color getMarkerColor( int itemIndex ) {
            return getMarkerColor();
        }
        abstract Color getMarkerColor();
        public Component getListCellRendererComponent( JList<? extends E> list,
                                                       E value, int index,
                                                       boolean isSelected,
                                                       boolean hasFocus ) {
            Component c = baseRenderer_
                         .getListCellRendererComponent( list, value, index,
                                                        isSelected, hasFocus );
            if ( c instanceof JLabel ) {
                JLabel label = (JLabel) c;
                if ( ! useText_ ) {
                    label.setText( null );
                }
                MarkerStyle style =
                      index >= 0
                    ? getMarkerShape( index ).getStyle( getMarkerColor( index ),
                                                        getMarkerSize( index ) )
                    : getMarkerShape().getStyle( getMarkerColor(),
                                                 getMarkerSize() );
                label.setIcon( style.getLegendIcon() );
            }
            return c;
        }
    }
}
