package com.easytravel.web.security;

/**
 * WHAT:   The website's two roles; the same names as the Keycloak realm roles.
 *
 * @author AoceanP
 */
public final class Roles
{
    /** WHAT: books, views and cancels their own trips. */
    public static final String CUSTOMER = "customer";

    /** WHAT: opens the Ops room and uses its controls. */
    public static final String OPS = "ops";

    /**
     * WHAT:   Not instantiable.
     * WHY:    Constants only.
     */
    private Roles()
    {
    }
}
