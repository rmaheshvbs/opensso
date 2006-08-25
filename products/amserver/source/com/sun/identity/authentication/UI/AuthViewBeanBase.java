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
 * $Id: AuthViewBeanBase.java,v 1.3 2006-08-25 21:20:07 veiming Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */



package com.sun.identity.authentication.UI;

import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.iplanet.am.util.BrowserEncoding;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.ViewBeanBase;
import com.iplanet.jato.view.html.StaticTextField;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.shared.Constants;
import java.io.IOException;
import java.util.ResourceBundle;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class is a default implementation of <code>ViewBean</code> auth UI.
 */
public abstract class AuthViewBeanBase extends ViewBeanBase {
    private  java.util.Locale accLocale ;
    static Debug loginDebug = Debug.getInstance("amLoginViewBean");
    
    /**
     * Creates <code>AuthViewBeanBase</code> object.
     * @param pageName name of page for auth UI.
     */
    public AuthViewBeanBase(String pageName ) {
        super(pageName);
        registerChildren();
    }
    
    /** registers child views */
    protected void registerChildren() {
        registerChild(PAGE_ENCODING, StaticTextField.class);
        registerChild(SERVICE_URI, StaticTextField.class);
    }
    
    
    protected View createChild(String name) {
        if (name.equals(PAGE_ENCODING)) {
            return new StaticTextField(this, PAGE_ENCODING, "");
        } else if (name.equals(SERVICE_URI)) {
            return new StaticTextField(this, name, serviceUri);
        }
        throw new IllegalArgumentException(
        "Invalid child name [" + name + "]");
        
    }
    
    protected void setPageEncoding(HttpServletRequest request,
    HttpServletResponse response) {
        /** Set the codeset of the page **/
        String client_type = au.getClientType(request);
        String content_type = au.getContentType(client_type);
        
        accLocale = fallbackLocale;
        if (accLocale == null) {
            ISLocaleContext localeContext = new ISLocaleContext();
            localeContext.setLocale(request);
            accLocale = localeContext.getLocale();
        }
        
        String charset = au.getCharSet(client_type, accLocale);
        response.setContentType(content_type+";charset="+charset);
        String jCharset = BrowserEncoding.mapHttp2JavaCharset(charset);
        if (loginDebug.messageEnabled()) {
            loginDebug.message("In setPageEncoding - charset : " + charset);
            loginDebug.message("In setPageEncoding - JCharset : " + jCharset);
        }
        setDisplayFieldValue(PAGE_ENCODING, jCharset);
        
        try {
            response.getWriter();
        } catch (IOException ex) {
            /* Problem in handling charset Ignore it*/
            response.setContentType(content_type);
        }
    }
    
    // Method to set Access Manager cookie (HostUrl) in
    // Session / Cookie hijacking mode.
    protected void setHostUrlCookie(HttpServletResponse response) {
        if (isSessionHijackingEnabled) {
            String authServerProtocol =
            SystemProperties.get(Constants.AM_SERVER_PROTOCOL);
            String authServer =
            SystemProperties.get(Constants.AM_SERVER_HOST);
            String authServerPort =
            SystemProperties.get(Constants.AM_SERVER_PORT);
            
            String hostUrlCookieValue   = authServerProtocol + "://"
            + authServer + ":" + authServerPort;
            
            if (loginDebug.messageEnabled()) {
                loginDebug.message("hostUrlCookieName : " + hostUrlCookieName);
                loginDebug.message(
                    "hostUrlCookieDomain : " + hostUrlCookieDomain);
                loginDebug.message(
                    "hostUrlCookieValue : " + hostUrlCookieValue);
            }
            
            // Create Cookie
            try {
                Cookie cookie = au.createCookie(hostUrlCookieName,
                hostUrlCookieValue, hostUrlCookieDomain);
                response.addCookie(cookie);
            } catch (Exception e) {
                loginDebug.message("Cound not set HostUrl Cookie!", e);
            }
        }
    }
    
    // Method to clear Access Manager cookie (HostUrl) in
    // Session / Cookie hijacking mode.
    protected void clearHostUrlCookie(HttpServletResponse response) {
        if (isSessionHijackingEnabled) {
            // Create Cookie
            try {
                Cookie cookie = au.createCookie(hostUrlCookieName,
                LOGOUTCOOKIEVAULE, hostUrlCookieDomain);
                cookie.setMaxAge(0);
                response.addCookie(cookie);
            } catch (Exception e) {
                loginDebug.message("Cound not clear HostUrl Cookie!", e);
            }
        }
    }
    
    /**
     * Returns <code>Locale</code> for auth request.
     * @return <code>Locale</code> for auth request.
     */
    public java.util.Locale getRequestLocale() {
        return accLocale;
    }
    
    /**
     * Returns tile index for auth UI.
     * @return tile index for auth UI.
     */
    public abstract String getTileIndex();
    
    /**
     * Parameter name for page encoding.
     */
    public static final String PAGE_ENCODING = "gx_charset";
    /**
     * Parameter name for service uri.
     */
    public static final String SERVICE_URI = "ServiceUri";
    
    /**
     * Configured service uri.
     */
    public static String serviceUri =
    SystemProperties.get(Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
    
    //to be used in case session is destroyed
    protected java.util.Locale fallbackLocale;
    /**
     * When HTTP request is made, we get authcontext and get current locale and
     * store it in fallbackLocale. Before the login page is displayed, login
     * modules can have LoginState object which has the locale settings. But
     * after going through login process LoginState might have changed locale
     * based on user preference  or LoginState may not exist if LoginFailure in
     * such case we need to fallback to this locale for responding to user
     */
    
    public static AMResourceBundleCache rbCache =
    AMResourceBundleCache.getInstance();
    /**
     * Resource bundle with <code>Locale</code>
     */
    public ResourceBundle rb = null;
    /**
     * AuthUtils object.
     */
    public static AuthUtils au = new AuthUtils();
    
    private static boolean isSessionHijackingEnabled =
    Boolean.valueOf(SystemProperties.get(
    Constants.IS_ENABLE_UNIQUE_COOKIE, "false")).booleanValue();
    private static String hostUrlCookieName =
    SystemProperties.get(Constants.AUTH_UNIQUE_COOKIE_NAME,
    "sunIdentityServerAuthNServer");
    private static String hostUrlCookieDomain =
    SystemProperties.get(Constants.AUTH_UNIQUE_COOKIE_DOMAIN);
    private static final String LOGOUTCOOKIEVAULE = "LOGOUT";
    
}

