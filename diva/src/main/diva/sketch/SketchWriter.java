/*
 * $Id: SketchWriter.java,v 1.21 2002/08/12 06:36:58 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;
import diva.sketch.recognition.TimedStroke;
import diva.sketch.recognition.SSTrainingParser;
import diva.sketch.recognition.SSTrainingWriter;
import diva.util.ModelWriter;
import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

/**
 * Given a sketch model, SketchWriter writes out the model to a
 * character stream.  The output is in XML format.
 *
 * @author Heloise Hse  (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.21 $
 */
public class SketchWriter implements ModelWriter {
    /** Construct an empty sketch writer.
     */
    public SketchWriter() {
    }
    
    /**
     * Write the symbols in the sketch model to the character stream.
     */
    public void writeModel(Object m, Writer writer)
            throws IOException {
        SketchModel model = (SketchModel)m;
        writeHeader(writer);
        writer.write("<" + SketchParser.MODEL_TAG +">\n");
        for(Iterator iter = model.symbols(); iter.hasNext();){
            Symbol symbol = (Symbol)iter.next();
            writeSymbol(symbol, writer);
        }    
        writer.write("</" + SketchParser.MODEL_TAG + ">\n");
        writer.flush();
    }

    /**
     * Write out the symbol information including color, pen width,
     * and stroke.
     */
    private void writeSymbol(Symbol symbol, Writer writer) throws IOException {
        if(symbol instanceof StrokeSymbol) {
            StrokeSymbol strokeSymbol = (StrokeSymbol)symbol;
            Color outline = strokeSymbol.getOutline();
            Color fill = strokeSymbol.getFill();
            float w = strokeSymbol.getLineWidth();
            TimedStroke stroke = strokeSymbol.getStroke();
            writer.write("<" + SketchParser.STROKE_TAG + " ");
            if(outline != Color.black) {
                float outlineArray[] = new float[3];
                outline.getRGBColorComponents(outlineArray);
                if(outlineArray[0] != 0 || outlineArray[1] != 0 ||
                        outlineArray[2] != 0) {
                    writer.write(SketchParser.OUTLINE_TAG + "=\"");
                    writer.write(String.valueOf(outlineArray[0]));
                    writer.write(" ");
                    writer.write(String.valueOf(outlineArray[1]));
                    writer.write(" ");
                    writer.write(String.valueOf(outlineArray[2]));
                    writer.write("\" ");
                }
            }
            if(fill != null) {
                float fillArray[] = new float[3];
                fill.getRGBColorComponents(fillArray);
                writer.write(SketchParser.FILL_TAG + "=\"");
                writer.write(String.valueOf(fillArray[0]));
                writer.write(" ");
                writer.write(String.valueOf(fillArray[1]));
                writer.write(" ");
                writer.write(String.valueOf(fillArray[2]));
                writer.write("\" ");
            }
            writer.write(SketchParser.LINEWIDTH_TAG + "=\"");
            writer.write(String.valueOf(w));
            writer.write("\" " + SSTrainingParser.POINTS_TAG + "=\"");
            SSTrainingWriter.writeStroke(stroke, writer);
            writer.write("\"/>\n");
        }
        else {
            writer.write("<" + SketchParser.COMPOSITE_TAG + ">\n");
            Symbol[] children = ((CompositeSymbol)symbol).getChildren();
            for(int i = 0; i < children.length; i++) {
                writeSymbol(children[i], writer);
            }
            writer.write("</" + SketchParser.COMPOSITE_TAG + ">\n");
        }
    }
            
    /**
     * Write out header information.
     */
    private void writeHeader(Writer writer) throws IOException {
        writer.write("<?xml version=\"1.0\" standalone=\"no\"?>\n");
        writer.write("<!DOCTYPE " + SketchParser.MODEL_TAG + " PUBLIC \""
                + SketchParser.PUBLIC_ID + "\"\n\t\""
                + SketchParser.DTD_URL + "\">\n\n");
    }
}



