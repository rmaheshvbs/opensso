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
 * $Id: SAML2SDKUtils.java,v 1.1 2006-10-30 23:16:14 qcheng Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */


package com.sun.identity.saml2.common;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import java.security.SecureRandom;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * The <code>SAML2SDKUtils</code> contains utility methods for SAML 2.0
 * implementation.
 *
 * @supported.all.api
 */
public class SAML2SDKUtils {
    //
    // This utility class will be run on client side as well,
    // so DO NOT add any static block which will not run on client side.
    //

    private static boolean isFM = true;

    // The deugging instance
    public static Debug debug = Debug.getInstance("libSAML2");
    //  SAML2 Resource bundle
    public static final String BUNDLE_NAME = "libSAML2";
    // The resource bundle for SAML 2.0 implementation.
    public static ResourceBundle bundle = Locale.
        getInstallResourceBundle(BUNDLE_NAME);
    protected static final String SAML2ID_PREFIX = "s2";
    public static SecureRandom random = new SecureRandom();

    /**
     * Defines mapping between interface and implementation class,
     * the properties are read from AMConfig.properties in following format:
     * com.sun.identity.saml2.sdk.mapping.<interface>=<implementation_class>
     * e.g.
     * com.sun.identity.saml2.sdk.mapping.Assertion=com.xxx.saml2.AssertionImpl
     */
    private static Map classMapping = new HashMap();

    // define constants for the interface names
    public static final String ACTION = "Action"; 
    public static final String ADVICE = "Advice"; 
    public static final String ASSERTION = "Assertion";
    public static final String ATTRIBUTE = "Attribute"; 
    public static final String ATTRIBUTE_STATEMENT = "AttributeStatement";
    public static final String AUDIENCE_RESTRICTION = "AudienceRestriction"; 
    public static final String AUTHN_CONTEXT = "AuthnContext"; 
    public static final String AUTHN_STATEMENT = "AuthnStatement"; 
    public static final String AUTHZ_DECISION_STATEMENT = 
        "AuthzDecisionStatement"; 
    public static final String BASEID = "BaseID"; 
    public static final String CONDITION = "Condition"; 
    public static final String CONDITIONS = "Conditions"; 
    public static final String ENCRYPTED_ASSERTION = "EncryptedAssertion";
    public static final String ENCRYPTED_ATTRIBUTE = "EncryptedAttribute"; 
    public static final String ENCRYPTED_ELEMENT = "EncryptedElement"; 
    public static final String ENCRYPTEDID = "EncryptedID"; 
    public static final String EVIDENCE = "Evidence";
    public static final String ISSUER = "Issuer"; 
    public static final String KEYINFO_CONFIRMATION_DATA = 
        "KeyInfoConfirmationData"; 
    public static final String NAMEID = "NameID"; 
    public static final String ONE_TIME_USE = "OneTimeUse"; 
    public static final String PROXY_RESTRICTION = "ProxyRestriction"; 
    public static final String STATEMENT = "Statement"; 
    public static final String SUBJECT_CONFIRMATION_DATA = 
        "SubjectConfirmationData"; 
    public static final String SUBJECT_CONFIRMATION = "SubjectConfirmation"; 
    public static final String SUBJECT = "Subject";
    public static final String SUBJECT_LOCALITY = "SubjectLocality"; 
    public static final String ARTIFACT = "Artifact"; 
    public static final String ARTIFACT_RESOLVE = "ArtifactResolve"; 
    public static final String ARTIFACT_RESPONSE = "ArtifactResponse";
    public static final String AUTHN_REQUEST = "AuthnRequest"; 
    public static final String EXTENSIONS = "Extensions"; 
    public static final String GET_COMPLETE = "GetComplete"; 
    public static final String IDPENTRY = "IDPEntry"; 
    public static final String IDPLIST = "IDPList";
    public static final String LOGOUT_REQUEST = "LogoutRequest"; 
    public static final String LOGOUT_RESPONSE = "LogoutResponse"; 
    public static final String MANAGE_NAMEID_REQUEST = "ManageNameIDRequest"; 
    public static final String MANAGE_NAMEID_RESPONSE = "ManageNameIDResponse"; 
    public static final String NAMEID_POLICY = "NameIDPolicy"; 
    public static final String NEW_ENCRYPTEDID = "NewEncryptedID"; 
    public static final String NEWID = "NewID";
    public static final String REQUESTED_AUTHN_CONTEXT = 
        "RequestedAuthnContext"; 
    public static final String REQUESTERID = "RequesterID"; 
    public static final String RESPONSE = "Response";
    public static final String SCOPING = "Scoping"; 
    public static final String SESSION_INDEX = "SessionIndex"; 
    public static final String STATUS_CODE = "StatusCode"; 
    public static final String STATUS_DETAIL = "StatusDetail"; 
    public static final String STATUS = "Status";
    public static final String STATUS_MESSAGE = "StatusMessage"; 
    public static final String STATUS_RESPONSE = "StatusResponse"; 

