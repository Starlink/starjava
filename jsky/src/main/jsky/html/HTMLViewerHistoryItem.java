/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: HTMLViewerHistoryItem.java,v 1.2 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.html;

import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.JComponent;

import jsky.catalog.URLQueryResult;
import jsky.util.gui.DialogUtil;


/**
 * Local class used to store information about previously viewed URLs.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class HTMLViewerHistoryItem extends AbstractAction implements Serializable {

    /** The URL of the page */
    protected String urlStr;

    /** The title of the page */
    protected String title;

    /**
     * Create a history item with the given title (for display) and URL.
     *
     * @param title The title of the HTML page
     * @param url The URL of the HTML page
     */
    public HTMLViewerHistoryItem(String title, URL url) {
        super(title);
        this.title = title;
        if (url != null)
            urlStr = url.toString();
    }

    /** Display the catalog */
    public void actionPerformed(ActionEvent evt) {
        try {
            HTMLViewer viewer = HTMLViewerMenuBar.getCurrentHTMLViewer();
            URL url = null;
            if (urlStr != null) {
                url = new URL(urlStr);
            }

            if (url != null) {
                viewer.setPage(url);
            }
        }
        catch (Exception e) {
            DialogUtil.error(e);
        }
    }

    /** Return the URL of the catalog, table or FITS file, if known, otherwise null. */
    public String getURLStr() {
        return urlStr;
    }

    /** Return the catalogs's name. */
    public String getTitle() {
        return title;
    }
}
