package com.sun.identity.monitoring;

//
// Generated by mibgen version 5.1 (05/20/05) when compiling SUN-OPENSSO-SERVER-MIB.
//

// java imports
//
import java.io.Serializable;

// jmx imports
//
import javax.management.MBeanServer;
import com.sun.management.snmp.SnmpString;
import com.sun.management.snmp.SnmpStatusException;

// jdmk imports
//
import com.sun.management.snmp.agent.SnmpMib;

/**
 * The class is used for implementing the "SsoServerPolicySvc" group.
 * The group is defined with the following oid: 1.3.6.1.4.1.42.2.230.3.1.1.2.1.13.
 */
public class SsoServerPolicySvc implements SsoServerPolicySvcMBean, Serializable {

    /**
     * Variable for storing the value of "SsoServerPolicyEvalsOut".
     * The variable is identified by: "1.3.6.1.4.1.42.2.230.3.1.1.2.1.13.3".
     */
    protected Long SsoServerPolicyEvalsOut = new Long(1);

    /**
     * Variable for storing the value of "SsoServerPolicyEvalsIn".
     * The variable is identified by: "1.3.6.1.4.1.42.2.230.3.1.1.2.1.13.2".
     */
    protected Long SsoServerPolicyEvalsIn = new Long(1);

    /**
     * Variable for storing the value of "SsoServerPolicyStatus".
     * The variable is identified by: "1.3.6.1.4.1.42.2.230.3.1.1.2.1.13.1".
     */
    protected String SsoServerPolicyStatus = new String("JDMK 5.1");


    /**
     * Constructor for the "SsoServerPolicySvc" group.
     * If the group contains a table, the entries created through an SNMP SET will not be registered in Java DMK.
     */
    public SsoServerPolicySvc(SnmpMib myMib) {
    }


    /**
     * Constructor for the "SsoServerPolicySvc" group.
     * If the group contains a table, the entries created through an SNMP SET will be AUTOMATICALLY REGISTERED in Java DMK.
     */
    public SsoServerPolicySvc(SnmpMib myMib, MBeanServer server) {
    }

    /**
     * Getter for the "SsoServerPolicyEvalsOut" variable.
     */
    public Long getSsoServerPolicyEvalsOut() throws SnmpStatusException {
        return SsoServerPolicyEvalsOut;
    }

    /**
     * Getter for the "SsoServerPolicyEvalsIn" variable.
     */
    public Long getSsoServerPolicyEvalsIn() throws SnmpStatusException {
        return SsoServerPolicyEvalsIn;
    }

    /**
     * Getter for the "SsoServerPolicyStatus" variable.
     */
    public String getSsoServerPolicyStatus() throws SnmpStatusException {
        return SsoServerPolicyStatus;
    }

}