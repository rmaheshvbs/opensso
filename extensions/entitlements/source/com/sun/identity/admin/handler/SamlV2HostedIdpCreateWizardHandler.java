/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
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
 * $Id: SamlV2HostedIdpCreateWizardHandler.java,v 1.1 2009-06-23 06:41:54 babysunil Exp $
 */

package com.sun.identity.admin.handler;

import com.icesoft.faces.component.dragdrop.DndEvent;
import com.icesoft.faces.component.dragdrop.DropEvent;
import com.icesoft.faces.component.inputfile.FileInfo;
import com.icesoft.faces.component.inputfile.InputFile;
import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.dao.SamlV2HostedIdpCreateDao;
import com.sun.identity.admin.effect.InputFieldErrorEffect;
import com.sun.identity.admin.effect.MessageErrorEffect;
import com.sun.identity.admin.model.LinkBean;
import com.sun.identity.admin.model.MessageBean;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.NextPopupBean;
import com.sun.identity.admin.model.SamlV2ViewAttribute;
import com.sun.identity.admin.model.ViewAttribute;
import com.sun.identity.admin.Resources;
import com.sun.identity.admin.model.SamlV2HostedIdpCreateWizardBean;
import com.sun.identity.admin.model.SamlV2HostedIdpCreateWizardStep;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;
import javax.faces.event.ValueChangeEvent;

