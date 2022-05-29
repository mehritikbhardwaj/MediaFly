package com.app.mediafly;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MediaModel {

    @SerializedName("eid")
    @Expose
    private Integer eid;

    @SerializedName("order")
    @Expose
    private Integer order;

    @SerializedName("duration")
    @Expose
    private Integer duration;

    @SerializedName("filename")
    @Expose
    private String filename;

    @SerializedName("format")
    @Expose
    private String format;

    @SerializedName("size")
    @Expose
    private String size;


    @SerializedName("qrcode")
    @Expose
    private String qrcode;

    @SerializedName("sdate")
    @Expose
    private String sdate;

    @SerializedName("stime")
    @Expose
    private String stime;

    @SerializedName("edate")
    @Expose
    private String edate;

    @SerializedName("etime")
    @Expose
    private String etime;

    @SerializedName("status")
    @Expose
    private String status;

    public Integer getEid() {
        return eid;
    }

    public void setEid(Integer eid) {
        this.eid = eid;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getSdate() {
        return sdate;
    }

    public void setSdate(String sdate) {
        this.sdate = sdate;
    }

    public String getStime() {
        return stime;
    }

    public void setStime(String stime) {
        this.stime = stime;
    }

    public String getEdate() {
        return edate;
    }

    public void setEdate(String edate) {
        this.edate = edate;
    }

    public String getEtime() {
        return etime;
    }

    public void setEtime(String etime) {
        this.etime = etime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getQrcode() {
        return qrcode;
    }

    public void setQrcode(String qrcode) {
        this.qrcode = qrcode;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }
}
