/*
 * $Id: SketchDocument.java,v 1.27 2001/07/22 22:02:26 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.whiteboard;

import diva.sketch.SketchModel;
import diva.sketch.SketchParser;
import diva.sketch.SketchWriter;
import diva.sketch.Symbol;
import diva.sketch.JSketch;
import diva.sketch.SketchPane;
import diva.gui.MultipageDocument;
import diva.gui.Application;
import diva.gui.BasicPage;
import diva.gui.Document;
import diva.gui.DocumentFactory;
import diva.gui.MultipageWriter;
import diva.gui.MultipageModel;
import diva.gui.MultipageParser;
import diva.gui.Page;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.awt.Graphics;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.awt.print.PrinterException;
import java.awt.print.PageFormat;
import java.net.URL;
import java.util.Iterator;

 /**
  * A class representing sketch-structured documents.  This class can
  * read files to construct instances of sketch models, and write
  * sketch models out to a file.
  *
  * A sketch document contains multiple pages each of which contains a
  * sketch model.  Currently, when a sketch document is written out to
  * a file, the page titles are lost.  This needs to be fixed through
  * SketchParser and SketchWriter.  Since these two classes operates
  * on iterators of sketch models, they do not have page title
  * information.  They may need to be modified to read and write pages
  * instead.
  * 
  * @author Heloise Hse (hwawen@eecs.berkeley.edu)
  * @version $Revision: 1.27 $
  * @rating Red
  */
 public class SketchDocument extends MultipageDocument {
     //DEBUG
     public static int COUNT = 1;

     /** Construct an untitled sketch document that is owned by the
      *  given application.
      */
     public SketchDocument(Application a) {
         this(a, "Untitled");
     }

     /** Construct a sketch document with the given title that is
      *  owned by the given application.
      */
     public SketchDocument(Application a, String title) {
         super(title, a, new SketchParser(), new SketchWriter());
     }
 
     /** SketchDocument.Factory is a factory for sketch documents.  We
     * put this in an inner class of SketchDocument because this
     * factory can only produce one kind of document.
     */
     public static class Factory implements DocumentFactory {
         /** Create an empty sketch document
          */
         public Document createDocument (Application app) {
             String title = "Untitled" + COUNT++;
             SketchDocument d = new SketchDocument(app, title);
             String pageName = "Page 1";
             MultipageModel m = d.getMultipageModel();
             m.addPage(new BasicPage(m, pageName, new SketchModel()));
             return d;
         }

         /** Throw an exception, as URLs are not supported.
          */
         public Document createDocument (Application app, URL url) {
             throw new UnsupportedOperationException(
                     "Sketch documents cannot yet be loaded from a URL");
         }

         /** Create a new sketch that contains the given file path.
          */
         public Document createDocument (Application app, File file) {
             String title = "Untitled" + COUNT++;
             SketchDocument d = new SketchDocument(app, title);
             d.setFile(file);
             return d;
         }
     }
 }