public class SamlV2HostedIdpCreateWizardHandler
        extends SamlV2HostedCreateWizardHandler {

    private SamlV2HostedIdpCreateDao samlV2HostedIdpCreateDao;
    private MessagesBean messagesBean;

    public void setSamlV2HostedIdpCreateDao(
            SamlV2HostedIdpCreateDao samlV2HostedIdpCreateDao) {
        this.samlV2HostedIdpCreateDao = samlV2HostedIdpCreateDao;
    }

    @Override
    public void previousListener(ActionEvent event) {
        int step = getStep(event);
        SamlV2HostedIdpCreateWizardStep pws =
                SamlV2HostedIdpCreateWizardStep.valueOf(step);

        switch (pws) {
            case REALM:
                break;
            case METADATA:
                if (!validateMetadata()) {
                    return;
                }
                break;
            case COT:
                if (!validateCot()) {
                    return;
                }
                break;
            case ATTRIBUTEMAPPING:
                break;
            case SUMMARY:
                break;
            default:
                assert false : "unhandled step: " + pws;
        }

        super.previousListener(event);
    }

    @Override
    public void nextListener(ActionEvent event) {
        int step = getStep(event);
        SamlV2HostedIdpCreateWizardStep pws =
                SamlV2HostedIdpCreateWizardStep.valueOf(step);

        switch (pws) {
            case REALM:
                break;
            case METADATA:
                if (!validateMetadata()) {
                    return;
                }
                break;
            case COT:
                if (!validateCot()) {
                    return;
                }
                break;
            case ATTRIBUTEMAPPING:
                break;
            case SUMMARY:
                break;
            default:
                assert false : "unhandled step: " + pws;
        }

        super.nextListener(event);
    }

    private SamlV2HostedIdpCreateWizardBean getSamlV2HostedIdpCreateWizardBean() {
        return (SamlV2HostedIdpCreateWizardBean) getWizardBean();
    }

    private boolean validateSteps() {
        if (!validateMetadata()) {
            return false;
        }

        if (!validateCot()) {
            return false;
        }

        return true;
    }

    @Override
    public void cancelListener(ActionEvent event) {
        getSamlV2HostedIdpCreateWizardBean().reset();
        doCancelNext();
    }

    @Override
    public void doCancelNext() {
        NextPopupBean npb = NextPopupBean.getInstance();
        npb.setVisible(true);
        Resources r = new Resources();
        npb.setTitle(r.getString(this, "cancelTitle"));
        npb.setMessage(r.getString(this, "cancelMessage"));
        npb.setLinkBeans(getCancelLinkBeans());

    }

    private List<LinkBean> getCancelLinkBeans() {
        List<LinkBean> lbs = new ArrayList<LinkBean>();
        lbs.add(LinkBean.HOME);
        lbs.add(LinkBean.SAMLV2_HOSTED_IDP_CREATE);
        lbs.add(LinkBean.SAMLV2_REMOTE_SP_CREATE);
        lbs.add(LinkBean.SAMLV2_HOSTED_SP_CREATE);

        return lbs;
    }

    @Override
    public void finishListener(ActionEvent event) {
        if (!validateSteps()) {
            return;
        }

        String cot;
        boolean choseFromExisintCot =
                getSamlV2HostedIdpCreateWizardBean().isCot();
        if (choseFromExisintCot) {
            cot = getSamlV2HostedIdpCreateWizardBean().getSelectedCot();
        } else {
            cot = getSamlV2HostedIdpCreateWizardBean().getNewCotName();
        }

        boolean isMeta = getSamlV2HostedIdpCreateWizardBean().isMeta();
        String selectedRealmValue =
                getSamlV2HostedIdpCreateWizardBean().getSelectedRealm();
        int idx = selectedRealmValue.indexOf("(");
        int end = selectedRealmValue.indexOf(")");
        String realm = selectedRealmValue.substring(idx + 1, end).trim();
        String name = getSamlV2HostedIdpCreateWizardBean().getNewEntityName();
        String key =
                getSamlV2HostedIdpCreateWizardBean().getSelectedSigningKey();
        List test = getSamlV2HostedIdpCreateWizardBean().getViewAttributes();
        List attrMapping = new ArrayList();
        attrMapping =
                getSamlV2HostedIdpCreateWizardBean().getToListOfStrings(test);
        if (!isMeta) {
            samlV2HostedIdpCreateDao.createSamlv2HostedIdp(
                    realm, name, cot, key, attrMapping);
        } else {
            String stdMeta =
                    getSamlV2HostedIdpCreateWizardBean().getStdMetaFile();
            String extMeta =
                    getSamlV2HostedIdpCreateWizardBean().getExtMetaFile();
            samlV2HostedIdpCreateDao.importSamlv2HostedIdp(
                    cot, stdMeta, extMeta);
        }
        getSamlV2HostedIdpCreateWizardBean().reset();
        getFinishAction();
    }

    public void getFinishAction() {
        getSamlV2HostedIdpCreateWizardBean().reset();
        doFinishNext();
    }

    public void doFinishNext() {
        NextPopupBean npb = NextPopupBean.getInstance();
        npb.setVisible(true);
        Resources r = new Resources();
        npb.setTitle(r.getString(this, "finishTitle"));
        npb.setMessage(r.getString(this, "finishMessage"));
        npb.setLinkBeans(getFinishLinkBeans());

    }

    private List<LinkBean> getFinishLinkBeans() {
        List<LinkBean> lbs = new ArrayList<LinkBean>();
        lbs.add(LinkBean.HOME);
        lbs.add(LinkBean.SAMLV2_HOSTED_SP_CREATE);
        lbs.add(LinkBean.SAMLV2_REMOTE_SP_CREATE);
        lbs.add(LinkBean.SAMLV2_REMOTE_IDP_CREATE);

        return lbs;
    }

    public boolean validateMetadata() {
        boolean usingMetaDataFile =
                getSamlV2HostedIdpCreateWizardBean().isMeta();
        String newEntityName =
                getSamlV2HostedIdpCreateWizardBean().getNewEntityName();

        if (!usingMetaDataFile) {
            if ((newEntityName == null) || (newEntityName.length() == 0)) {
                MessageBean mb = new MessageBean();
                Resources r = new Resources();
                mb.setSummary(r.getString(this, "invalidNameSummary"));
                mb.setDetail(r.getString(this, "invalidNameDetail"));
                mb.setSeverity(FacesMessage.SEVERITY_ERROR);

                Effect e;
                e = new InputFieldErrorEffect();
                getSamlV2HostedIdpCreateWizardBean().
                        setSamlV2EntityNameInputEffect(e);

                e = new MessageErrorEffect();
                getSamlV2HostedIdpCreateWizardBean().
                        setSamlV2EntityNameMessageEffect(e);

                getMessagesBean().addMessageBean(mb);
                getSamlV2HostedIdpCreateWizardBean().gotoStep(
                        SamlV2HostedIdpCreateWizardStep.METADATA.toInt());

                return false;
            }
        }

        return true;
    }

    public boolean validateCot() {
        boolean usingExistingCot = getSamlV2HostedIdpCreateWizardBean().isCot();
        String cotname = getSamlV2HostedIdpCreateWizardBean().getNewCotName();

        if (!usingExistingCot) {
            if (cotname.length() == 0 || (cotname == null)) {
                MessageBean mb = new MessageBean();
                Resources r = new Resources();
                mb.setSummary(r.getString(this, "invalidCotSummary"));
                mb.setDetail(r.getString(this, "invalidCotDetail"));
                mb.setSeverity(FacesMessage.SEVERITY_ERROR);

                Effect e;
                e = new InputFieldErrorEffect();
                getSamlV2HostedIdpCreateWizardBean().
                        setSamlV2EntityNameInputEffect(e);

                e = new MessageErrorEffect();
                getSamlV2HostedIdpCreateWizardBean().
                        setSamlV2EntityNameMessageEffect(e);

                getMessagesBean().addMessageBean(mb);
                getSamlV2HostedIdpCreateWizardBean().gotoStep(
                        SamlV2HostedIdpCreateWizardStep.COT.toInt());


                return false;
            }
        }
        return true;
    }

    public void stdMetaUploadFile(ActionEvent event) throws IOException {
        InputFile inputFile = (InputFile) event.getSource();
        FileInfo fileInfo = inputFile.getFileInfo();
        if (fileInfo.getStatus() == FileInfo.SAVED) {
            // read the file into a string
            // reference our newly updated file for display purposes and
            // added it to filename string object in our bean
            File file = new File(fileInfo.getFile().getAbsolutePath());

            StringBuffer contents = new StringBuffer();
            BufferedReader reader = null;

            try {
                reader = new BufferedReader(new FileReader(file));
                String text = null;

                // repeat until all lines is read
                while ((text = reader.readLine()) != null) {
                    contents.append(text).append(System.getProperty(
                            "line.separator"));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            getSamlV2HostedIdpCreateWizardBean().setStdMetaFilename(
                    fileInfo.getFileName());
            getSamlV2HostedIdpCreateWizardBean().setStdMetaFile(
                    contents.toString());
            file.delete();
        }
    }

    public void stdMetaFileUploadProgress(EventObject event) {
        InputFile ifile = (InputFile) event.getSource();
        getSamlV2HostedIdpCreateWizardBean().setStdMetaFileProgress(
                ifile.getFileInfo().getPercent());
    }

    public void extMetaUploadFile(ActionEvent event) throws IOException {
        InputFile inputFile = (InputFile) event.getSource();
        FileInfo fileInfo = inputFile.getFileInfo();
        if (fileInfo.getStatus() == FileInfo.SAVED) {
            // read the file into a string
            // reference our newly updated file for display purposes and
            // added it to filename string object in our bean
            File file = new File(fileInfo.getFile().getAbsolutePath());
            StringBuffer contents = new StringBuffer();
            BufferedReader reader = null;

            try {
                reader = new BufferedReader(new FileReader(file));
                String text = null;

                // repeat until all lines is read
                while ((text = reader.readLine()) != null) {
                    contents.append(text).append(System.getProperty(
                            "line.separator"));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            getSamlV2HostedIdpCreateWizardBean().setExtMetaFilename(
                    fileInfo.getFileName());
            getSamlV2HostedIdpCreateWizardBean().setExtMetaFile(
                    contents.toString());
            file.delete();
        }
    }

    // for attrmapping
    public void dropListener(DropEvent dropEvent) {
        int type = dropEvent.getEventType();
        if (type == DndEvent.DROPPED) {
            Object dragValue = dropEvent.getTargetDragValue();
            assert (dragValue != null);
            ViewAttribute va = (ViewAttribute) dragValue;

            getSamlV2HostedIdpCreateWizardBean().getViewAttributes().add(va);
        }
    }

    protected ViewAttribute getViewAttribute(FacesEvent event) {
        ViewAttribute va = (ViewAttribute) event.getComponent().
                getAttributes().get("viewAttribute");
        assert (va != null);

        return va;
    }

    public void removeListener(ActionEvent event) {
        ViewAttribute va = getViewAttribute(event);
        getSamlV2HostedIdpCreateWizardBean().getViewAttributes().remove(va);
    }

    public void addListener(ActionEvent event) {
        ViewAttribute va = newViewAttribute();
        va.setEditable(true);
        SamlV2ViewAttribute sva = (SamlV2ViewAttribute) va;
        sva.setValueEditable(true);
        sva.setIsAdded(true);

        getSamlV2HostedIdpCreateWizardBean().getViewAttributes().add(va);
    }

    public void editNameListener(ActionEvent event) {
        ViewAttribute va = (ViewAttribute) getViewAttribute(event);
        va.setNameEditable(true);
    }

    public void nameEditedListener(ValueChangeEvent event) {
        ViewAttribute va = (ViewAttribute) getViewAttribute(event);
        va.setNameEditable(false);
    }

    public void editValueListener(ActionEvent event) {
        SamlV2ViewAttribute sva = (SamlV2ViewAttribute) getViewAttribute(event);
        sva.setValueEditable(true);
    }

    public void valueEditedListener(ValueChangeEvent event) {
        SamlV2ViewAttribute sva = (SamlV2ViewAttribute) getViewAttribute(event);
        sva.setValueEditable(false);
    }

    public ViewAttribute newViewAttribute() {
        return new SamlV2ViewAttribute();
    }

    public void extMetaFileUploadProgress(EventObject event) {
        InputFile ifile = (InputFile) event.getSource();
        getSamlV2HostedIdpCreateWizardBean().setExtMetaFileProgress(
                ifile.getFileInfo().getPercent());
    }

    public void setMessagesBean(MessagesBean messagesBean) {
        this.messagesBean = messagesBean;
    }

    public MessagesBean getMessagesBean() {
        return messagesBean;
    }
}