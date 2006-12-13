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
 * $Id: EventListener.java,v 1.5 2006-12-13 00:27:13 rarcot Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.iplanet.am.sdk.remote;

import com.iplanet.am.sdk.AMEvent;
import com.iplanet.am.sdk.AMEventManagerException;
import com.iplanet.am.sdk.AMObjectListener;
import com.iplanet.am.sdk.common.ICachedDirectoryServices;
import com.iplanet.am.sdk.common.IDirectoryServices;
import com.iplanet.am.sdk.common.MiscUtils;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.comm.client.NotificationHandler;
import com.iplanet.services.comm.client.PLLClient;
import com.iplanet.services.comm.share.Notification;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.IdRepoListener;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import com.sun.identity.sm.CreateServiceConfig;
import com.sun.identity.sm.SMSSchema;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import netscape.ldap.util.DN;

/**
 * The <code>EventListener</code> handles the events generated from the
 * server. If notification URL is provided, the class registers the URL with the
 * Server and waits for notifications. Else it polls the server at configurable
 * time periods to get updates from the server.
 */
class EventListener {

    private static Debug debug = RemoteServicesImpl.getDebug();
    
    private static SOAPClient client;
    
    private static boolean initialized = false;

    private static Set listeners = new HashSet();

    private static String notificationID;

    private static String idRepoNotificationID;
    
    private static EventListener instance = null;
    
    public synchronized static EventListener getInstance() {
        if (instance == null) {
            instance = new EventListener();
        }
        return instance;
    }

    /**
     * Constructor for <class>EventListener</class>. Should be instantiated
     * once by <code>RemoteServicesImpl</code>
     * 
     * @param set
     *            of listeners interested in obtaining change events
     */
    private EventListener() {

        // Construct the SOAP Client
        client = new SOAPClient(RemoteServicesImpl.SDK_SERVICE);

        // Check if notification URL is provided
        try {
            URL url = WebtopNaming.getNotificationURL();

            // Register for notification with AM Server
            notificationID = (String) client.send(
                    "registerNotificationURL", url.toString(), null);

            // Register with PLLClient for notificaiton
            PLLClient.addNotificationHandler(
                    RemoteServicesImpl.SDK_SERVICE,
                    new EventNotificationHandler());

            // Register for IdRepo Service
            idRepoNotificationID = (String) client.send(
                    "registerNotificationURL_idrepo", url.toString(), null);

            // Register with PLLClient for notificaiton
            PLLClient.addNotificationHandler(
                    RemoteServicesImpl.IDREPO_SERVICE,
                    new IdRepoEventNotificationHandler());

            if (debug.messageEnabled()) {
                debug.message("EventService: Using notification "
                        + "mechanism for cache updates: "
                        + url.toString());
            }
        } catch (Exception e) {
            // Use polling mechanism to update caches
            if (debug.warningEnabled()) {
                debug.warning("EventService: Registering for "
                        + "notification via URL failed: "
                        + e.getMessage()
                        + "\nUsing polling mechanism for updates");
            }
            // Start the daemon thread to check for changes
            NotificationThread nt = new NotificationThread();
            nt.start();
        }
    }

