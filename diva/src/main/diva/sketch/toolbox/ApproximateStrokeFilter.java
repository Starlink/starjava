/*
 * $Id: ApproximateStrokeFilter.java,v 1.6 2000/08/10 18:52:00 michaels Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.TimedStroke;
import java.awt.geom.Line2D;
import java.util.ArrayList;

/**
 * An object which filters a pen stroke using "approximation by line
 * segments" technique, (Hanaki, Temma, Yoshida, "An On-line Character
 * Recognition Aimed at a Substitution for a Billing Machine
 * Keyboard", Pattern Recognition, Vol.8, pp63-71, 1976).
 *
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.6 $
 */
public class ApproximateStrokeFilter extends StrokeFilter {

    /**
     *  Offset of the index.  For each recursion, we pass in new
     *  arrays which start with index 0, by adding the offset to the
     *  index, we get the index of the original arrays.
     */
    private int _threshDistance = 2;

    private void approximateRecurse(ArrayList indexes, double xpath[], double ypath[], int num, int offset){
        if(num > 2){
            int index = num-1;
            //Line2D line = new Line2D.Double(xpath[0], ypath[0], xpath[index], ypath[index]);
            int maxIndex = -1;
            double maxDist = 0;
            for(int i=1; i<num-1; i++){ //exclude start and end points
                //double d = line.pointDistance(xpath[i], ypath[i]);
                double d = Line2D.ptLineDist(xpath[0], ypath[0], xpath[index], ypath[index], xpath[i], ypath[i]);
                if((d>_threshDistance)&&(d>maxDist)){
                    maxDist = d;
                    maxIndex = i;
                }
            }

            if(maxIndex > -1){
                int realIndex = maxIndex + offset;
                //found an end point.
                int size1 = maxIndex+1;
                double xpath1[] = new double[size1];
                double ypath1[] = new double[size1];
                System.arraycopy(xpath, 0, xpath1, 0, size1);
                System.arraycopy(ypath, 0, ypath1, 0, size1);
                int size2 = num-maxIndex;
                double xpath2[] = new double[size2];
                double ypath2[] = new double[size2];
                System.arraycopy(xpath, maxIndex, xpath2, 0, size2);
                System.arraycopy(ypath, maxIndex, ypath2, 0, size2);
                for(int j=0; j<indexes.size(); j++){ // store the indexes in order
                    Integer i = (Integer)indexes.get(j);
                    if(realIndex < i.intValue()){
                        indexes.add(j, new Integer(realIndex));
                        break;
                    }
                    else if(realIndex == i.intValue()){
                        break;
                    }
                    else if(j==(indexes.size()-1)){
                        indexes.add(new Integer(realIndex)); // add to the end
                    }
                }
                approximateRecurse(indexes, xpath1, ypath1, size1, offset);
                approximateRecurse(indexes, xpath2, ypath2, size2, realIndex);
            }
        }
    }

    /**
     * Reduce the number of points in the given pen stroke using the
     * "approximation by line segments" algorithm.  <p>
     *
     * Algorithm: Form a line with the start and end points of the
     * stroke.  Find a point, P, such that distance between the line
     * and P exceeds a threshold value and P is maximum for all points
     * on the stroke.  Then, take the start point and P, and the end
     * point and P, and repeat the process.  <p>
     */
    public TimedStroke apply(TimedStroke s) {
        TimedStroke fs = new TimedStroke();
        int numpoints = s.getVertexCount();
        if(numpoints > 2){
            double xpos[] = new double[numpoints];
            double ypos[] = new double[numpoints];
            long timestamps[] = new long[numpoints];
            for(int i=0; i<s.getVertexCount(); i++){
                xpos[i] = s.getX(i);
                ypos[i] = s.getY(i);
                timestamps[i] = s.getTimestamp(i);
            }

            ArrayList indexes = new ArrayList();
            indexes.add(new Integer(0));
            indexes.add(new Integer(numpoints-1));
            approximateRecurse(indexes, xpos, ypos, numpoints, 0);
            int size = indexes.size();
            for(int i=0; i<indexes.size(); i++){
                Integer idx = (Integer)indexes.get(i);
                int id = idx.intValue();
                try {
                    fs.addVertex((float)xpos[id], (float)ypos[id],
                            timestamps[id]);
                }
                catch (Exception e) {
                    System.out.println(e);
                    System.exit(-1);
                }
            }
        }
        else {
            for(int i=0; i<numpoints; i++){
                try {
                    fs.addVertex((float)s.getX(i), (float)s.getY(i),
                            s.getTimestamp(i));
                }
                catch (Exception e) {
                    System.out.println(e);
                    System.exit(-1);
                }
            }
        }
        return fs;
    }

}


