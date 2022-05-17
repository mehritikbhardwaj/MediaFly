package com.app.mediafly;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class AppInfoModel {

    @SerializedName("IsValidDevice")
    @Expose
    private Integer IsValidDevice;

    @SerializedName("version")
    @Expose
    private Integer version;

    @SerializedName("link")
    @Expose
    private String link;

    public Integer getIsValidDevice() {
        return IsValidDevice;
    }

    public void setIsValidDevice(Integer isValidDevice) {
        IsValidDevice = isValidDevice;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
