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
 * $Id: FSAssertionArtifactHandler.java,v 1.1 2006-10-30 23:14:26 qcheng Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.federation.services.fednsso;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfo;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfoKey;
import com.sun.identity.federation.accountmgmt.FSAccountManager;
import com.sun.identity.federation.accountmgmt.FSAccountMgmtException;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.key.KeyUtil;
import com.sun.identity.federation.message.FSAssertion;
import com.sun.identity.federation.message.FSAuthenticationStatement;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.FSAuthnResponse;
import com.sun.identity.federation.message.FSSubject;
import com.sun.identity.federation.message.common.AuthnContext;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.FSSPAuthenticationContextInfo;
import com.sun.identity.federation.services.FSAttributeMapper;
import com.sun.identity.federation.services.FSServiceManager;
import com.sun.identity.federation.services.FSSession;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.federation.services.FSSessionPartner;
import com.sun.identity.federation.services.logout.FSTokenListener;
import com.sun.identity.federation.services.registration.FSNameRegistrationHandler;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.saml.assertion.AttributeStatement;
import com.sun.identity.saml.assertion.AudienceRestrictionCondition;
import com.sun.identity.saml.assertion.Conditions;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.assertion.Statement;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.SubjectConfirmation;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.protocol.Response;
import com.sun.identity.saml.servlet.POSTCleanUpThread;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.encode.CookieUtils;
import com.sun.identity.shared.DateUtils;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Handler that runs on <code>SP</code> side to receive and process 
 * <code>AuthnResponse</code>.
 */
public class FSAssertionArtifactHandler {
    private String idpSessionIndex = null;
    private Date reAuthnOnOrAfterDate = null; // TODO: not used currently
    private AuthnContext authnContextStmt = null;
    private List cookieDomainList = FSServiceUtils.getCookieDomainList();

    protected HttpServletRequest request = null;
    protected HttpServletResponse response = null;
    protected IDPDescriptorType idpDescriptor = null;
    protected String idpEntityId = null;
    protected FSAuthnRequest authnRequest = null;
    protected String relayState= null;
    
    protected static Map idTimeMap = new HashMap();
    protected static Thread cThread = new POSTCleanUpThread(idTimeMap);
    protected boolean doFederate = false;
    protected String nameIDPolicy = null;
    
    protected AttributeStatement bootStrapStatement = null;
    protected AttributeStatement _autoFedStatement = null;
    protected Map autoFedSearchMap = null;
    protected List securityAssertions = null;
    protected Object ssoToken = null;
    protected FSAuthnResponse authnResponse = null;
    protected Element samlResponseElt = null;
    protected List attrStatements = new ArrayList();
    protected SPDescriptorType hostDesc = null;
    protected BaseConfigType hostConfig = null;
    protected String hostEntityId = null;
    protected String hostMetaAlias = null;
    protected static String ANONYMOUS_PRINCIPAL = "anonymous";
    protected static FSAttributeMapper attributeMapper = null;
    
    static {
        cThread.start();
    }
    
    /**
     * Sets hosted SP entity ID.
     * @param entityId hosted SP's entity ID to be set
     */
    public void setHostEntityId(String entityId) {
        hostEntityId = entityId;
    }
 
    /**
     * Sets hosted SP meta descriptor.
     * @param desc SP's meta descriptor to be set.
     * @see #getHostEntityId()
     */
    public void setHostDescriptor(SPDescriptorType desc) {
        hostDesc = desc;
    }

    /**
     * Sets hosted SP extended meta config.
     * @param config SP's extended meta to be set.
     */
    public void setHostDescriptorConfig(BaseConfigType config) {
        hostConfig = config;
    }

    /**
     * Sets hosted SP's meta alias.
     * @param metaAlias SP's meta alias to be set
     */
    public void setMetaAlias(String metaAlias) {
        hostMetaAlias = metaAlias;
    }
    /**
     * Gets hosted SP's Entity ID.provider id.
     * @return hosted entity id.
     * @see #setHostEntityId(String)Id(String)
     */
    public String getHostEntityId() {
        return hostEntityId;
    }
    
    /**
     * Gets <code>FSAuthnRequest</code> object.
     * @return <code>FSAuthnRequest</code> object
     * @see #setAuthnRequest(FSAuthnRequest)
     */
    public FSAuthnRequest getAuthnRequest() {
        return authnRequest;
    }

    /**
     * Sets <code>FSAuthnRequest</code> object.
     * @param authnRequest <code>FSAuthnRequest</code> object to be set.
     * @see #getAuthnRequest()
     */
    public void setAuthnRequest(FSAuthnRequest authnRequest) {
        this.authnRequest = authnRequest;
    }
    
    /**
     * Default constructor.
     */
    protected FSAssertionArtifactHandler(){
    }
    
    /**
     * Constructs a <code>FSAssertionArtifactHandler</code> object.
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object
     * @param idpDescriptor <code>IDP</code> provider descriptor
     * @param idpEntityId entity ID of the <code>IDP</code>
     * @param doFederate a flag indicating if it is a federation request
     * @param nameIDPolicy <code>nameIDPolicy</code> used
     * @param relayState <code>RelayState</code> url
     */
    public FSAssertionArtifactHandler(
        HttpServletRequest request, 
        HttpServletResponse response, 
        IDPDescriptorType idpDescriptor, 
        String idpEntityId,
        boolean doFederate,
        String nameIDPolicy,
        String relayState
    )
    {
        this.request = request;
        this.response = response;
        this.relayState = relayState;
        this.idpDescriptor = idpDescriptor;
        this.idpEntityId = idpEntityId;
        
        this.doFederate = doFederate;
        this.nameIDPolicy = nameIDPolicy;
    }
    
    /**
     * Constructs a <code>FSAssertionArtifactHandler</code> object.
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object
     * @param idpDescriptor <code>IDP</code> provider descriptor
     * @param idpEntityId entity ID of the <code>IDP</code>
     * @param authnRequest <code>FSAuthnRequest</code> from soap
     * @param doFederate a flag indicating if it is a federation request
     * @param relayState <code>RelayState</code> url
     */
    public FSAssertionArtifactHandler(
        HttpServletRequest request, 
        HttpServletResponse response, 
        IDPDescriptorType idpDescriptor, 
        String idpEntityId,
        FSAuthnRequest authnRequest, 
        boolean doFederate,
        String relayState
    )
    {
        this.request = request;
        this.response = response;
        this.relayState = relayState;
        this.idpDescriptor = idpDescriptor;
        this.idpEntityId = idpEntityId;
        if (authnRequest != null) {
            this.authnRequest = authnRequest;            
            this.nameIDPolicy = authnRequest.getNameIDPolicy();
        }
        
        this.doFederate = doFederate;
    }
    
