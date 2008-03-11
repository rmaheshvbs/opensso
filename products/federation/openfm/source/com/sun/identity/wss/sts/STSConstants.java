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
 * $Id: STSConstants.java,v 1.6 2008-03-11 20:12:16 mrudul_uchil Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.wss.sts;

/**
 * This class defines the Constants related to Security Token
 * Service.
 */
public class STSConstants {

    /** WS-Trust namespace URI */
    public static final String WST_NAMESPACE = 
        "http://schemas.xmlsoap.org/ws/2005/02/trust/";
   
    /**
     * URI for KeyType
     */
    public static final String PUBLIC_KEY = WST_NAMESPACE+ "PublicKey";
    public static final String SYMMETRIC_KEY = WST_NAMESPACE + "SymmetricKey";

    /**
     * URI for TokenType
     */
    public static final String SAML11_ASSERTION_TOKEN_TYPE = 
        "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1";

    public static final String SAML20_ASSERTION_TOKEN_TYPE = 
        "urn:oasis:names:tc:SAML:2.0:assertion";
    
    public static String ASSERTION_ELEMENT = "Assertion";
    public static final String SAML20_NAMESPACE = "xmlns:saml2";
    public static final String SAML10_NAMESPACE = "xmlns:saml";
            
    public static final String SAML10_ASSERTION = 
        "urn:oasis:names:tc:SAML:1.0:assertion";
            
    public static final String SSO_TOKEN_TYPE = "FAMSSOToken";
    
    public static final String FAM_TOKEN_NS = 
                              "http://www.sun.com/identity/famtoken";
        
    public static final String STS_CLIENT_USER_TOKEN_PLUGIN =
                               "com.sun.identity.wss.sts.clientusertoken";            

}

