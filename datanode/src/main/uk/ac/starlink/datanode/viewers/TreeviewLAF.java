package uk.ac.starlink.datanode.viewers;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;

/**
 * Repository for look-and-feel type items used in Treeview.
 */
public class TreeviewLAF {

    private static Color bgColor = Color.WHITE;
    private static Border gapBorder = 
        BorderFactory.createEmptyBorder( 5, 5, 5, 5 );
    private static Border lineBorder =
        BorderFactory.createLineBorder( Color.BLACK, 2 );
    private static Border gapLineBorder =
        BorderFactory.createCompoundBorder( lineBorder, 
            BorderFactory.createMatteBorder( 5, 5, 5, 5, bgColor ) );

    public static void configureControlPanel( JComponent panel ) {
        panel.setBorder( gapBorder );
    }

    public static void configureControl( JComponent control ) {
        control.setBorder( gapBorder );
    }

    public static void configureMainPanel( JComponent panel ) {
        panel.setBackground( bgColor );
        panel.setBorder( gapLineBorder );
    }

}
