package common;

import java.io.Serializable;

public class VisitorLoginResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String travelerId;
    private final String nationalId;
    private final boolean newVisitor;
    private final boolean guide;
    private final boolean clubMember;

    public VisitorLoginResult(String travelerId, String nationalId, boolean newVisitor) {
        this(travelerId, nationalId, newVisitor, false, false);
    }

    public VisitorLoginResult(String travelerId, String nationalId, boolean newVisitor, boolean guide) {
        this(travelerId, nationalId, newVisitor, guide, false);
    }

    public VisitorLoginResult(String travelerId, String nationalId, boolean newVisitor, boolean guide, boolean clubMember) {
        this.travelerId = travelerId;
        this.nationalId = nationalId;
        this.newVisitor = newVisitor;
        this.guide = guide;
        this.clubMember = clubMember;
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

    public boolean isClubMember() {
        return clubMember;
    }
}
