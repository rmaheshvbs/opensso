/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
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
 * $Id: CDCServlet.java,v 1.13 2009-11-13 23:43:17 dknab Exp $
 *
 */

package com.iplanet.services.cdc;

import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.TokenRestriction;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.message.FSAssertion;
import com.sun.identity.federation.message.FSAuthenticationStatement;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.FSAuthnResponse;
import com.sun.identity.federation.message.FSSubject;
import com.sun.identity.federation.message.common.AuthnContext;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.message.common.IDPProvidedNameIdentifier;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.saml.assertion.AssertionIDReference;
import com.sun.identity.saml.assertion.AudienceRestrictionCondition;
import com.sun.identity.saml.assertion.Conditions;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.assertion.SubjectConfirmation;
import com.sun.identity.saml.assertion.SubjectLocality;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.protocol.Status;
import com.sun.identity.saml.protocol.StatusCode;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * The <code>CDCServlet</code> is the heart of the Cross Domain Single
 * Signon mechanism of OpenSSO.
 * <p>
 * The following is the algorithm used by the program.
 * <ol>
 * <li> If request does not contain SSO related cookie redirect request to
 *      the auth service</li>
 * <li> if request contains SSO related cookie
 *      <ul>
 *      <li>Retrieve the cookie related to SSO namely
 *          <code>iPlanetDirectoryPro</code> from request.</li>
 *      <li> Create Liberty <code>AuthnResponse</code> with the SSO cookie as
 *           the Name Identifier.</li>
 *      <li>Send the Response as Form POST to the original request
 *          requested using the goto parameter in the query string.</li>
 *      </ul>
 * </li>
 * </ol>
 */
public class CDCServlet extends HttpServlet {
    private static final String UNIQUE_COOKIE_NAME =
        "sunIdentityServerAuthNServer";
    private static final String DEFAULT_DEPLOY_URI = "/amserver";
    private static final String GOTO_PARAMETER = "goto";
    private static final String TARGET_PARAMETER = "TARGET";
    private static final String CDCURI = "/cdcservlet";
    private static final String AUTHURI = "/UI/Login";
    private static final String PROVIDER_ID = "ProviderID";
    private static final String REQUEST_ID = "RequestID";
    private static final String RELAY_STATE = "RelayState";
    private static final String SELF_PROVIDER_ID =
        FSServiceUtils.getBaseURL() + CDCURI;
    
    private static final List adviceParams = new ArrayList();
    private static HashSet invalidSet = new HashSet();
    private static final String LEFT_ANGLE              = "<";
    private static final String RIGHT_ANGLE             = ">";
    private static final String URLENC_RIGHT_ANGLE      = "%3e";
    private static final String URLENC_LEFT_ANGLE       = "%3c";
    private static final String URLENC_JAVASCRIPT       = "javascript%3a";
    private static final String JAVASCRIPT              = "javascript:";
    private static final String DELIM                   = ",";
    private static final String DEBUG_FILE_NAME         = "amCDC";
    static Debug  debug = Debug.getInstance(DEBUG_FILE_NAME);
    static {
        adviceParams.add("module");
        adviceParams.add("authlevel");
        adviceParams.add("role");
        adviceParams.add("service");
        adviceParams.add("user");
        adviceParams.add("realm");
        adviceParams.add("org");
        adviceParams.add("domain");
        adviceParams.add("sunamcompositeadvice");
        adviceParams.add("resource");
        String invalidStrings = SystemPropertiesManager.get(
            Constants.INVALID_GOTO_STRINGS);
        if (invalidSet.isEmpty()) {
            debug.message("CDCServlet:static block: creating invalidSet");
            if (invalidStrings == null) {
                debug.message("CDCServlet: invalidStrings is null");
                invalidSet.add(LEFT_ANGLE);
                invalidSet.add(RIGHT_ANGLE);
                invalidSet.add(URLENC_LEFT_ANGLE);
                invalidSet.add(URLENC_RIGHT_ANGLE);
                invalidSet.add(JAVASCRIPT);
                invalidSet.add(URLENC_JAVASCRIPT);
            } else {
                debug.message("CDCServlet: invalidStrings is NOT null");
                StringTokenizer st = new StringTokenizer(invalidStrings, DELIM);
                while ( st.hasMoreTokens()) {
                    invalidSet.add((String)st.nextToken());
                }
            }
        }
    }
    private static final String AUTHN_RESPONSE_HTML = 
        "<HTML><BODY Onload=\"document.Response.submit()\">" +
        "<FORM NAME=\"Response\" METHOD=\"POST\" ACTION=\"{0}\">" +
        "<INPUT TYPE=\"HIDDEN\" NAME=\"" +
            IFSConstants.POST_AUTHN_RESPONSE_PARAM + "\" " + "VALUE=\"{1}\"/>" +
        "<NOSCRIPT><CENTER>" +
        "<INPUT TYPE=\"SUBMIT\" VALUE=\"" +
            FSUtils.bundle.getString("laresPostCustomKey") +
            "\"/></CENTER></NOSCRIPT>" +
        "</FORM></BODY></HTML>";
    
