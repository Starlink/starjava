package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
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
    private final AutocompleteMatcher autocompleteMatcher_;
    private ComboBoxModel<ColumnData> model_;
    private boolean isSelectingIndex_;

    /** Default option for autocomplete popup menu. */
    public static final AutocompleteMatcher DFLT_AUTOCOMPLETE =
        AutocompleteMatcher.CONTAINS;

    /**
     * Constructs a default selector
     */
    public ColumnDataComboBox() {
        this( (Domain) null, DFLT_AUTOCOMPLETE );
    }

    /**
     * Constructs a selector with configuration options.
     *
     * @param  domain   required value domain
     * @param  autocomplete  autocompletion option, null for none
     */
    @SuppressWarnings("this-escape")
    public ColumnDataComboBox( Domain<?> domain,
                               AutocompleteMatcher autocomplete ) {
        domain_ = domain;
        mapperSelector_ = domain == null
                        ? null
                        : new DomainMapperComboBox( domain, this );
        autocompleteMatcher_ = autocomplete;
        addPopupMenuListener( new PopupMenuListener() {
            public void popupMenuCanceled( PopupMenuEvent evt ) {
                clearPopupFilter();
            }
            public void popupMenuWillBecomeVisible( PopupMenuEvent evt ) {
            }
            public void popupMenuWillBecomeInvisible( PopupMenuEvent evt ) {
            }
        } );
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
        model_ = model;
        if ( model instanceof ColumnDataComboBoxModel ) {
            ColumnDataComboBoxModel emodel =
                (ColumnDataComboBoxModel) model;
            ColumnDataEditor ed = new ColumnDataEditor( emodel, this );
            setEditor( ed );
            ed.addActionListener( evt -> clearPopupFilter() );
            final ColumnDataComboBox cbox = this;
            Component edComp = ed.getEditorComponent();
            assert edComp instanceof JTextField;
            if ( edComp instanceof JTextField &&
                 autocompleteMatcher_ != null ) {
                JTextField tfield = (JTextField) edComp;
                tfield.addKeyListener( new KeyAdapter() {
                    String txt_;

                    /* The KeyListener documentation suggests that keyTyped
                     * would be the method to override here, but doing that
                     * seems to generate messages about the previously
                     * typed character.  This one seems to work OK. */
                    @Override
                    public void keyReleased( KeyEvent evt ) {
                        String txt = tfield.getText();
                        int code = evt.getKeyCode();
                        boolean isEditKey = ! ( evt.isActionKey() ||
                                                code == KeyEvent.VK_ENTER );
                        if ( isEditKey && ! txt.equals( txt_ ) ) {
                            txt_ = txt;
                            setPopupFilter( txt );
                            tfield.setText( txt );
                            if ( cbox.isPopupVisible() ) {
                                if ( txt == null || txt.trim().length() == 0 ) {
                                    cbox.hidePopup();
                                }
                            }
                            else {
                                if ( getModel() != model_ ) {
                                    cbox.showPopup();
                                }
                            }
                        }
                    }
                } );
            }
        }
        super.setModel( model );
    }

    @Override
    public void setSelectedIndex( int ix ) {
        assert SwingUtilities.isEventDispatchThread();
        isSelectingIndex_ = true;
        super.setSelectedIndex( ix );
        isSelectingIndex_ = false;
    }

    @Override
    public void setSelectedItem( Object obj ) {

        /* This is hairy.  If a new selection is definitely set, then we want
         * to get rid of the filter popup and accept the new value.
         * But there is one circumstance in which this method is called when
         * we don't want to do that, which is when the user is navigating
         * up and down the filter model popup with the arrow keys,
         * then we just want to reflect that movement in the text field.
         * In that circumstance, this method gets called as a consequence
         * of setSelectedIndex.  So we put a flag in place to check if
         * that's the call stack, and in that case leave the popup in place.
         * This is clearly fragile and dependent on the details of
         * JComboBox implementation, but it works at time of writing and
         * (after much effort) I haven't found a better way to do it. */
        if ( getModel() == model_ || ! isSelectingIndex_ ) {
            clearPopupFilter();
            assert getModel() == model_;
        }
        super.setSelectedItem( obj );
    }

    @Override
    public Object getSelectedItem() {
        return model_.getSelectedItem();
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

    /**
     * Returns an object that aggregates the selected column data
     * with an identifier that can be used to assess content equality.
     *
     * @return  identified column data for currently selected choice
     */
    public IdentifiedColumnData getIdentifiedColumnData() {
        Object item = getSelectedItem();
        if ( item instanceof ColumnData ) {
            ColumnData cdata = (ColumnData) item;
            ComboBoxModel<ColumnData> model = getModel();
            if ( model instanceof ColumnDataComboBoxModel ) {
                TopcatModel tcModel =
                    ((ColumnDataComboBoxModel) model).getTopcatModel();
                String id = tcModel.getColumnDataContentIdentifier( cdata );
                return new IdentifiedColumnData() {
                    public ColumnData getColumnData() {
                        return cdata;
                    }
                    public String getId() {
                        return id;
                    }
                };
            }
        }
        return null;
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
     * Configures this component so that the options displayed in the popup
     * menu correspond to only those objects from the model that match
     * the supplied entry string.
     *
     * @param   txt  string to match, may be null for no restriction
     */
    private void setPopupFilter( String txt ) {
        if ( autocompleteMatcher_ != null ) {
            List<ColumnData> filterList = new ArrayList<>();
            if ( txt != null && txt.trim().length() > 0 ) {
                int nel = model_.getSize();
                for ( int i = 0; i < nel; i++ ) {
                    ColumnData cdata = model_.getElementAt( i );
                    if ( cdata != null &&
                         autocompleteMatcher_.textMatchesColumn( txt, cdata ) ){
                        filterList.add( cdata );
                    }
                }
            }
            if ( filterList.size() > 0 ) {

                /* Set the publicly visible model for this JComboBox
                 * to the one that should appear in the popup, but at the same
                 * time keep track of the real model in this class.
                 * The actual selection has to be managed in both models. */
                ComboBoxModel<ColumnData> filterModel =
                        new DefaultComboBoxModel<ColumnData>
                                ( filterList.toArray( new ColumnData[ 0 ] ) ) {
                    @Override
                    public void setSelectedItem( Object item ) {
                        model_.setSelectedItem( item );
                        super.setSelectedItem( item );
                    }
                };
                filterModel.setSelectedItem( null );
                super.setModel( filterModel );
            }
            else {
                clearPopupFilter();
            }
        }
    }

    /**
     * Resets this component so that the options displayed in the popup menu
     * are all of the ones in the model.
     */
    private void clearPopupFilter() {
        if ( super.getModel() != model_ ) {
            assert autocompleteMatcher_ != null;
            Component edComp = getEditor().getEditorComponent();
            assert edComp instanceof JTextField;
            if ( edComp instanceof JTextField ) {
                JTextField txtField = (JTextField) edComp;
                String txt = txtField.getText();
                super.setModel( model_ );
                txtField.setText( txt );
            }
        }
    }

    /**
     * ComboBoxEditor implementation suitable for use with a 
     * ColumnDataComboBoxModel.
     */
    private static class ColumnDataEditor extends BasicComboBoxEditor {

        private final ColumnDataComboBoxModel model_;
        private final ColumnDataComboBox comboBox_;
        private final ComboBoxEditor base_;
        private final Color okColor_;
        private final Color errColor_;
        private String text_;
        private ColumnData data_;

        /**
         * Constructor.
         *
         * @param  model   model which this editor can work with
         * @param  comboBox  owner combo box
         */
        public ColumnDataEditor( ColumnDataComboBoxModel model,
                                 ColumnDataComboBox comboBox ) {
            model_ = model;
            comboBox_ = comboBox;
            base_ = new JComboBox<Object>().getEditor();
            okColor_ = UIManager.getColor( "ComboBox.foreground" );
            errColor_ = UIManager.getColor( "ComboBox.disabledForeground" );
        }

        @Override
        public void setItem( Object obj ) {
            base_.setItem( toStringOrData( obj ) );
        }

        @Override
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
                        comboBox_.hidePopup();
                        JOptionPane
                       .showMessageDialog( comboBox_, err.getMessage(),
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

        @Override
        public Component getEditorComponent() {
            return base_.getEditorComponent();
        }

        @Override
        public void selectAll() {
            base_.selectAll();
        }

        @Override
        public void removeActionListener( ActionListener listener ) {
            base_.removeActionListener( listener );
        }

        @Override
        public void addActionListener( ActionListener listener ) {
            base_.addActionListener( listener );
        }
    }

    /**
     * Defines what columns appear in the autocomplete popup menu
     * for a string partially entered by the user.
     */
    @FunctionalInterface
    public static interface AutocompleteMatcher {

        /** Instance that matches entry text at the start of the column name. */
        static final AutocompleteMatcher PREFIX = (txt, cdata) ->
            cdata.toString().toLowerCase().startsWith( txt );

        /** Instance that matches entry text anywhere in the column name. */
        static final AutocompleteMatcher CONTAINS = (txt, cdata) ->
            cdata.toString().toLowerCase().indexOf( txt.toLowerCase() ) >= 0;

        /**
         * Returns true if the given ColumnData is considered a match
         * for the supplied text, presumed to be a partially complete
         * string entered by the user.
         *
         * @param  entryTxt  entered text
         * @param  cdata     column data
         * @return  true iff cdata is considered to match the supplied string
         */
        boolean textMatchesColumn( String entryTxt, ColumnData cdata );
    }
}