    public void addListener(SSOToken token, AMObjectListener listener)
            throws AMEventManagerException {
        // Validate the token
        try {
            SSOTokenManager.getInstance().validateToken(token);
        } catch (SSOException ssoe) {
            throw (new AMEventManagerException(ssoe.getMessage(), "902"));
        }

        // Add to listeners
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Sends notifications to listeners added via <code>addListener</code>.
     * The parameter <code>nItem</code> is an XML document having a single
     * notification event, using the following DTD.
     * <p>
     * 
     * <pre>
     *       &lt;!-- EventNotification element specifes the change notification
     *       which contains AttributeValuePairs. The attributes defined
     *       are &quot;method&quot;, &quot;entityName&quot;, &quot;
     *       eventType&quot; and &quot;attrNames&quot;. --&gt;
     *       &lt;!ELEMENT EventNotification ( AttributeValuePairs )* &gt;
     *  
     *       &lt;!-- AttributeValuePair element contains attribute name and 
     *       values --&gt;
     *       &lt;!ELEMENT AttributeValuPair ( Attribute, Value*) &gt;
     *  
     *       &lt;!-- Attribute contains the attribute names, and the allowed 
     *       names are &quot;method&quot;, &quot;entityName&quot;, 
     *       &quot;eventType&quot; and &quot;attrNames&quot; --&gt;
     *       &lt;!ELEMENT Attribute EMPTY&gt;
     *       &lt;!ATTRLIST Attribute
     *       name ( method | entityName | eventType | attrNames ) 
     *       &quot;method&quot;
     *       &gt;
     *  
     *       &lt;!-- Value element specifies the values for the attributes 
     *       --&gt; &lt;!ELEMENT Value (#PCDATA) &gt;
     * </pre>
     * 
     * @param nItem
     *            notification event as a xml document
     * 
     */
    static void sendNotification(String nItem) {
        if (debug.messageEnabled()) {
            debug.message("EventListener::sendNotification: "
                    + "Received notification: " + nItem);
        }

        // Construct the XML document
        StringBuffer sb = new StringBuffer(nItem.length() + 50);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(nItem);
        try {
            Map attrs = CreateServiceConfig.getAttributeValuePairs(SMSSchema
                    .getXMLDocument(sb.toString(), false).getDocumentElement());
            if (debug.messageEnabled()) {
                debug.message("EventListener::sendNotification "
                        + "Decoded Event: " + attrs);
            }
            // Get method name
            String method = getAttributeValue(attrs, METHOD);
            if (method == null) {
                handleError("invalid method name: " + attrs.get(METHOD));
            }
            // Get entity name
            String entityName = getAttributeValue(attrs, ENTITY_NAME);
            if (entityName == null) {
                handleError("invalid entity Name: " + attrs.get(ENTITY_NAME));
            }
            String entryDN = MiscUtils.formatToRFC(entityName);
            IDirectoryServices dsServices = RemoteServicesFactory.getInstance();
            // Switch based on method
            if (method.equalsIgnoreCase(OBJECT_CHANGED)) {
                int eventType = getEventType((Set) attrs.get(EVENT_TYPE));
                // Call objectChanged method on the listeners
                // Update the Remote Cache

                if (RemoteServicesFactory.isCachingEnabled()) {
                    ((ICachedDirectoryServices) dsServices).dirtyCache(entryDN,
                            eventType, false, false, Collections.EMPTY_SET);
                }
                synchronized (listeners) {
                    for (Iterator items = listeners.iterator(); 
                        items.hasNext();) 
                    {
                        AMObjectListener listener = (AMObjectListener) items
                                .next();
                        listener.objectChanged(entityName, eventType, null);
                    }
                }
            } else if (method.equalsIgnoreCase(OBJECTS_CHANGED)) {
                int eventType = getEventType((Set) attrs.get(EVENT_TYPE));
                Set attributes = (Set) attrs.get(attrs.get(ATTR_NAMES));
                if (RemoteServicesFactory.isCachingEnabled()) {
                    ((ICachedDirectoryServices) dsServices).dirtyCache(entryDN,
                            eventType, true, false, attributes);
                }
                // Call objectsChanged method on the listeners
                synchronized (listeners) {
                    for (Iterator items = listeners.iterator(); 
                        items.hasNext();) 
                    {
                        AMObjectListener listener = (AMObjectListener) items
                                .next();
                        listener.objectsChanged(entityName, eventType,
                                attributes, null);
                    }
                }
            } else if (method.equalsIgnoreCase(PERMISSIONS_CHANGED)) {
                if (RemoteServicesFactory.isCachingEnabled()) {
                    ((ICachedDirectoryServices) dsServices).dirtyCache(entryDN,
                            AMEvent.OBJECT_CHANGED, false, true,
                            Collections.EMPTY_SET);
                }
                // Call permissionChanged method on the listeners
                synchronized (listeners) {
                    for (Iterator items = listeners.iterator(); 
                        items.hasNext();) 
                    {
                        AMObjectListener listener = (AMObjectListener) items
                                .next();
                        listener.permissionsChanged(entityName, null);
                    }
                }
            } else if (method.equalsIgnoreCase(ALL_OBJECTS_CHANGED)) {
                if (RemoteServicesFactory.isCachingEnabled()) {
                    ((ICachedDirectoryServices) dsServices).clearCache();
                }
                // Call allObjectsChanged method on listeners
                synchronized (listeners) {
                    for (Iterator items = listeners.iterator(); 
                        items.hasNext();) 
                    {
                        AMObjectListener listener = (AMObjectListener) items
                                .next();
                        listener.allObjectsChanged();
                    }
                }
            } else {
                // Invalid method name
                handleError("invalid method name: " + method);
            }
            if (debug.messageEnabled()) {
                debug.message("EventListener::sendNotification: Sent "
                        + "notification: " + nItem);
            }
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning("EventListener::sendNotification: Unable to send"
                        + " notification: " + nItem, e);
            }
        }
    }

    static void sendIdRepoNotification(String nItem) {
        if (debug.messageEnabled()) {
            debug.message("EventListener::sendIdRepoNotification: "
                    + "Received notification: " + nItem);
        }

        // Construct the XML document
        StringBuffer sb = new StringBuffer(nItem.length() + 50);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(nItem);
        try {
            Map attrs = CreateServiceConfig.getAttributeValuePairs(SMSSchema
                    .getXMLDocument(sb.toString(), false).getDocumentElement());
            if (attrs == null || attrs.isEmpty()) {
                if (debug.warningEnabled()) {
                    debug.warning("EventListener::sendIdRepoNotification: "
                            + "Invalid event: " + attrs);
                }
                return;
            } else if (debug.messageEnabled()) {
                debug.message("EventListener::sendIdRepoNotification "
                        + "Decoded Event: " + attrs);
            }

            // Parse the entity name to get the realm name, and method
            String entityName = getAttributeValue(attrs, ENTITY_NAME);
            String method = getAttributeValue(attrs, METHOD);
            if (entityName == null || entityName.length() == 0
                    || method == null || method.length() == 0) {
                if (debug.warningEnabled()) {
                    debug.warning("EventListener::sendIdRepoNotification: "
                            + "Invalid universalID or method: " + entityName
                            + " method");
                }
                return;
            }
            String pureId = entityName;
            String amsdkDN = null;
            int dnIndex = entityName.indexOf(",amsdkdn=");
            if (dnIndex > 0) {
                pureId = entityName.substring(0, dnIndex);
                amsdkDN = entityName.substring(dnIndex + 9);
            }

            DN dnObject = new DN(pureId);
            DN realmDN = dnObject.getParent().getParent();

            // Check if AMSDK is present
            String isAmsdk = (amsdkDN == null) ? "false" : "true";

            // Construct IdRepoListener
            Map configMap = new HashMap();
            configMap.put("realm", realmDN.toRFCString());
            configMap.put("amsdk", isAmsdk);
            IdRepoListener cb = new IdRepoListener();
            cb.setConfigMap(configMap);

            if (method.equalsIgnoreCase(OBJECT_CHANGED)) {
                int eventType = getEventType((Set) attrs.get(EVENT_TYPE));
                cb.objectChanged(entityName, eventType, null);
            } else if (method.equalsIgnoreCase(OBJECTS_CHANGED)) {
                int eventType = getEventType((Set) attrs.get(EVENT_TYPE));
                Set attributes = (Set) attrs.get(attrs.get(ATTR_NAMES));
                // cb.objectsChanged(entityName, eventType, attributes, null);
            } else if (method.equalsIgnoreCase(PERMISSIONS_CHANGED)) {
                // cb.permissionsChanged(entityName, null);
            } else if (method.equalsIgnoreCase(ALL_OBJECTS_CHANGED)) {
                cb.allObjectsChanged();
            } else {
                // Invalid method name
                handleError("invalid method name: " + method);
            }
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning("EventListener::sendIdRepoNotification: "
                        + "Unable to send notification: " + nItem, e);
            }
        }
    }

