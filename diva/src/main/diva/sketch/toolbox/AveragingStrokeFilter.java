/*
 * $Id: AveragingStrokeFilter.java,v 1.2 2002/07/31 23:10:10 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.TimedStroke;

import diva.sketch.recognition.*;
import java.io.*;
import java.util.Iterator;
/**
 * Smooth a stroke by averaging a point with its neighboring points.
 *
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public class AveragingStrokeFilter extends StrokeFilter {
    /**
     * Smooth the given stroke by averaging each point with its
     * neighboring points.
     */
    public TimedStroke apply(TimedStroke s){
        return averaging(s);
    }

    /**
     * Smooth the given stroke by averaging each point with its
     * neighboring points.
     */
    public static TimedStroke averaging(TimedStroke s) {
        TimedStroke filteredStroke = new TimedStroke();
        int num = s.getVertexCount();
        filteredStroke.addVertex((float)s.getX(0),(float)s.getY(0),s.getTimestamp(0));
        for(int i=1; i<num-1; i++){
            float x = (float)(s.getX(i)*0.5+s.getX(i-1)*0.25+s.getX(i+1)*0.25);
            float y = (float)(s.getY(i)*0.5+s.getY(i-1)*0.25+s.getY(i+1)*0.25);
            long t = (s.getTimestamp(i)+s.getTimestamp(i-1)+s.getTimestamp(i+1))/3;
            filteredStroke.addVertex(x,y,t);
        }
        filteredStroke.addVertex((float)s.getX(num-1),(float)s.getY(num-1),s.getTimestamp(num-1));
        return filteredStroke;
    }

    public static void main(String argv[]){
        try{
            System.out.println(argv[0]);
            MSTrainingParser parser = new MSTrainingParser();
            BufferedReader br = new BufferedReader(new FileReader(argv[0]));
            MSTrainingModel model = (MSTrainingModel)parser.parse(br);
            //model should only contain 1 example
            TimedStroke t = null;
            for(Iterator iter = model.types(); iter.hasNext();){
                String type = (String)iter.next();
                for(Iterator ex = model.positiveExamples(type); ex.hasNext();){
                    TimedStroke strokes[] = (TimedStroke[])ex.next();
                    t = strokes[0];
                    break;
                }
            }
            if(t != null){
                BufferedWriter bw = new BufferedWriter(new FileWriter("out.orig"));
                for(int i=0; i<t.getVertexCount(); i++){
                    bw.write(t.getX(i)+"\t"+t.getY(i)+"\t"+t.getTimestamp(i)+"\n");
                }
                bw.close();

                TimedStroke ft=AveragingStrokeFilter.averaging(t);
                bw = new BufferedWriter(new FileWriter("out.avg"));
                for(int i=0; i<ft.getVertexCount(); i++){
                    bw.write(ft.getX(i)+"\t"+ft.getY(i)+"\t"+ft.getTimestamp(i)+"\n");
                }
                bw.close();
                
                ft=RemoveDupPtsStrokeFilter.removeDupPts(ft);
                bw = new BufferedWriter(new FileWriter("out.dup"));
                for(int i=0; i<ft.getVertexCount(); i++){
                    bw.write(ft.getX(i)+"\t"+ft.getY(i)+"\t"+ft.getTimestamp(i)+"\n");
                }
                bw.close();
                
                ft=DehookingStrokeFilter.dehook(ft);
                bw = new BufferedWriter(new FileWriter("out.deh"));
                for(int i=0; i<ft.getVertexCount(); i++){
                    bw.write(ft.getX(i)+"\t"+ft.getY(i)+"\t"+ft.getTimestamp(i)+"\n");
                }
                bw.close();

                ft=ApproximateStrokeFilter.approximate(ft,1.0);
                bw = new BufferedWriter(new FileWriter("out.app"));
                for(int i=0; i<ft.getVertexCount(); i++){
                    bw.write(ft.getX(i)+"\t"+ft.getY(i)+"\t"+ft.getTimestamp(i)+"\n");
                }
                bw.close();
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
            
    }
}

