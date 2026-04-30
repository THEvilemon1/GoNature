GoNature System

The GoNature system is a park management platform that brings together everything needed to run and manage park activities efficiently. It handles bookings, traveler registration, employee management, and report generation in one place. The system is built using a structured architectural approach (similar to MVC/BCE) and relies on the OCSF (Open Client-Server Framework) for communication between the client and server.

System Architecture

GoNature follows a classic client-server architecture:

Client Side:
The client is implemented using GoNatureClient, which extends AbstractClient. The user interface is built with JavaFX, providing an interactive experience for users.
Server Side:
The server is based on GoNatureServer, extending AbstractServer. It manages client connections through ConnectionToClient and handles all core system logic.
Database:
The system uses JDBC to communicate with the database, storing and retrieving data for all main components like users, bookings, and parks.

User Roles:

The system supports different types of users, organized through inheritance:
Traveler: Regular users who book visits to parks. This group includes both general visitors and club members.
Employee: Staff members who manage park operations.
Manager: Has access to advanced features such as viewing and generating reports.
Guide: A specialized role that can overlap with traveler-related functionality in some cases.


Key Modules & Functionalities
1. Booking Management
This part of the system is handled by BookingBoundary, BookingController, and the Booking entity.
Users can create, confirm, or cancel bookings as part of the normal reservation flow.
If a park reaches full capacity, bookings can be moved to a waiting list, managed by the WaitingListController.
Pricing is calculated through the PaymentController, which applies promotions and generates billing details.

2. Park Configuration
Managed by ParkConfigurationController along with the Park entity.
Keeps track of important values like maximum capacity, current number of visitors, and available space.
Allows updates to park settings such as default visit duration and price per visitor.

3. Authentication & Access Control
LoginController and LoginBoundary handle user authentication and login.
EmployeeController is responsible for managing employee-related actions, including registering guides and club members.

4. Reporting System
The reporting module is designed using the Strategy Pattern, making it flexible and easy to extend.
ReportController selects the appropriate strategy for generating reports.
ReportStrategy defines a common interface with a generate() method.
Different implementations like CancelReport and VisitorsReport provide specific report types.
Reports can be generated in multiple formats, such as plain text, graphs, or JSON.


Notifications
Managed by the NotificationsController.
The system can send automatic updates to users via email or SMS, keeping them informed about bookings and changes.


Class Structure Overview
Boundaries: Handle user interaction (e.g., BookingBoundary, ManagerBoundary).
Controllers: Contain the main business logic (e.g., ReportController, PaymentController).
Entities: Represent the core data of the system (e.g., User, Park, Booking, Promotion).