    static void handleError(String msg) throws Exception {
        // Debug the message and throw an exception
        debug.error("EventListener::sendNotification: " + msg);
        throw (new Exception(msg));
    }

    static String getAttributeValue(Map attrs, String attrName) {
        String answer = null;
        Set set = (Set) attrs.get(attrName);
        if (set != null && set.size() == 1) {
            answer = (String) set.iterator().next();
        }
        return (answer);
    }

    static int getEventType(Set eventSet) throws Exception {
        if (eventSet == null || eventSet.size() != 1) {
            // throw an exception
            throw (new Exception("EventListener::sendNotification: "
                    + "invalid event type: " + eventSet));
        }
        String eventString = (String) eventSet.iterator().next();
        return (Integer.parseInt(eventString));
    }

    static class NotificationThread extends Thread {
        static final String CACHE_TIME_PROPERTY = 
            "com.iplanet.am.sdk.remote.pollingTime";

        static int pollingTime = 1;

        static int sleepTime = 1000 * 60;

        NotificationThread() {
            // Set this as a dameon thread
            setDaemon(true);
            // Read polling time (in minutes)
            String pTime = SystemProperties.get(CACHE_TIME_PROPERTY);
            if (pTime != null) {
                try {
                    pollingTime = Integer.parseInt(pTime);
                    if (pollingTime > 0) {
                        sleepTime = pollingTime * 1000 * 60;
                    }
                } catch (NumberFormatException nfe) {
                    debug.error("EventListener::NotificationThread:: "
                            + "Invalid Polling Time: " + pTime, nfe);
                }
            }
        }

