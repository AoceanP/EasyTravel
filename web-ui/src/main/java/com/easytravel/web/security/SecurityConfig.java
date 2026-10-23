package com.easytravel.web.security;

import jakarta.annotation.security.DeclareRoles;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.security.enterprise.authentication.mechanism.http.OpenIdAuthenticationMechanismDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.ClaimsDefinition;
import jakarta.security.enterprise.authentication.mechanism.http.openid.LogoutDefinition;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * WHAT:   Turns on sign-in with OpenID Connect (Keycloak) for the whole website,
 *         and supplies its settings to the annotation as #{oidcConfig.…}.
 * WHY:    Jakarta Security 4.0's standard mechanism, so no vendor-specific
 *         code: the authorization code flow, tokens kept server-side in the
 *         session, and the user's realm roles (the "groups" claim) become the
 *         caller's groups, which web.xml's security constraints check.
 *         The client secret is read only from configuration (the
 *         EASYTRAVEL_OIDC_SECRET environment variable, from the local .env);
 *         it is never in the code or the repository.
 *
 * @author AoceanP
 */
@OpenIdAuthenticationMechanismDefinition(
        providerURI = "${oidcConfig.providerUri}",
        clientId = "${oidcConfig.clientId}",
        clientSecret = "${oidcConfig.clientSecret}",
        redirectURI = "${baseURL}/callback",
        redirectToOriginalResource = true,
        scope = { "openid", "profile", "email" },
        claimsDefinition = @ClaimsDefinition(callerNameClaim = "preferred_username", callerGroupsClaim = "groups"),
        // request.logout() redirects here; SignOutServlet fills in Keycloak's logout
        // URL with the user's id_token_hint first (deferred #{} = read at logout time)
        logout = @LogoutDefinition(notifyProvider = false, redirectURI = "#{signOut.target}"))
@DeclareRoles({ Roles.CUSTOMER, Roles.OPS })
@Named("oidcConfig")
@ApplicationScoped
public class SecurityConfig
{
    /**
     * WHAT:   The realm's address, e.g. http://keycloak.localhost:8180/realms/easytravel.
     * WHY:    The same address works for the browser and inside Docker, so
     *         the issuer in every token matches what the website expects.
     * PRE:    none
     * RETURN: the provider URI
     *
     * @return the provider URI
     */
    public String getProviderUri()
    {
        return value("easytravel.oidc.provider-uri");
    }

    /**
     * WHAT:   The website's client id in Keycloak.
     * PRE:    none
     * RETURN: the client id
     *
     * @return the client id
     */
    public String getClientId()
    {
        return value("easytravel.oidc.client-id");
    }

    /**
     * WHAT:   The website's client secret.
     * WHY:    Only from configuration; startup fails clearly if it is missing,
     *         rather than running with a guessable default.
     * PRE:    EASYTRAVEL_OIDC_SECRET is set
     * RETURN: the secret
     * THROWS: java.util.NoSuchElementException if it is not configured
     *
     * @return the secret
     */
    public String getClientSecret()
    {
        return value("easytravel.oidc.secret");
    }

    /**
     * WHAT:   Reads one required setting.
     * PRE:    name is a configured property
     * RETURN: its value
     * THROWS: java.util.NoSuchElementException if missing
     *
     * @param name the property
     * @return the value
     */
    private static String value(String name)
    {
        return ConfigProvider.getConfig().getValue(name, String.class);
    }
}