    /**
     * Processes <code>FSAuthnResponse</code>.
     * @param authnResponse <code>FSAuthnResponse</code> objec to be processed
     */
    public void processAuthnResponse(FSAuthnResponse authnResponse) {
        FSUtils.debug.message(
            "FSAssertionArtifactHandler.ProcessAuthnResponse: Called");
        this.authnResponse = authnResponse;
        String baseURL = FSServiceUtils.getBaseURL(request);
        String framedLoginPageURL = FSServiceUtils.getCommonLoginPageURL(
            hostMetaAlias, 
            authnRequest.getRelayState(),
            null,
            request,
            baseURL);
        
        try {
            if (authnResponse == null) {
                String[] data = 
                    { FSUtils.bundle.getString("missingAuthnResponse") };
                LogUtil.error(Level.INFO,LogUtil.MISSING_AUTHN_RESPONSE,data);
                FSUtils.debug.error("FSAssertionArtifactHandler."
                    + "processAuthnResponse: " 
                    + FSUtils.bundle.getString("missingAuthnResponse")
                    + " AuthnRequest Processing Failed at the IDP "
                    + "Redirecting to the Framed Login Page");
                response.sendRedirect(framedLoginPageURL);
                return;
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSAssertionArtifactHandler.doPost:Received " +
                    authnResponse.toXMLString());
            }
            
            boolean valid = verifyResponseStatus(authnResponse);
            if (!valid) {
                FSSessionManager sessionManager =
                    FSSessionManager.getInstance(hostEntityId);
                // clean request map
                String inResponseTo = authnResponse.getInResponseTo();
                sessionManager.removeAuthnRequest(inResponseTo);
                String[] data = 
                    { authnResponse.toXMLString() };
                LogUtil.error(Level.INFO,LogUtil.INVALID_AUTHN_RESPONSE,data);
                FSUtils.debug.warning("FSAssertionArtifactHandler."
                    + " processAuthnResponse: "
                    + FSUtils.bundle.getString("invalidResponse")
                    + " AuthnRequest Processing Failed at the IDP"
                    + " Redirecting to the Framed Login Page");
                response.sendRedirect(framedLoginPageURL);
                return;
            }
            
            // check Assertion
            List assertions = authnResponse.getAssertion();
            FSSubject validSubject =(FSSubject)validateAssertions(assertions);
            if (validSubject == null) {
                String[] data = { FSUtils.bundle.getString("invalidAssertion")};
                LogUtil.error(Level.INFO,LogUtil.INVALID_ASSERTION,data);
                FSUtils.debug.error("FSAssertionArtifactHandler."
                    + "processAuthnResponse: " 
                    + FSUtils.bundle.getString("InvalidResponse")
                    + " AuthnRequest Processing Failed at the IDP"
                    + " Redirecting to the Framed Login Page");
                response.sendRedirect(framedLoginPageURL);
                return;
            }
           
            FSSessionManager sessionManager = 
                FSSessionManager.getInstance(hostEntityId);

            if (doFederate){
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionArtifactHandler." +
                        "processAuthnResponse: Initiate Account Federation");
                }
                NameIdentifier ni = validSubject.getIDPProvidedNameIdentifier();
                if (ni == null) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSAssertionArtifactHandler.processAuthnResponse:" +
                            " IDPProvided NameIdentifier is null");
                    }
                    ni = validSubject.getNameIdentifier();
                }
                if (ni != null){
                    if (doAccountFederation(ni)) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSAssertionArtifactHandler."
                                + "processAuthnResponse: Account federation"
                                + " successful");
                        }
                        
                        String inResponseTo = authnResponse.getInResponseTo();
                        sessionManager.removeAuthnRequest(inResponseTo);
                        sessionManager.removeLocalSessionToken(inResponseTo);
                        return;
                    } else {
                        String[] data = {FSUtils.bundle.getString(
                            "AccountFederationFailed")};
                        LogUtil.error(Level.INFO,
                                    LogUtil.ACCOUNT_FEDERATION_FAILED,
                                    data);
                        FSUtils.debug.error("FSAssertionArtifactHandler."
                            + "processAuthnResponse: " + 
                            FSUtils.bundle.getString("AccountFederationFailed")
                                + " AuthnRequest Processing Failed at the IDP"
                                + " Redirecting to the Framed Login Page");
                        response.sendRedirect(framedLoginPageURL);
                    }
                } else {
                    throw new FSException("missingNIofSubject", null);
                }
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionArtifactHandler."
                        + "processAuthnResponse: Initiate SingleSign-On");
                }
                //check for SPProvidedNameIdentifier
                NameIdentifier niIdp = 
                    validSubject.getIDPProvidedNameIdentifier();
                NameIdentifier ni = validSubject.getNameIdentifier();
                if (niIdp == null) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "FSAssertionArtifactHandler.processAuthnResponse:" +
                            " IDPProvided NameIdentifier is null");
                    }
                    niIdp = ni;
                }
                if ((niIdp == null) ||(ni == null)){
                    String[] data =
                        { FSUtils.bundle.getString("invalidResponse")};
                    LogUtil.error(
                        Level.INFO,LogUtil.INVALID_AUTHN_RESPONSE, data);
                    FSUtils.debug.error("FSAssertionArtifactHandler."
                        + " processAuthnResponse: "
                        + FSUtils.bundle.getString("invalidResponse")
                        + " AuthnRequest Processing Failed at the IDP"
                        + " Redirecting to the Framed Login Page");
                    response.sendRedirect(framedLoginPageURL);
                    return;
                }
                
                String idpHandle = niIdp.getName();
                String spHandle = ni.getName();
                int handleType;
                if ((idpHandle == null) || (spHandle == null)) {
                    String[] data = 
                        { FSUtils.bundle.getString("invalidResponse")};
                    LogUtil.error(
                        Level.INFO,LogUtil.INVALID_AUTHN_RESPONSE,data);
                    FSUtils.debug.error("FSAssertionArtifactHandler."
                        + "processAuthnResponse: "
                        + FSUtils.bundle.getString("invalidResponse")
                        + " AuthnRequest Processing Failed at the IDP"
                        + " Redirecting to the Framed Login Page");
                    response.sendRedirect(framedLoginPageURL);
                    return;
                }
                if (idpHandle.equals(spHandle)){
                    ni = niIdp;
                    handleType = IFSConstants.REMOTE_OPAQUE_HANDLE;
                } else {
                    handleType = IFSConstants.LOCAL_OPAQUE_HANDLE;
                }
                
                String orgDN = IDFFMetaUtils.getFirstAttributeValueFromConfig(
                    hostConfig, IFSConstants.REALM_NAME);
                Map env = new HashMap();
                env.put(IFSConstants.FS_USER_PROVIDER_ENV_AUTHNRESPONSE_KEY,
                    authnResponse);
                if (doSingleSignOn(ni, handleType, orgDN, niIdp, env)){
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSAssertionArtifactHandler."
                        + "processAuthnResponse: Accountfederation successful");
                    }
                    
                    String requestID = authnResponse.getInResponseTo();
                    sessionManager.removeAuthnRequest(requestID);
                    if (isIDPProxyEnabled(requestID)) {
                        sendProxyResponse(requestID);
                        return;
                    }
                    String[] data = {this.relayState};
                    LogUtil.access(
                        Level.INFO,LogUtil.ACCESS_GRANTED_REDIRECT_TO, data); 
                    response.setHeader("Location", this.relayState);
                    redirectToResource(this.relayState);
                    return;
                } else {
                    String[] data = { FSUtils.bundle.getString("SSOfailed") };
                    LogUtil.error(Level.INFO,LogUtil.SINGLE_SIGNON_FAILED,data);
                    FSUtils.debug.error("FSAssertionArtifactHandler."
                        + "processAuthnResponse: "
                        + FSUtils.bundle.getString("invalidResponse")
                        + " AuthnRequest Processing Failed at the IDP"
                        + " Redirecting to the Framed Login Page");
                    response.sendRedirect(framedLoginPageURL);
                    return;
                }
                
            }
        } catch(Exception e){
            FSUtils.debug.error("FSAssertionArtifactHandler."
                + "processAuthnResponse: Exception Occured: ", e);
            try {
                FSUtils.debug.error("FSAssertionArtifactHandler."
                    + "processAuthnResponse: "
                    + FSUtils.bundle.getString("invalidResponse")
                    + " AuthnRequest Processing Failed at the IDP"
                    + " Redirecting to the Framed Login Page");
                response.sendRedirect(framedLoginPageURL);
            } catch(IOException ioe){
                FSUtils.debug.error("FSAssertionArtifactHandler."
                    + "processAuthnResponse: IOException Occured: ", ioe);
                return;
            }
            return;
        }
    }
    
    protected boolean verifyResponseStatus(Response resp) {
        FSUtils.debug.message(
            "FSAssertionArtifactHandler.verifyResponseStatus: Called");
        // check status of the AuthnResponse
        if (!resp.getStatus().getStatusCode().getValue().endsWith(
            IFSConstants.STATUS_CODE_SUCCESS_NO_PREFIX)) 
        {
            FSUtils.debug.warning("FSAssertionArtifactHandler.verifyResponse: "
                + "Incorrect StatusCode value.");
            return false;
        }

        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSAssertionArtifactHandler.verifyResponse: "
                + "StatusCode value verified.");
        }
        return true;
    }
    
    protected Subject validateAssertions(List assertions) {
        FSUtils.debug.message(
            "FSAssertionArtifactHandler.validateAssertions: Called");
        // loop to check assertions
        FSSubject subject = null;
        
        Iterator iter = assertions.iterator();
        FSAssertion assertion = null;
        String aIDString = null;
        String issuer = null;
        
        Iterator stmtIter = null;
        Statement statement = null;
        int stmtType = Statement.NOT_SUPPORTED;
        SubjectConfirmation subConf = null;
        Set confMethods = null;
        String confMethod = null;
        Date date = null;
        
        long time = System.currentTimeMillis() + 180000;
        while (iter.hasNext()) {
            assertion =(FSAssertion) iter.next();
            
            //check for valid AuthnRequest correspondence
            
            if (!authnRequest.getRequestID().equals(
                    assertion.getInResponseTo()))
            {
                FSUtils.debug.error("FSAssertionArtifactHandler."
                    + "validateAssertion:"
                    + " assertion does not correspond to any valid request");
                return null;
            }
            
            if (FSServiceUtils.isSigningOn()){
                if (!verifyAssertionSignature(assertion)) {
                    FSUtils.debug.error("FSAssertionArtifactHandler."
                        + "validateAssertion:"
                        + " assertion signature verification failed");
                    return null;
                }
            }
            
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionArtifactHandler."
                    + "validateAssertion: Assertion signature verified");
            }
            aIDString = assertion.getAssertionID();
            // make sure it's not being used            
            if (idTimeMap.containsKey(aIDString)) {
                FSUtils.debug.error(
                    "FSAssertionArtifactHandler.validateAssertion: Assertion: "
                    + aIDString + " is used");
                return null;
            }
            // check issuer of the assertions
            issuer = assertion.getIssuer();
            try {
                if (idpEntityId != null) {
                    if (!idpEntityId.equals(issuer)) {
                        FSUtils.debug.error("FSAssertionArtifactHandler."
                            + "validateAssertion: "
                            + "Assertion issuer is not the entity where "
                            + "AuthnRequest was sent originally.");
                        return null;
                    }
                } else {
                    FSUtils.debug.error("FSAssertionArtifactHandler."
                        + "validateAssertion: "
                        + "Assertion issuer is: " + issuer);
                    IDFFMetaManager metaManager = 
                        FSUtils.getIDFFMetaManager();
                    IDPDescriptorType idpDesc= 
                        metaManager.getIDPDescriptor(issuer);
                    if (idpDesc == null){
                        FSUtils.debug.error("FSAssertionArtifactHandler."
                            + "validateAssertion:"
                            + " Assertion issuer is not on the trust list");
                        return null;
                    }
                    setProviderDescriptor(idpDesc);
                    setProviderEntityId(issuer);
                }
            } catch(Exception ex){
                FSUtils.debug.error("FSAssertionArtifactHandler."
                    + "validateAssertion: "
                    + "Assertion issuer is not on the trust list");
                return null;
            }
            
            // must be valid(timewise)
            if (!assertion.isTimeValid()) {
                FSUtils.debug.error(
                    "FSAssertionArtifactHandler.validateAssertion:"
                    + " Assertion's time is not valid.");
                return null;
            }
            
            
            // TODO: IssuerInstant of the assertion is within a few minutes
            // This is a MAY in spec. Which number to use for the few minutes?
            
            // if present, target of the assertions must == local server IP
            Conditions conds = assertion.getConditions();
            if (!forThisServer(conds)) {
                FSUtils.debug.error("FSAssertionArtifactHandler."
                    + "validateAssertion: "
                    + "assertion is not issued for this site.");
                return null;
            }
            
            //for each assertion, loop to check each statement
            boolean authnStatementFound = false;
            if (assertion.getStatement() != null){
                stmtIter = assertion.getStatement().iterator();
                while(stmtIter.hasNext()) {
                    statement =(Statement) stmtIter.next();
                    stmtType = statement.getStatementType();
                    if (stmtType == Statement.AUTHENTICATION_STATEMENT) {
                        FSAuthenticationStatement authStatement = 
                            (FSAuthenticationStatement)statement;
                        authnStatementFound = true;
                        try {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                    "FSAssertionArtifactHandler."
                                    + "validateAssertion: "
                                    + "validating AuthenticationStatement:"
                                    + authStatement.toXMLString());
                            }
                        } catch(FSException e){
                            FSUtils.debug.error("FSAssertionArtifactHandler."
                                + "validateAssertion: Exception. "
                                + "Invalid AuthenticationStatement: ", e);
                            return null;
                        }
                        //check ReauthenticateOnOrAfter
                        reAuthnOnOrAfterDate = 
                            authStatement.getReauthenticateOnOrAfter();
                        //process SessionIndex
                        idpSessionIndex = authStatement.getSessionIndex();
                        
                        authnContextStmt = authStatement.getAuthnContext();
                        
                        subject =(FSSubject)authStatement.getSubject();
                        if (subject == null ){
                            FSUtils.debug.error("FSAssertionArtifactHandler." 
                                + "validateAssertion: Subject is null");
                            return null;
                        } else {
                            try {
                                if (FSUtils.debug.messageEnabled()) {
                                    FSUtils.debug.message(
                                        "FSAssertionArtifactHandler."
                                        + "validateAssertion: "
                                        + "found Authentication Statement. "
                                        + "Subject = " 
                                        + subject.toXMLString());
                                }
                            } catch(FSException e){
                                FSUtils.debug.error(
                                    "FSAssertionArtifactHandler."
                                    + "validateAssertion: "
                                    + " Exception. Invalid subject: ", e);
                                continue;
                            }
                        }
                        
                        // ConfirmationMethod of each subject must be set to 
                        //bearer
                        if (((subConf = 
                            subject.getSubjectConfirmation()) == null) ||
                            ((confMethods = 
                                subConf.getConfirmationMethod())== null) ||
                            (confMethods.size() != 1)) 
                        {
                            FSUtils.debug.error("FSAssertionArtifactHandler."
                                + "validateAssertion: "
                                + "missing or extra ConfirmationMethod.");
                            return null;
                        }
                        if (((confMethod = 
                            (String)confMethods.iterator().next()) == null) ||
                            !((confMethod.equals(
                                SAMLConstants.CONFIRMATION_METHOD_BEARER)) ||
                            (confMethod.equals(
                                SAMLConstants.CONFIRMATION_METHOD_ARTIFACT)) ||
                            (confMethod.equals(SAMLConstants.
                                DEPRECATED_CONFIRMATION_METHOD_ARTIFACT))))
                        {
                            FSUtils.debug.error("FSAssertionArtifactHandler."
                                + "validateAssertion: wrong "
                                + "ConfirmationMethod");
                            return null;
                        }
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSAssertionArtifactHandler."
                                + "validateAssertion: Confirmation method: " 
                                + confMethod);      
                        }
                    } else if (stmtType == Statement.ATTRIBUTE_STATEMENT) {
                        AttributeStatement attrStatement = 
                                  (AttributeStatement)statement;
                        if (!checkForAttributeStatement(attrStatement)) {
                            attrStatements.add(attrStatement);
                        }
                        
                    }
                }
            }
            
            if (!authnStatementFound){
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionArtifactHandler."
                        + "validateAssertion: "
                        + "No Authentication statement found in the Assertion. "
                        + "User is not authenticated by the IDP");
                }
                return null;
            }
            
            // add the assertion to idTimeMap
            if ((date = conds.getNotOnorAfter()) != null) {
                time = date.getTime();
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionArtifactHandler."
                    + "validateAssertion: Adding " 
                    + aIDString 
                    + " to idTimeMap.");
            }
            synchronized(idTimeMap) {
                idTimeMap.put(aIDString, new Long(time));
            }
            securityAssertions = assertion.getDiscoveryCredential();
        }
        
        if (subject == null) {
            FSUtils.debug.error("FSAssertionArtifactHandler.validateAssertion:"
                + " couldn't find Subject.");
            return null;
        }
        return subject;
    }

    /**
     * Checks the attribute statement for boot strap statement or auto fed
     * attribute  statement. 
     * @param attrStatement AttributeStatement.
     * @return true if the <code>AttributeStatement</code> is of type
     *          discovery boot strap or the auto federation statement.
     */
    private boolean checkForAttributeStatement(
       AttributeStatement attrStatement) {

        List attributes = attrStatement.getAttribute();
        if (attributes == null || attributes.size() == 0) {
            return false;
        }

        Iterator iter = attributes.iterator();
        Attribute attribute = (Attribute)iter.next();

        if (attribute.getAttributeName().equals(
            IFSConstants.DISCO_RESOURCE_OFFERING_NAME)) 
        {
            bootStrapStatement = attrStatement;
            return true;

        } else if(attribute.getAttributeName().equals(
            IFSConstants.AUTO_FED_ATTR))
        {
            _autoFedStatement = attrStatement;
            List attrValue = null;

            try {
                attrValue = attribute.getAttributeValue();
            } catch (SAMLException se) {
                FSUtils.debug.error("FSAssertionArtifactHandler.checkFor" +
                    "AttributeStatement: ", se);
            }

            String _autoFedValue = null;
            if (attrValue != null && attrValue.size() != 0) {
                Iterator iter2 = attrValue.iterator();
                Element elem = (Element)iter2.next();
                _autoFedValue = XMLUtils.getElementValue(elem);
            }

            String enabledStr = IDFFMetaUtils.getFirstAttributeValueFromConfig(
                hostConfig, IFSConstants.ENABLE_AUTO_FEDERATION);
            
            if (enabledStr != null && enabledStr.equalsIgnoreCase("true") &&
                _autoFedValue != null) 
            {
                autoFedSearchMap = new HashMap(); 
                Set set = new HashSet();
                set.add(_autoFedValue);
                autoFedSearchMap.put(
                    IDFFMetaUtils.getFirstAttributeValueFromConfig(
                        hostConfig, IFSConstants.AUTO_FEDERATION_ATTRIBUTE),
                    set);
            }

            return true;
        }
        return false;
    }
    
    protected boolean verifyAssertionSignature(FSAssertion assertion) {
        FSUtils.debug.message(
            "FSAssertionArtifactHandler.verifyAssertionSignature: Called");
        try {
            if (!assertion.isSigned()){
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionArtifactHandler."
                        + "verifyAssertionSignature: Assertion is not signed");
                }
                return false;
            }

            X509Certificate cert = KeyUtil.getVerificationCert(
                idpDescriptor, idpEntityId, true);
            
            if (cert == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionArtifactHandler."
                        + "verifyAssertionSignature: couldn't obtain "
                        + "this site's cert.");
                }
                throw new SAMLResponderException(
                    FSUtils.bundle.getString(IFSConstants.NO_CERT));
            }
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            if (authnResponse != null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionArtifactHander." +
                    "verifyAssertionSignature:  xml string to be verified:" +
                    XMLUtils.print((Node)
                        authnResponse.getDOMElement().getOwnerDocument())); 
                }
                return manager.verifyXMLSignature(
                    authnResponse.getDOMElement().getOwnerDocument(),cert);
            } else if(samlResponseElt != null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionArtifactHander." +
                    "verifyAssertionSignature:  xml string to be verified:" +
                    XMLUtils.print((Node) samlResponseElt.getOwnerDocument())); 
                }
                return manager.verifyXMLSignature(
                    samlResponseElt.getOwnerDocument(), cert);
            } else {
                return false;
            }

        } catch(Exception e){
            FSUtils.debug.error("FSAssertionArtifactHandler."
                + "verifyAssertionSignature: "
                + "Exception occured while verifying IDP's signature:", e);
            return false;
        }
    }
    
    protected boolean forThisServer(Conditions conds) {
        FSUtils.debug.message(
            "FSAssertionArtifactHandler.forThisServer: Called");
        if ((conds == null) ||
            (hostEntityId == null) ||
            (hostEntityId.length() == 0)) 
        {
            return true;
        }        
        Set targetConds = conds.getAudienceRestrictionCondition();
        if ((targetConds == null) ||(targetConds.isEmpty())) {
            return true;
        }        
        boolean forThis = false;
        Iterator tcIter = targetConds.iterator();
        AudienceRestrictionCondition targetCond = null;
        while(tcIter.hasNext()) {
            targetCond =(AudienceRestrictionCondition) tcIter.next();
            if (targetCond.containsAudience(hostEntityId)) {
                forThis = true;
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionArtifactHandler."
                        + "forThisServer: Assertion is validated to be"
                        + "for this server");
                }
                break;
            }
        }
        return forThis;
    }
    
    protected boolean generateToken(
        NameIdentifier ni,
        int handleType, 
        String orgDN,
        NameIdentifier niIdp,
        Map env
    ) 
    {
        FSUtils.debug.message(
            "FSAssertionArtifactHandler.generateToken: Called");
        if ((ni == null)){
            FSUtils.debug.error("FSAssertionArtifactHandler."
                + "generateToken: Invalid userDN input");
            return false;
        }
        if ((orgDN == null) ||(orgDN.length() == 0)){
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSAssertionArtifactHandler.generateToken:" +
                    "Invalid orgDN input using default orgDN");
            }
            orgDN = SystemConfigurationUtil.getProperty(
                "com.iplanet.am.defaultOrg");
        }
        try {
            String name = ni.getName();
            String nameSpace = ni.getNameQualifier();
            if ((nameSpace == null) || (nameSpace.length() == 0)) {
                nameSpace = hostEntityId;
            }

            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionArtifactHandler."
                    + "generateToken: Trying to get userDN for opaqueHandle= "
                    + name 
                    + " ,securityDomain= " 
                    + nameSpace 
                    + " And HandleType=" 
                    + handleType);
            }
            
            String affiliationID = authnRequest.getAffiliationID();
            FSAccountFedInfoKey fedKey = 
                new FSAccountFedInfoKey(nameSpace, name);
            FSAccountManager accountManager = FSAccountManager.getInstance(
                hostMetaAlias);
            String userID = accountManager.getUserID(fedKey, orgDN, env);
            if (userID == null) {
                if (niIdp != null && nameSpace.equals(affiliationID)) {
                    fedKey = new FSAccountFedInfoKey(affiliationID, 
                        niIdp.getName());
                    userID =  accountManager.getUserID(fedKey, orgDN, env);
                    if (userID != null) { 
                        FSAccountFedInfo oldInfo = 
                            accountManager.readAccountFedInfo(
                                userID, affiliationID);
                        if (oldInfo != null) {
                            accountManager.removeAccountFedInfo(
                                userID, oldInfo);
                        }
                        FSAccountFedInfo accountInfo = new FSAccountFedInfo(
                            idpEntityId, ni, niIdp, true);
                        accountInfo.setAffiliation(true);
                        fedKey = new FSAccountFedInfoKey(nameSpace, name); 
                        accountManager.writeAccountFedInfo(
                            userID, fedKey, accountInfo);
                    } else {
                        FSUtils.debug.error(
                            "FSAssertionArtifactHandler.generateToken: " +
                            "Can't dereference handle. fedKey=" +
                            fedKey.toString());
                        return false;
                    }
                } else {
                    // Check if there is any 6.2 format? 
                    FSAccountFedInfoKey oldKey = new FSAccountFedInfoKey(
                        idpEntityId, name);

                    if (oldKey != null) {
                        userID = accountManager.getUserID(oldKey, orgDN, env);
                        if (userID != null) {
                            FSAccountFedInfo fedInfo = 
                                accountManager.readAccountFedInfo(
                                    userID, idpEntityId); 
                            if (fedInfo != null && fedInfo.isFedStatusActive()){
                                // rewrite it.
                                NameIdentifier localNI = 
                                    fedInfo.getLocalNameIdentifier();
                                if (localNI != null) {
                                    localNI.setNameQualifier(hostEntityId);
                                }
                                accountManager.removeAccountFedInfo(
                                    userID, fedInfo);
                                NameIdentifier remoteNI =
                                    fedInfo.getRemoteNameIdentifier();
                                if (remoteNI != null) {
                                    remoteNI.setNameQualifier(hostEntityId);
                                }
                                FSAccountFedInfo newInfo = new FSAccountFedInfo(
                                    idpEntityId,
                                    localNI,
                                    remoteNI,
                                    true);
                                accountManager.removeAccountFedInfoKey(
                                    userID, oldKey);
                                FSAccountFedInfoKey newKey = 
                                    new FSAccountFedInfoKey(hostEntityId, name);
                                accountManager.writeAccountFedInfo(
                                    userID, newKey, newInfo);
                            } else {
                                FSUtils.debug.error(
                                    "FSAssertionArtifactHandler." +
                                    "generateToken: Can't dereference handle.");
                                return false;
                            }
                        } else {
                            String enabledStr =
                                IDFFMetaUtils.getFirstAttributeValueFromConfig(
                                    hostConfig, 
                                    IFSConstants.ENABLE_AUTO_FEDERATION);
                            if (enabledStr != null && 
                                enabledStr.equalsIgnoreCase("true") &&
                                _autoFedStatement != null) 
                            {
                                userID = accountManager.getUserID(
                                    autoFedSearchMap, orgDN, null); 
                                if (userID != null) {
                                    FSAccountFedInfoKey newKey = 
                                        new FSAccountFedInfoKey(
                                            hostEntityId, name);
                                    FSAccountFedInfo newInfo = 
                                        new FSAccountFedInfo(
                                            idpEntityId,
                                            null,
                                            ni,
                                            true);
                                    accountManager.writeAccountFedInfo(
                                        userID, newKey, newInfo);
                                } else {
                                    FSUtils.debug.error(
                                        "FSAssertionArtifactHandler. " +
                                        "generateToken:" +
                                        "Can't dereference handle.");
                                    return false;
                                }
                            } else {
                                FSUtils.debug.error(
                                    "FSAssertionArtifactHandler." +
                                    "generateToken: Can't dereference handle.");
                                return false;
                            }
                        }
                    } else {
                        FSUtils.debug.error("FSAssertionArtifactHandler." +
                            "generateToken: Can't dereference handle.");
                        return false;
                    }
                }
            } else {
                FSAccountFedInfo fedInfo = null;
                if (affiliationID != null) {
                    fedInfo = accountManager.readAccountFedInfo(
                        userID, affiliationID);
                } else {
                    fedInfo = accountManager.readAccountFedInfo(
                        userID, idpEntityId);
                }
                if (fedInfo == null){
                    FSUtils.debug.error(
                        "FSAssertionArtifactHandler.generateToken: "
                        + "User's account is not federated, id=" + userID);
                    return false;
                }
            }
            //get AuthnLevel from authnContext
            String authnContextClassRef = null;
            int authnLevel = 0;
            Map authnContextInfoMap = 
                FSServiceUtils.getSPAuthContextInfo(hostConfig);
            if (authnContextStmt != null && 
                authnContextStmt.getAuthnContextClassRef() != null && 
                authnContextStmt.getAuthnContextClassRef().length() != 0)
            {
                   
                authnContextClassRef = 
                    authnContextStmt.getAuthnContextClassRef();
                if (authnContextClassRef != null && 
                    authnContextClassRef.length() != 0)
                {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSAssertionArtifactHandler."
                            + "generateToken: AuthnContextClassRef "
                            + "found in AuthenticationStatement:" 
                            + authnContextClassRef);                    
                    }
                    FSSPAuthenticationContextInfo authnContextInfo = 
                        (FSSPAuthenticationContextInfo)authnContextInfoMap
                            .get(authnContextClassRef);
                    if (authnContextInfo != null){
                        authnLevel = authnContextInfo.getAuthenticationLevel();
                    } else {
                        FSUtils.debug.error("FSAssertionArtifactHandler."
                            + "generateToken: Could not find "
                            + "AuthnContextClassInfo for authnContextClassRef: " 
                            + authnContextClassRef 
                            + "Using default authnContextClass");
                        authnContextClassRef = null;
                    }
                }
            } else {
                FSUtils.debug.warning(
                    "FSAssertionArtifactHandler.generateToken: " +
                    "Could not find AuthnContextClassRef in the " +
                    "AuthenticationStatement. Using default authnContextClass");
            }
            if (authnContextClassRef == null || 
                authnContextClassRef.length() == 0)
            {
                authnContextClassRef = 
                    IDFFMetaUtils.getFirstAttributeValueFromConfig(
                        hostConfig, IFSConstants.DEFAULT_AUTHNCONTEXT);
                FSSPAuthenticationContextInfo authnContextInfo = 
                    (FSSPAuthenticationContextInfo)authnContextInfoMap
                        .get(authnContextClassRef);
                if (authnContextInfo != null){
                    authnLevel = authnContextInfo.getAuthenticationLevel();
                } else {
                    FSUtils.debug.error("FSAssertionArtifactHandler."
                        + "generateToken: Could not find authentication level "
                        + "for default authentication context class");
                    return false;
                }
            }
            
            Map valueMap = new HashMap();
            valueMap.put(SessionProvider.PRINCIPAL_NAME, userID);
            valueMap.put(SessionProvider.REALM, orgDN);
            valueMap.put(
                SessionProvider.AUTH_LEVEL, String.valueOf(authnLevel));
            valueMap.put(SessionProvider.AUTH_INSTANT, getAuthInstant());
            valueMap.put("idpEntityID", idpEntityId);
            //valueMap.put("resourceOffering",            
            //valueMap.put("securityToken",
            
            SessionProvider sessionProvider = SessionManager.getProvider();
            Object ssoSession = sessionProvider.createSession(
                valueMap, request, response, new StringBuffer(this.relayState));
            try {
                sessionProvider.addListener(
                    ssoSession, new FSTokenListener(hostEntityId));
            } catch (Exception e) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSAssertionArtifactHandler.generateToken:" +
                        "Couldn't add listener to session:", e);
                }
            }
            String value = sessionProvider.getSessionID(ssoSession);
            
            ssoToken = ssoSession;
            Iterator iter = null;
            //Set fed cookie
            String fedCookieName = SystemConfigurationUtil.getProperty(
                                   IFSConstants.FEDERATE_COOKIE_NAME);
            String fedCookieValue = "yes";
            Cookie fedCookie = null;
            if (cookieDomainList != null) {
                iter = cookieDomainList.iterator();
                while(iter != null && iter.hasNext()) {
                    fedCookie = CookieUtils.newCookie(fedCookieName,
                                        fedCookieValue,
                                        IFSConstants.PERSISTENT_COOKIE_AGE,
                                        "/",(String)iter.next());
                    response.addCookie(fedCookie);
                }
            } else {
                fedCookie = CookieUtils.newCookie(fedCookieName,
                                        fedCookieValue,
                                        IFSConstants.PERSISTENT_COOKIE_AGE,
                                        "/",null);
                response.addCookie(fedCookie);
            }
            
            //keep local session ref
            FSSessionManager sessionManager = 
                        FSSessionManager.getInstance(hostEntityId);
            FSSession session = sessionManager.getSession(userID, value);
            if (session != null){
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionArtifactHandler."
                        + "generateToken: An Existing session found for userID:"
                        + userID + " And SessionID: " + value 
                        + " Adding partner to the Session");
                }
                session.addSessionPartner(new FSSessionPartner(
                    idpEntityId, true));
                session.setSessionIndex(idpSessionIndex);
                sessionManager.addSession(userID,session);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionArtifactHandler."
                        + "generateToken: No existing session found for userID:"
                        + userID + " And SessionID: " + value 
                        + " Creating a new Session");
                    }
                    session = new FSSession(value);
                    session.addSessionPartner(new FSSessionPartner(
                        idpEntityId, true));
                    if (idpSessionIndex != null){
                        session.setSessionIndex(idpSessionIndex);
                    }
                    sessionManager.addSession(userID, session);
            }

            // keep the attr statement in FSSession.
            if (bootStrapStatement != null) {
                session.setBootStrapAttributeStatement(bootStrapStatement);
            }

            if (_autoFedStatement != null) {
                session.setAutoFedStatement(_autoFedStatement);
            }

            if (attrStatements.size() != 0) {
                session.setAttributeStatements(attrStatements);
                attributeMapper = getAttributeMapper();
                if (attributeMapper != null) { 
                    Map attributeMap = attributeMapper.getAttributes(
                        attrStatements, hostEntityId, 
                        idpEntityId, ssoToken);
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSAssertionArtifactHandler." +
                            "generateToken: Attribute map :" + attributeMap);
                    } 
                    if (attributeMap != null) {
                        setAttributeMap(ssoToken, attributeMap);  
                    }
                }
            }

            if (securityAssertions != null) {
                session.setBootStrapCredential(securityAssertions);
            }

            return true;            
        } catch(Exception e) {
            FSUtils.debug.error("FSAssertionArtifactHandler.generateToken: "
                + "Exception Occured ", e );
            return false;
        }
    }
    
    
    protected void processSAMLRequest() {
    }
    
    
    protected boolean doSingleSignOn(
        NameIdentifier ni, 
        int handleType, 
        String orgDN,
        NameIdentifier niIdp,
        Map env
    ) 
    {
        FSUtils.debug.message(
            "FSAssertionArtifactHandler.doSingleSignOn: Called");
        boolean created = true;
        try {
            created = generateToken(ni, handleType, orgDN, niIdp, env);
            if (!created) {
                String[] data = 
                    { FSUtils.bundle.getString("failGenerateSSOToken") };
                LogUtil.error(Level.INFO,LogUtil.FAILED_SSO_TOKEN_GENERATION,
                              data);
                return false;
            }
            
            return true;
        } catch(Exception e){
            FSUtils.debug.error("FSAssertionArtifactHandler.doSingleSignOn", e);
            return false;
        }
    }
    
    protected void redirectToResource(String resourceURL) throws FSException {
    }
    
    
    protected boolean doAccountFederation(NameIdentifier ni) {
        FSUtils.debug.message(
            "FSAssertionArtifactHandler.doAccountFederation:Called");
        if (ni == null){
            FSUtils.debug.error(
                "FSAssertionArtifactHandler.doAccountFederation:" +
                FSUtils.bundle.getString("invalidInput"));
            return false;
        }
        Object ssoToken = null;
        SessionProvider sessionProvider = null;
        try {
            sessionProvider = SessionManager.getProvider();
        } catch (SessionException se) {
            FSUtils.debug.error(
                "FSAssertionArtifactHandler.doAccountFederation: " +
                "Couldn't obtain session provider:", se);
            String[] data = 
                { FSUtils.bundle.getString("failGenerateSSOToken") };
            LogUtil.error(Level.INFO,LogUtil.FAILED_SSO_TOKEN_GENERATION, data);
            return false;
        }
        try {
            ssoToken = sessionProvider.getSession(request);
            if ((ssoToken == null) ||(!sessionProvider.isValid(ssoToken))) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionArtifactHandler."
                    + "doAccountFederation: couldn't obtain session from "
                    + "cookie");
                }
                ssoToken = null;
            }
        } catch(SessionException se) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionArtifactHandler."
                    + "doAccountFederation: exception when getting session "
                    + "from cookie:");
            }
            ssoToken = null;
        }

        // try URL rewriting
        FSSessionManager sessionManager = null;
        /*
        String cookieRewriteEnabled = SystemConfigurationUtil.getProperty(
               "com.sun.identity.cookieRewritingInPath", "false");
        if (Boolean.valueOf(cookieRewriteEnabled).booleanValue()
            && ssoToken == null) 
        */
        if (ssoToken == null)
        {
            try {
                sessionManager = FSSessionManager.getInstance(hostEntityId);
                ssoToken = sessionManager.getLocalSessionToken(
                                        authnRequest.getRequestID());
                if ((ssoToken == null) ||(!sessionProvider.isValid(ssoToken))) {
                    FSUtils.debug.error("FSAssertionArtifactHandler."
                        + "doAccountFederation: "
                        + FSUtils.bundle.getString("failGenerateSSOToken"));
                    String[] data = 
                        { FSUtils.bundle.getString("failGenerateSSOToken") };
                    LogUtil.error(
                        Level.INFO,LogUtil.FAILED_SSO_TOKEN_GENERATION, data);
                    return false;
                }
                this.relayState = sessionProvider.rewriteURL(
                    ssoToken, this.relayState);
                /*
                request.setAttribute(
                    SystemConfigurationUtil.getProperty(
                        "com.iplanet.am.cookie.name"),
                    sessionProvider.getSessionID(ssoToken));
                */
            } catch (Exception ex) {
                FSUtils.debug.error("FSAssertionArtifactHandler."
                    + "doAccountFederation: " 
                    + FSUtils.bundle.getString("failGenerateSSOToken"), ex);
                String[] data = 
                    { FSUtils.bundle.getString("failGenerateSSOToken") };
                LogUtil.error(
                    Level.INFO,LogUtil.FAILED_SSO_TOKEN_GENERATION, data);
                return false;
            }
        }

        if (ssoToken == null && nameIDPolicy != null && 
                nameIDPolicy.equals(IFSConstants.NAME_ID_POLICY_ONETIME)) 
        {
            ssoToken = generateAnonymousToken(response);
        }

        if (ssoToken == null) {
            FSUtils.debug.error(
                "FSAssertionArtifactHandler.doAccountFederation:"
                + "Account federation failed. Invalid session");
            return false;
        }

        try {
            String opaqueHandle = ni.getName();
            String userID = sessionProvider.getPrincipalName(ssoToken);
            String securityDomain = ni.getNameQualifier(); 
            if ((securityDomain == null) || (securityDomain.length() == 0)){
               securityDomain = hostEntityId;
            }
            FSAccountFedInfo accountInfo = new FSAccountFedInfo(
                idpEntityId, null, ni, true);
            FSAccountManager accountManager = FSAccountManager.getInstance(
                hostMetaAlias);
            FSAccountFedInfoKey fedKey = null;
            String affiliationID = authnRequest.getAffiliationID();
            if (affiliationID != null) {
                fedKey = new FSAccountFedInfoKey(affiliationID, opaqueHandle);
                accountInfo.setAffiliation(true);
            } else {
                fedKey = new FSAccountFedInfoKey(securityDomain, opaqueHandle);
            }

            if (nameIDPolicy == null ||
                !nameIDPolicy.equals(IFSConstants.NAME_ID_POLICY_ONETIME)) 
            {
                accountManager.writeAccountFedInfo(userID, fedKey, accountInfo);
            }
            //keep local session ref
            if (sessionManager == null) {
                sessionManager = FSSessionManager.getInstance(hostEntityId);
            }
            String sessionID = sessionProvider.getSessionID(ssoToken);
            FSSession session = sessionManager.getSession(
                userID, sessionID);
            if (session != null){
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionArtifactHandler."
                        + "doAccountFederation: No existing session found "
                        + " for userID:"
                        + userID + " And SessionID: " + sessionID 
                        + " Creating a new Session");
                }
                session.addSessionPartner(
                    new FSSessionPartner(idpEntityId, true));
                session.setSessionIndex(idpSessionIndex);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertionArtifactHandler."
                        + "doAccountFederation: An Existing session found"
                        + "for userID:"
                        + userID + " And SessionID: " + sessionID 
                        + " Adding partner to the Session");
                }
                session = new FSSession(sessionID);
                session.addSessionPartner(
                    new FSSessionPartner(idpEntityId, true));
                if (idpSessionIndex != null){
                    session.setSessionIndex(idpSessionIndex);
                }
            }
            if (nameIDPolicy != null &&
                nameIDPolicy.equals(IFSConstants.NAME_ID_POLICY_ONETIME)) 
            {
                session.setOneTime(true);
                session.setAccountFedInfo(accountInfo);
                session.setUserID(userID);
            }

            if (bootStrapStatement != null) {
                session.setBootStrapAttributeStatement(bootStrapStatement);
            }
            
            if (attrStatements.size() != 0) {
                attributeMapper = getAttributeMapper();
                if (attributeMapper != null) {
                    Map attributeMap = attributeMapper.getAttributes(
                        attrStatements, hostEntityId,
                        idpEntityId, ssoToken);
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSAssertionArtifactHandler." +
                            "generateToken: Attribute map :" + attributeMap);
                    }
                    setAttributeMap(ssoToken, attributeMap);
                }
            }

            sessionManager.addSession(userID, session);

        } catch(Exception ex){
            FSUtils.debug.error(
                "FSAssertionArtifactHandler.doAccountFederation:"
                + FSUtils.bundle.getString("ExceptionOccured") , ex);
            return false;
        }
        String[] data = {this.relayState} ;
        LogUtil.access(Level.INFO,LogUtil.ACCESS_GRANTED_REDIRECT_TO,data); 
        response.setHeader("Location", this.relayState);
        //Set fed cookie
        if (nameIDPolicy == null ||
            !nameIDPolicy.equals(IFSConstants.NAME_ID_POLICY_ONETIME)) 
        {
            String fedCookieName = SystemConfigurationUtil.getProperty(
                IFSConstants.FEDERATE_COOKIE_NAME);
            String fedCookieValue = "yes";
            Cookie fedCookie = null;
            Iterator iter = null;
            if (cookieDomainList != null) {
                iter = cookieDomainList.iterator();
                while(iter != null && iter.hasNext()) {
                    fedCookie = CookieUtils.newCookie(
                        fedCookieName,
                        fedCookieValue,
                        IFSConstants.PERSISTENT_COOKIE_AGE,
                        "/",
                        (String)iter.next());
                    response.addCookie(fedCookie);
                }
            } else {
                fedCookie = CookieUtils.newCookie(
                    fedCookieName,
                    fedCookieValue,
                    IFSConstants.PERSISTENT_COOKIE_AGE,
                    "/",
                    null);
                response.addCookie(fedCookie);
            }
        }
        
        //Name registration        
        // comment it out for now as the spec doesn't mendate this.
        /*
        try {
            // get if need name registration from sp extended meta
            String indicator = IDFFMetaUtils.getFirstAttributeValueFromConfig(
                hostConfig, IFSConstants.ENABLE_REGISTRATION_AFTER_SSO);
            if (indicator != null && indicator.equalsIgnoreCase("true")) {
                FSServiceManager serviceManager = 
                    FSServiceManager.getInstance();
                FSNameRegistrationHandler handlerObj = 
                    serviceManager.getNameRegistrationHandler(
                        idpEntityId,
                        IFSConstants.IDP);
                if (handlerObj != null) {
                    handlerObj.setHostedDescriptor(hostDesc);
                    handlerObj.setHostedDescriptorConfig(hostConfig);
                    handlerObj.setHostedEntityId(hostEntityId);
                    handlerObj.setMetaAlias(hostMetaAlias);
                    handlerObj.handleRegistrationAfterFederation(
                        this.relayState, response);
                }
                if (!FSServieUtils.isRegisProfileSOAP(
                    sessionProvider.getPrincipalName(ssoToken),
                    idpEntityId,
                    idpDescriptor,
                    hostMetaAlias,
                    hostDesc)) 
                {
                    return true;
                }
            }
        } catch (SessionException se) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("doAccountFederation: exception:", se);
            }
        }
        */
        try {
            redirectToResource(this.relayState);
            return true;
        } catch(Exception e){
            return false;
        }
    }

    /**
     * Generates an anonymous token for onetime case.
     */
    protected Object generateAnonymousToken(HttpServletResponse response) {

        FSUtils.debug.message("FSAssertionArtifactHandler.generateAnonymous");
 
        try {
            Map valueMap = new HashMap();
            valueMap.put(SessionProvider.PRINCIPAL_NAME, ANONYMOUS_PRINCIPAL);
            valueMap.put(
                SessionProvider.REALM,
                IDFFMetaUtils.getFirstAttributeValueFromConfig(
                    hostConfig, IFSConstants.REALM_NAME));
            valueMap.put(SessionProvider.AUTH_INSTANT, getAuthInstant());
            valueMap.put("idpEntityID", idpEntityId);

            SessionProvider sessionProvider = SessionManager.getProvider();
            Object ssoSession = sessionProvider.createSession(
                valueMap, request, response, new StringBuffer(this.relayState));            try {
                sessionProvider.addListener(
                    ssoSession, new FSTokenListener(hostEntityId));
            } catch (Exception e) {
                FSUtils.debug.error(
                    "FSAssertionArtifactHandler.generateAnonymousToken:" +
                    "Couldn't add listener to session:", e);
            }

            return ssoSession;
        } catch (Exception ae) {
           FSUtils.debug.error(
               "FSAssertionArtifactHandler.generateAnonymousToken failed.", ae);
           return null;
        }

    }
    
    protected FSAuthnRequest getInResponseToRequest(String requestID) {
        FSUtils.debug.message(
            "FSBrowserArtifactConsumerHandler.getInResponseToRequest: Called");
        FSSessionManager sessionManager = 
            FSSessionManager.getInstance(hostEntityId);
        return authnRequest = sessionManager.getAuthnRequest(requestID);
    }
    
    protected String getProvider(String requestID) {
        FSUtils.debug.message(
            "FSAssertionArtifactHandler.getProvider: Called");
        FSSessionManager sessionManager = 
            FSSessionManager.getInstance(hostEntityId);
        return sessionManager.getIDPEntityID(requestID);
    }
    
    /**
     * Sets <code>IDP</code> provider descriptor.
     * @param idpDescriptor identity provider descriptor.
     */
    public void setProviderDescriptor(IDPDescriptorType idpDescriptor) {
        this.idpDescriptor = idpDescriptor;
    }

    /**
     * Sets <code>IDP</code> provider entity ID.
     * @param idpEntityId identity provider entity id.
     */
    public void setProviderEntityId(String idpEntityId) {
        this.idpEntityId = idpEntityId;
    }

    /**
     * Gets <code>AuthInstant</code>.
     * @return <code>AuthInstant</code> in UTC date format.
     */
    public String getAuthInstant() {
        return DateUtils.toUTCDateFormat(new Date());
    }
    
    /**
     * Checks if the proxying is enabled. It will be checking if the proxy
     * service provider descriptor is set in the session manager for the
     * specific request ID.
     * @param requestID authentication request id which is created by the
     *                  proxying IDP to the authenticating IDP.
     * @return true if the proxying is enabled.
     */
    protected boolean isIDPProxyEnabled(String requestID) {
        FSSessionManager sessionManager = 
            FSSessionManager.getInstance(hostEntityId);
        return (sessionManager.getProxySPDescriptor(requestID) != null);
    }

    /**
     * Sends the proxy authentication response to the proxying service
     * provider which has originally requested for the authentication.
     * @param requestID authnRequest id that is sent to the authenticating
     *  Identity Provider.
     */
    protected void sendProxyResponse(String requestID) {
        FSUtils.debug.message("FSAssertionArtifactHandler.sendProxyResponse::");
        FSSessionManager sessionManager =
            FSSessionManager.getInstance(hostEntityId);
        FSAuthnRequest origRequest = 
            sessionManager.getProxySPAuthnRequest(requestID);
        if (FSUtils.debug.messageEnabled()) {
            try {
                FSUtils.debug.message("FSAssertionHandler.sendProxyResponse:" +
                    origRequest.toXMLString());
            } catch (Exception ex) {
                FSUtils.debug.error("FSAssertionHandler.sendProxyResponse:" +
                    "toString(): Failed.", ex);
            }
        }
        SPDescriptorType proxyDescriptor = 
            sessionManager.getProxySPDescriptor(requestID);
        String proxySPEntityId = origRequest.getProviderId();
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSAssertionArtifactHandler.sendProxyResponse"
                + ":Original requesting service provider id:"
                + proxySPEntityId);
        }
 
        FSSession session = sessionManager.getSession(ssoToken);
        if (authnContextStmt != null) {
            String authnContext = authnContextStmt.getAuthnContextClassRef();
            session.setAuthnContext(authnContext);
        }

        session.addSessionPartner(new FSSessionPartner(
            proxySPEntityId, false));

        if (FSUtils.debug.messageEnabled()) {
            Iterator partners = session.getSessionPartners().iterator();
            while (partners.hasNext()) {
               FSSessionPartner part = (FSSessionPartner)partners.next();
               if (FSUtils.debug.messageEnabled()) {
                   FSUtils.debug.message("PARTNERS" + part.getPartner()); 
               }
            }
        }

        IDFFMetaManager metaManager = FSUtils.getIDFFMetaManager();
        BaseConfigType proxySPConfig = null;
        try {
            proxySPConfig = metaManager.getSPDescriptorConfig(proxySPEntityId);
        } catch (Exception e) {
            FSUtils.debug.error("FSAssertionArtifactHandler.sendProxyResponse:"
                + "Couldn't obtain proxy sp meta:", e);
        }
        FSProxyHandler handler = new FSProxyHandler(
            request, response, origRequest, proxyDescriptor, 
            proxySPConfig,
            proxySPEntityId,
            origRequest.getRelayState(), ssoToken);
        IDPDescriptorType localIDPDesc = null;
        BaseConfigType localIDPConfig = null;
        String localIDPMetaAlias = null;
        try {
            localIDPDesc = metaManager.getIDPDescriptor(hostEntityId);
            localIDPConfig = metaManager.getIDPDescriptorConfig(hostEntityId);
            localIDPMetaAlias = localIDPConfig.getMetaAlias();
        } catch (Exception e) {
            FSUtils.debug.error("FSAssertionartifactHandler.sendProxyResponse:"
                + "Exception when obtaining local idp meta:", e);
        }
        
        handler.setHostedEntityId(hostEntityId);
        handler.setHostedDescriptor(localIDPDesc);
        handler.setHostedDescriptorConfig(localIDPConfig);
        handler.setMetaAlias(localIDPMetaAlias);
        handler.processAuthnRequest(origRequest, true);
    }

    /**
     * Sets the attribute map to the Single sign on token.
     */
    private void setAttributeMap(Object token, Map attributeMap) {

        if (attributeMap == null || attributeMap.isEmpty()) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertionArtifactHandler.setAttribute"+
                    "Map: Attribute map is empty");
            }
            return;
        }

        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSAssertionArtifactHandler.setAttributeMap:"+
                " Attribute map that will be populated to ssotoken:" +
                attributeMap);
        }

        try {
            Set entrySet = attributeMap.entrySet();
            SessionProvider sessionProvider = SessionManager.getProvider();
            for (Iterator iter = entrySet.iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry)iter.next();
                String[] values = { (String)entry.getValue() };
                sessionProvider.setProperty(
                    token, (String)entry.getKey(), values);
            }
        } catch (Exception e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSAssertionArtifactHandler.setAttributeMapper:" +
                    "Cannot set attributes to session:", e);
            }
        }
    }

    private FSAttributeMapper getAttributeMapper() {

        if (attributeMapper != null) {
            return attributeMapper;
        }

        String mapperStr = IDFFMetaUtils.getFirstAttributeValueFromConfig(
            hostConfig, IFSConstants.ATTRIBUTE_MAPPER_CLASS);
        try {
            Class mapperClass = Class.forName(mapperStr);
            attributeMapper = (FSAttributeMapper) mapperClass.newInstance();
        } catch (Exception e) {
            FSUtils.debug.error(
                "FSAssertionArtifactHandler.getAttributeMapper:", e);
        }
        
        return attributeMapper;
    }

}
