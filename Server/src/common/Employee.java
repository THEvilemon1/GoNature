package common;

import java.io.Serializable;

public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;

    // Roles
    public static final String ROLE_PARK_WORKER = "park_worker";
    public static final String ROLE_PARK_MANAGER = "park_manager";
    public static final String ROLE_DEPARTMENT_MANAGER = "department_manager";
    public static final String ROLE_SERVICE_REP = "service_rep";

    private int employeeId;
    private int salary;
    private int parkId;
    private String username;
    private String role;
    private int userId;

    // User fields (from user table)
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Integer parkMaxCapacity;
    private Integer parkGap;
    private Integer parkDefaultStayTime;

    public Employee(int employeeId, int salary, int parkId, String username,
                    String role, int userId, String firstName, String lastName,
                    String email, String phoneNumber) {
        this(employeeId, salary, parkId, username, role, userId, firstName, lastName,
            email, phoneNumber, null, null, null);
    }

    public Employee(int employeeId, int salary, int parkId, String username,
                    String role, int userId, String firstName, String lastName,
                    String email, String phoneNumber, Integer parkMaxCapacity,
                    Integer parkGap, Integer parkDefaultStayTime) {
        this.employeeId = employeeId;
        this.salary = salary;
        this.parkId = parkId;
        this.username = username;
        this.role = role;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.parkMaxCapacity = parkMaxCapacity;
        this.parkGap = parkGap;
        this.parkDefaultStayTime = parkDefaultStayTime;
    }

    public int getEmployeeId()    { return employeeId; }
    public int getSalary()        { return salary; }
    public int getParkId()        { return parkId; }
    public String getUsername()   { return username; }
    public String getRole()       { return role; }
    public int getUserId()        { return userId; }
    public String getFirstName()  { return firstName; }
    public String getLastName()   { return lastName; }
    public String getEmail()      { return email; }
    public String getPhoneNumber(){ return phoneNumber; }
    public Integer getParkMaxCapacity()     { return parkMaxCapacity; }
    public Integer getParkGap()             { return parkGap; }
    public Integer getParkDefaultStayTime() { return parkDefaultStayTime; }

    @Override
    public String toString() {
        return "Employee{" + firstName + " " + lastName + ", role=" + role + "}";
    }
}