    /**
     * List of Interfaces in assertion and protocol packages which could have 
     * customized implementation
     */
    private static String[] interfactNames = {
        ACTION, ADVICE, ASSERTION, ATTRIBUTE, ATTRIBUTE_STATEMENT,
        AUDIENCE_RESTRICTION, AUTHN_CONTEXT, AUTHN_STATEMENT, 
        AUTHZ_DECISION_STATEMENT, BASEID, 
        CONDITION, CONDITIONS, ENCRYPTED_ASSERTION,
        ENCRYPTED_ATTRIBUTE, ENCRYPTED_ELEMENT, ENCRYPTEDID, EVIDENCE,
        ISSUER, KEYINFO_CONFIRMATION_DATA, NAMEID,
        ONE_TIME_USE, PROXY_RESTRICTION, STATEMENT, 
        SUBJECT_CONFIRMATION_DATA, SUBJECT_CONFIRMATION, SUBJECT,
        SUBJECT_LOCALITY, ARTIFACT, ARTIFACT_RESOLVE, ARTIFACT_RESPONSE,
        AUTHN_REQUEST, EXTENSIONS, GET_COMPLETE, IDPENTRY, IDPLIST,
        LOGOUT_REQUEST, LOGOUT_RESPONSE, MANAGE_NAMEID_REQUEST, 
        MANAGE_NAMEID_RESPONSE, NAMEID_POLICY, NEW_ENCRYPTEDID, NEWID,
        REQUESTED_AUTHN_CONTEXT, REQUESTERID, RESPONSE,
        SCOPING, SESSION_INDEX, STATUS_CODE, STATUS_DETAIL, STATUS,
        STATUS_MESSAGE, STATUS_RESPONSE }; 

    /**
     * Class array for Artifact constructor
     */
    private static Class[] artParam = new Class[] { (new byte[2]).getClass(), 
        int.class, String.class, String.class };

    /**
     * Class array for String as parameter
     */
    private static Class[] stringParam = new Class[] {String.class};

    /**
     * Class array for Element as parameter
     */
    private static Class[] elementParam = new Class[] {Element.class};

    static {
        String am_or_fm =
            SystemPropertiesManager.get(SAML2Constants.AM_OR_FM);
        if (am_or_fm != null &&
            am_or_fm.trim().equalsIgnoreCase("am")) {
            isFM = false;
        }

        // initialize class mapper
        int len = interfactNames.length;
        for (int i = 0; i < len; i++) {
            String iName = interfactNames[i];
            try {
                String implClass = SystemPropertiesManager.get(
                    SAML2Constants.SDK_CLASS_MAPPING + iName);
                if (implClass != null && implClass.trim().length() != 0) {
                    // try it out
                    if (debug.messageEnabled()) {
                        debug.message("SAML2SDKUtils.init: mapper for " + iName
                            + "=" + implClass);
                    }
                    classMapping.put(iName, Class.forName(implClass.trim()));
                }
            } catch (ClassNotFoundException cnfe) {
                debug.error("SAML2SDKUtils.init: " + iName, cnfe);
            } 
        }
    }
    
    /**
     * Protected contstructor.
     */
    protected SAML2SDKUtils() {}
     
    /**
     * Returns default object instance for a given interface. 
     * @param iName name of the interface.
     * @return object instance corresponding to the interface implementation. 
     *         return null if the object instance could not be obtained.
     */
    public static Object getObjectInstance(String iName) {
        Class implClass = (Class) classMapping.get(iName);
        if (implClass == null) {
            return null;
        } else {
            try {
                return implClass.newInstance();
            } catch (InstantiationException ie) {
                debug.error("SAML2SDKUtils.getDefaultInstance: " + iName, ie);
            } catch (IllegalAccessException iae) {
                debug.error("SAML2SDKUtils.getDefaultInstance: " + iName, iae);
            } 
            return null;
        }
    }

    /**
     * Returns new object instance taking String parameter in constructor. 
     * @param iName name of the interface.
     * @param value String value to be used as parameter in constructor.
     * @return object instance corresponding to the interface implementation. 
     *         return null if the object instance could not be obtained.
     */
    public static Object getObjectInstance(String iName, String value){
        Class implClass = (Class) classMapping.get(iName);
        if (implClass == null) {
            return null;
        } else {
            if (debug.messageEnabled()) {
                debug.message("SAML2SDKUtils.getObjectInstance: new customized "
                    + "impl (String) instance for " + iName);
            }
            Object[] params = new Object[] { value }; 
            return getObjectInstance(implClass, stringParam, params);
        }
    }

