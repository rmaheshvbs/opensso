/**
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
 * $Id: SAML2SessionPartner.java,v 1.1 2007-08-07 23:39:06 weisun2 Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


package com.sun.identity.saml2.profile;

/**
 * This class encapsulates session partner.
 */
public class SAML2SessionPartner{
    private boolean isRoleIDP = false;
    private String sessionPartner = null;
    
    /**
     * Constructs a new <code>SAML2SessionPartner</code> object.
     * @param sessionPartner session partner's provider ID
     * @param isRoleIDP if the session partner's role is IDP
     */
    public SAML2SessionPartner (String sessionPartner, boolean isRoleIDP) {
        this.sessionPartner = sessionPartner;
        this.isRoleIDP = isRoleIDP;
    }
    
    /**
     * Returns session partner's provider ID.
     * @return session partner's provider ID
     */
    public String getPartner () {
        return sessionPartner;
    }
    
    /**
     * Sets session partner's provider ID.
     * @param sessionPartner session partner's provider ID
     */
    public void setPartner (String sessionPartner) {
        this.sessionPartner = sessionPartner;
    }
    
    /**
     * Returns the role of the session partner.
     * @return <code>true</code> if the role of the session partner is
     *  <code>IDP</code>; <code>false</code> otherwise.
     */
    public boolean isIDPRole () {
        return isRoleIDP;
    }
    
    /**
     * Sets the role of the session partner.
     * @param roleIDP <code>true</code> if the role of the session partner is
     *  <code>IDP</code>; <code>false</code> otherwise.
     */
    public void setIDPRole (boolean roleIDP) {
        this.isRoleIDP = roleIDP;
    }
    
    /**
     * Checks if the session partner's provider ID equals to the one with this
     * object.
     * @param partnerID session partner's provider ID to compare to
     * @return <code>true</code> if the two session partner's provider IDs
     *  are the same; <code>false</code> otherwise.
     */
    public boolean isEquals(String partnerID) {
        return this.sessionPartner.equals(partnerID);
    }

    /**
     * Checks if input partner is equal to this object.
     * @param partner session partner to compare to
     * @return <code>true</code> the two objects are equal; <code>false</code>
     *  otherwise.
     */
    boolean equals(SAML2SessionPartner partner) {
        if (this.sessionPartner.equals(partner.getPartner()) && 
            (this.isRoleIDP == partner.isIDPRole()))
        {
            return true;
        }
        return false;
    }
}
