package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.Domain;
import uk.ac.starlink.table.DomainMapper;

/**
 * JComboBox suitable for use with a ColumnComboBoxModel.
 * It installs (and deinstalls as appropriate)
 * {@link javax.swing.ComboBoxEditor}s which allow for
 * textual expressions to be interpreted as JEL expressions based
 * on the TopcatModel on which this model is based.
 * This facility is only available/useful in the case that the 
 * combo box is editable; so the returned combo box is editable.
 * Currently no default renderer is required or installed.
 *
 * @author   Mark Taylor
 * @since    7 Apr 2020
 */
public class ColumnDataComboBox extends FixedJComboBox<ColumnData> {

    private final Domain<?> domain_;
    private final DomainMapperComboBox mapperSelector_;

    /**
     * Constructs a domain-less selector.
     */
    public ColumnDataComboBox() {
        this( null );
    }

    /**
     * Constructs a selector with a given target value domain.
     *
     * @param  domain   required value domain
     */
    @SuppressWarnings("this-escape")
    public ColumnDataComboBox( Domain<?> domain ) {
        domain_ = domain;
        mapperSelector_ = domain == null
                        ? null
                        : new DomainMapperComboBox( domain, this );
        setEditable( true );
    }

    /**
     * Returns a component that should be presented to the user for
     * selecting domain mapper alongside the input value selector.
     * This method returns non-null only if there is a material
     * choice to be made; if there's only one option, null is returned.
     *
     * @return  domain mapper selector component, or null
     */
    public DomainMapperComboBox getDomainMapperSelector() {
        return mapperSelector_ != null && mapperSelector_.getItemCount() > 1
             ? mapperSelector_
             : null;
    }

    @Override
    public void setModel( ComboBoxModel<ColumnData> model ) {
        if ( model instanceof ColumnDataComboBoxModel ) {
            ColumnDataComboBoxModel emodel =
                (ColumnDataComboBoxModel) model;
            setEditor( new ColumnDataEditor( emodel, this ) );
        }
        super.setModel( model );
    }

    /**
     * Returns the currently selected DomainMapper.
     * This may have been actively selected by the user, or may be the
     * only option.  If no guess can be made about what mapper to use,
     * including if no domain has been specified, the return value may be null.
     *
     * @return   selected domain mapper
     */
    public DomainMapper getDomainMapper() {
        if ( mapperSelector_ != null ) {
            return mapperSelector_
                  .getItemAt( mapperSelector_.getSelectedIndex() );
        }
        else {
            return null;
        }
    }

    @Override
    public void addActionListener( ActionListener listener ) {
        super.addActionListener( listener );
        if ( mapperSelector_ != null ) {
            mapperSelector_.addActionListener( listener );
        }
    }

    @Override
    public void removeActionListener( ActionListener listener ) {
        super.removeActionListener( listener );
        if ( mapperSelector_ != null ) {
            mapperSelector_.removeActionListener( listener );
        }
    }

    /**
     * ComboBoxEditor implementation suitable for use with a 
     * ColumnDataComboBoxModel.
     */
    private static class ColumnDataEditor extends BasicComboBoxEditor {

        private final ColumnDataComboBoxModel model_;
        private final Component parent_;
        private final ComboBoxEditor base_;
        private final Color okColor_;
        private final Color errColor_;
        private String text_;
        private ColumnData data_;

        /**
         * Constructor.
         *
         * @param  model   model which this editor can work with
         * @param  parent  parent component
         */
        public ColumnDataEditor( ColumnDataComboBoxModel model,
                                 Component parent ) {
            model_ = model;
            parent_ = parent;
            base_ = new JComboBox<Object>().getEditor();
            okColor_ = UIManager.getColor( "ComboBox.foreground" );
            errColor_ = UIManager.getColor( "ComboBox.disabledForeground" );
        }

        public void setItem( Object obj ) {
            base_.setItem( toStringOrData( obj ) );
        }

        public Object getItem() {
            return toStringOrData( base_.getItem() );
        }

        /**
         * Configures the editor component to give a visual indication
         * of whether a legal column data object is currently visible.
         *
         * @param  isOK  true for usable data, false for not
         */
        private void setOk( boolean isOk ) {
            base_.getEditorComponent()
                 .setForeground( isOk ? okColor_ : errColor_ );
        }

        /**
         * Takes an object which is a possible content of this editor and
         * returns a corresponding ColumnData object if possible,
         * otherwise a string.
         * This method informs the user via a popup if a string cannot
         * be converted to data.  It caches values so that it does not
         * keep showing the same popup for the same string.
         *
         * @param  item   input object
         * @return   output object, should be of type String or
         *           (preferably) ColumnData
         */
        private Object toStringOrData( Object item ) {
            if ( item instanceof ColumnData ) {
                text_ = null;
                data_ = (ColumnData) item;
            }
            else if ( item instanceof String ) {
                String txt = (String) item;
                if ( ! txt.equals( text_ ) ) {
                    ColumnData colData;
                    CompilationException err;
                    try {
                        colData = model_.stringToColumnData( txt );
                        err = null;
                    }
                    catch ( CompilationException e ) {
                        colData = null;
                        err = e;
                    }
                    text_ = txt;
                    data_ = colData;
                    if ( err != null ) {
                        setOk( false );
                        JOptionPane
                       .showMessageDialog( parent_, err.getMessage(),
                                           "Evaluation Error",
                                           JOptionPane.ERROR_MESSAGE );
                    }
                }
            }
            else if ( item == null ) {
                data_ = null;
                text_ = null;
            }
            setOk( data_ != null || text_ == null
                                 || text_.trim().length() == 0 );
            return data_ != null ? data_ : text_;
        }

        public Component getEditorComponent() {
            return base_.getEditorComponent();
        }

        public void selectAll() {
            base_.selectAll();
        }

        public void removeActionListener( ActionListener listener ) {
            base_.removeActionListener( listener );
        }

        public void addActionListener( ActionListener listener ) {
            base_.addActionListener( listener );
        }
    }
}
