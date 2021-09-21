package uk.ac.starlink.ttools.plot2.config;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.ErrorModeSelection;
import uk.ac.starlink.ttools.plot2.layer.MultiPointShape;
import uk.ac.starlink.util.IconUtils;

/**
 * Config key that specifies a multi-point shape.
 *
 * @author   Mark Taylor
 * @since    25 Feb 2013
 */
public class MultiPointConfigKey extends OptionConfigKey<MultiPointShape> {

    private final MultiPointShape[] shapes_;
    private final ErrorMode[] modes_;

    /**
     * Constructor.
     *
     * @param   meta  metadata
     * @param   shapes   shape options
     * @param   modes   error mode objects, used with shapes to draw icon
     */
    public MultiPointConfigKey( ConfigMeta meta, MultiPointShape[] shapes,
                                ErrorMode[] modes ) {
        super( meta, MultiPointShape.class, shapes, chooseDefault( shapes )  );
        shapes_ = shapes;
        modes_ = modes;
    }

    public String getXmlDescription( MultiPointShape shape ) {
        return null;
    }

    /**
     * Returns the error mode array used which combines with a shape to
     * work out how to paint an icon.
     *
     * @return  error mode array
     */
    public ErrorMode[] getErrorModes() {
        return modes_;
    }

    @Override
    public Specifier<MultiPointShape> createSpecifier() {
        int naxis = modes_.length;
        ErrorModeSelection[] modeSelections = new ErrorModeSelection[ naxis ];
        for ( int ia = 0; ia < naxis; ia++ ) {
            final ErrorMode mode = modes_[ ia ];
            modeSelections[ ia ] = new ErrorModeSelection() {
                public ErrorMode getErrorMode() {
                    return mode;
                }
                public void addActionListener( ActionListener listener ) {
                }
                public void removeActionListener( ActionListener listener ) {
                }
            };
        }
        ComboBoxModel<MultiPointShape> model =
            new MultiPointShapeComboBoxModel( shapes_, getDefaultValue(),
                                              modeSelections );
        ListCellRenderer<MultiPointShape> renderer =
            new MultiPointShapeRenderer( modeSelections );
        JComboBox<MultiPointShape> shapeComboBox = new JComboBox<>( model );
        shapeComboBox.setRenderer( renderer );
        return new ComboBoxSpecifier<MultiPointShape>( MultiPointShape.class,
                                                       shapeComboBox ) {
            public String stringify( MultiPointShape value ) {
                return valueToString( value );
            }
        };
    }

    /**
     * Selects a sensible default from a list of shapes.
     * It just picks the first non-blank one.
     *
     * @param  shapes  list of all options
     * @return  sensible default
     */
    private static MultiPointShape chooseDefault( MultiPointShape[] shapes ) {
        for ( int ir = 0; ir < shapes.length; ir++ ) {
            MultiPointShape shape = shapes[ ir ];
            if ( shape != null && shape != MultiPointShape.NONE ) {
                return shape;
            }
        }
        assert false;
        return MultiPointShape.DEFAULT;
    }

    /**
     * ComboBoxModel suitable for selecting MultiPointShape objects.
     * The contents of the model may change according to the ErrorMode
     * values currently in force.
     */
    private static class MultiPointShapeComboBoxModel
            extends AbstractListModel<MultiPointShape>
            implements ComboBoxModel<MultiPointShape>, ActionListener {

        private final MultiPointShape[] allShapes_;
        private final MultiPointShape defaultShape_;
        private final ErrorModeSelection[] modeSelections_;
        private List<MultiPointShape> activeShapeList_;
        private MultiPointShape selected_;

        /**
         * Constructor.
         *
         * @param   shapes  list of all the shapes that may be used
         * @param   defaultShape  default shape to use if no other is known
         * @param   modeSelections  selection models for the ErrorMode values
         *          in force
         */
        MultiPointShapeComboBoxModel( MultiPointShape[] shapes,
                                      MultiPointShape defaultShape,
                                      ErrorModeSelection[] modeSelections ) {
            allShapes_ = shapes;
            defaultShape_ = defaultShape;
            modeSelections_ = modeSelections;
            selected_ = defaultShape;
            updateState();

            /* Listen out for changes in the ErrorMode selectors, since they
             * may trigger changes in this model. */
            for ( int idim = 0; idim < modeSelections.length; idim++ ) {
                modeSelections[ idim ].addActionListener( this );
            }
        }

        public MultiPointShape getElementAt( int index ) {
            return activeShapeList_.get( index );
        }

        public int getSize() {
            return activeShapeList_.size();
        }

        public Object getSelectedItem() {
            return selected_;
        }

        public void setSelectedItem( Object item ) {
            if ( activeShapeList_.contains( item ) &&
                 item instanceof MultiPointShape ) {
                selected_ = (MultiPointShape) item;
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

            /* Assemble a list of the shapes which know how to draw
             * error bars in this dimensionality. */
            List<MultiPointShape> shapeList = new ArrayList<>();
            for ( int ir = 0; ir < allShapes_.length; ir++ ) {
                MultiPointShape shape = allShapes_[ ir ];
                if ( shape.supportsDimensionality( ndim ) ) {
                    shapeList.add( shape );
                }
            }

            /* If the current selection does not exist in the new list,
             * use the default one. */
            if ( ! shapeList.contains( selected_ ) ) {
                selected_ = defaultShape_;
            }

            /* Install the new list into this model and inform listeners. */
            activeShapeList_ = shapeList;
            fireContentsChanged( this, 0, activeShapeList_.size() - 1 );
        }
    }

    /**
     * Class which performs rendering of MultiPointShape objects in a JComboBox.
     */
    private static class MultiPointShapeRenderer
            implements ListCellRenderer<MultiPointShape> {
        private final ErrorModeSelection[] errModeSelections_;
        private final BasicComboBoxRenderer baseRenderer_;
        MultiPointShapeRenderer( ErrorModeSelection[] errorModeSelections ) {
            errModeSelections_ = errorModeSelections;
            baseRenderer_ = new BasicComboBoxRenderer();
        }
        public Component getListCellRendererComponent(
                JList<? extends MultiPointShape> list, MultiPointShape shape,
                int index, boolean isSelected, boolean hasFocus ) {
            Component c =
                baseRenderer_
               .getListCellRendererComponent( list, shape, index,
                                              isSelected, hasFocus );
            if ( c instanceof JLabel ) {
                JLabel label = (JLabel) c;
                Icon icon = null;
                ErrorMode[] modes = new ErrorMode[ errModeSelections_.length ];
                for ( int imode = 0; imode < modes.length; imode++ ) {
                    modes[ imode ] = errModeSelections_[ imode ]
                                    .getErrorMode();
                }
                icon = shape.getLegendIcon( modes, 40, 15, 5, 1 );
                icon = IconUtils.colorIcon( icon, c.getForeground() );
                label.setText( icon == null ? "??" : null );
                label.setIcon( icon );
            }
            return c;
        }
    }
}
