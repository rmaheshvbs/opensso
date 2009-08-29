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
 * $Id: SignatureProvider.java,v 1.10 2009-08-29 03:06:47 mallas Exp $
 *
 */



package com.sun.identity.saml.xmlsig;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * This is an interface to be implemented to sign and verify XML signature.
 */
public interface SignatureProvider {

    /**
     * Initialize the key provider 
     * @param keyProvider <code>KeyProvider</code> object 
     */
    public void initialize(KeyProvider keyProvider);
      
    /**
     * Sign the XML document using enveloped signatures.
     * @param doc XML dom object 
     * @param certAlias Signer's certificate alias name
     * @return signature Element object 
     * @throws XMLSignatureException if the document could not be signed
     */ 
     public org.w3c.dom.Element signXML(org.w3c.dom.Document doc, 
                                        java.lang.String certAlias)
        throws XMLSignatureException;
  
    /**
     * Sign the XML document using enveloped signatures.
     * @param doc XML dom object 
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML Signature Algorithm, such as 
              <code>SAMLConstants.ALGO_ID_SIGNATURE_DSA</code>
     * @return signature Element object 
     * @throws XMLSignatureException if the document could not be signed
     */ 
     public org.w3c.dom.Element signXML(org.w3c.dom.Document doc, 
                                        java.lang.String certAlias, 
                                        java.lang.String algorithm)
         throws XMLSignatureException;
  
    /**
     * Sign the XML string using enveloped signatures.
     * @param xmlString XML string to be signed
     * @param certAlias Signer's certificate alias name
     * @return XML signature string
     * @throws XMLSignatureException if the XML string could not be signed
     */
    public java.lang.String signXML(java.lang.String xmlString,
                                    java.lang.String certAlias)
        throws XMLSignatureException;
    
    /**
     * Sign the XML string using enveloped signatures.
     * @param xmlString XML string to be signed
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML Signature Algorithm, such as 
              <code>SAMLConstants.ALGO_ID_SIGNATURE_DSA</code>
     * @return XML signature string
     * @throws XMLSignatureException if the XML string could not be signed
     */
    public java.lang.String signXML(java.lang.String xmlString,
                                    java.lang.String certAlias,
                                    java.lang.String algorithm)
        throws XMLSignatureException;

    /**                         
     * Sign part of the XML document referred by the supplied id attribute using
       enveloped signatures and use exclusive XML canonicalization.            
     * @param doc XML dom object                                               
     * @param certAlias Signer's certificate alias name                        
     * @param algorithm XML signature algorithm                                
     * @param id attribute value of the node to be signed                   
     * @return signature dom object                                            
     * @throws XMLSignatureException if the document could not be signed       
     */                                                                        
    public org.w3c.dom.Element signXML(org.w3c.dom.Document doc,               
                                       java.lang.String certAlias,             
                                       java.lang.String algorithm,             
                                       java.lang.String id)                    
        throws XMLSignatureException;                                          

    /**
     * Sign part of the XML document referred by the supplied id attribute
     * using enveloped signatures and use exclusive XML canonicalization.
     * @param doc XML dom object
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML signature algorithm
     * @param id id attribute value of the node to be signed
     * @param xpath expression should uniquely identify a node before which
     * @return signature dom object
     * @throws XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signXML(org.w3c.dom.Document doc,
                                       java.lang.String certAlias,
                                       java.lang.String algorithm,
                                       java.lang.String id,
                                       java.lang.String xpath)
        throws XMLSignatureException;

     /**
     * Sign part of the XML document referred by the supplied id attribute
     * using enveloped signatures and use exclusive XML canonicalization.
     * @param doc XML dom object
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML signature algorithm
     * @param idAttrName attribute name for the id attribute of the node to be
     *        signed.
     * @param id id attribute value of the node to be signed
     * @param includeCert if true, include the signing certificate in
     *        <code>KeyInfo</code>. if false, does not include the signing
     *        certificate.
     * @return signature dom object
     * @throws XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signXML(org.w3c.dom.Document doc,
                                       java.lang.String certAlias,
                                       java.lang.String algorithm,
                                       java.lang.String idAttrName,
                                       java.lang.String id,
                                       boolean includeCert)
        throws XMLSignatureException;
 
    /**
     * Sign part of the XML document referred by the supplied id attribute
     * using enveloped signatures and use exclusive XML canonicalization.
     * @param xmlString a string representing XML dom object
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML signature algorithm
     * @param idAttrName attribute name for the id attribute of the node to be
     *        signed
     * @param id id attribute value of the node to be signed
     * @param includeCert if true, include the signing certificate in 
     *        <code>KeyInfo</code>. if false, does not include the signing
     *        certificate.
     * @return a string of signature dom object
     * @throws XMLSignatureException if the document could not be signed
     */
    public java.lang.String signXML(java.lang.String xmlString,
                                       java.lang.String certAlias,
                                       java.lang.String algorithm,
                                       java.lang.String idAttrName,
                                       java.lang.String id,
                                       boolean includeCert)
        throws XMLSignatureException;
        
