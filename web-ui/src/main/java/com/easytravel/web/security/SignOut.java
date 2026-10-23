package com.easytravel.web.security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

/**
 * WHAT:   Where the browser goes after signing out, for this one request.
 * WHY:    request.logout() makes Jakarta Security send its own redirect, so a
 *         servlet cannot redirect afterwards. Instead SignOutServlet stores the
 *         Keycloak logout URL (with the user's id_token_hint) here first, and
 *         the logout definition reads it as #{signOut.target} at logout time.
 *
 * @author AoceanP
 */
@Named("signOut")
@RequestScoped
public class SignOut
{
    /** WHAT: the address to send the browser to; the Sign in page unless set. */
    private String target = "/login.xhtml";

    /**
     * WHAT:   The address the logout redirect uses.
     * RETURN: the Keycloak logout URL, or the Sign in page
     *
     * @return the target
     */
    public String getTarget()
    {
        return target;
    }

    /**
     * WHAT:   Sets the address.
     * PRE:    target is an absolute URL or a path of this site
     * POST:   used by the next logout in this request
     *
     * @param target the address
     */
    public void setTarget(String target)
    {
        this.target = target;
    }
}
