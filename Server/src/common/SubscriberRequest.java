package common;

import java.io.Serializable;

/**
 * SubscriberRequest
 * Carries the data needed to register (or upgrade) a traveler as
 * either a Club Member or a Tour Guide.
 *
 * familyMembers and creditCard are only relevant for Club Members.
 * For Tour Guides they are ignored (familyMembers = 0, creditCard = null).
 */
public class SubscriberRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TYPE_CLUB_MEMBER = "CLUB_MEMBER";
    public static final String TYPE_GUIDE = "GUIDE";

    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phoneNumber;
    private final String nationalId;
    private final String type; // TYPE_CLUB_MEMBER or TYPE_GUIDE

    // Club-member-only fields
    private final int familyMembers;   // total family members (subscriber + others), min 2
    private final String creditCard;   // optional, may be null

    public SubscriberRequest(String firstName, String lastName, String email,
                             String phoneNumber, String nationalId, String type,
                             int familyMembers, String creditCard) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.nationalId = nationalId;
        this.type = type;
        this.familyMembers = familyMembers;
        this.creditCard = creditCard;
    }

    public String getFirstName()   { return firstName; }
    public String getLastName()    { return lastName; }
    public String getEmail()       { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getNationalId()  { return nationalId; }
    public String getType()        { return type; }
    public int getFamilyMembers()  { return familyMembers; }
    public String getCreditCard()  { return creditCard; }
}