     /**
     * Sign part of the XML document referred by the supplied id attribute
     * using enveloped signatures and use exclusive XML canonicalization.
     * @param doc XML dom object
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML signature algorithm
     * @param idAttrName attribute name for the id attribute of the node to be
     *        signed
     * @param id id attribute value of the node to be signed
     * @param includeCert if true, include the signing certificate in 
     *        <code>KeyInfo</code>. if false, does not include the signing
     *        certificate.
     * @param xpath expression should uniquely identify a node before which
     * @return signature dom object
     * @throws XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signXML(org.w3c.dom.Document doc,
        java.lang.String certAlias,
        java.lang.String algorithm,
        java.lang.String idAttrName,
        java.lang.String id,
        boolean includeCert,
        java.lang.String xpath)
        throws XMLSignatureException;

    /**                                                                    
     * Sign part of the XML document referred by the supplied id attribute using
       enveloped signatures and use exclusive XML canonicalization.            
     * @param xmlString XML dom object                                               
     * @param certAlias Signer's certificate alias name                        
     * @param algorithm XML signature algorithm                                
     * @param id id attribute value of the node to be signed                   
     * @return signature dom object                                            
     * @throws XMLSignatureException if the document could not be signed       
     */                                                                        
    public java.lang.String signXML(java.lang.String xmlString,                
                                       java.lang.String certAlias,             
                                       java.lang.String algorithm,             
                                       java.lang.String id)                    
        throws XMLSignatureException;                                          
    
    /**
     *
     * Sign part of the XML document referred by the supplied a list
     * of id attributes of nodes
     * @param doc XML dom object
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @return signature dom object
     * @throws XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signXML(org.w3c.dom.Document doc,
                                       java.lang.String certAlias,
                                       java.lang.String algorithm,
                                       java.util.List ids)
        throws XMLSignatureException;

    /**
     *
     * Sign part of the XML document referred by the supplied a list
     * of id attributes of nodes
     * @param xmlString XML dom object
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @return XML signature string
     * @throws XMLSignatureException if the document could not be signed
     */
    public java.lang.String signXML(java.lang.String xmlString,                
                                       java.lang.String certAlias,             
                                       java.lang.String algorithm,             
                                       java.util.List ids)                    
        throws XMLSignatureException;                                          
    