        public void run() {
            if (debug.messageEnabled()) {
                debug.message("EventListener:NotificationThread "
                        + "Polling Time: " + sleepTime);
            }
            boolean gotoSleep = false;
            while (true) {
                try {
                    if (gotoSleep) {
                        sleep(sleepTime);
                    }
                    Object obj[] = { new Integer(pollingTime) };
                    Set mods = (Set) client.send("objectsChanged", obj, null);
                    if (debug.messageEnabled()) {
                        debug.message("EventListener:NotificationThread "
                                + "retrived changes: " + mods);
                    }
                    Iterator items = mods.iterator();
                    while (items.hasNext()) {
                        sendNotification((String) items.next());
                    }

                    // Check for IdRepo changes
                    mods = (Set) client
                            .send("objectsChanged_idrepo", obj, null);
                    if (debug.messageEnabled()) {
                        debug.message("EventListener:NotificationThread "
                                + "retrived idrepo changes: " + mods);
                    }
                    items = mods.iterator();
                    while (items.hasNext()) {
                        sendIdRepoNotification((String) items.next());
                    }

                    gotoSleep = true;
                } catch (NumberFormatException nfe) {
                    // Should not happend
                    debug.warning("EventListener::NotificationThread:run "
                            + "Number Format Exception for polling Time: "
                            + pollingTime, nfe);
                } catch (InterruptedException ie) {
                    gotoSleep = false;
                    debug.warning("EventListener::NotificationThread:run "
                            + "Interrupted Exception", ie);
                } catch (Exception re) {
                    gotoSleep = true;
                    debug.warning("EventListener::NotificationThread:run "
                            + "Exception", re);
                }
            }
        }
    }

    static class EventNotificationHandler implements NotificationHandler {

        EventNotificationHandler() {
            // Empty constructor
        }

        public void process(Vector notifications) {
            for (int i = 0; i < notifications.size(); i++) {
                Notification notification = (Notification) notifications
                        .elementAt(i);
                String content = notification.getContent();
                if (debug.messageEnabled()) {
                    debug.message("EventListener:" 
                            + "IdRepoEventNotificationHandler: received " 
                            + "notification: " + content);

                }
                // Send notification
                sendNotification(content);
            }
        }
    }

    static class IdRepoEventNotificationHandler implements NotificationHandler {

        IdRepoEventNotificationHandler() {
            // Empty constructor
        }

        public void process(Vector notifications) {
            for (int i = 0; i < notifications.size(); i++) {
                Notification notification = (Notification) notifications
                        .elementAt(i);
                String content = notification.getContent();
                if (debug.messageEnabled()) {
                    debug.message("EventListener:" 
                            + "IdRepoEventNotificationHandler: "
                            + " received notification: " + content);
                }
                // Send notification
                sendIdRepoNotification(content);
            }
        }
    }

    // Static varaibles
    static final String METHOD = "method";

    static final String ENTITY_NAME = "entityName";

    static final String EVENT_TYPE = "eventType";

    static final String ATTR_NAMES = "attrNames";

    static final String OBJECT_CHANGED = "objectChanged";

    static final String OBJECTS_CHANGED = "objectsChanged";

    static final String PERMISSIONS_CHANGED = "permissionsChanged";

    static final String ALL_OBJECTS_CHANGED = "allObjectsChanged";
}
