package com.easytravel.web.security;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * WHAT:   /callback: where Keycloak sends the browser back after sign-in.
 * WHY:    Jakarta Security handles the callback itself (it swaps the code for
 *         tokens and returns the user to the page they asked for). This
 *         servlet only gives the address a home, so a stray visit lands on
 *         the Sign in page instead of a 404.
 *
 * @author AoceanP
 */
@WebServlet("/callback")
public class CallbackServlet extends HttpServlet
{
    /**
     * WHAT:   Sends a stray visit to the Sign in page.
     * PRE:    the security mechanism did not already answer
     * POST:   the response is a redirect
     * THROWS: IOException if the redirect cannot be sent
     *
     * @param request  the request
     * @param response the response
     * @throws IOException if the redirect fails
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        if (!response.isCommitted())
        {
            response.sendRedirect(request.getContextPath() + "/signin");
        }
    }
}
