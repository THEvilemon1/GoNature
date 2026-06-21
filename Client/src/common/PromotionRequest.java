package common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PromotionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum ActionType { ADD, UPDATE, DELETE }

    private String requestId;
    private ActionType actionType;
    private int promotionId;     // 0 for ADD, existing id for UPDATE/DELETE
    private int parkId;
    private int promoCode;
    private int percentage;
    private LocalDateTime endDate;
    private String description;
    private String requestedByUsername;
    private int employeeId;
    private String requestTitle;

    public PromotionRequest(String requestId, ActionType actionType, int promotionId,
                            int parkId, int promoCode, int percentage, LocalDateTime endDate,
                            String description, String requestedByUsername, int employeeId,
                            String requestTitle) {
        this.requestId = requestId;
        this.actionType = actionType;
        this.promotionId = promotionId;
        this.parkId = parkId;
        this.promoCode = promoCode;
        this.percentage = percentage;
        this.endDate = endDate;
        this.description = description;
        this.requestedByUsername = requestedByUsername;
        this.employeeId = employeeId;
        this.requestTitle = requestTitle;
    }

    public String getRequestId()            { return requestId; }
    public ActionType getActionType()       { return actionType; }
    public int getPromotionId()             { return promotionId; }
    public int getParkId()                  { return parkId; }
    public int getPromoCode()               { return promoCode; }
    public int getPercentage()              { return percentage; }
    public LocalDateTime getEndDate()       { return endDate; }
    public String getDescription()          { return description; }
    public String getRequestedByUsername()  { return requestedByUsername; }
    public int getEmployeeId()              { return employeeId; }
    public String getRequestTitle()         { return requestTitle; }
}