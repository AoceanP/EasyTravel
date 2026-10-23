package com.easytravel.web.book;

import com.easytravel.web.security.Roles;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.security.enterprise.SecurityContext;

import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;

/**
 * WHAT:   The person using this browser session: who they are (from the
 *         Keycloak sign-in), what they may do, and their live-update key.
 * WHY:    Bookings are made for the signed-in user, so each customer sees only
 *         their own trips. The random push key lets live updates reach only
 *         the browser that made the booking.
 *
 * @author AoceanP
 */
@Named
@SessionScoped
public class Visitor implements Serializable
{
    /** WHAT: the random key the Faces websocket uses to address this session. */
    private final String key = UUID.randomUUID().toString();

    /** WHAT: Jakarta Security's view of the caller (a serializable CDI proxy). */
    @Inject
    SecurityContext security;

    /**
     * WHAT:   The push key for this session.
     * PRE:    none
     * RETURN: a random UUID, fixed for the session
     *
     * @return the key
     */
    public String getKey()
    {
        return key;
    }

    /**
     * WHAT:   Who bookings are made for: the Keycloak username.
     * PRE:    the caller is signed in (every page that books or lists trips requires it)
     * RETURN: the username, e.g. "alice"; "guest" if not signed in
     *
     * @return the user id
     */
    public String getUserId()
    {
        Principal p = security.getCallerPrincipal();
        return p == null ? "guest" : p.getName();
    }

    /**
     * WHAT:   The first letter for the avatar.
     * RETURN: e.g. "A"
     *
     * @return the initial
     */
    public String getInitial()
    {
        String id = getUserId();
        return id.isEmpty() ? "?" : id.substring(0, 1).toUpperCase();
    }

    /**
     * WHAT:   Whether someone is signed in.
     * RETURN: true when there is a caller
     *
     * @return whether signed in
     */
    public boolean isSignedIn()
    {
        return security.getCallerPrincipal() != null;
    }

    /**
     * WHAT:   Whether the user may open the Ops room.
     * RETURN: true for the ops role
     *
     * @return whether ops
     */
    public boolean isOps()
    {
        return security.isCallerInRole(Roles.OPS);
    }
}
