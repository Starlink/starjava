/*
 * $Id: AbstractRule.java,v 1.9 2001/07/22 22:01:49 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.parser2d;
import diva.sketch.recognition.Scene;
import diva.sketch.recognition.SceneElement;
import diva.sketch.recognition.CompositeElement;
import diva.sketch.recognition.Type;

import java.awt.geom.Rectangle2D; 

import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * A parse rule implementation that matches the RHS
 * of a production and generates the LHS.  Subclasses
 * fill in the match() method to impose the constraints
 * of the rule.
 * 
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.9 $
 */
public abstract class AbstractRule implements Rule {    

    public static final double NO_MIN = Double.NEGATIVE_INFINITY;
    
    public static final double NO_MAX = Double.POSITIVE_INFINITY;
	
    /**
     * The north-east direction constant.
     */
    public static final int NORTH_EAST = 0;
    
    /**
     * The north direction constant.
     */
    public static final int NORTH = 1;
    
    /**
     * The north-west direction constant.
     */
    public static final int NORTH_WEST = 2;

    /**
     * The west direction constant.
     */
    public static final int WEST = 3;
    
    /**
     * The south-west direction constant.
     */
    public static final int SOUTH_WEST = 4;
    
    /**
     * The south direction constant.
     */
    public static final int SOUTH = 5;
    
    /**
     * The south-east direction constant.
     */
    public static final int SOUTH_EAST = 6;
    
    /**
     * The east direction constant.
     */
    public static final int EAST = 7;
    
    /**
     * The center direction constant.
     */
    public static final int CENTER = 8;	
	
	
    /**
     * The resultant type of the rule if the rule matches some input.
     */
    private Type _lhsType;

    /**
     * The array of rhs types to try to match.
     */
    private Type[] _rhsTypes;

    /**
     * The array of rhs names if there is a match.
     */
    private String[] _rhsNames;

    /**
     * A utility constructor which simply takes strings
     * with single words separated by whitespace, separates
     * these strings into arrays, and calls the standard
     * array constructor.
     */	
    public AbstractRule (String lhsType,
            String rhsNames, String rhsTypes) {
        String[] ns = split(rhsNames);
        String[] ts = split(rhsTypes);
        _lhsType = Type.makeType(lhsType);
        _rhsNames = ns;
        _rhsTypes = new Type[ts.length];
        for(int i = 0; i < ts.length; i++) {
            _rhsTypes[i] = Type.makeType(ts[i]);
        }
    }
	
    private String[] split(String in) {
        StringTokenizer tok = new StringTokenizer(in);
        ArrayList l = new ArrayList();
        while(tok.hasMoreTokens()) {
            l.add(tok.nextToken());
        }
        String[] out = new String[l.size()];
        l.toArray(out);
        return out;
    }
		
    /**
     * Construct a new basic rule with the given LHS and RHS
     * structure.  The RHS consists of an array of types to
     * match, and an array of names to use if a match occurs.
     */
    public AbstractRule (Type lhsType,
            String[] rhsNames, Type[] rhsTypes) {
        if(rhsNames.length != rhsTypes.length) {
            String err = "AbstractRule(): type/name arrays != length\n";
            err = err + "rhsNames:" + rhsNames.length + "\n";
            err = err + "rhsTypes:" + rhsTypes.length + "\n";
            throw new IllegalArgumentException(err);
        }
        _lhsType = lhsType;
        _rhsNames = rhsNames;
        _rhsTypes = rhsTypes;
    }
    
    /**
     * Return the LHS type of the rule.
     */
    public Type getLHSType() {
        return _lhsType;
    }
	
    /**
     * Return the RHS types of the rule.
     */
    public Type[] getRHSTypes() {
        return _rhsTypes;
    }
	
    /**
     * Return the RHS names of the rule.
     */
    public String[] getRHSNames() {
        return _rhsNames;
    }	
	
    /**
     * Match the given scene elements and return a resulting
     * element, or return null if there is no match.
     */	
    public abstract CompositeElement match(CompositeElement[] rhs, Scene db);
	
	
    /**
     * Return the X coordinate of the given site for the given
     * rectangle.
     */
    public static final double siteX (SceneElement e, int site) {
        Rectangle2D r = e.getBounds();
        switch(site) {
        case NORTH_EAST:
            return r.getCenterX()+r.getWidth()/2;
        case NORTH:
            return r.getCenterX();
        case NORTH_WEST:
            return r.getCenterX()-r.getWidth()/2;
        case WEST:
            return r.getCenterX()-r.getWidth()/2;
        case SOUTH_WEST:
            return r.getCenterX()-r.getWidth()/2;
        case SOUTH:
            return r.getCenterX();
        case SOUTH_EAST:
            return r.getCenterX()+r.getWidth()/2;
        case EAST:
            return r.getCenterX()+r.getWidth()/2;
        case CENTER:
            return r.getCenterX();
        default:
            String err = "Unknown site: " + site;
            throw new IllegalArgumentException(err);
        }
    }


