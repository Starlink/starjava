package uk.ac.starlink.frog.iface.themes;
/*
 * @(#)BigContrastMetalTheme.java	1.9 99/10/03
 *
 * Copyright (c) 1998, 1999 by Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */


import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * This class describes a theme using "green" colors.
 *
 * @version 1.9 10/03/99
 * @author Steve Wilson
 */
public class BigContrastMetalTheme extends ContrastTheme {

    public String getName() { return "Low Vision"; }

    private final FontUIResource controlFont = 
        new FontUIResource("Dialog", Font.BOLD, 20);
    private final FontUIResource systemFont = 
        new FontUIResource("Dialog", Font.PLAIN, 20);
    private final FontUIResource windowTitleFont = 
        new FontUIResource("Dialog", Font.BOLD, 20);
    private final FontUIResource userFont = 
        new FontUIResource("SansSerif", Font.PLAIN, 20);
    private final FontUIResource smallFont = 
        new FontUIResource("Dialog", Font.PLAIN, 16);


    public FontUIResource getControlTextFont() { return controlFont;}
    public FontUIResource getSystemTextFont() { return systemFont;}
    public FontUIResource getUserTextFont() { return userFont;}
    public FontUIResource getMenuTextFont() { return controlFont;}
    public FontUIResource getWindowTitleFont() { return windowTitleFont;}
    public FontUIResource getSubTextFont() { return smallFont;}

    public void addCustomEntriesToTable(UIDefaults table) {
        super.addCustomEntriesToTable(table);

        final int internalFrameIconSize = 30;
        table.put("InternalFrame.closeIcon", MetalIconFactory.getInternalFrameCloseIcon(internalFrameIconSize));
        table.put("InternalFrame.maximizeIcon", MetalIconFactory.getInternalFrameMaximizeIcon(internalFrameIconSize));
        table.put("InternalFrame.iconifyIcon", MetalIconFactory.getInternalFrameMinimizeIcon(internalFrameIconSize));
        table.put("InternalFrame.minimizeIcon", MetalIconFactory.getInternalFrameAltMaximizeIcon(internalFrameIconSize));

	Border blackLineBorder = new BorderUIResource( new MatteBorder( 2,2,2,2, Color.black) );
	Border textBorder = blackLineBorder;

        table.put( "ToolTip.border", blackLineBorder);
	table.put( "TitledBorder.border", blackLineBorder);

        table.put( "TextField.border", textBorder);
        table.put( "PasswordField.border", textBorder);
        table.put( "TextArea.border", textBorder);
        table.put( "TextPane.font", textBorder);

        table.put( "ScrollPane.border", blackLineBorder);

        table.put( "ScrollBar.width", new Integer(25) );

    }
}
