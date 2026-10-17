package com.inventory.negosyopro.model;

public class Template {
    public enum TemplateType {
        PRODUCT,
        SERVICE
    }
    private long templateId;
    private String templateName;
    private TemplateType templateType;
    private String description;
    private boolean usesExpiry;
    private boolean usesPurchaseOrders;
    private boolean isActive;

    public Template(long templateId, String templateName, TemplateType templateType, String description,
            boolean usesExpiry, boolean usesPurchaseOrder, boolean isActive) {
        this.templateId = templateId;
        this.templateName = templateName;
        this.templateType = templateType;
        this.description = description;
        this.usesExpiry = usesExpiry;
        this.usesPurchaseOrders = usesPurchaseOrder;
        this.isActive = isActive;
    }
    
    public long getTemplateId() {
        return templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public TemplateType getTemplateType() {
        return templateType;
    }

    public String getDescription() {
        return description;
    }

    public boolean isUsesExpiry() {
        return usesExpiry;
    }

    public boolean isUsesPurchaseOrders() {
        return usesPurchaseOrders;
    }

    public boolean isActive() {
        return isActive;
    }

}
