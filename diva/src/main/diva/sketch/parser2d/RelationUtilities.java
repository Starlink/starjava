/*
 * $Id: RelationUtilities.java,v 1.9 2001/07/22 22:01:50 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.parser2d;
import java.awt.geom.Rectangle2D;

/**
 * A set of static utilties for dealing with relationships between
 * rectangles, specifying important sites on rectangles, or angular
 * directions.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.9 $
 * @rating  Red
 */
public class RelationUtilities {
    /**
     * Positional constraint: does A intersect B?
     */
    public static final int INTERSECTS = 1;

    /**
     * Positional constraint: does A contain B?
     */
    public static final int CONTAINS = 2;
    
    /**
     * Positional constraint: is B adjacent to A?
     */
    public static final int ADJACENT = 3;
    
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
     * No size constant.
     */
    public static final int NO_SIZE_RATIO = -1;
    
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
     * Makes no sense to instantiate one of these, so we make the
     * constructor private.
     */
    private RelationUtilities() {
    }
    
    /**
     * Check the given site's validity; throw an illegal argument
     * exception if it is not valid.
     */
    public static void checkSite (int site) {
        if(site < NORTH_EAST || site > CENTER) {
            String err = "Invalid site ID: " + site;
            throw new IllegalArgumentException(err);
        }
    }

    /**
     * Return the angle in radians corresponding to the given
     * direction constant (EAST is 0 radians).  This method throws an
     * IllegalArgumentException if it gets CENTER or an input it
     * doesn't recognize.
     */
    public static double directionToAngle (int direction) {
        switch(direction) {
        case NORTH_EAST:
            return Math.PI/4;
        case NORTH:
            return Math.PI/2;     
        case NORTH_WEST:
            return 3*Math.PI/4;
        case WEST:
            return Math.PI;
        case SOUTH_WEST:
            return 5*Math.PI/4;
        case SOUTH:
            return 3*Math.PI/2;     
        case SOUTH_EAST:
            return 7*Math.PI/4;
        case EAST:
            return 0;
        case CENTER:
            String err1 = "CENTER direction implies no angle";
            throw new IllegalArgumentException(err1);
        default:
            String err2 = "Unknown direction: " + direction;
            throw new IllegalArgumentException(err2);
        }
    }

    /**
     * Return a site constant given a string.  This method is
     * case insensitive, and accepts input of format "NORTH_EAST",
     * "NORTH-EAST" or "NORTHEAST".  This method throws an
     * IllegalArgumentException if it gets an input it doesn't
     * recognize.
     */
    public static int parseSite (String site) {
        return parseDirection(site);
    }

    /**
     * Return a direction constant given a string.  This method is
     * case insensitive, and accepts input of format "NORTH_EAST",
     * "NORTH-EAST" or "NORTHEAST".  This method throws an
     * IllegalArgumentException if it gets an input it doesn't
     * recognize.
     */
    public static int parseDirection (String dir) {
        if(dir.equalsIgnoreCase("NORTH_EAST") ||
                dir.equalsIgnoreCase("NORTH-EAST") ||
                dir.equalsIgnoreCase("NORTHEAST")) {
            return NORTH_EAST;
        }
        else if(dir.equalsIgnoreCase("NORTH_WEST") ||
                dir.equalsIgnoreCase("NORTH-WEST") ||                
                dir.equalsIgnoreCase("NORTHWEST")) {
            return NORTH_WEST;
        }
        else if(dir.equalsIgnoreCase("SOUTH_WEST") ||
                dir.equalsIgnoreCase("SOUTH-WEST") ||                
                dir.equalsIgnoreCase("SOUTHWEST")) {
            return SOUTH_WEST;
        }
        else if(dir.equalsIgnoreCase("SOUTH_EAST") ||
                dir.equalsIgnoreCase("SOUTH-EAST") ||                
                dir.equalsIgnoreCase("SOUTHEAST")) {
            return SOUTH_EAST;
        }
        else if(dir.equalsIgnoreCase("NORTH")) {
            return NORTH;
        }
        else if(dir.equalsIgnoreCase("SOUTH")) {
            return SOUTH;
        }
        else if(dir.equalsIgnoreCase("EAST")) {
            return EAST;
        }
        else if(dir.equalsIgnoreCase("WEST")) {
            return WEST;
        }
        else if(dir.equalsIgnoreCase("CENTER")) {
            return CENTER;
        }
        throw new IllegalArgumentException("Unknown direction: " + dir);
    }
    
