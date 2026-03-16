package uk.ac.starlink.vo;

import ari.ucidy.UCDWord;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

/**
 * Components for user selection of a UCD.  This is intended for
 * selection of UCD1+ values.
 *
 * <p>Rather than a single JComponent, this class provides two separate
 * components, one for the selection and one giving a status report on
 * the selected text.  This is more convenient for inserting the GUI
 * elements into a stack of single-line components.
 *
 * <p>The combo box comes with a list of all known UCD1+ basic words,
 * that can get displayed in the selection popup window.
 * Note though that UCDs composed from several of these are legal,
 * and will be accepted.
 *
 * @author   Mark Taylor
 * @since    16 Mar 2026
 * @see  <a href="https://www.ivoa.net/documents/UCD1+/"
 *          >UCD1+ Controlled Vocabulary</a>
 */
public class UcdSelector { 

    private final JComboBox<String> comboBox_;
    private final JLabel msgLabel_;
    private static String[] ucdWords_;

    /**
     * Constructor.
     */
    public UcdSelector() {
        msgLabel_ = new JLabel();
        comboBox_ = new JComboBox<String>();
        comboBox_.setEditable( true );
        comboBox_.setModel( new DefaultComboBoxModel<String>( getUcdWords() ) );
        ItemListener listener = evt -> {
            Object txtObj = comboBox_.getSelectedItem();
            String txt = txtObj instanceof String ? (String) txtObj : null;
            final String msg;
            if ( txt == null || txt.trim().length() == 0 ) {
                msg = "(no UCD)";
            }
            else {
                UcdStatus status = UcdStatus.getStatus( txt );
                UcdStatus.Code code = status.getCode();
                msg = code.isError() || code.isWarning()
                    ? status.getMessage()
                    : "OK";
            }
            msgLabel_.setText( msg );
        };
        comboBox_.addItemListener( listener );
        comboBox_.setSelectedItem( null );
    }

    /**
     * Returns the component that allows actual selection of a UCD.
     *
     * @return  combo box
     */
    public JComboBox<String> getComboBox() {
        return comboBox_;
    }

    /**
     * Returns a single-line component that reports a status message on
     * the UCD entered into the combo box.
     *
     * @return   message label component
     */
    public JLabel getMessageLabel() {
        return msgLabel_;
    }

    /**
     * Returns the list of UCD1+ basic words.
     *
     * @return   list of UCD words
     */
    private static String[] getUcdWords() {
        if ( ucdWords_ == null ) {
            List<String> list = new ArrayList<>();
            for ( UCDWord word : UcdStatus.getParser().knownWords ) {
                list.add( word.toString() );
            }
            ucdWords_ = list.toArray( new String[ 0 ] );
        }
        return ucdWords_;
    }
}
