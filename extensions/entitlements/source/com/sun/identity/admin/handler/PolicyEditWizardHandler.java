package com.sun.identity.admin.handler;

public class PolicyEditWizardHandler extends PolicyWizardHandler {
    public String getFinishAction() {
        return "policy-manage";
    }

    public String getCancelAction() {
        return "policy-manage";
    }

    @Override
    public String cancelPopupOkAction() {
        getPolicyManageBean().reset();
        return super.cancelPopupOkAction();
    }

}