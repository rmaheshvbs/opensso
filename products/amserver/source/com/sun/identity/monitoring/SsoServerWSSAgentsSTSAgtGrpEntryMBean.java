package com.sun.identity.monitoring;

//
// Generated by mibgen version 5.1 (05/20/05) when compiling SUN-OPENSSO-SERVER-MIB in standard metadata mode.
//


// jmx imports
//
import com.sun.management.snmp.SnmpStatusException;

/**
 * This interface is used for representing the remote management interface for the "SsoServerWSSAgentsSTSAgtGrpEntry" MBean.
 */
public interface SsoServerWSSAgentsSTSAgtGrpEntryMBean {

    /**
     * Getter for the "WssAgentsSTSAgtGrpSvcMEXEndPoint" variable.
     */
    public String getWssAgentsSTSAgtGrpSvcMEXEndPoint() throws SnmpStatusException;

    /**
     * Getter for the "WssAgentsSTSAgtGrpSvcEndPoint" variable.
     */
    public String getWssAgentsSTSAgtGrpSvcEndPoint() throws SnmpStatusException;

    /**
     * Getter for the "WssAgentsSTSAgtGrpName" variable.
     */
    public String getWssAgentsSTSAgtGrpName() throws SnmpStatusException;

    /**
     * Getter for the "WssAgentsSTSAgtGrpIndex" variable.
     */
    public Integer getWssAgentsSTSAgtGrpIndex() throws SnmpStatusException;

    /**
     * Getter for the "SsoServerRealmIndex" variable.
     */
    public Integer getSsoServerRealmIndex() throws SnmpStatusException;

}