' The contents of this file are subject to the terms
' of the Common Development and Distribution License
' (the License). You may not use this file except in
' compliance with the License.
'
' You can obtain a copy of the License at
' https://opensso.dev.java.net/public/CDDLv1.0.html or
' opensso/legal/CDDLv1.0.txt
' See the License for the specific language governing
' permission and limitations under the License.
'
' When distributing Covered Code, include this CDDL
' Header Notice in each file and include the License file
' at opensso/legal/CDDLv1.0.txt.
' If applicable, add the following below the CDDL Header,
' with the fields enclosed by brackets [] replaced by
' your own identifying information:
' "Portions Copyrighted [year] [name of copyright owner]"
'
' $Id: IIS7CreateConfig.vbs,v 1.2 2009-02-16 23:13:05 robertis Exp $
'
' Copyright 2007 Sun Microsystems Inc. All Rights Reserved
'
'
'--------------------------------------------------------------------
Const ForReading = 1, ForWriting = 2
Dim installDir, setInstallDir, configInstallDir, portNumber,setPortNumber
Dim objWWW, Item, setIdentifier, protocol, setProtocol, agentDeploymentURI
Dim setAgentDeploymentURI, setPrimaryServerHost, primaryServerPort
Dim setPrimaryServerPort, primaryServerProtocol, setPrimaryServerProtocol
Dim primaryServerDeploymentURI, setPrimaryServerDeploymentURI, userName
Dim primaryServerConsoleURI, setPrimaryServerConsoleURI, failoverServer
Dim setFailoverServerPort, failoverServerProtocol, setFailoverServerProtocol
Dim failoverServerDeploymentURI, setFailoverServerDeploymentURI, orgName
Dim failoverServerConsoleURI, setFailoverServerConsoleURI, failoverServerPort
Dim setOrgName, setUserName, setUserPassword, setPasswdKey, encryptFile, primaryServerURL
Dim encrypt, encryptedPwd, delEncryptedFile, cdssoEnabled, setCDSSOEnabled
Dim isVersion, correctVersion, setISVersionNumber, correctAgentPort
Dim correctIdentifier, correctAgentProtocol, correctPrimaryServerPort
Dim correctPrimaryServerProtocol, correctFailoverServerPort, tmpInstallDir
Dim correctFailoverServerProtocl, correctPassword, correctCDSSOFlag, dict
Dim setHostName, expHostName, encryptedPasswd, setFailoverServerHost, failoverServerURL
Dim scriptFullName, currDir, WshShell, AppCmd
Dim oIIS, oSites, oSite, tmp


Set Args = WScript.Arguments
if Args.Count < 1 Then
   WScript.Echo "Incorrect Number of arguments"
   WScript.Echo "Syntax: IIS7CreateConfig.vbs <config-filename>"
   WScript.Quit(1)
end if

WScript.Echo ""
WScript.Echo "Copyright c 2007 Sun Microsystems, Inc. All rights reserved"
WScript.Echo "Use is subject to license terms"

Set oFSO = CreateObject("Scripting.FileSystemObject")
Set dict = CreateObject("Scripting.Dictionary")
Set WshShell = CreateObject("WScript.Shell")

WScript.Echo "---------------------------------------------------------"
WScript.Echo "    Microsoft (TM) Internet Information Server (7.0)     "
WScript.Echo "---------------------------------------------------------"

'// Set the correct path where the script is located
scriptFullName = WScript.ScriptFullName
currDir = split(scriptFullName, "\IIS7CreateConfig.vbs")
WshShell.currentDirectory = currDir(0)

'// Load the locale specific resource file
Call LoadResourceFile(oFSO, dict)

'// Prompt user to enter Agent Details
Call GetAgentDetails(oFSO, dict)

'// Prompt user to enter Access Manager Details
Call GetAccessManagerDetails(oFSO, dict, WshShell)

'// Create config file
Call WriteConfigFile(oFSO, Args(0))

WScript.Quit(1)

