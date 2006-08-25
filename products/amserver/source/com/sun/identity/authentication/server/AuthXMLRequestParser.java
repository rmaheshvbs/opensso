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
 * $Id: AuthXMLRequestParser.java,v 1.2 2006-08-25 21:20:26 veiming Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */



package com.sun.identity.authentication.server;

import java.io.ByteArrayInputStream;

import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.share.AuthXMLTags;
import com.sun.identity.authentication.share.AuthXMLUtils;

/**
 * <code>AuthXMLRequestParser</code> parses the XML data received from the
 * client.
 */
public class AuthXMLRequestParser {
    static Debug debug = Debug.getInstance("amXMLHandler");
    Document xmlDocument = null;
    AuthXMLRequest authXMLRequest = null;
    HttpServletRequest servletReq;

    /**
     * Create <code>AuthXMLRequestParser</code> object 
     * @param  xmlString reprsents request
     * @param req <code>HttpServletRequest</code> contains the request.
     */
    public AuthXMLRequestParser(String xmlString, HttpServletRequest req) {
        servletReq = req;
        try  {
           xmlDocument = XMLUtils.getXMLDocument(new ByteArrayInputStream(
                xmlString.toString().getBytes("UTF-8")));

           if (debug.messageEnabled()) {
               debug.message("AuthXMLRequestParser : xmlDoc : " + xmlDocument);
           }
       } catch (Exception e) {
            debug.message("AuthXMLRequest Parser error : " , e);
       }
    }


    /**
     * Parses the authentication request xml document. 
     * 
     * @return a AuthXMLRequest object.
     * @throws AuthException if it fails to parse the xml.
     */
    public AuthXMLRequest parseXML() throws AuthException {
        try {
            debug.message("entering parseXML"); 
            if (xmlDocument == null) {
                return null;
            }

            authXMLRequest = new AuthXMLRequest();
        
            // get the document root
            Element docElem = xmlDocument.getDocumentElement();
            // get the attributes for the root element

            if (docElem != null) {
                String temp = docElem.getAttribute("version");
                if (debug.messageEnabled()) {
                    debug.message("Request Version is.. : " + temp);
                }
                if (temp != null) {
                    authXMLRequest.setRequestVersion(temp);
                }

                Node requestNode = XMLUtils.getChildNode(
                    (Node)docElem,"Request");
                String authIdentifier = null;
                if (requestNode != null) {
                    authIdentifier = 
                        parseNodeAttributes(requestNode,"authIdentifier");
                 
                    if (debug.messageEnabled()) {
                        debug.message("AuthIdentifier is : " + authIdentifier);
                    }
                    authXMLRequest.setAuthIdentifier(authIdentifier);
                }

                // get the Nodes for the Request Element

                // get new auth context node 
                Node newAuthContextNode = XMLUtils.getChildNode(
                    requestNode,"NewAuthContext");
                if (newAuthContextNode != null) {
                    String orgName =
                        parseNodeAttributes(newAuthContextNode,"orgName");
                    authXMLRequest.setOrgName(orgName);
                    authXMLRequest.setRequestType(
                        AuthXMLRequest.NewAuthContext);
                    AuthContextLocal authContext = 
                        AuthUtils.getAuthContext(orgName,servletReq);
                    authXMLRequest.setAuthContext(authContext);
                }
        
                // get query node 

                Node queryInfoNode = 
                    XMLUtils.getChildNode(requestNode,"QueryInformation");
                if (queryInfoNode != null) {
                    String queryType = parseNodeAttributes(
                        queryInfoNode,"requestedInformation");
                    authXMLRequest.setRequestInformation(queryType);
                    authXMLRequest.setRequestType(
                        AuthXMLRequest.QueryInformation);
                    String orgName = parseNodeAttributes(
                        queryInfoNode, "orgName");
                    AuthContextLocal authContext = null;

                    if (orgName != null) {
                        authContext = AuthUtils.getAuthContext(
                            orgName, servletReq);
                    } else {
                        authContext = AuthUtils.getAuthContext(
                            null, authIdentifier,false);
                    }
                    authXMLRequest.setAuthContext(authContext);
                }

                // get login node 
                Node loginNode = XMLUtils.getChildNode(requestNode,"Login");
                if (loginNode != null) {
                    debug.message("found login node !!");
                    String orgName =
                        parseNodeAttributes(loginNode,"orgName");
                    AuthContextLocal authContext = null;
                    if (orgName != null) {
                        authXMLRequest.setOrgName(orgName);
                        authContext = AuthUtils.getAuthContext(
                            orgName,servletReq);
                    } else {
                        authContext = AuthUtils.getAuthContext(
                            servletReq, authIdentifier);
                    }
                    authXMLRequest.setRequestType(AuthXMLRequest.Login);
                    parseLoginNodeElements(loginNode,authXMLRequest);
                    authXMLRequest.setAuthContext(authContext);
                }        

                // get submit requirements node
                Node submitReqNode = XMLUtils.getChildNode(
                    requestNode, "SubmitRequirements");
                if (submitReqNode != null) {
                    authXMLRequest.setRequestType(
                        AuthXMLRequest.SubmitRequirements);
                    AuthContextLocal authContext = AuthUtils.getAuthContext(
                        servletReq, authIdentifier);
                    authXMLRequest.setAuthContext(authContext);
                    AuthUtils authUtils = new AuthUtils();
                    Callback[] callbacks = authUtils.getRecdCallback(
                        authContext);
                    parseSubmitReqElements(
                        submitReqNode, authXMLRequest, callbacks);
                }

                // get  logout node
                Node logoutNode = XMLUtils.getChildNode(requestNode,"Logout");
                if (logoutNode != null) {
                    authXMLRequest.setRequestType(AuthXMLRequest.Logout);
                    AuthContextLocal authContext = 
                        AuthUtils.getAuthContext(null,authIdentifier,true);
                    authXMLRequest.setAuthContext(authContext);
                }

                // get abort node
                Node abortNode = XMLUtils.getChildNode(requestNode,"Abort");
                if (abortNode!= null) {
                    authXMLRequest.setRequestType(AuthXMLRequest.Abort);
                    AuthContextLocal authContext =
                        AuthUtils.getAuthContext(null,authIdentifier,true);
                    authXMLRequest.setAuthContext(authContext);
                }
            }
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            debug.message("Error in parseXML: : " , e);
        }

        return authXMLRequest;
    }

