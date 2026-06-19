package common;

import java.io.Serializable;

public class VisitorLoginResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String travelerId;
    private final String nationalId;
    private final boolean newVisitor;
    private final boolean guide;

    public VisitorLoginResult(String travelerId, String nationalId, boolean newVisitor) {
        this(travelerId, nationalId, newVisitor, false);
    }

    public VisitorLoginResult(String travelerId, String nationalId, boolean newVisitor, boolean guide) {
        this.travelerId = travelerId;
        this.nationalId = nationalId;
        this.newVisitor = newVisitor;
        this.guide = guide;
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

    public boolean isGuide() {
        return guide;
    }
}