    /**
     * Return a position constant given a string.  This method is case
     * insensitive, and throws an illegal argument exception if it
     * gets an input it doesn't recognize.
     */
    public static int parseOverlap (String overlap) {
        if(overlap.equalsIgnoreCase("INTERSECTS")) {
            return INTERSECTS;
        }
        else if(overlap.equalsIgnoreCase("CONTAINS")) {
            return CONTAINS;
        }
        else if(overlap.equalsIgnoreCase("ADJACENT")) {
            return ADJACENT;
        }
        throw new IllegalArgumentException("Unknown overlap: " + overlap);
    }

    /**
     * Return a string given a position constant.  This method throws
     * an IllegalArgumentException if it gets an input it doesn't
     * recognize.
     */
    public static String printOverlap (int overlap) {
        switch(overlap) {
        case ADJACENT:
            return "ADJACENT";
        case INTERSECTS:
            return "INTERSECTS";
        case CONTAINS:
            return "CONTAINS";
        default:
            String err = "Unknown overlap: " + overlap;
            throw new IllegalArgumentException(err);
        }
    }


    /**
     * Return a string given a site constant.  This method throws
     * an IllegalArgumentException if it gets an input it doesn't
     * recognize.
     */
    public static String printSite (int site) {
        return printDirection(site);
    }
    
    /**
     * Return a string given a direction constant.  This method throws
     * an IllegalArgumentException if it gets an input it doesn't
     * recognize.
     */
    public static String printDirection (int direction) {
        switch(direction) {
        case NORTH_EAST:
            return "NORTH_EAST";
        case NORTH:
            return "NORTH";
        case NORTH_WEST:
            return "NORTH_WEST";
        case WEST:
            return "WEST";
        case SOUTH_WEST:
            return "SOUTH_WEST";
        case SOUTH:
            return "SOUTH";
        case SOUTH_EAST:
            return "SOUTH_EAST";
        case EAST:
            return "EAST";
        case CENTER:
            return "CENTER";
        default:
            String err = "Unknown direction: " + direction;
            throw new IllegalArgumentException(err);
        }
    }

    /**
     * Return the X coordinate of the given site for the given
     * rectangle.
     */
    public static double siteX (Rectangle2D r, int site) {
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
     * Return a relation with the given name and the
     * two given sites.  If the given relation uses
     * sites, the default RelationUtilities.CENTER
     * will be used.
     */
    public static Relation makeRelation(String relationType) {
        return makeRelation(relationType, RelationUtilities.CENTER,
                RelationUtilities.CENTER);
    }

    /**
     * Return a relation with the given name and the
     * two given sites.  If the given relation does not
     * use sites, these arguments are ignored.
     */
    public static Relation makeRelation(String relationType,
            int site1, int site2) {
        if(relationType.equals(AngleRelation.NAME)) {
            return new AngleRelation(site1, site2);
        }
        else if(relationType.equals(AreaRatioRelation.NAME)) {
            return new AreaRatioRelation();
        }
        else if(relationType.equals(DeltaXRelation.NAME)) {
            return new DeltaXRelation(site1, site2);
        }
        else if(relationType.equals(DeltaYRelation.NAME)) {
            return new DeltaYRelation(site1, site2);
        }
        else if(relationType.equals(DistanceRelation.NAME)) {
            return new DistanceRelation(site1, site2);
        }
        else if(relationType.equals(HeightRatioRelation.NAME)) {
            return new HeightRatioRelation();
        }
        else if(relationType.equals(OverlapRelation.NAME)) {
            return new OverlapRelation();
        }
        else if(relationType.equals(WidthRatioRelation.NAME)) {
            return new WidthRatioRelation();
        }
        else {
            throw new IllegalArgumentException("Illegal relation type: " +
                    relationType);
        }
    }

    /**
     * Return the Y coordinate of the given site for the given
     * rectangle.
     */
    public static double siteY (Rectangle2D r, int site) {
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
}