    /**
     *
     * Sign part of the XML document referred by the supplied a list
     * of id attributes of nodes
     * @param doc XML dom object
     * @param certAlias Signer's certificate alias name
     * @param algorithm XML signature algorithm
     * @param transformAlag XML signature transform algorithm 
     *        Those transfer constants are defined as
     *        <code>SAMLConstants.TRANSFORM_XXX</code>.
     * @param ids list of id attribute values of nodes to be signed
     * @return XML signature element
     * @throws XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signXML(org.w3c.dom.Document doc,          
                                    java.lang.String certAlias,             
                                    java.lang.String algorithm,           
                                    java.lang.String transformAlag,
                                    java.util.List ids)                    
        throws XMLSignatureException;      
    
    /**
     * Sign part of the XML document referred by the supplied a list
     * of id attributes of nodes
     * @param doc XML dom object
     * @param cert signer's Certificate
     * @param assertionID assertion ID for the SAML Security Token
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @return SAML Security Token  signature
     * @throws XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signWithWSSSAMLTokenProfile(
				   org.w3c.dom.Document doc,
				   java.security.cert.Certificate cert,
				   java.lang.String assertionID,
                                   java.lang.String algorithm,
                                   java.util.List ids)
        throws XMLSignatureException;

    /**
     * Sign part of the XML document referred by the supplied a list
     * of id attributes of nodes
     * @param doc XML dom object
     * @param cert signer's Certificate
     * @param assertionID assertion ID for the SAML Security Token
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @param wsfVersion the web services framework that should be used.
     *     For WSF1.1, the version must be "1.1" and for WSF1.0,
     *     it must be "1.0"
     * @return SAML Security Token  signature
     * @exception XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signWithWSSSAMLTokenProfile(
        org.w3c.dom.Document doc, java.security.cert.Certificate cert,
        String assertionID, String algorithm, java.util.List ids,
        String wsfVersion) throws XMLSignatureException;
    
    /**
     * Sign part of the XML document referred by the supplied a list
     * of id attributes of nodes
     * @param doc XML dom object
     * @param cert signer's Certificate
     * @param assertionID assertion ID for the SAML Security Token
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @return SAML Security Token  signature
     * @throws XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signWithSAMLToken(
        org.w3c.dom.Document doc,
        java.security.cert.Certificate cert,
        java.lang.String assertionID,
        java.lang.String algorithm,
        java.util.List ids)
        throws XMLSignatureException;
    
    /**
     * Sign part of the XML document referred by the supplied a list
     * of id attributes of nodes using SAML Token.
     * @param doc XML dom object
     * @param key the key that will be used to sign the document.
     * @param symmetricKey true if the supplied key is a symmetric key type.     
     * @param signingCert signer's Certificate. If present, this certificate
     *        will be added as part of signature <code>KeyInfo</code>.
     * @param encryptCert the certificate if present will be used to encrypt
     *        the symmetric key and replay it as part of <code>KeyInfo</code>
     * @param assertionID assertion ID for the SAML Security Token
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @return SAML Security Token  signature
     * @throws XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signWithSAMLToken(
        org.w3c.dom.Document doc,
        java.security.Key key,
        boolean symmetricKey,
        java.security.cert.Certificate signingCert,
        java.security.cert.Certificate encryptCert,
        java.lang.String assertionID,
        java.lang.String algorithm,
        java.util.List ids)
        throws XMLSignatureException;

    /**
     * Sign part of the XML document wth binary security token using
     * referred by the supplied a list of id attributes of nodes.
     * @param doc the XML <code>DOM</code> document.
     * @param cert Signer's certificate
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @param refenceType signed element reference type
     * @return X509 Security Token  signature
     * @exception XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signWithBinarySecurityToken(
                 org.w3c.dom.Document doc,
                 java.security.cert.Certificate cert,
                 java.lang.String algorithm,
                 java.util.List ids,
                 java.lang.String refenceType)
        throws XMLSignatureException; 
    
    /**
     * Sign part of the XML document wth kerberos security token using
     * referred by the supplied a list of id attributes of nodes.
     * @param doc the XML <code>DOM</code> document.
     * @param key Security Key.
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @return Kerberos Security Token  signature
     * @exception XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signWithKerberosToken(
            org.w3c.dom.Document doc,
            java.security.Key key,
            java.lang.String algorithm,
            java.util.List ids)
            throws XMLSignatureException;

    /**
     * Sign part of the XML document wth UserName security token using
     * referred by the supplied a list of id attributes of nodes.
     * @param doc the XML <code>DOM</code> document.
     * @param cert Signer's certificate
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @return X509 Security Token  signature
     * @exception XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signWithUserNameToken(
                 org.w3c.dom.Document doc,
                 java.security.cert.Certificate cert,
                 java.lang.String algorithm,
                 java.util.List ids)
        throws XMLSignatureException; 

    /**
     *
     * Sign part of the XML document referred by the supplied a list
     * of id attributes of nodes
     * @param doc XML dom object
     * @param cert Signer's certificate
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @return X509 Security Token  signature
     * @throws XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signWithWSSX509TokenProfile(
        org.w3c.dom.Document doc, java.security.cert.Certificate cert,
        String algorithm, java.util.List ids) throws XMLSignatureException;

    /**
     *
     * Sign part of the XML document referred by the supplied a list
     * of id attributes of nodes
     * @param doc XML dom object
     * @param cert Signer's certificate
     * @param algorithm XML signature algorithm
     * @param ids list of id attribute values of nodes to be signed
     * @param wsfVersion the web services framework that should be used.
     *     For WSF1.1, it should be "1.1" and for WSF1.0,
     *     it should be "1.0"
     * @return X509 Security Token  signature
     * @exception XMLSignatureException if the document could not be signed
     */
    public org.w3c.dom.Element signWithWSSX509TokenProfile(
        org.w3c.dom.Document doc, java.security.cert.Certificate cert,
        String algorithm, java.util.List ids, String wsfVersion)
        throws XMLSignatureException;

    /**                                                                       
     * Verify all the signatures of the XML document                           
     * @param document XML dom document whose signature to be verified              
     * @return true if the XML signature is verified, false otherwise          
     * @throws XMLSignatureException if problem occurs during verification     
     */                                                                        
    public boolean verifyXMLSignature(org.w3c.dom.Document document)           
        throws XMLSignatureException;                                          
     /**                                                                       
     * Verify all the signatures of the XML document                           
     * @param document XML dom document whose signature to be verified              
     * @param certAlias alias for Signer's certificate, this is used to search 
     *        signer's public certificate if it is not presented in
     *        <code>ds:KeyInfo</code>.
     * @return true if the XML signature is verified, false otherwise          
     * @throws XMLSignatureException if problem occurs during verification     
     */                                                                        
    public boolean verifyXMLSignature(org.w3c.dom.Document document,           
                                       java.lang.String certAlias)             
        throws XMLSignatureException;