    /**
     * Returns new object instance taking Element parameter in constructor. 
     * @param iName name of the interface.
     * @param value Element value to be used as parameter in constructor.
     * @return object instance corresponding to the interface implementation. 
     *         return null if the object instance could not be obtained.
     */
    public static Object getObjectInstance(String iName, Element value) {
        Class implClass = (Class) classMapping.get(iName);
        if (implClass == null) {
            return null;
        } else {
            if (debug.messageEnabled()) {
                debug.message("SAML2SDKUtils.getObjectInstance: new customized "
                    + "impl instance (Element) for " + iName);
            }
            Object[] params = new Object[] { value }; 
            return getObjectInstance(implClass, elementParam, params);
        }
    }

    /**
     * Returns new object instance with given parameters. 
     * @param iName name of the interface.
     * @param typecode type code.
     * @param endpointIndex end point index.
     * @param sourceID source ID.
     * @param messageHandle message handler.
     * @return object instance corresponding to the interface implementation. 
     *         return null if the object instance could not be obtained.
     */

    public static Object getObjectInstance(String iName, byte[] typecode,
        int endpointIndex, String sourceID, String messageHandle) {
        Class implClass = (Class) classMapping.get(iName);
        if (implClass == null) {
            return null;
        } else {
            if (debug.messageEnabled()) {
                debug.message("SAML2SDKUtils.getObjectInstance: new customized "
                    + "impl (4) instance for " + iName);
            }
            Object[] params = new Object[] 
                { typecode, new Integer(endpointIndex), 
                  sourceID, messageHandle }; 
            return getObjectInstance(implClass, artParam, params);
        }
    }


    /**
     * Returns new object instance with given parameter in constructor. 
     * @param impl Class instance.
     * @param paramObj Class array for constructor parameters.
     * @param valueObj Object array for values of constructor parameters.
     * @return object instance corresponding to the interface implementation. 
     *         return null if the object instance could not be obtained.
     */
    private static Object getObjectInstance(Class impl, 
        Class[] paramObj, Object[] valueObj) {
        try {
            Constructor constr = impl.getConstructor(paramObj);
            return constr.newInstance(valueObj);
        } catch (NoSuchMethodException nsme) {
            debug.error("SAML2SDKUtils.getObjectInstance: " + impl.getName(), 
                nsme);
        } catch (SecurityException se) {
            debug.error("SAML2SDKUtils.getObjectInstance: " + impl.getName(), 
                se);
        } catch (InstantiationException ie) {
            debug.error("SAML2SDKUtils.getObjectInstance: " + impl.getName(), 
                ie);
        } catch (IllegalAccessException iae) {
            debug.error("SAML2SDKUtils.getObjectInstance: " + impl.getName(), 
                iae);
        } catch (IllegalArgumentException iae) {
            debug.error("SAML2SDKUtils.getObjectInstance: " + impl.getName(), 
                iae);
        } catch (InvocationTargetException ite) {
            debug.error("SAML2SDKUtils.getObjectInstance: " + impl.getName(), 
                ite);
        } 
        return null;
    }

    /**
     * Returns whether the existing installation is FM or AM.
     * @return true if the existing installation is FM, false
     * if the installation is AM
     */
    public static boolean isFM() {
        return isFM;
    }

    /**
     * Verifies if an element is a type of a specific statement.
     * Currently, this method is used by class AuthnStatementImpl,
     * AuthzDecisionStatement and AttributeStatementImpl.
     * @param element a DOM Element which needs to be verified.
     * @param statementname A specific name of a statement, for example,
     *          AuthnStatement, AuthzStatement or AttributeStatement
     * @return <code>true</code> if the element is of the specific type;
     *          <code>false</code> otherwise.
     */
    public static boolean checkStatement(Element element, String statementname){
        if (element == null || statementname == null) {
            return false;
        }
        
        String tag = element.getLocalName();
        if (tag == null) {
            return false;
        } else if (tag.equals("Statement")) {
            NamedNodeMap nm = element.getAttributes();
            int len = nm.getLength();
            String attrName = null;
            Attr attr = null;
            for (int j = 0; j < len; j++) {
                attr = (Attr) nm.item(j);
                attrName = attr.getLocalName();
                if ((attrName != null) && (attrName.equals("type")) &&
                (attr.getNodeValue().equals(statementname + "Type"))) {
                    return true;
                }
            }
        } else if (tag.equals(statementname)) {
            return true;
        }
        return false;
    }   

