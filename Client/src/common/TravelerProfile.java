package common;

import java.io.Serializable;

public class TravelerProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String travelerId;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phoneNumber;
    private final boolean clubMember;

    public TravelerProfile(String travelerId, String firstName, String lastName,
                           String email, String phoneNumber, boolean clubMember) {
        this.travelerId = travelerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.clubMember = clubMember;
    }

    public String getTravelerId() { return travelerId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public boolean isClubMember() { return clubMember; }

    public String getFullName() {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        return (first + " " + last).trim();
    }
}
