package com.easytravel.web.security;

import jakarta.inject.Inject;
import jakarta.security.enterprise.identitystore.openid.OpenIdContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * WHAT:   /signout: signs the user out of the website AND Keycloak, in one click.
 * WHY:    POST only, so a link on another site cannot sign someone out.
 *         The website then sends the browser to Keycloak's logout with the
 *         user's ID token as "id_token_hint": proof that this signed-in user
 *         asked to leave, so Keycloak ends its session without an extra
 *         "Do you want to log out?" question. (Jakarta Security's built-in
 *         provider logout sends no hint, which is why Keycloak asked.)
 *         request.logout() sends the redirect itself (a servlet cannot add one
 *         after it), so the URL is handed over through the SignOut bean.
 *
 * @author AoceanP
 */
@WebServlet("/signout")
public class SignOutServlet extends HttpServlet
{
    /** WHAT: the signed-in user's tokens and Keycloak's addresses. */
    @Inject
    OpenIdContext openId;

    /** WHAT: carries the logout target to the logout definition for this request. */
    @Inject
    SignOut signOut;

    /** WHAT: where Keycloak sends the browser afterwards (an allowed post-logout URL of the client). */
    @Inject
    @ConfigProperty(name = "easytravel.oidc.post-logout-uri")
    String afterLogout;

    /**
     * WHAT:   Signs out locally, then at Keycloak.
     * PRE:    none (signing out twice is harmless)
     * POST:   no session here; the browser is on its way to Keycloak's logout, or the Sign in page
     * THROWS: IOException if the redirect cannot be sent; ServletException if logout fails
     *
     * @param request  the request
     * @param response the response
     * @throws IOException      if the redirect fails
     * @throws ServletException if the container cannot log out
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        if (request.getUserPrincipal() == null)
        {
            response.sendRedirect(request.getContextPath() + "/login.xhtml");
            return;
        }
        // read what Keycloak needs BEFORE the local sign-out discards the tokens;
        // request.logout() then redirects to it (see SecurityConfig's logout definition)
        String keycloakLogout = keycloakLogoutUrl();
        signOut.setTarget(keycloakLogout != null ? keycloakLogout : request.getContextPath() + "/login.xhtml");

        request.logout();
        HttpSession session = request.getSession(false);
        if (session != null)
        {
            session.invalidate();
        }
        if (!response.isCommitted())
        {
            response.sendRedirect(signOut.getTarget());
        }
    }

    /**
     * WHAT:   Keycloak's logout URL with the ID token hint and the return address.
     * PRE:    the caller is signed in with OpenID Connect
     * RETURN: the URL, or null if the tokens or the endpoint are not available
     *
     * @return the logout URL
     */
    private String keycloakLogoutUrl()
    {
        try
        {
            String endSession = openId.getProviderMetadata().getString("end_session_endpoint", null);
            String idToken = openId.getIdentityToken().getToken();
            if (endSession == null || idToken == null)
            {
                return null;
            }
            return endSession + "?id_token_hint=" + encode(idToken) + "&post_logout_redirect_uri=" + encode(afterLogout);
        }
        catch (RuntimeException notAvailable)
        {
            return null;
        }
    }

    /**
     * WHAT:   URL-encodes a query value.
     * PRE:    value is not null
     * RETURN: the encoded value
     *
     * @param value the value
     * @return the encoded value
     */
    private static String encode(String value)
    {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