    /**
     * Verify the signature of the XML document
     * @param document XML dom document whose signature to be verified
     * @param cert Signer's certificate, this is used to search signer's
     *        public certificate if it is not presented in
     *        <code>ds:KeyInfo</code>.
     * @return true if the XML signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyXMLSignature(org.w3c.dom.Document document,
                                      java.security.cert.Certificate cert)
        throws XMLSignatureException;
    
    /**
     * Verify the signature of the XML document 
     * @param element XML dom document whose signature to be verified 
     * @return true if the XML signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyXMLSignature(org.w3c.dom.Element element)
        throws XMLSignatureException;
    
    /**
     * Verify the signature of the XML document 
     * @param element XML dom document whose signature to be verified 
     * @param certAlias <code>certAlias</code> Signer's certificate alias name
     * @return true if the XML signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyXMLSignature(org.w3c.dom.Element element,  
                                      java.lang.String certAlias)
        throws XMLSignatureException;

    /**
     * Verify the signature of the XML document
     * @param element XML dom document whose signature to be verified
     * @param idAttrName Attribute name for the id attribute
     * @param certAlias <code>certAlias</code> Signer's certificate alias name
     * @return true if the XML signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyXMLSignature(org.w3c.dom.Element element,
                                      java.lang.String idAttrName, 
                                      java.lang.String certAlias)
        throws XMLSignatureException;

    /**
     * Verify the signature of the XML string 
     * @param xmlString XML string whose signature to be verified 
     * @return true if the XML signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyXMLSignature(java.lang.String xmlString)
        throws XMLSignatureException;
    
    /**
     * Verify the signature of the XML string 
     * @param xmlString XML string whose signature to be verified 
     * @param certAlias <code>certAlias</code> signer's certificate alias name 
     * @return true if the XML signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyXMLSignature(java.lang.String xmlString, 
                                      java.lang.String certAlias)
        throws XMLSignatureException;

    /**
     * Verify the signature of the XML string
     * @param xmlString XML string whose signature to be verified
     * @param idAttrName Attribute name for the id attribute
     * @param certAlias <code>certAlias</code> alias for Signer's certificate,
     *        this is used to search signer's public certificate if it is not
     *        presented in <code>ds:KeyInfo</code>.
     * @return true if the XML signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyXMLSignature(java.lang.String xmlString,
                                      java.lang.String idAttrName,
                                      java.lang.String certAlias)
        throws XMLSignatureException;

    /**
     * Verify all the signatures of the XML document
     * @param wsfVersion the web services framework that should be used.
     *     For WSF1.1, it should be "1.1" and for WSF1.0, it should be "1.0"
     * @param certAlias alias for Signer's certificate, this is used to search
     *     signer's public certificate if it is not presented in
     *     <code>ds:KeyInfo</code>.
     * @param document XML dom document whose signature to be verified
     * @return true if the XML signature is verified, false otherwise
     * @exception XMLSignatureException if problem occurs during verification
     */
    public boolean verifyXMLSignature(
        String wsfVersion,
        String certAlias,
        org.w3c.dom.Document document
    ) throws XMLSignatureException;
    
    /**
     * Verify all the signatures of the XML document for the
     * web services security.
     * @param document XML dom document whose signature to be verified
     *
     * @param certAlias alias for Signer's certificate, this is used to search
     *        signer's public certificate if it is not presented in
     *        <code>ds:KeyInfo</code>.
     * @return true if the XML signature is verified, false otherwise
     * @throws XMLSignatureException if problem occurs during verification
     */
    public boolean verifyWSSSignature(org.w3c.dom.Document document,
                                       java.lang.String certAlias)
        throws XMLSignatureException;
    
    /**
     * Verify web services message signature using specified key
     * @param document the document to be validated
     * @param key the secret key to be used for validating signature
     * @return true if verification is successful.
     * @throws com.sun.identity.saml.xmlsig.XMLSignatureException
     */
    public boolean verifyWSSSignature(org.w3c.dom.Document document,
                         java.security.Key key)
        throws XMLSignatureException;

    /**
     * Verify web services message signature using specified key
     * @param doc the document to be validated
     * @param key the secret key to be used for validating signature
     * @param certAlias the certificate alias used for validating the signature
     *        if the key is not available.
     * @param encryptAlias the certificate alias that may be used to decrypt
     *        the symmetric key that may be part of <code>KeyInfo</code>
     * @return true if verification is successful.
     * @throws com.sun.identity.saml.xmlsig.XMLSignatureException
     */
    public boolean verifyWSSSignature(org.w3c.dom.Document document,
                         java.security.Key key,
                         String certAlias,
                         String encryptAlias)
        throws XMLSignatureException;
    
    /**
     * Returns the real key provider.
     *
     * @return the real key provider. 
     */
    public KeyProvider getKeyProvider(); 
}
