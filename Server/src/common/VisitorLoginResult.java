package common;

import java.io.Serializable;

public class VisitorLoginResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String travelerId;
    private final String nationalId;
    private final boolean newVisitor;

    public VisitorLoginResult(String travelerId, String nationalId, boolean newVisitor) {
        this.travelerId = travelerId;
        this.nationalId = nationalId;
        this.newVisitor = newVisitor;
    }

    public String getTravelerId() {
        return travelerId;
    }

    public String getNationalId() {
        return nationalId;
    }

    public boolean isNewVisitor() {
        return newVisitor;
    }
    
    
}
