package common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ParkManagerActivityLogEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String requestId;
    private final String requestTitle;
    private final LocalDateTime requestDate;
    private final ParkChangeRequest.ParameterType parameterType;
    private final int newValue;
    private final Boolean approved;

    public ParkManagerActivityLogEntry(String requestId, String requestTitle,
                                       ParkChangeRequest.ParameterType parameterType,
                                       int newValue, Boolean approved) {
        this(requestId, requestTitle, null, parameterType, newValue, approved);
    }

    public ParkManagerActivityLogEntry(String requestId, String requestTitle,
                                       LocalDateTime requestDate,
                                       ParkChangeRequest.ParameterType parameterType,
                                       int newValue, Boolean approved) {
        this.requestId = requestId;
        this.requestTitle = requestTitle;
        this.requestDate = requestDate;
        this.parameterType = parameterType;
        this.newValue = newValue;
        this.approved = approved;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getRequestTitle() {
        return requestTitle;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public ParkChangeRequest.ParameterType getParameterType() {
        return parameterType;
    }

    public int getNewValue() {
        return newValue;
    }

    public Boolean getApproved() {
        return approved;
    }
}
