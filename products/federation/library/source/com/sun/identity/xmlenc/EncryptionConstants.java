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
 * $Id: EncryptionConstants.java,v 1.1 2006-10-30 23:16:57 qcheng Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */


package com.sun.identity.xmlenc;

public class EncryptionConstants {

    public static final String ENC_XML_NS = "http://www.w3.org/2001/04/xmlenc#";
    public static final String RSA = "RSA";
    public static final String AES = "AES";
    public static final String TRIPLEDES = "DESede";
    public static final String XML_ENCRYPTION_PROVIDER_KEY = 
           "com.sun.identity.xmlenc.EncryptionProviderImpl";
}
