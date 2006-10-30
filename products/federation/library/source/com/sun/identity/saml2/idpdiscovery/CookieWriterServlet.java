/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: CookieWriterServlet.java,v 1.1 2006-10-30 23:16:17 qcheng Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.saml2.idpdiscovery;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import java.io.IOException;
import java.util.StringTokenizer;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

/**
 * The Writer Service is used by the identity provider. After successful 
 * authentication, the common domain cookie is appended with the query parameter
 * _saml_idp=entity-ID-of-identity-provider. This parameter is used to redirect
 * the principal to the Writer Service URL defined for the identity provider.
 * The URL is configured as the value for the Writer Service URL attribute when
 * an authentication domain is created. Use the format 
 * http://common-domain-host:port/deployment-uri/saml2writer where 
 * common-domain-host:port refers to the machine on which the Common Domain 
 * Services are installed and deployment-uri tells the web container where to 
 * look for information specific to the application (such as classes or JARs).
 * The default URI is amcommon.
 */
public class CookieWriterServlet extends HttpServlet {
    private static String INTRODUCTION_COOKIE_TYPE =
        SystemPropertiesManager.get(
            IDPDiscoveryConstants.IDPDISCOVERY_COOKIE_TYPE);
    private static String INTRODUCTION_URL_SCHEME =
        SystemPropertiesManager.get(
            IDPDiscoveryConstants.IDPDISCOVERY_URL_SCHEME);
    private static String INTRODUCTION_COOKIE_DOMAIN =
        SystemPropertiesManager.get(
            IDPDiscoveryConstants.IDPDISCOVERY_COOKIE_DOMAIN);
   
    private String preferred_cookie_name = null; 
        
    /**
     * Gets handle to debug.
     * @param config the ServletConfig object that contains configutation
     *                  information for this servlet.
     * @exception ServletException if an exception occurs that interrupts
     *                  the servlet's normal operation.
     */
    public void init(ServletConfig config)
    throws ServletException {
        super.init(config);
        if (CookieUtils.debug.messageEnabled()) {
            CookieUtils.debug.message("CookieWriterServlet Initializing...");
        }
    }
    
    /**
     * Handles the HTTP GET request.
     * @param request an HttpServletRequest object that contains the request
     *                the client has made of the servlet.
     * @param response an HttpServletResponse object that contains the response
     *                 the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the GET request
     * @exception IOException if the request for the GET could not be handled
     */
    public void doGet(HttpServletRequest  request,
    HttpServletResponse response)
    throws ServletException, IOException {
        doGetPost(request, response);
    }
    
    /**
     * Handles the HTTP POST request.
     *
     * @param request an HttpServletRequest object that contains the request
     *                the client has made of the servlet.
     * @param response an HttpServletResponse object that contains the response
     *                 the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the GET request
     * @exception IOException if the request for the GET could not be handled
     */
    public void doPost(HttpServletRequest  request,
    HttpServletResponse response)
    throws ServletException, IOException {
        doGetPost(request, response);
    }
    
