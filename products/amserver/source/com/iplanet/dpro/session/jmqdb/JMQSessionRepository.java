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
 * $Id: JMQSessionRepository.java,v 1.2 2006-08-25 21:19:40 veiming Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.iplanet.dpro.session.jmqdb;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.AMSessionRepository;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.services.naming.WebtopNaming;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import java.util.HashMap;
import java.util.Map;
import javax.jms.IllegalStateException;

/**
 * This class implements JMQ-based session repository which
 * is used in session failover mode to store/recover serialized
 * state of InternalSession object
 */
public class JMQSessionRepository implements AMSessionRepository, Runnable {

    /* Operations */
    static public final String READ = "READ";

    static public final String WRITE = "WRITE";

    static public final String DELETE = "DELETE";

    static public final String DELETEBYDATE = "DELETEBYDATE";

    /* JMQ Queue/Topic names */
    static public final String DBREQUEST = "AM_DBREQUEST";

    static public final String DBRESPONSE = "AM_DRESPONSE";

    /* JMQ Properties */
    static public final String ID = "ID";

    /* JMQ Map keys */
    static public final String SESSIONID = "SessionId";

    static public final String EXPIRYDATE = "ExpiryDate";

    static public final String DATA = "Data";

    static public final String MASTER_SID = "MASTER_SID";

    static public final String UUID = "UUID";

    static public final String COUNT = "COUNT";

    static public final String SESSIONSTATE = "SESSIONSTATE";

    static public final String VERSION = "Version";

    // Private data members
    String serverId;

    /* Config data */
    private static boolean isDatabaseUp = true;

    /**
     * grace period before expired session records are removed from the
     * repository
     */
    private static long gracePeriod = 5 * 60; /* 5 mins in secs */

    private static final String CLEANUP_GRACE_PERIOD = 
        "com.sun.identity.session.repository.cleanupGracePeriod";

    private static final String BRIEF_DB_ERROR_MSG = 
        "Session failover service is not functional due to DB unavailability.";

    private static final String DB_ERROR_MSG = 
        "Session database is not available at this moment."
            + "Please check with the system administrator " +
                    "for appropriate actions";

    private static final String LOG_MSG_DB_BACK_ONLINE = 
        "SESSION_DATABASE_BACK_ONLINE";

    private static final String LOG_MSG_DB_UNAVAILABLE = 
        "SESSION_DATABASE_UNAVAILABLE";

    private static boolean lastLoggedDBStatusIsUp = true;

    /**
     * Time period between two successive runs of repository cleanup thread
     * which checks and removes expired records
     */

    private static long cleanUpPeriod = 5 * 60 * 1000; // 5 min in milliseconds

    private static long cleanUpValue = 0;

    public static final String CLEANUP_RUN_PERIOD = 
        "com.sun.identity.session.repository.cleanupRunPeriod";

    /**
     * Time period between two successive runs of DBHealthChecker thread which
     * checks for Database availability.
     */
    private static long healthCheckPeriod = 1 * 60 * 1000;

    public static final String HEALTH_CHECK_RUN_PERIOD = 
        "com.sun.identity.session.repository.healthCheckRunPeriod";

    /**
     * This period is actual one that is used by the thread. The value is set to
     * the smallest value of cleanUPPeriod and healthCheckPeriod.
     */
    private static long runPeriod = 1 * 60 * 1000; // 1 min in milliseconds

    /**
     * Repository clean up background thread
     */
    private Thread checker;

    static Debug debug = SessionService.sessionDebug;

    static {
        try {
            gracePeriod = Integer.parseInt(SystemProperties.get(
                    CLEANUP_GRACE_PERIOD, String.valueOf(gracePeriod)));
        } catch (Exception e) {
            debug.error("Invalid value for " + CLEANUP_GRACE_PERIOD
                    + ", using default");
        }

        try {
            cleanUpPeriod = Integer.parseInt(SystemProperties.get(
                    CLEANUP_RUN_PERIOD, String.valueOf(cleanUpPeriod)));
        } catch (Exception e) {
            debug.error("Invalid value for " + CLEANUP_RUN_PERIOD
                    + ", using default");
        }

        try {
            healthCheckPeriod = Integer
                    .parseInt(SystemProperties.get(HEALTH_CHECK_RUN_PERIOD,
                            String.valueOf(healthCheckPeriod)));
        } catch (Exception e) {
            debug.error("Invalid value for " + HEALTH_CHECK_RUN_PERIOD
                    + ", using default");
        }

        runPeriod = (cleanUpPeriod <= healthCheckPeriod) ? cleanUpPeriod
                : healthCheckPeriod;
        cleanUpValue = cleanUpPeriod;
    }

