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
 * $Id: X509CertificateCallback.java,v 1.1 2006-01-28 09:16:51 veiming Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */


package com.sun.identity.authentication.service;

import javax.security.auth.callback.Callback;
import java.security.cert.X509Certificate;

/**
 * Underlying security services instantiate and pass an
 * <code>X509CertificateCallback</code> to the <code>invokeCallback</code>
 * method of a <code>CallbackHandler</code> to retrieve the contents of
 * an X.509 Certificate.
 */
public class X509CertificateCallback implements Callback {
    private String prompt;
    private X509Certificate certificate;

    /**
     * Creates <code>X509CertificateCallback</code> object with a prompt.
     *
     * @param prompt the prompt used to request the X.509 Certificate.
     * @exception IllegalArgumentException if <code>prompt</code> is null
     *            or if <code>prompt</code> has a length of 0 (zero).
     */
    public X509CertificateCallback(String prompt) {
        this.prompt = prompt;
    }

    /**
     * Construct an <code>X509CertificateCallback</code> with a prompt
     * and X.509 Certificate.
     *
     * @param prompt the prompt used to request the X.509 Certificate
     * @param certificate the X.509 Certificate
     * @exception IllegalArgumentException if <code>prompt</code> is null
     *            or if <code>prompt</code> has a length of 0 (zero).
     */
    public X509CertificateCallback(String prompt, X509Certificate certificate) {
        this.prompt = prompt;
        this.certificate = certificate;
    }

    /**
     * Returns the prompt.
     *
     * @return the prompt.
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Sets the retrieved certificate.
     *
     * @param certificate the retrieved certificate contents (which may be null)
     */
    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    /**
     * Returns the retrieved certificate.
     *
     * @return the retrieved certificate contents (which may be null).
     */
    public X509Certificate getCertificate() {
        return certificate;
    }
}
