package common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Promotion implements Serializable {
    private static final long serialVersionUID = 1L;

    private int promotionId;
    private int parkId;
    private int promoCode;
    private int percentage;
    private LocalDateTime endDate;
    private String description;

    public Promotion(int promotionId, int parkId, int promoCode, int percentage,
                      LocalDateTime endDate, String description) {
        this.promotionId = promotionId;
        this.parkId = parkId;
        this.promoCode = promoCode;
        this.percentage = percentage;
        this.endDate = endDate;
        this.description = description;
    }

    public int getPromotionId()       { return promotionId; }
    public int getParkId()            { return parkId; }
    public int getPromoCode()         { return promoCode; }
    public int getPercentage()        { return percentage; }
    public LocalDateTime getEndDate() { return endDate; }
    public String getDescription()    { return description; }

    @Override
    public String toString() {
        return "Promo #" + promoCode + " — " + percentage + "% off — " + description;
    }
}