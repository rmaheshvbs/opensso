package com.sun.identity.admin.model;

import java.io.Serializable;

public class WizardStepBean implements Serializable {
    private boolean expanded = false;
    private boolean enabled = false;

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