'----------------------------------------------------------------------------
' Function Name : LoadResourceFile
' Input : oFSO, dict
' Output : None
' Description : The AgentConfigure() function performs the following tasks:
' 1. By default loads the English Resource file if the user does not specifies
'    any other resource file
' 2. Checks whether the resource file exists, if not will display an error and
'    prompts the user to re-enter the resource file name
' 3. Opens the resource file and reads the token as name value pairs and
'    populates the dictionary table to be used by subsequent functions.
'----------------------------------------------------------------------------
Function LoadResourceFile(oFSO, dict)

  Dim resourceFile, resFile, sLine, aLine, firstPart, secondPart
  Dim correctResourceFile

  correctResourceFile = false
  do 
    WScript.Echo "Enter the Agent Resource File Name [IIS7Resource.en] :"
    resourceFile = WScript.StdIn.ReadLine
    if (resourceFile = "") then
       resourceFile = "IIS7Resource.en"
       correctResourceFile = true
    elseif (oFSO.FileExists(resourceFile) = false) then
          WScript.Echo "Resource File specified does not exist"
        else 
	  correctResourceFile = true
    end if
  loop until (correctResourceFile = true)

  Set resFile = oFSO.OpenTextFile(resourceFile,ForReading,True)

  Do While resFile.AtEndOfStream <> True
     '//Read a line of the file
     sLine = resFile.ReadLine
     if (instr(sLine, "=") > 0) then
        'split the line on the = sign
        aLine = split(sLine, "=")
        firstPart = Trim(aline(0))
        secondPart = Trim(aline(1))
        'Read all the tokens and add to the dictionary
        dict.add firstPart, secondPart
     end if
  loop
End Function

