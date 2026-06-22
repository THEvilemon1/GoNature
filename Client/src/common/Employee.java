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
    private String userId;

    // User fields (from user table)
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;

    public Employee(int employeeId, int salary, int parkId, String username,
                    String role, String userId, String firstName, String lastName,
                    String email, String phoneNumber) {
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
    }

    public int getEmployeeId()    { return employeeId; }
    public int getSalary()        { return salary; }
    public int getParkId()        { return parkId; }
    public String getUsername()   { return username; }
    public String getRole()       { return role; }
    public String getUserId()     { return userId; }
    public String getFirstName()  { return firstName; }
    public String getLastName()   { return lastName; }
    public String getEmail()      { return email; }
    public String getPhoneNumber(){ return phoneNumber; }

    @Override
    public String toString() {
        return "Employee{" + firstName + " " + lastName + ", role=" + role + "}";
    }
}