package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.faces.model.SelectItem;

public class UrlResourcesBean implements Serializable {
    private boolean addPopupVisible = false;
    private String addPopupPrefix;
    private String addPopupSuffix;
    private String addExceptionPopupName;
    private String searchFilter;
    private boolean addExceptionPopupVisible;
    private UrlResource addExceptionPopupResource;
    private List<Resource> addPopupAvailableResources;
    private Effect resourcesMessageEffect;

    public boolean isAddPopupVisible() {
        return addPopupVisible;
    }

    public void setAddPopupVisible(boolean addPopupVisible) {
        this.addPopupVisible = addPopupVisible;
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
    }

    public boolean isAddExceptionPopupVisible() {
        return addExceptionPopupVisible;
    }

    public void setAddExceptionPopupVisible(boolean addExceptionPopupVisible) {
        this.addExceptionPopupVisible = addExceptionPopupVisible;
    }

    public UrlResource getAddExceptionPopupResource() {
        return addExceptionPopupResource;
    }

    public void setAddExceptionPopupResource(UrlResource addExceptionPopupResource) {
        this.addExceptionPopupResource = addExceptionPopupResource;
    }

    public String getAddExceptionPopupName() {
        return addExceptionPopupName;
    }

    public void setAddExceptionPopupName(String addExceptionPopupName) {
        this.addExceptionPopupName = addExceptionPopupName;
    }

    public List<Resource> getAddPopupAvailableResources() {
        return addPopupAvailableResources;
    }

    public List<SelectItem> getAddPopupAvailableResourceItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        if (addPopupAvailableResources == null) {
            return items;
        }

        for (Resource r: addPopupAvailableResources) {
            SelectItem i = new SelectItem(r, r.getName());
            items.add(i);
        }

        return items;
    }

    public List<SelectItem> getAddPopupAvailableResourcePrefixItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();
        if (addPopupAvailableResources == null) {
            return items;
        }

        for (Resource r: addPopupAvailableResources) {
            UrlResource ur = (UrlResource)r;
            if (ur.isSuffixable()) {
                SelectItem i = new SelectItem(ur.getPrefix());
                items.add(i);
            }
        }

        return items;
    }

    public void setAddPopupAvailableResources(List<Resource> addPopupAvailableResources) {
        this.addPopupAvailableResources = addPopupAvailableResources;
    }

    public Effect getResourcesMessageEffect() {
        return resourcesMessageEffect;
    }

    public void setResourcesMessageEffect(Effect resourcesMessageEffect) {
        this.resourcesMessageEffect = resourcesMessageEffect;
    }

    public String getAddPopupPrefix() {
        return addPopupPrefix;
    }

    public void setAddPopupPrefix(String addPopupPrefix) {
        this.addPopupPrefix = addPopupPrefix;
    }

    public String getAddPopupSuffix() {
        return addPopupSuffix;
    }

    public void setAddPopupSuffix(String addPopupSuffix) {
        this.addPopupSuffix = addPopupSuffix;
    }
}
