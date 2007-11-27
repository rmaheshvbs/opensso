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
 * $Id: STSAgent.java,v 1.3 2007-11-27 17:43:48 mrudul_uchil Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.wss.provider.plugins;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.ArrayList;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wss.provider.STSConfig;
import com.sun.identity.wss.security.PasswordCredential;
import com.sun.identity.wss.provider.ProviderException;
import com.sun.identity.wss.provider.ProviderUtils;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdRepoException;


public class STSAgent extends STSConfig {
    
    
    private static Set agentConfigAttribute;
    private static final String AGENT_CONFIG_ATTR = 
                       "AgentType";
    
    private static final String NAME = "Name";
    private static final String TYPE = "Type";
    private static final String ENDPOINT = "STSEndpoint";
    private static final String MEX_ENDPOINT = "STSMexEndpoint";
    private static final String SEC_MECH = "SecurityMech";
    private static final String RESPONSE_SIGN = "isResponseSign";
    private static final String RESPONSE_ENCRYPT = "isResponseEncrypt";
    private static final String REQUEST_SIGN = "isRequestSign";     
    private static final String REQUEST_ENCRYPT = "isRequestEncrypt";
    private static final String REQUEST_HEADER_ENCRYPT = 
                                "isRequestHeaderEncrypt";
    private static final String USER_NAME = "UserName";
    private static final String USER_PASSWORD = "UserPassword";
    private static final String USER_CREDENTIAL = "UserCredential";
     
    private static Debug debug = ProviderUtils.debug;
    
    private AMIdentityRepository idRepo;
    private boolean profilePresent = false;
    private SSOToken token = null;
    
    /** Creates a new instance of STSAgent */
    public STSAgent() {
    }
    
    public STSAgent(AMIdentity amIdentity) throws ProviderException {
        try {
            Set attributeValues = amIdentity.getAttribute(AGENT_CONFIG_ATTR);
            if(attributeValues != null && !attributeValues.isEmpty()) {
               profilePresent = true;
               parseAgentKeyValues(attributeValues);
            }
        } catch (IdRepoException ire) {
            debug.error("STSAgent.constructor: Idrepo exception", ire);
            throw new ProviderException(ire.getMessage());            
        } catch (SSOException se) {
            debug.error("STSAgent.constructor: SSO exception", se);
            throw new ProviderException(se.getMessage());            
        }
    }
    
    public void init(String name, String type, SSOToken token) 
        throws ProviderException {
        
        this.name = name;
        this.type = type;                
        this.token = token;

        // Obtain the provider from Agent profile
        try {
            if (idRepo == null) {
                idRepo = new AMIdentityRepository(token, "/");
            }

            if (agentConfigAttribute == null) {
                agentConfigAttribute = new HashSet();
                agentConfigAttribute.add(AGENT_CONFIG_ATTR);
            }
            IdSearchControl control = new IdSearchControl();
            control.setReturnAttributes(agentConfigAttribute);
            IdSearchResults results = idRepo.searchIdentities(IdType.AGENT,
                name, control);
            Set agents = results.getSearchResults();
            if (!agents.isEmpty()) {
                Map attrs = (Map) results.getResultAttributes();
                AMIdentity provider = (AMIdentity) agents.iterator().next();
                profilePresent = true;
                Map attributes = (Map) attrs.get(provider);
                Set attributeValues = (Set) attributes.get(
                          AGENT_CONFIG_ATTR.toLowerCase());
                if (attributeValues != null) {
                    // Get the values and initialize the properties
                    parseAgentKeyValues(attributeValues);
                }
            }
        } catch (Exception e) {
            debug.error("STSAgent.init: Unable to get idRepo", e);
            throw (new ProviderException("idRepo exception: "+ e.getMessage()));
        }        
         
    }
    
     private void parseAgentKeyValues(Set keyValues) throws ProviderException {
        if(keyValues == null || keyValues.isEmpty()) {
           return;
        }
        Iterator iter = keyValues.iterator(); 
        while(iter.hasNext()) {
           String entry = (String)iter.next();
           int index = entry.indexOf("=");
           if(index == -1) {
              continue;
           }
           setConfig(entry.substring(0, index),
                      entry.substring(index+1, entry.length()));
        }
    }