    /**
     * Converts byte array to String.
     *
     * @param bytes     Byte Array to be converted.
     * @return          result of the conversion.
     */
    public static String byteArrayToString(byte[] bytes) {
        char chars[] = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            chars[i] = (char) bytes[i];
        }
        return new String(chars);
    }

    /**
     * Converts integer to byte array.
     *
     * @param i         an integer value between 0 and 65535.
     * @return          a byte array whose length is 2.
     * @throws SAML2Exception if the input is not between 0 and 65535.
     */
    public static byte[] intToTwoBytes(int i)
    throws SAML2Exception {
        if (i < 0 || i > 65535) {
            debug.error("SAML2Utils.intToTwoBytes: wrong index value range.");
            throw new SAML2Exception(
            bundle.getString("wrongInput"));
        }
        
        String hexStr = Integer.toHexString(i);
        
        //System.out.println("Original="+hexStr);
        
        int len = hexStr.length();
        String norm = null;
        if (len > 4) {
            norm = hexStr.substring(0,4);
        } else {
            switch (len) {
                case 1:
                    norm = "000"+hexStr;
                    break;
                case 2:
                    norm = "00"+hexStr;
                    break;
                case 3:
                    norm = "0"+hexStr;
                    break;
                default:
                    norm = hexStr;
            }
        }
        
        byte[] bytes = hexStringToByteArray(norm);
        
        return bytes;
    }
    
    /**
     * Converts two bytes to an integer.
     *
     * @param bytes     byte array whose length is 2.
     * @return          an integer value between 0 and 65535.
     * @throws SAML2Exception if the input is null or the length is not 2.
     */
    public static int twoBytesToInt(byte[] bytes)
    throws SAML2Exception {
        if (bytes == null || bytes.length != 2) {
            debug.error("SAML2Utils.twoBytesToInt: input is null or length is "
            + "not 2.");
            throw new SAML2Exception(bundle.getString("wrontInput"));
        }
        
        String str0 = Integer.toHexString(bytes[0]);
        int len0 = str0.length();
        String norm0 = null;
        if (len0 > 2) {
            norm0 = str0.substring(len0-2, len0);
        } else {
            norm0 = str0;
        }
        String str1 = Integer.toHexString(bytes[1]);
        int len1 = str1.length();
        String norm1 = null;
        if (len1 > 2) {
            norm1 = str1.substring(len1-2, len1);
        } else if (len1 == 1) {
            norm1 = "0"+str1;
        } else {
            norm1 = str1;
        }
        
        String wholeHexStr = norm0+norm1;
        
        int i = Integer.parseInt(wholeHexStr, 16);
        
        return i;
    }
    
    /**
     * Generates message handle used in an <code>Artifact</code>.
     *
     * @return          String format of 20-byte sequence identifying
     *                  a message.
     */
    public static String generateMessageHandle() {
        if (random == null) {
            return null;
        }
        byte bytes[] = new byte[SAML2Constants.ID_LENGTH];
        random.nextBytes(bytes);
        return byteArrayToString(bytes);
    }
    
    /**
     * Converts String to Byte Array.
     *
     * @param input     String to be converted.
     * @return          result of the conversion.
     */
    public static byte[] stringToByteArray(String input) {
        char chars[] = input.toCharArray();
        byte bytes[] = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) {
            bytes[i] = (byte) chars[i];
        }
        return bytes;
    }
    
    /**
     * Converts byte array to <code>Hex</code> String.
     *
     * @param byteArray Byte Array to be converted.
     * @return result of the conversion.
     */
    public static String byteArrayToHexString(byte[] byteArray) {
        int readBytes = byteArray.length;
        StringBuffer hexData = new StringBuffer();
        int onebyte;
        for (int i=0; i < readBytes; i++) {
            onebyte = ((0x000000ff & byteArray[i]) | 0xffffff00);
            hexData.append(Integer.toHexString(onebyte).substring(6));
        }
        return hexData.toString();
    }
    
    /**
     * Converts <code>Hex</code> String to Byte Array.
     *
     * @param hexString <code>Hex</code> String to be converted.
     * @return result of the conversion.
     */
    public static byte[] hexStringToByteArray(String hexString) {
        int read = hexString.length();
        byte[] byteArray = new byte[read/2];
        for (int i=0, j=0; i < read; i++, j++) {
            String part = hexString.substring(i,i+2);
            byteArray[j] =
            new Short(Integer.toString(Integer.parseInt(part,16))).
            byteValue();
            i++;
        }
        return byteArray;
    }
    
    /**
     * Generates ID.
     * @return ID value.
     */
    public static String generateID() {
        if (random == null) {
            return null;
        }
        byte bytes[] = new byte[SAML2Constants.ID_LENGTH];
        random.nextBytes(bytes);
        return (SAML2ID_PREFIX + byteArrayToHexString(bytes));
    }    

}
