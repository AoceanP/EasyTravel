package com.easytravel.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * WHAT:   Facts about this build that pages can read as #{app.…}.
 * WHY:    The build stamp is added to the stylesheet URL, so a redeployed
 *         site is never shown with a browser's cached copy of old styles.
 *
 * @author AoceanP
 */
@Named("app")
@ApplicationScoped
public class AppInfo
{
    /** WHAT: when the WAR was built, e.g. "20261023103112"; "dev" if unfiltered. */
    @Inject
    @ConfigProperty(name = "easytravel.build", defaultValue = "dev")
    String build;

    /**
     * WHAT:   The build stamp.
     * PRE:    none
     * RETURN: the stamp
     *
     * @return the stamp
     */
    public String getBuild()
    {
        return build;
    }
}
