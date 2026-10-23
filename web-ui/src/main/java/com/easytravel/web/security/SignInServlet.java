package com.easytravel.web.security;

import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * WHAT:   /signin: the Sign in button's target. Being protected, it starts the
 *         Keycloak sign-in; once signed in, it sends each role to its home page.
 * WHY:    One entry point for everyone: an ops user lands in the Ops room, a
 *         customer on the Book page.
 *
 * @author AoceanP
 */
@WebServlet("/signin")
@ServletSecurity(@HttpConstraint(rolesAllowed = { Roles.CUSTOMER, Roles.OPS }))
public class SignInServlet extends HttpServlet
{
    /**
     * WHAT:   Redirects a signed-in user to their home page.
     * PRE:    the caller is signed in (the constraint guarantees it)
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
        String home = request.isUserInRole(Roles.OPS) ? "/ops" : "/book";
        response.sendRedirect(request.getContextPath() + home);
    }
}
