package uk.ac.starlink.vo;

import javax.swing.JMenu;

/**
 * Mixin interface describing some behaviours of a load dialogue
 * operating with Data Access Layer services.
 *
 * @author   Mark Taylor
 * @since    7 Jul 2015
 */
public interface DalLoader {

    /**
     * Returns an array of menus which may be presented in the window
     * alongside the query component.
     *
     * @return   menu array; may be empty
     */
    JMenu[] getMenus();

    /**
     * Sets the menus for this dialogue.
     *
     * @param  menus  menu array
     */
    void setMenus( JMenu[] menus );

    /**
     * Returns the registry panel for this dialogue.
     *
     * @return  registry panel
     */
    RegistryPanel getRegistryPanel();

    /**
     * Takes a list of resource ID values and may load them or a subset
     * into this object's dialogue as appropriate.
     *
     * @param  ivoids  ivo:-type identifier strings
     * @param  msg   text of user-directed message to explain where the
     *         IDs came from
     * @return  true iff at least some of the resources were, or may be,
     *          loaded into this window
     */
    boolean acceptResourceIdList( String[] ivoids, String msg );
}