    private SSOTokenManager tokenManager;
    private SessionService sessionService;
    private SPValidator spValidator;
    private String DNSAddress = "localhost";
    private String IPAddress = "127.0.0.1";
    private String authURLCookieName;
    private String authURLCookieDomain;
    private String deployDescriptor;
    private String policyAdviceList;
    private String responseID;
    private boolean uniqueCookieEnabled;
    
    /**
     * Initiates the servlet.
     *
     * @param config Servlet Configuration object that contains configutation
     *        information for this servlet.
     * @throws ServletException if servlet failed to initialize.
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        debug.message("CDCServlet Initializing...");
        
        try {
            tokenManager = SSOTokenManager.getInstance();
            sessionService = SessionService.getSessionService();
            spValidator = new LdapSPValidator();
            
            DNSAddress = SystemConfigurationUtil.getProperty(
                Constants.AM_SERVER_HOST);
            IPAddress = InetAddress.getByName(DNSAddress).getHostAddress();
            authURLCookieName = SystemConfigurationUtil.getProperty(
                Constants.AUTH_UNIQUE_COOKIE_NAME, UNIQUE_COOKIE_NAME);
            authURLCookieDomain = SystemConfigurationUtil.getProperty(
                Constants.AUTH_UNIQUE_COOKIE_DOMAIN, "");
            deployDescriptor = SystemConfigurationUtil.getProperty(
                Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR, DEFAULT_DEPLOY_URI);
            
            // Check if CDC needs to generate restricted SSO Tokens
            uniqueCookieEnabled = Boolean.valueOf(
                SystemConfigurationUtil.getProperty(
                Constants.IS_ENABLE_UNIQUE_COOKIE, "false")).booleanValue();
            
            if (debug.messageEnabled()) {
                debug.message("CDCServlet init params:" +
                    " Restricted Token Enabled = " + uniqueCookieEnabled +
                    " Auth URL Cookie Name = " + authURLCookieName +
                    " Auth URL Cookie Domain = " + authURLCookieDomain +
                    " Deployment Descriptor: " + deployDescriptor);
            }
        } catch (SSOException e) {
            debug.error("CDCServlet.init: Unable to get SSOTokenManager", e);
            throw new ServletException(e.getMessage());
        } catch(UnknownHostException e) {
            debug.error("CDCServlet.init", e);
            throw new ServletException(e.getMessage());
        }
        
    }
    
    /**
     * Handles the HTTP GET request.
     *
     * @param request HTTP Servlet Request object that contains the request
     *        the client has made of the servlet.
     * @param response an HTTP Servlet Response object that contains the
     *        response the servlet sends to the client.
     * @throws ServletException if an input or output error is detected when
     *         the servlet handles the GET request
     * @throws IOException if the request for the GET could not be handled.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        doGetPost(request, response);
    }
    
    /**
     * Handles the HTTP POST request.
     *
     * @param request HTTP Servlet Request object that contains the request
     *        the client has made of the servlet.
     * @param response an HTTP Servlet Response object that contains the
     *        response the servlet sends to the client.
     * @throws ServletException if an input or output error is detected when
     *         the servlet handles the GET request.
     * @throws IOException if the request for the GET could not be handled.
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        doGetPost(request, response);
    }
    
    /**
     * Redirects the user to the authentication module if he is not
     * authenticated; otherwise redirects him back to the original referrer.
     *
     * @param request HTTP Servlet Request object that contains the request
     *        the client has made of the servlet.
     * @param response an HTTP Servlet Response object that contains the
     *        response the servlet sends to the client.
     * @throws ServletException if an input or output error is detected when
     *         the servlet handles the GET request
     * @throws IOException if the request for the GET could not be handled.
     */
    private void doGetPost(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException, IOException {
        if (debug.messageEnabled()) {
            debug.message("CDCServlet.doGetPost: Query String received: " +
                request.getQueryString());
        }
        String gotoParameter = request.getParameter(GOTO_PARAMETER);
        String targetParameter = request.getParameter(TARGET_PARAMETER);
        if (targetParameter == null) {
            targetParameter =
                request.getParameter(TARGET_PARAMETER.toLowerCase());
        }
        // if check if goto ot target have invalid strings, to avoid
        // accepting invalid injected javascript.

        if ((gotoParameter != null ) || (targetParameter != null)) {
            debug.message("CDCServlet:doGetPost():goto or target is not null");
            for (Iterator it = invalidSet.iterator(); it.hasNext();) {
                String invalidStr = (String)it.next();
                if ((gotoParameter != null ) &&
                    (gotoParameter.toLowerCase().indexOf(invalidStr) != -1 ))
                {
                    showError(response, "GOTO parameter has invalid "
                        +"characters");
                    return;
                }
                if ((targetParameter != null ) &&
                   (targetParameter.toLowerCase().indexOf(invalidStr) != -1 ))
                {
                    showError(response, "TARGET parameter has invalid "
                        +"characters");
                    return;
                }
            }
        }

        /* Steps
         * 1. If no SSOToken, forward to authentication
         * 2. If SSOToken is valid construct AuthN response and return
         */
        
        /*
         * Check for a valid SSOToken in the request. If it is not found or
         * it is invalid, redirect the user for authentication URL.
         * Also re-direct if there are policy advices in the query string
         */
        SSOToken token = getSSOToken(request, response);
        if (token == null) {
            policyAdviceList = null;
        }

        policyAdviceList = checkForPolicyAdvice(request, response);
        if ((token == null) || (policyAdviceList != null)) {
            redirectForAuthentication(request, response);
        } else {
            redirectWithAuthNResponse(request, response, token);
        }
    }
    
    /**
     * Constructs the Liberty AuthNResponse with Restricted SSOToken
     * and redirects the user to the requested resouce
     */
    private void redirectWithAuthNResponse(
        HttpServletRequest request,
        HttpServletResponse response,
        SSOToken token
    ) throws ServletException, IOException {
        String gotoURL = getRedirectURL(request, response);
        if (debug.messageEnabled()) {
            debug.message("CDCServlet.redirectWithAuthNResponse: gotoURL = " + gotoURL);
        }

        // Special characters in the path or the query need to be encoded
        try {
            gotoURL = URLEncDec.encodeUrlPath(gotoURL);
        } catch (MalformedURLException mue) {
            debug.error("CDCServlet.redirectWithAuthNResponse:MalformedURLException occured",mue);
            showError(response);
        }
        if (debug.messageEnabled()) {
            debug.message("CDCServlet.redirectWithAuthNResponse: After encoding: gotoURL = " + gotoURL);
        }

        if (gotoURL != null) {
            try {
                String inResponseTo = request.getParameter(REQUEST_ID);
                String spDescriptor = request.getParameter(PROVIDER_ID);
                
                String resTokenID = null;
                /**
                 * validateAndGetRestriction throws an exception if an agent
                 * profile with provider id and goto url is not present
                 */
                TokenRestriction tokenRes = 
                    spValidator.validateAndGetRestriction(
                            FSAuthnRequest.parseURLEncodedRequest(request),
                            gotoURL);
                if (uniqueCookieEnabled) {
                    resTokenID = sessionService.getRestrictedTokenId(
                        token.getTokenID().toString(), tokenRes);
                } else {
                    resTokenID = token.getTokenID().toString();
                }
                
                FSAssertion assertion = createAssertion(spDescriptor,
                    SELF_PROVIDER_ID,  resTokenID, token.getAuthType(),
                    token.getProperty("authInstant"),
                    token.getPrincipal().getName(), inResponseTo);
                
                String relayState = request.getParameter(RELAY_STATE);
                Status status = new Status(
                    new StatusCode(IFSConstants.STATUS_CODE_SUCCESS));
                FSAuthnResponse authnResponse = createAuthnResponse(
                    SELF_PROVIDER_ID, responseID, inResponseTo, status,
                    assertion, relayState);
                sendAuthnResponse(request, response, authnResponse, gotoURL);
            } catch(SAMLException se) {
                debug.error("CDCServlet.doGetPost", se);
                showError(response);
            } catch(FSMsgException fe){
                debug.error("CDCServlet.doGetPost", fe);
                showError(response);
            } catch(FSException fse){
                debug.error("CDCServlet.doGetPost", fse);
                showError(response);
            } catch (SessionException e) {
                debug.error("CDCServlet.doGetPost", e);
            } catch (SSOException ssoe) {
                debug.error("CDCServlet.doGetPost", ssoe);
            } catch (Exception e) {
                debug.error("CDCServlet.doGetPost", e);
                showError(response, "Access to resource is denied.");
            }
        }
    }
    
    private String getRedirectURL(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String gotoURL = request.getParameter(GOTO_PARAMETER);
        String targetURL = request.getParameter(TARGET_PARAMETER);
        
        if (gotoURL == null || (gotoURL.length() == 0)) {
            gotoURL = request.getParameter(TARGET_PARAMETER);
        }
        
        if (gotoURL == null || (gotoURL.length() == 0)) {
            // this is unlikely tohappen in a normal execution.
            debug.error("No GOTO or TARGET URL present in the Query !!");
            showError(response);
            return null;
        } else {
            if (debug.messageEnabled()) {
                debug.message("CDCServlet.getRedirectURL, URL =" + gotoURL);
            }
            return gotoURL;
        }
    }
    
    /**
     * Returns the parameters in the request as a HTTP URL string.
     * It returns all the parameters from the original request except
     * the original goto url.
     * Note: All the paramters will be url decoded by default., we should
     * make sure that these values are encoded again.
     **
     * @param request an HttpServletRequest object that contains the request
     *        the client has made of the servlet.
     * @return The parameters of the request as String.
     */
    private String getParameterString(HttpServletRequest request) {
        StringBuffer parameterString = new StringBuffer(1024);
        
        for (Enumeration e = request.getParameterNames(); e.hasMoreElements();){
            String paramName = ((String)e.nextElement());
            
            if (!paramName.equalsIgnoreCase(GOTO_PARAMETER) &&
                !adviceParams.contains(paramName)
            ) {
                String[] values = request.getParameterValues(paramName);
                if (values != null) {
                    for (int i = 0; i < values.length; i++) {
                        parameterString.append("&")
                            .append(paramName)
                            .append("=")
                            .append(URLEncDec.encode(values[i]));
                    }
                }
            }
        }
        return (parameterString.deleteCharAt(0).toString());
    }
    
    /**
     * Returns policy advices
     */
    private String checkForPolicyAdvice(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        StringBuffer adviceList = null;
        
        for (Enumeration e = request.getParameterNames(); e.hasMoreElements();){
            String paramName = (String)e.nextElement();
            if (adviceParams.contains(paramName)) {
                if (adviceList == null) {
                    adviceList = new StringBuffer();
                } else {
                    adviceList.append("&");
                }
                String[] values = request.getParameterValues(paramName);
                if (values != null) {
                    for (int i = 0; i < values.length; i++) {
                        adviceList.append(paramName).append("=")
                            .append(values[i]);
                    }
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message("CDCServlet.checkForPolicyAdvice: Advice List is : "
                + adviceList);
        }
        
        return (adviceList == null) ? null : adviceList.toString();
    }
    
    /**
     * Redirects the HTTP request to the Authentication module.
     * It gets the authentication URL from <code>SystemProperties</code>.
     *
     * @param request HTTP Servlet Request object that contains the request
     *        the client has made of the servlet.
     * @param response an HTTP Servlet Response object that contains the
     *        response the servlet sends to the client.
     * @exception IOException if an input or output exception occurred.
     */
    private void redirectForAuthentication(
        HttpServletRequest  request,
        HttpServletResponse response
    ) throws IOException {
        StringBuffer redirectURL = new StringBuffer(1024);
        
        // Check if user has authenticated to another OpenSSO
        // instance
        String authURL = null;
        Cookie authCookie = CookieUtils.getCookieFromReq(
            request, authURLCookieName);
        if (authCookie != null) {
            authURL = CookieUtils.getCookieValue(authCookie);
            if (debug.messageEnabled()) {
                debug.message("CDCServlet.redirectForAuthentiation: " +
                    "got an authenticated URL: " + authURL);
            }
        }
        
        try {
            if ((authURL == null) || (authURL.length() == 0) ||
                (policyAdviceList != null) ||
                !authURL.toLowerCase().startsWith("http")
            ) {
                String finalURL = getRedirectURL(request, response);
                
                if (finalURL != null) {
                    StringBuffer gotoURL = new StringBuffer(1024);
                    gotoURL.append(deployDescriptor).append(CDCURI)
                        .append("?").append(TARGET_PARAMETER)
                        .append("=").append(URLEncDec.encode(finalURL))
                        .append("&").append(getParameterString(request));
                
                    // Construct the login URL
                    redirectURL.append(AUTHURI).append("?");
                    // check if this is resource based auth case
                    String resourceAuth = request.getParameter(
                        ISAuthConstants.IP_RESOURCE_ENV_PARAM);
                    if ((resourceAuth != null) && 
                        resourceAuth.equalsIgnoreCase("true")) {
                        // this is the resource based authentication case,
                        // append resourceURL since original goto is modified
                        redirectURL.append(ISAuthConstants.RESOURCE_URL_PARAM)
                                .append("=");
                        // check if resourceURL is present : J2EE agent case
                        String resourceUrl = request.getParameter(
                            ISAuthConstants.RESOURCE_URL_PARAM);
                        if (resourceUrl == null) {
                            // not presnet, use goto/TARGET as the resource URL
                            redirectURL.append(URLEncDec.encode(finalURL))
                                .append("&");
                        } else {
                            // resourceURL present in request
                            redirectURL.append(URLEncDec.encode(resourceUrl))
                                .append("&");
                        }
                    }
                    if (policyAdviceList != null) {
                        redirectURL.append(policyAdviceList).append("&");
                    }
                    redirectURL.append(GOTO_PARAMETER)
                        .append("=")
                        .append(URLEncDec.encode(gotoURL.toString()));
                    
                    // Check for policy advices
                    if (policyAdviceList != null) {
                        redirectURL.append("&").append(policyAdviceList);
                    }
                    if (debug.messageEnabled()) {
                        debug.message("CDCServlet.redirectForAuthentication:" +
                            " final forward URL=" + redirectURL.toString());
                    }
                    RequestDispatcher dispatcher =
                        request.getRequestDispatcher(redirectURL.toString());
                    dispatcher.forward(request, response);
                }
            } else {
                // Redirect the user to the authenticated URL
                redirectURL.append(authURL).append(deployDescriptor)
                    .append(CDCURI).append("?")
                    .append(request.getQueryString());
                
                /*
                 * Reset the cookie value to null, to avoid continous loop
                 * when a load balancer is used.
                 */
                if (authCookie != null) {
                    authCookie.setValue("");
                    response.addCookie(authCookie);
                }
                response.sendRedirect(redirectURL.toString());
            }
            
            if (debug.messageEnabled()) {
                debug.message("Forwarding for authentication to: " +
                    redirectURL);
            }
        } catch (IOException e) {
            debug.error("CDCServlet.redirectForAuthentication", e);
            showError(response);
        } catch (ServletException e) {
            debug.error("CDCServlet.redirectForAuthentication", e);
            showError(response);
        } catch (IllegalStateException e) {
            debug.error("CDCServlet.redirectForAuthentication", e);
            showError(response);
        }
    }
    
    private void showError(HttpServletResponse response) {
        showError(response, "ERROR: An application error has occured.");
    }
    
    private void showError(HttpServletResponse response, String msg) {
        ServletOutputStream out = null;
        try {
            out = response.getOutputStream();
            out.println(msg);
            out.flush();
        } catch (IOException e) {
            debug.error("CDCServlet.showError: " +
                "Could not show error message to the user", e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                //ignored
            }
        }
    }
    
    /**
     * Returns the SSOToken of the user. If user has not authenticated
     * re-directs the user to login page
     */
    private SSOToken getSSOToken(
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        SSOToken token = null;
        try {
            /* SSOTokenManager.createSSOToken() throws an SSOException if the
             * token is not valid, so for a invalid token manager.isValidToken()
             * will never get executed for an invalid token.
             */
            if (((token = tokenManager.createSSOToken(request)) == null) ||
                !tokenManager.isValidToken(token)) {
                if (debug.messageEnabled()) {
                    debug.message("CDCSerlvet.getSSOToken: " +
                        "SSOToken is either null or not valid: " + token +
                        "\nRedirecting for authentication");
                }
                token = null;
            }
        } catch (SSOException e) {
            if (debug.messageEnabled()) {
                debug.message("CDCServlet.getSSOToken" + e);
            }
            token = null;
        }
        return token;
    }
    
    private FSAuthnResponse createAuthnResponse(
        String providerID, 
        String responseID,
        String inResponseTo,
        Status status,
        FSAssertion assertion,
        String relayState
    ) throws SAMLException, FSMsgException{
        List contents = new ArrayList(1);
        contents.add(assertion);
        FSAuthnResponse response = new FSAuthnResponse(null, inResponseTo,
            status, contents, relayState);
        response.setProviderId(providerID);
        return response;
    }
    
    private FSAssertion createAssertion(
        String destID,
        String sourceID,
        String tokenID,
        String authType,
        String strAuthInst,
        String userDN,
        String inResponseTo
    ) throws FSException, SAMLException {
        debug.message("Entering CDCServlet.createAssertion Method");

        if ((destID == null) ||(sourceID == null) || (tokenID == null) ||
            (authType == null) || (userDN == null) || (inResponseTo == null)) {
            debug.message("CDCServlet,createAssertion: null input");
            throw new FSException(FSUtils.bundle.getString("nullInput"));
        }
        
        String securityDomain = sourceID;
        NameIdentifier idpHandle = new NameIdentifier(
             URLEncDec.encode(tokenID), sourceID);
        NameIdentifier spHandle = idpHandle;
        String authMethod = authType;
        Date authInstant = convertAuthInstanceToDate(strAuthInst);
        
        if (debug.messageEnabled()) {
            debug.message("CDCServlet.createAssertion " +
                "Creating Authentication Assertion for user with opaqueHandle ="
                + spHandle.getName() + " and SecurityDomain = "  + 
                securityDomain);
        }
        
        SubjectConfirmation subConfirmation = new SubjectConfirmation(
            IFSConstants.CONFIRMATION_METHOD_BEARER);
        IDPProvidedNameIdentifier idpNi =
            new IDPProvidedNameIdentifier(idpHandle.getNameQualifier(),
            idpHandle.getName());
        FSSubject sub = new FSSubject(spHandle, subConfirmation, idpNi);
        SubjectLocality authLocality = new SubjectLocality(
            IPAddress, DNSAddress);

        AuthnContext authnContextStmt = new AuthnContext(null, null);
        FSAuthenticationStatement statement = new FSAuthenticationStatement(
            authMethod, authInstant, sub, authLocality, null, authnContextStmt);

        //setReauthenticateOnOrAfter date
        Date issueInstant = new Date();
        // get this period from the config
        Integer assertionTimeout = new Integer(
            IFSConstants.ASSERTION_TIMEOUT_DEFAULT);
        long period =(assertionTimeout.intValue()) * 1000;
        if (period < IFSConstants.ASSERTION_TIMEOUT_ALLOWED_DIFFERENCE) {
            period = IFSConstants.ASSERTION_TIMEOUT_ALLOWED_DIFFERENCE;
        }
        Date notAfter = new Date(issueInstant.getTime() + period);
        statement.setReauthenticateOnOrAfter(notAfter);
        if (debug.messageEnabled()) {
            debug.message("CDCServlet.createAssertion: " +
                "Authentication Statement: " + statement.toXMLString());
        }
        
        Conditions cond = new Conditions(issueInstant, notAfter);
        if((destID != null) &&(destID.length() != 0)) {
            List targets = new ArrayList(1);
            targets.add(destID);
            cond.addAudienceRestrictionCondition(
                new AudienceRestrictionCondition(targets));
        }
        
        if (debug.messageEnabled()) {
            debug.message("CDCServlet.createAssertion: " +
                "Condition: " + cond.toString());
        }
        
        AssertionIDReference aID = new AssertionIDReference();
        Set statements = new HashSet(2);
        statements.add(statement);
        FSAssertion assertion = new FSAssertion(aID.getAssertionIDReference(),
            sourceID, issueInstant, cond, statements, inResponseTo);
        assertion.setID(aID.getAssertionIDReference());
        
        String[] params = {FSUtils.bundle.getString("assertionCreated") + ":"
            + assertion.toString()};
        LogUtil.access(Level.INFO, "FSAssertionManager", params);
        
        if (debug.messageEnabled()) {
            debug.message("CDCServlet.createAssertion:"
                + " Returning Assertion: " + assertion.toXMLString());
        }
        return assertion;
    }
    
    private Date convertAuthInstanceToDate(String strAuthInst) {
        Date authInstant = null;
        if (strAuthInst != null){
            try {
                authInstant = DateUtils.stringToDate(strAuthInst);
            } catch (ParseException ex) {
                if (debug.messageEnabled()) {
                    debug.message("CDCServlet.convertAuthInstanceToDate: " +
                        "cannot convert " + strAuthInst);
                }
            }
        }
        return (authInstant == null) ? new java.util.Date() : authInstant;
    }
    
    private void sendAuthnResponse(HttpServletRequest request,
        HttpServletResponse response, FSAuthnResponse authnResponse, String destURL) {
        if (debug.messageEnabled()) {
            debug.message("CDCServlet.sendAuthnResponse: Called");
        }
        try{
            String respStr = authnResponse.toXMLString(true, true);
            if (debug.messageEnabled()) {
                debug.message("CDCServlet.sendAuthnResponse: " +
                    "AuthnResponse: " + respStr);
            }
            String b64Resp = Base64.encode(respStr.getBytes());
            
            response.setContentType("text/html");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("cdcservlet_auto_post", "true");
            
            Object[] params = {destURL, b64Resp};
            PrintWriter out = response.getWriter();
            out.println(MessageFormat.format(AUTHN_RESPONSE_HTML, params));
            out.close();
            if (debug.messageEnabled()) {
                debug.message("CDCServlet:sendAuthnResponse: "
                    + "AuthnResponse sent successfully to: " + destURL);
            }
        } catch (FSMsgException fme) {
            debug.error("CDCServlet.sendAuthnResponse:" + fme);
        } catch (IOException ioe) {
            debug.error("CDCServlet.sendAuthnResponse:" + ioe);
        }
    }
}
