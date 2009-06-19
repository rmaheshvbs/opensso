package com.sun.identity.monitoring;

//
// Generated by mibgen version 5.1 (05/20/05) when compiling SUN-OPENSSO-SERVER-MIB in standard metadata mode.
//


// jmx imports
//
import com.sun.management.snmp.SnmpStatusException;

/**
 * This interface is used for representing the remote management interface for the "SsoServerServerEntry" MBean.
 */
public interface SsoServerServerEntryMBean {

    /**
     * Getter for the "SsoServerServerStatus" variable.
     */
    public Integer getSsoServerServerStatus() throws SnmpStatusException;

    /**
     * Getter for the "SsoServerServerPort" variable.
     */
    public Integer getSsoServerServerPort() throws SnmpStatusException;

    /**
     * Getter for the "SsoServerServerHostName" variable.
     */
    public String getSsoServerServerHostName() throws SnmpStatusException;

    /**
     * Getter for the "SsoServerServerProtocol" variable.
     */
    public String getSsoServerServerProtocol() throws SnmpStatusException;

    /**
     * Getter for the "SsoServerServerId" variable.
     */
    public Integer getSsoServerServerId() throws SnmpStatusException;

}