    /**
     * Description :  The QueryString will contain providerid=<URL encoded URI>,
     *   LRURL=URL to redirect to after setting the preferred IDP cookie
     *
     * @param request an HttpServletRequest object that contains the request
     *                the client has made of the servlet.
     * @param response an HttpServletResponse object that contains the response
     *                 the servlet sends to the client.
     * @exception ServletException if an input or output error is detected when
     *                             the servlet handles the GET request
     * @exception IOException if the request for the GET could not be handled
     */
    private void doGetPost( HttpServletRequest  request,
    HttpServletResponse response)
    throws ServletException, IOException {

        String classMethod = "CookieWriterServlet.doGetPost: ";
        preferred_cookie_name = CookieUtils.getPreferCookieName(
            request.getRequestURI());
        if (preferred_cookie_name == null) {
            CookieUtils.debug.error( classMethod + 
                "The request uri is null.");
            response.sendError(
                response.SC_INTERNAL_SERVER_ERROR,
                "The request uri is null."); 
            return;
        } else if (preferred_cookie_name.equals("")) { 
            CookieUtils.debug.error( classMethod + 
                "Cannot match the cookie name from " +
                "the request uri.");
            response.sendError(
                response.SC_INTERNAL_SERVER_ERROR,
                "Cookie name is not found based on " + 
                "the request uri."); 
            return;
        }
        if (CookieUtils.debug.messageEnabled()) {
            CookieUtils.debug.message(classMethod + 
                "Preferred Cookie Name is " + preferred_cookie_name); 
        }
        try {
            if (INTRODUCTION_COOKIE_TYPE == null || 
                INTRODUCTION_COOKIE_TYPE.trim().length() <= 0) {
                if (CookieUtils.debug.messageEnabled()) {
                    CookieUtils.debug.message(
                        classMethod +
                        "Cookie type is null, set to persistent."
                    );
                }
                INTRODUCTION_COOKIE_TYPE =
                    IDPDiscoveryConstants.PERSISTENT_COOKIE;
            }  
            if (INTRODUCTION_URL_SCHEME == null || 
                INTRODUCTION_URL_SCHEME.trim().length() <= 0) {
                if (CookieUtils.debug.messageEnabled()) {
                    CookieUtils.debug.message(
                        classMethod +
                        "URL Scheme is null, set to https."
                    );
                }
                INTRODUCTION_URL_SCHEME = IDPDiscoveryConstants.HTTPS;
            }
            boolean isValidReturn = false;
            String returnURL =
                request.getParameter(IDPDiscoveryConstants.LRURL);
            if (returnURL == null ||
                returnURL.trim().length() <= 0) {
                // Redirect URL not specified. Do nothing
                // Do not throw any error page to user as this operation is done
                // behind the screens.
                CookieUtils.debug.error(
                    classMethod +
                    "Redirect URL not specified. " +
                    "Cannot reset Preferred IDP cookie.");
                isValidReturn = false;
            } else {
                isValidReturn = true;
            }
            String providerId = request.getParameter(
                preferred_cookie_name
            );
            if (providerId == null ||
                providerId.trim().length() <= 0) {
                // Nothing to reset in preferred IDP cookie. Do nothing
                // Do not throw any error page to user as this operation is
                // done behind the screens.                
                CookieUtils.debug.error(
                    classMethod +
                    "Provider Id not in request, "+
                    "Cannot reset preferred idp."
                );
                if (isValidReturn) {
                    response.sendRedirect(returnURL);
                } else {
                    response.sendError(
                        response.SC_INTERNAL_SERVER_ERROR,
                        "No Redirect URL"
                    );
                }
                return;
            }            
            String cookieValue =
                CookieUtils.getCookieValueFromReq(
                    request,
                    preferred_cookie_name
                );
            if (cookieValue == null ||
                cookieValue.trim().length() <= 0) {
                // Preferred IDP Cookie does not exist.
                // Create a new cookie with this provider id as the only
                // value                   
                if (CookieUtils.debug.messageEnabled()) {
                     
                CookieUtils.debug.message(
                    classMethod +
                    "Preferred IDP Cookie Not found"
                );           
                }                                               
                cookieValue = Base64.encode(
                    generateSuccintID(providerId)
                );
            } else {
                cookieValue =
                    resetPreferredIDPCookie(
                        cookieValue, providerId
                    );
            }
            if (CookieUtils.debug.messageEnabled()) {
                CookieUtils.debug.message(
                    classMethod +
                    "Cookie Type is " +
                    INTRODUCTION_COOKIE_TYPE
                );
                CookieUtils.debug.message(
                    classMethod +
                    "Cookie value is " +
                    cookieValue
                );
                CookieUtils.debug.message(
                    classMethod +
                    "Preferred Cookie Name " +
                    preferred_cookie_name
                );                    
            }
            int maxAge;
            String domain = null;
            if (!(INTRODUCTION_COOKIE_DOMAIN == null || 
                  INTRODUCTION_COOKIE_DOMAIN.length() < 1)) {
                domain = INTRODUCTION_COOKIE_DOMAIN;
            } 
            if (INTRODUCTION_COOKIE_TYPE.equalsIgnoreCase(
                IDPDiscoveryConstants.SESSION_COOKIE)) {
                maxAge = IDPDiscoveryConstants.SESSION_COOKIE_AGE;
            } else {
                maxAge = IDPDiscoveryConstants.PERSISTENT_COOKIE_AGE;
            }
            Cookie idpListCookie =
                CookieUtils.newCookie(
                    preferred_cookie_name,
                    cookieValue,
                    maxAge,
                    "/",
                    domain
                );
            response.addCookie(idpListCookie);
            if(isValidReturn) {
                if (CookieUtils.debug.messageEnabled()) {
                    CookieUtils.debug.message(
                        classMethod +
                        "Redirect to " +
                        returnURL
                    );                        
                }
                response.sendRedirect(returnURL);
            } else {
                if (CookieUtils.debug.messageEnabled()) {
                    CookieUtils.debug.message(
                        classMethod +
                        "No return URL. " +
                        "Set preferred IDP cookie and "+
                        "return error page"
                    );
                }
                response.sendError(
                    response.SC_INTERNAL_SERVER_ERROR,
                    "No Redirect URL"
                );
            }
            return;
        } catch(IOException e) {
            CookieUtils.debug.error(classMethod, e);
        }
    }    
    
    /**
     * This function is used to reset the preferred IDP cookie based on the
     * present value and the providerId of the IDP that calls this service
     * @param existing cookie value
     * @param current IDP provider ID that will be added to the top of the list
     * @return cookie value that needs to be set as the preferred IDP cookie
     */
    private String resetPreferredIDPCookie(
        String existingCookieValue,
        String toAddCookieValue) {
        // Steps
        // 1. Check if existingCookieValue has toAddCookieValue
        // 2. If yes remove that value from existingCookieValue
        // 3. append toAddCookieValue to existingCookieValue at end and return
        StringBuffer returnCookie = new StringBuffer();        
        String encodedCookieToAdd = 
            Base64.encode(
                generateSuccintID(toAddCookieValue)
            );
        StringTokenizer st =
            new StringTokenizer(
                existingCookieValue,
                IDPDiscoveryConstants.PREFERRED_COOKIE_SEPERATOR
            );
        while (st.hasMoreTokens()) {
            String curIdpString = (String)st.nextToken();
            if (curIdpString.equals(encodedCookieToAdd)) {
                continue;
            } else {
                returnCookie.append(curIdpString + " ");
            }            
        }
        returnCookie.append(encodedCookieToAdd);
        return returnCookie.toString();
    }

    public byte[] generateSuccintID(String providerURL) {
        if (providerURL == null ||
            providerURL.length() == 0) {
            return null;
        }
        byte[] returnBytes = null;
        try {
            returnBytes = providerURL.getBytes("UTF-8");
        } catch (Exception e) {
            CookieUtils.debug.error("CookieWriterServlet.generateSuccintID: ",
                                   e);
            returnBytes = null;
        }
        return returnBytes;
    }
}
