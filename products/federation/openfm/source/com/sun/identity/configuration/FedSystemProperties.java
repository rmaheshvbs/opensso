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
 * $Id: FedSystemProperties.java,v 1.1 2006-10-30 23:17:58 qcheng Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.configuration;

import java.net.URL;
import java.util.Collection;

import com.iplanet.services.naming.WebtopNaming;

/**
 * This is the adapter class for Federation Manager to the shared library.
 * Mainly to provide system configuration information.
  */
public class FedSystemProperties extends FedLibSystemProperties {
    
    /**
     * Creates a new instance of <code>FedSystemProperties</code>
     */
    public FedSystemProperties() {
    }
    
    /**
     * Returns system properties.
     *
     * @param key Key to the properties.
     */
    public String get(String key) {
        String value = super.get(key);
        if ((value != null) && (value.trim().length() > 0)) {
            return value;
        }
        return com.iplanet.am.util.SystemProperties.get(key);
        /*
        return ((value != null) && (value.trim().length() > 0)) ?
            value : com.iplanet.am.util.SystemProperties.get(key);
        */
    }

    /**
     * Returns server list.
     *
     * @return Server List.
     * @throws Exception if server list cannot be returned.
     */
    public Collection getServerList()
        throws Exception {
        return WebtopNaming.getPlatformServerList();
    }
    
    /**
     * Returns the URL of the specified service on the specified host.
     *
     * @param serviceName The name of the service.
     * @param protocol The service protocol.
     * @param hostname The service host name.
     * @param port The service listening port.
     * @return The URL of the specified service on the specified host.
     * @throws Exception if the URL could not be found.
     */
    public URL getServiceURL(
        String serviceName, 
        String protocol,
        String hostname,
        int port
    ) throws Exception {
        return WebtopNaming.getServiceURL(
            serviceName, protocol, hostname, "" + port);
    }
}
