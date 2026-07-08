package com.moto.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "system_config")
public class SystemConfig {
    @Id
    private String id = "global_config";
    private String companyName = "Homero Motos";
    private String contactPhone = "";
    private String address = "";
    private String emailConfigHost = "";
    private String emailConfigPort = "";
    private String emailConfigUser = "";
    private String emailConfigPassword = "";
    private String whatsappToken = "";
    private String colorPrimary = "#2563eb";
    private String colorSecondary = "#0f172a";
    private Double maxFinancingTimeMonths = 36.0;
    private Double defaultInterestRate = 10.0;
    private String announcementBanner = "";
    private boolean maintenanceMode = false;

    public SystemConfig() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getEmailConfigHost() { return emailConfigHost; }
    public void setEmailConfigHost(String emailConfigHost) { this.emailConfigHost = emailConfigHost; }

    public String getEmailConfigPort() { return emailConfigPort; }
    public void setEmailConfigPort(String emailConfigPort) { this.emailConfigPort = emailConfigPort; }

    public String getEmailConfigUser() { return emailConfigUser; }
    public void setEmailConfigUser(String emailConfigUser) { this.emailConfigUser = emailConfigUser; }

    public String getEmailConfigPassword() { return emailConfigPassword; }
    public void setEmailConfigPassword(String emailConfigPassword) { this.emailConfigPassword = emailConfigPassword; }

    public String getWhatsappToken() { return whatsappToken; }
    public void setWhatsappToken(String whatsappToken) { this.whatsappToken = whatsappToken; }

    public String getColorPrimary() { return colorPrimary; }
    public void setColorPrimary(String colorPrimary) { this.colorPrimary = colorPrimary; }

    public String getColorSecondary() { return colorSecondary; }
    public void setColorSecondary(String colorSecondary) { this.colorSecondary = colorSecondary; }

    public Double getMaxFinancingTimeMonths() { return maxFinancingTimeMonths; }
    public void setMaxFinancingTimeMonths(Double maxFinancingTimeMonths) { this.maxFinancingTimeMonths = maxFinancingTimeMonths; }

    public Double getDefaultInterestRate() { return defaultInterestRate; }
    public void setDefaultInterestRate(Double defaultInterestRate) { this.defaultInterestRate = defaultInterestRate; }

    public String getAnnouncementBanner() { return announcementBanner; }
    public void setAnnouncementBanner(String announcementBanner) { this.announcementBanner = announcementBanner; }

    public boolean isMaintenanceMode() { return maintenanceMode; }
    public void setMaintenanceMode(boolean maintenanceMode) { this.maintenanceMode = maintenanceMode; }

    private String tenantId;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