    // Message queues
    // One REQUEST queue/topic is suffcient
    // Multiple RESPONSE queues/topics may be necessary
    PersistSession pSession = null;

   /**
    *
    * Constructs new JMQSessionRepository
    * @exception Exception when cannot create a new Session repository
    *
    */
   public JMQSessionRepository() throws Exception {

        String thisSessionServerProtocol = SystemProperties
                .get(Constants.AM_SERVER_PROTOCOL);
        String thisSessionServer = SystemProperties
                .get(Constants.AM_SERVER_HOST);
        String thisSessionServerPortAsString = SystemProperties
                .get(Constants.AM_SERVER_PORT);

        if (thisSessionServerProtocol == null
                || thisSessionServerPortAsString == null
                || thisSessionServer == null) {
            throw new SessionException(SessionBundle.rbName,
                    "propertyMustBeSet", null);
        }

        serverId = WebtopNaming.getServerID(thisSessionServerProtocol,
                thisSessionServer, thisSessionServerPortAsString);

        initPersistSession();

        checker = new Thread(this);
        checker.setName("JDBCSessionRepositoryChecker");
        checker.setDaemon(true);
        checker.start();
    }

    /**
     *
     * Initialize new persistant session
     */
    private void initPersistSession() {
        try {
            pSession = new PersistSession(serverId,
                    SessionService.getJdbcURL(), SessionService
                            .getSessionStoreUserName(), SessionService
                            .getSessionStorePassword(), SessionService
                            .getConnectionMaxWaitTime(), SessionService
                            .getMaxWaitTimeForConstraint());
            isDatabaseUp = true;
        } catch (Exception e) {
            isDatabaseUp = false;
            debug.error(BRIEF_DB_ERROR_MSG);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
        }

    }

   /**
    * Retrives new </code>InternalSession</code> for the session
    * @param sid Session Id
    * @return InternalSession 
    * @throws Exception when cannot create a retrieve internal session
    */
   public InternalSession retrieve(SessionID sid) throws Exception {
        if (!isDatabaseUp) {
            return null;
        }
        try {
            String key = SessionUtils.getEncryptedStorageKey(sid);
            Object message = pSession.createMessage();
            pSession.setString(message, SESSIONID, key);

            message = pSession.read(message);

            byte[] blob = pSession.getBytes(message, DATA);

            InternalSession is = (InternalSession) SessionUtils.decode(blob);

            /*
             * ret.put(SESSIONID, message.getString(SESSIONID)); ret.put(DATA,
             * message.getString(DATA));
             */
            return is;
        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            logDBStatus();
            debug.error(BRIEF_DB_ERROR_MSG, e);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
            return null;
        } catch (Exception e) {
            debug.message("JMQSessionRepository.retrieve(): failed retrieving "
                    + "session", e);
            return null;
        }
    }

   /**
    * Deletes the given <code>Session</code>from the repository
    * @param sid SessionId
    * @throws Exception when cannot delete a session
    */
   public void delete(SessionID sid) throws Exception {
        if (!isDatabaseUp) {
            return;
        }
        try {
            String key = SessionUtils.getEncryptedStorageKey(sid);
            Object message = pSession.createMessage();
            pSession.setString(message, SESSIONID, key);
            pSession.delete(message);
        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            logDBStatus();
            debug.error(BRIEF_DB_ERROR_MSG, e);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
        } catch (Exception e) {
            debug.error("JMQSessionRepository.delete(): failed deleting "
                    + "session", e);
        }
    }

    /**
     * Deletes all expired Sessions from the repository
     * @exception When Unable to delete the expired sessions
     */
    public void deleteExpired() throws Exception {
        if (!isDatabaseUp) {
            return;
        }
        try {
            long date = System.currentTimeMillis() / 1000;
            Object message = pSession.createMessage();
            pSession.setLong(message, EXPIRYDATE, date);
            pSession.delete_bydate(message);
        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            logDBStatus();
            debug.error(BRIEF_DB_ERROR_MSG, e);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
        } catch (Exception e) {
            debug.error("JMQSessionRepository.deleteExpired(): failed "
                    + "deleting Expired Sessions", e);
        }
    }