    /**
     * Return the Y coordinate of the given site for the given
     * rectangle.
     */
    public static final double siteY (SceneElement e, int site) {
        Rectangle2D r = e.getBounds();
        switch(site) {
        case NORTH_EAST:
            return r.getCenterY()-r.getHeight()/2;
        case NORTH:
            return r.getCenterY()-r.getHeight()/2;
        case NORTH_WEST:
            return r.getCenterY()-r.getHeight()/2;
        case WEST:
            return r.getCenterY();
        case SOUTH_WEST:
            return r.getCenterY()+r.getHeight()/2;
        case SOUTH:
            return r.getCenterY()+r.getHeight()/2;
        case SOUTH_EAST:
            return r.getCenterY()+r.getHeight()/2;
        case EAST:
            return r.getCenterY();
        case CENTER:
            return r.getCenterY();
        default:
            String err = "Unknown site: " + site;
            throw new IllegalArgumentException(err);
        }
    }
	 
    /**
     * Return the angle between the constructor-specified
     * sites on r1 and r2, respectively. The answer is given
     * in degrees between 0 and 360.
     */
    protected static final double angle (SceneElement from, int fromSite,
            SceneElement to, int toSite) {
        double dx = siteX(to, toSite)-siteX(from, fromSite);
        double dy = siteY(to, toSite)-siteY(from, fromSite);
        double theta = Math.atan2(dy,dx);
        theta = (theta < 0) ? (2*Math.PI)+theta : theta;
        return theta*360/Math.PI;
    }
	
    /**
     * Calculate the ratio of areas with the constructor-specified
     * rectangle as the numerator for the calculation.
     */
    public static final double areaRatio (SceneElement num, 
            SceneElement denom) {
        Rectangle2D numRect = num.getBounds();
        Rectangle2D denomRect = denom.getBounds();
        return (numRect.getWidth()*numRect.getHeight()) /
            (denomRect.getWidth()*denomRect.getHeight());
    }
	
    /**
     * Return the distance between the constructor-specified sites on
     * r1 and r2, respectively.
     */
    protected static final double distance (SceneElement from, int fromSite,
            SceneElement to, int toSite) {
        double dx = siteX(to, toSite)-siteX(from, fromSite);
        double dy = siteY(to, toSite)-siteY(from, fromSite);
        return Math.sqrt(dx*dx + dy*dy);
    }

	
    /**
     * Calculate the height ratio of the bounding boxes of
     * the two given scene elements.
     */
    public static final double heightRatio (SceneElement num, 
            SceneElement denom) {
        return num.getBounds().getHeight() / 
            denom.getBounds().getHeight();
    }	

    /**
     * Calculate the width ratio of the bounding boxes of
     * the two given scene elements.
     */
    public static final double widthRatio (SceneElement num, 
            SceneElement denom) {
        return num.getBounds().getWidth() / 
            denom.getBounds().getWidth();
    }
	
    /**
     * Calculate the ratio of the area of the intersection
     * of the bounding boxes of the two given scene elements to
     * the area of the bounding box of the "which" argument.
     */
    public static final double overlap (SceneElement which, 
            SceneElement other) {
        Rectangle2D whichRect = which.getBounds();
        Rectangle2D otherRect = other.getBounds();
        if(whichRect.intersects(otherRect)) {
            Rectangle2D intersect = whichRect.createIntersection(otherRect);
            return (intersect.getWidth()*intersect.getHeight()) /
                (whichRect.getWidth()*whichRect.getHeight());
        }
        return 0;
    }
	
    /**
     * Return a probability calculation of an aggregate based on the
     * probabilities of the children.
     */
    public static final double calcProb(List children) {
        double prod = 1;
        for(Iterator i = children.iterator(); i.hasNext(); ) {
            CompositeElement child = (CompositeElement)i.next();
            prod *= (child.getConfidence() / 100);
        }
        prod = Math.pow(prod, 1.0/children.size());
        return prod*100;
    }
}