    /* get the attribute value for a node */
    private String parseNodeAttributes(Node requestNode,String attrName) {
        try {
            if (requestNode == null) {
                return null;
            }

            String attrValue = 
                XMLUtils.getNodeAttributeValue(requestNode,attrName);

            if (debug.messageEnabled()) {
                debug.message("Attr Value is : " + attrValue);
            }

            return attrValue;
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("Error getting " + attrName);
                debug.message("Exception " ,e);
            }
            return null;
        }
    }

    /* parse the login node elements */
    private void parseLoginNodeElements(
        Node loginNode,
        AuthXMLRequest authXMLRequest) {
        authXMLRequest.setRequestType(AuthXMLRequest.Login);
        // get the  the Login Nodes and their values.

        /* get the indexType , indexName */

        Node indexTypeNamePair =
            XMLUtils.getChildNode(loginNode, AuthXMLTags.INDEX_TYPE_PAIR);

        if (indexTypeNamePair != null) {
            String indexType =
                parseNodeAttributes(indexTypeNamePair, AuthXMLTags.INDEX_TYPE);
            if (debug.messageEnabled()) {
                debug.message("indexType is .. : " + indexType);
            }
            authXMLRequest.setIndexType(indexType);
            Node indexNameNode = XMLUtils.getChildNode(
                indexTypeNamePair, AuthXMLTags.INDEX_NAME);

            if (indexNameNode != null) {
                String indexName =
                    XMLUtils.getValueOfValueNode(indexNameNode);
                if (debug.messageEnabled()) {
                    debug.message("indexName is .. : " + indexName);
                }
                authXMLRequest.setIndexName(indexName);
            }
            authXMLRequest.setRequestType(AuthXMLRequest.LoginIndex);
        } 

        // get the default values for callbacks if any.
        Node paramsNode = XMLUtils.getChildNode(loginNode,AuthXMLTags.PARAMS);
        if (paramsNode != null) {
            authXMLRequest.setParams(XMLUtils.getValueOfValueNode(paramsNode));
        }
    }

    /* parse submit requirements node */
    void parseSubmitReqElements(
        Node submitReqNode,
        AuthXMLRequest authXMLRequest,
        Callback[] recdCallbacks) {
        Node callbacksNode = XMLUtils.getChildNode(submitReqNode,"Callbacks");
        Callback[] submittedCallbacks = 
                AuthXMLUtils.getCallbacks(callbacksNode,true,recdCallbacks);
        authXMLRequest.setSubmittedCallbacks(submittedCallbacks);
        return;
    }
}
