// Copyright 1996, Marimba Inc. All Rights Reserved.
// @(#)Sortable.java, 1.3, 06/10/96

package jsky.util;

/**
 * An object that can be sorted.
 *
 * @author  Jonathan Payne
 * @version     1.3, 06/10/96
 */
public interface Sortable {

    int compareTo(Sortable other, Object rock);
}
