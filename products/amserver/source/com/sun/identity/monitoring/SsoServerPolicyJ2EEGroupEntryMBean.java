package com.sun.identity.monitoring;

//
// Generated by mibgen version 5.1 (05/20/05) when compiling SUN-OPENSSO-SERVER-MIB in standard metadata mode.
//


// jmx imports
//
import com.sun.management.snmp.SnmpStatusException;

/**
 * This interface is used for representing the remote management interface for the "SsoServerPolicyJ2EEGroupEntry" MBean.
 */
public interface SsoServerPolicyJ2EEGroupEntryMBean {

    /**
     * Getter for the "SsoServerPolicyJ2EEGroupServerURL" variable.
     */
    public String getSsoServerPolicyJ2EEGroupServerURL() throws SnmpStatusException;

    /**
     * Getter for the "SsoServerPolicyJ2EEGroupName" variable.
     */
    public String getSsoServerPolicyJ2EEGroupName() throws SnmpStatusException;

    /**
     * Getter for the "SsoServerPolicyJ2EEGroupIndex" variable.
     */
    public Integer getSsoServerPolicyJ2EEGroupIndex() throws SnmpStatusException;

    /**
     * Getter for the "SsoServerRealmIndex" variable.
     */
    public Integer getSsoServerRealmIndex() throws SnmpStatusException;

}