package gui.login;

import common.Employee;

/**
 * EmployeeAwareController
 * Implemented by all role screens that need to know which employee is logged in.
 */
public interface EmployeeAwareController {
    void setEmployee(Employee employee);
}