'----------------------------------------------------------------------------
' Function Name : GetAgentDetails
' Input : oFSO, dict
' Output : None
' Description : The GetAgentDetails() function performs the following tasks:
' 1. Using the dictionary table prompts the user to enter Agent details and
'    stores the value in the variables
'----------------------------------------------------------------------------
Function GetAgentDetails(oFSO, dict)

  Set WshNetwork = WScript.CreateObject("WScript.Network")

  installDir = oFSO.GetAbsolutePathName("..\")
  tmpInstallDir = installDir

  setInstallDir = Replace(tmpInstallDir,"\","/")

  configInstallDir = Replace(installDir,"/","\")
  computerName = WshNetwork.ComputerName

  WScript.Echo dict("101")
  setHostName = WScript.StdIn.ReadLine
  WScript.Echo ""

  WScript.Echo dict("102")
  WScript.Echo ""

  strCmd = "%systemroot%\system32\inetsrv\appcmd list sites" 
  Set objExecObject = WshShell.Exec(strCmd)
  Do Until objExecObject.StdOut.AtEndOfStream
     WScript.Echo objExecObject.StdOut.ReadLine
  Loop 

  WScript.Echo ""
  WScript.Echo dict("104")
  setIdentifier = WScript.StdIn.ReadLine

  WScript.Echo ""
  correctAgentProtocol = false
  do 
    protocol = "http"
    WScript.Echo dict("105")
    setProtocol = LCase(WScript.StdIn.ReadLine)
    if (setProtocol="") then
       setProtocol = protocol
    end if
    if ((setProtocol = "http") or (setProtocol = "https")) then
       correctAgentProtocol = true
    else
       WScript.Echo dict("122")
    end if
  loop until (correctAgentProtocol = true)

  WScript.Echo ""
  correctAgentPort = false
  do 
    if (setProtocol = "http") then
       portNumber = "80"
    elseif (setProtocol = "https") then
       portNumber = "443"
    end if
    WScript.Echo dict("106") & " [" & portNumber & "] :"
    setPortNumber = WScript.StdIn.ReadLine
    if setPortNumber="" then
       setPortNumber = portNumber
    end if
    if (isNumeric(setPortNumber)) then
       correctAgentPort = true
    else 
       WScript.Echo dict("123")
    end if
  loop until (correctAgentPort = true)

  WScript.Echo ""
  agentDeploymentURI = "/amagent"
  WScript.Echo dict("107")
  setAgentDeploymentURI = WScript.StdIn.ReadLine
  if (setAgentDeploymentURI="") then
     setAgentDeploymentURI = agentDeploymentURI
  end if

End Function

'----------------------------------------------------------------------------
' Function Name : GetAccessManagerDetails
' Input : oFSO, dict, WshShell
' Output : None
' Description : The GetAccessManagerDetails() function performs the following 
' tasks:
' 1. Using the dictionary table prompts the user to enter Access Manager
'    details and stores the value in the variables
' 2. Encrypts the shared secret password
'----------------------------------------------------------------------------
Function GetAccessManagerDetails(oFSO, dict, WshShell)

  WScript.Echo "------------------------------------------------"
  WScript.Echo "Sun OpenSSO Enterprise 8.0" 
  WScript.Echo "------------------------------------------------"

  WScript.Echo dict("108")
  setPrimaryServerHost = WScript.StdIn.ReadLine

  WScript.Echo ""
  correctPrimaryServerProtocol = false
  do
    primaryServerProtocol = "http"
    WScript.Echo dict("109")
    setPrimaryServerProtocol = LCase(WScript.StdIn.ReadLine)
    if (setPrimaryServerProtocol="") then
       setPrimaryServerProtocol = primaryServerProtocol
    end if
    if (setPrimaryServerProtocol = "http") or (setPrimaryServerProtocol="https") then
       correctPrimaryServerProtocol = true
    else
       WScript.Echo dict("122")
    end if
  loop until correctPrimaryServerProtocol = true

  WScript.Echo ""
  correctPrimaryServerPort = false
  do 
    if (setPrimaryServerProtocol = "http") then
       primaryServerPort = "58080"
    elseif (setPrimaryServerProtocol = "https") then
       primaryServerPort = "443"
    end if
    WScript.Echo dict("110") & " [" & primaryServerPort & "] :"
    setPrimaryServerPort = WScript.StdIn.ReadLine
    if (setPrimaryServerPort="") then
       setPrimaryServerPort = primaryServerport
    end if
    if (isNumeric(setPrimaryServerPort)) then
       correctPrimaryServerPort = true
    else
     WScript.Echo dict("124")
    end if
  loop until (correctPrimaryServerPort = true)

  WScript.Echo ""
  primaryServerDeploymentURI = "/opensso"
  WScript.Echo dict("111")
  setPrimaryServerDeploymentURI = WScript.StdIn.ReadLine
  if (setPrimaryServerDeploymentURI="") then
     setPrimaryServerDeploymentURI = primaryServerDeploymentURI
  end if

  WScript.Echo ""
  primaryServerConsoleURI = "/opensso"
  WScript.Echo dict("112")
  setPrimaryServerConsoleURI = WScript.StdIn.ReadLine
  if (setPrimaryServerConsoleURI="") then
     setPrimaryServerConsoleURI = primaryServerConsoleURI
  end if

  setUserName = "UrlAccessAgent"

  WScript.Echo ""

    do 
        WScript.Echo "Enter the password file : "
        passwdFile = WScript.StdIn.ReadLine
        WScript.Echo
        'First check if the file exist.
        if(oFSO.FileExists(passwdFile)) then
            Set passwdFile = oFSO.GetFile(passwdFile)
            if(passwdFile.Size > 0) then
                Set passwd = oFSO.OpenTextFile(passwdFile,ForReading,True)
                setUserPassword = passwd.ReadLine
                passwd.Close
                if (Trim(setUserPassword) = "") then
                    WScript.Echo ""
                    WScript.Echo dict("119")
                    WScript.Echo ""
                end if
            else
                WScript.Echo dict("119")
            end if
        else
            WScript.Echo "The password file does not exist."
        end if

    loop until (Trim(setUserPassword) <> "")

    setUserPassword = Trim(setUserPassword)

  '// Encrypt the password
  encryptFile = configInstallDir + "\encryptPasswd"
  setPasswdKey = "x5uThMxwF"
  WshShell.Run "cmd /c cd " + configInstallDir + "\bin" + "& cryptit.exe " + setUserPassword + " "+ setPasswdKey + " > " + encryptFile, 0, true

  Set encrypt = oFSO.OpenTextFile(encryptFile,ForReading,True)
  encryptedPasswd = encrypt.ReadLine
  encrypt.Close
  WScript.Sleep(100)

  oFSO.DeleteFile(encryptFile)

  Set WshShell = nothing

End Function

'----------------------------------------------------------------------------
' Function Name : WriteConfigFile
' Input : oFSO, configFile
' Output : None
' Description : The WriteConfigFile() function performs the following tasks:
' 1.Creates the config file
' 2 Populates the name and value pairs to be used by the IIS7admin.vbs
'----------------------------------------------------------------------------
Function WriteConfigFile(oFSO, configFile)

  '// Create the agentConfig file
  Set wTF = oFSO.OpenTextFile(configFile,ForWriting,True)
  wTF.WriteLine "INSTALL_DIRECTORY = " + configInstallDir
  wTF.WriteLine "IDENTIFIER = " + setIdentifier
  wTF.WriteLine "@AGENT_HOST@ = " + setHostName
  wTF.WriteLine "@AGENT_PREF_PORT@ = " + setPortNumber
  wTF.WriteLine "@AGENT_PREF_PROTO@ = " + setProtocol
  wTF.WriteLine "@AGENT_DEPLOY_URI@ = " + setAgentDeploymentURI

  primaryServerURL = setPrimaryServerProtocol + "://" + setPrimaryServerHost + ":"+ setPrimaryServerPort 

  wTF.WriteLine "@AM_SERVICES_HOST@ = " + setPrimaryServerHost
  wTF.WriteLine "@AM_SERVICES_PROTO@ = " + setPrimaryServerProtocol
  wTF.WriteLine "@AM_SERVICES_PORT@ = " + setPrimaryServerPort
  wTF.WriteLine "@AM_SERVICES_DEPLOY_URI@ = " + setPrimaryServerDeploymentURI
                                      
  wTF.WriteLine "@AGENT_PROFILE_NAME@ = " + setUserName
  wTF.WriteLine "@AGENT_ENCRYPTED_PASSWORD@ = " + encryptedPasswd
  wTF.WriteLine "@AGENT_ENCRYPT_KEY@ = " + setPasswdKey
  

  wTF.WriteLine "AGENT_URL_PREFIX = " + setProtocol + "://" + setHostName + ":" + setPortNumber + agentDeploymentURI

  wTF.WriteLine "@DEBUG_LOGS_DIR@ = " + setInstallDir + "/Identifier_" + setIdentifier + "/logs/debug"
  wTF.WriteLine "TEMP_DIR_PREFIXDEBUG_DIR_PREFIX = " + setInstallDir 

  wTF.WriteLine "@AUDIT_LOGS_DIR@ = " + setInstallDir + "/Identifier_" + setIdentifier + "/logs/audit"

  expHostName = Replace(setHostName,".","_")
  wTF.WriteLine "@AUDIT_LOG_FILENAME@ = " + "amAgent_"+ expHostName + ".log"

  wTF.WriteLine "SERVER_DIR =  "  + setInstallDir + "/iis7/cert"

  wTF.WriteLine "@NOTIFICATION_ENABLE@ = true" 
  wTF.WriteLine "@LOG_ROTATION@ = true" 
  wTF.WriteLine "AGENT_CERT_PREFIX =  " 

  WScript.Echo "-----------------------------------------------------"
  WScript.Echo "Agent Configuration file created ==>  " + configFile
  WScript.Echo "-----------------------------------------------------"
  wTF.Close

  Set oFSO = nothing

End Function
