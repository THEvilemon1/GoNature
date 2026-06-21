package common;

import java.io.Serializable;

public class ParkOption implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String name;
    private final int price;

    public ParkOption(int id, String name, int price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return name;
    }
}
