// AnalysisResult.java
package com.example.billboarddetector;

import com.google.gson.annotations.SerializedName;

public class AnalysisResult {

    @SerializedName("is_authorized")
    private boolean is_authorized;

    @SerializedName("reason")
    private String reason;

    // Add this constructor
    public AnalysisResult(boolean is_authorized, String reason) {
        this.is_authorized = is_authorized;
        this.reason = reason;
    }

    // Getter methods
    public boolean isAuthorized() {
        return is_authorized;
    }

    public String getReason() {
        return reason;
    }
}