    private void setConfig(String attr, String value) {
        
        if(attr.equals(NAME)) {
           this.name = value;
        } else if(attr.equals(TYPE)) {
           this.type = value; 
        } else if(attr.equals(ENDPOINT)) {
            this.endpoint = value;
        } else if(attr.equals(MEX_ENDPOINT)) {
            this.mexEndpoint = value;
        } else if(attr.equals(SEC_MECH)) {
           if (secMech == null) {
               secMech = new ArrayList();
           }
           StringTokenizer st = new StringTokenizer(value, ","); 
           while(st.hasMoreTokens()) {
               secMech.add(st.nextToken());
           }
        } else if(attr.equals(RESPONSE_SIGN)) {
           this.isResponseSigned = Boolean.valueOf(value).booleanValue();
        } else if(attr.equals(RESPONSE_ENCRYPT)) {
           this.isResponseEncrypted = Boolean.valueOf(value).booleanValue();
        } else if(attr.equals(REQUEST_SIGN)) {
           this.isRequestSigned = Boolean.valueOf(value).booleanValue();
        } else if(attr.equals(REQUEST_ENCRYPT)) {
           this.isRequestEncrypted = Boolean.valueOf(value).booleanValue();
        } else if(attr.equals(REQUEST_HEADER_ENCRYPT)) {
           this.isRequestHeaderEncrypted = Boolean.valueOf(value).booleanValue();
        }  else if(attr.equals(USER_CREDENTIAL)) {
           int index = value.indexOf("|");
           if(index == -1) {
              return;
           }
           String usertmp = value.substring(0, index);
           String passwordtmp = value.substring(index+1, value.length()); 

           String user = null;
           String password = null;
           StringTokenizer st = new StringTokenizer(usertmp, ":"); 
           if(USER_NAME.equals(st.nextToken())) {
              if(st.hasMoreTokens()) {
                 user = st.nextToken();
              }               
           }
           StringTokenizer st1 = new StringTokenizer(passwordtmp, ":"); 
           if(USER_PASSWORD.equals(st1.nextToken())) {
              if(st1.hasMoreTokens()) {
                 password = st1.nextToken();
              }              
           }
        }
    }
        
    public void delete() throws ProviderException {
        if (!profilePresent) {
            return;
        }
        // Delete the agent profile
        try {
            if (idRepo == null) {
                idRepo = new AMIdentityRepository(token, "/");
            }
            // Construct AMIdentity object to delete
            AMIdentity id = new AMIdentity(token, name,
                            IdType.AGENT, "/", null);
            Set identities = new HashSet();
            identities.add(id);
            idRepo.deleteIdentities(identities);
        } catch (Exception e) {
            debug.error("STSAgent.delete: Unable to get idRepo", e);
            throw (new ProviderException("idRepo exception: "+ e.getMessage()));
        }
        
    }
    
    public void store() throws ProviderException {
        
        Set set = new HashSet();
        
        if(name != null) {
           set.add(getKeyValue(NAME, name)); 
        }
        
        if(type != null) { 
           set.add(getKeyValue(TYPE, type));
        }

        if(endpoint != null) {
           set.add(getKeyValue(ENDPOINT, endpoint));
        }        

        if(mexEndpoint != null) {
           set.add(getKeyValue(MEX_ENDPOINT, mexEndpoint));
        }
        
        if(secMech != null) {
           Iterator iter = secMech.iterator();
           StringBuffer sb =  new StringBuffer(100);
           while(iter.hasNext()) {
              sb.append((String)iter.next()).append(",");
           }
           sb = sb.deleteCharAt(sb.length() - 1);
           set.add(getKeyValue(SEC_MECH, sb.toString()));
        }
        
        set.add(getKeyValue(RESPONSE_SIGN, 
                            Boolean.toString(isResponseSigned)));
        set.add(getKeyValue(RESPONSE_ENCRYPT, 
                            Boolean.toString(isResponseEncrypted)));
        set.add(getKeyValue(REQUEST_SIGN, 
                            Boolean.toString(isRequestSigned)));
        set.add(getKeyValue(REQUEST_ENCRYPT, 
                            Boolean.toString(isRequestEncrypted)));
        set.add(getKeyValue(REQUEST_HEADER_ENCRYPT,
                            Boolean.toString(isRequestHeaderEncrypted)));
        
        if(usercredentials != null) {
           Iterator iter = usercredentials.iterator();
           while(iter.hasNext()) {
              PasswordCredential cred = (PasswordCredential)iter.next();
              String user = cred.getUserName();
              String password = cred.getPassword();
              if(user == null || password == null) {
                 continue;
              }
              StringBuffer sb = new StringBuffer(100);
              sb.append(USER_NAME).append(":").append(user)
                .append("|").append(USER_PASSWORD).append(":").append(password);
              set.add(getKeyValue(USER_CREDENTIAL, sb.toString()));
           }
        }
        
        // Save the entry in Agent's profile
        try {
            Map attributes = new HashMap();
            attributes.put(AGENT_CONFIG_ATTR, set);
            if (profilePresent) {
                // Construct AMIdentity object and save
                AMIdentity id = new AMIdentity(token,
                    name, IdType.AGENT, "/", null);                
                id.setAttributes(attributes);
                id.store();
            } else {
                // Create a new Agent profile
                if (idRepo == null) {
                    idRepo = new AMIdentityRepository(token, "/");
                }
                idRepo.createIdentity(IdType.AGENT, name, attributes);
            }
        } catch (Exception e) {
            debug.error("STSAgent.store: Unable to get idRepo", e);
            throw (new ProviderException("idRepo exception: "+ e.getMessage()));
        }
        
    }
    
    private String getKeyValue(String key, String value) {
        return key + "=" + value;
    }

}