   /**
    * Saves<code> InternalSession</code> into the <code>SessionRepository</code>
    * @param is InternalSession
    * @exception when cannot save the internal session
    */
   public void save(InternalSession is) throws Exception {
        if (!isDatabaseUp) {
            return;
        }

        try {
            SessionID sid = is.getID();
            String key = SessionUtils.getEncryptedStorageKey(sid);
            byte[] blob = SessionUtils.encode(is);
            long expirationTime = is.getExpirationTime() + gracePeriod;
            String uuid = is.getUUID();

            Object message = pSession.createMessage();
            pSession.setString(message, SESSIONID, key);
            pSession.setLong(message, EXPIRYDATE, expirationTime);
            pSession.setString(message, UUID, uuid);
            pSession.setString(message, MASTER_SID, sid.toString());
            pSession.setInt(message, SESSIONSTATE, is.getState());
            pSession.setBytes(message, DATA, blob);

            pSession.write(message);
        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            logDBStatus();
            debug.error(BRIEF_DB_ERROR_MSG, e);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
        } catch (Exception e) {
            debug.error("JMQSessionRepository.save(): failed "
                    + "to save Session", e);
        }
    }

    /**
     * Returns the expiration information of all sessions belonging to a user.
     * The returned value will be a Map (sid->expiration_time).
     * 
     * @param uuid
     *            User's universal unique ID.
     * @return Map of all Session for the user
     * @throws Exception
     *             if there is any problem with accessing the session
     *             repository.
     */
    public Map getSessionsByUUID(String uuid) throws Exception {

        if (!isDatabaseUp) {
            throw new SessionException("Session repository is not "
                    + "available.");
        }
        Map sessions = new HashMap();
        int retCount = 0;

        try {
            Object message = pSession.createMessage();
            pSession.setString(message, UUID, uuid);
            message = pSession.getSessionsByUUID(message);

            if (message != null) {
                retCount = pSession.getInt(message, COUNT);
                for (int i = 0; i < retCount; i++) {
                    String sid = pSession.getString(message, COUNT);
                    Long expirationTime = new Long(pSession.getLong(message,
                            COUNT));
                    sessions.put(sid, expirationTime);
                }
            }
        } catch (IllegalStateException e) {
            isDatabaseUp = false;
            logDBStatus();
            debug.error(BRIEF_DB_ERROR_MSG, e);
            if (debug.messageEnabled()) {
                debug.message(DB_ERROR_MSG, e);
            }
            throw new SessionException(e);
        } catch (Exception e) {
            throw new SessionException(e);
        }
        return sessions;
    }

    /**
     * This method is invoked to log a message in the following two cases:
     * 
     * (1) the DB is detected down by either the user requests
     * (retrieve/save/delete/getSessionCount) or the background checker thread:
     * Log message: SESSION_DATABASE_UNAVAILABLE (2) the DB is detected
     * available again by the background health checker thread => Log message:
     * SESSION_DATABASE_BACK_ONLINE
     * 
     * The flag "lastLoggedDBStatusIsUp" is used to avoid logging the same DB
     * status again and again if the status actually doesn't change over time.
     * 
     * Please also note that if the DB is already down in the very beginning
     * when starting the AM instance, there will be no message being logged
     * since at this time the session service is not fully initialized yet
     * therefore no sso token can be generated and used for the logging purpose.
     * Nevertheless, the appropriate logging will be done later when the
     * background thread kicks in.
     * 
     */
    private void logDBStatus() {

        SessionService ss = SessionService.getSessionService();

        if (!isDatabaseUp && lastLoggedDBStatusIsUp) {
            ss.logSystemMessage(LOG_MSG_DB_UNAVAILABLE,
                    java.util.logging.Level.WARNING);
            lastLoggedDBStatusIsUp = false;
        }
        if (isDatabaseUp && !lastLoggedDBStatusIsUp) {
            ss.logSystemMessage(LOG_MSG_DB_BACK_ONLINE,
                    java.util.logging.Level.INFO);
            lastLoggedDBStatusIsUp = true;
        }
    }

    /**
     * Monitoring logic used by background thread This thread is used for both
     * clenup expired sessions in the repository and for the Database health
     * checking. The thread always runs with smallest value of cleanUpPeriod and
     * healthCheckPeriod.
     */
    public void run() {
        while (true) {

            try {
                long nextRun = System.currentTimeMillis() + runPeriod;

                if (debug.messageEnabled()) {
                    debug.message("Cleaning expired session records");
                }

                /*
                 * Clean up is done based on the cleanUpPeriod even though the
                 * thread runs based on the runPeriod.
                 */
                if (cleanUpValue <= 0) {
                    deleteExpired();
                    cleanUpValue = cleanUpPeriod;
                }
                cleanUpValue = cleanUpValue - runPeriod;

                /*
                 * HealthChecking is done based on the runPeriod but only when
                 * the Database is down.
                 */
                if (!isDatabaseUp) {
                    initPersistSession();
                    logDBStatus();
                }

                long sleeptime = nextRun - System.currentTimeMillis();

                if (sleeptime > 0) {
                    Thread.sleep(sleeptime);
                }
            } catch (Exception e) {
                debug.error("JMQSessionRepository.run(): Exception in thread",
                        e);
            }

        }
    }

}
