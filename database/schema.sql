-- ============================================================================
--  GoNature — National Parks Reservation System
--  MySQL schema (reverse-engineered from the server-side SQL in the codebase)
--
--  Conventions chosen for consistency:
--    * Surrogate UUID keys (booking, waiting list, requests)  -> VARCHAR(36)
--    * The shared person identity (user_id / traveler_id)     -> VARCHAR(20)
--      (travelers log in with their national-ID string, and booking.traveler_id
--       joins straight onto user.user_id, so one string type is used everywhere)
--    * Money (price, pricePerPerson)                          -> DECIMAL(10,2)
--    * Yes/No flags                                           -> BOOLEAN (TINYINT(1))
--    * Fixed value sets (role, status, ...)                   -> ENUM (self-documenting)
--    * Dates with a time component                            -> DATETIME
--
--  Run order matters because of foreign keys: parents first, children after.
-- ============================================================================

-- CREATE DATABASE gonature CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE gonature;


-- ----------------------------------------------------------------------------
--  user
--  One row per person in the system (both travelers and employees).
--  username / password live here and are only filled in for employees;
--  travelers have them NULL because they log in with their national ID.
-- ----------------------------------------------------------------------------
CREATE TABLE `user` (
    user_id      VARCHAR(20)  NOT NULL,
    username     VARCHAR(50)  NULL,
    password     VARCHAR(100) NULL,
    firstName    VARCHAR(50)  NOT NULL,
    lastName     VARCHAR(50)  NOT NULL,
    email        VARCHAR(100) NULL,
    phoneNumber  VARCHAR(20)  NULL,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_user_username (username)
) ENGINE=InnoDB;


-- ----------------------------------------------------------------------------
--  park
--  One row per national park. currentVisitors is the live headcount that
--  check-in / check-out keep up to date. The "effective capacity" used when
--  booking is (maxCapacity - gap).
-- ----------------------------------------------------------------------------
CREATE TABLE park (
    park_id               INT           NOT NULL,
    name                  VARCHAR(100)  NOT NULL,
    currentVisitors       INT           NOT NULL DEFAULT 0,
    maxCapacity           INT           NOT NULL,
    gap                   INT           NOT NULL DEFAULT 0,
    defaultStayTime       INT           NOT NULL DEFAULT 240,   -- minutes
    pricePerPerson        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    department_manager_id INT           NULL,
    PRIMARY KEY (park_id)
) ENGINE=InnoDB;
-- FK added after employee table to avoid circular dependency:
--   ALTER TABLE park ADD CONSTRAINT fk_park_dep_manager
--       FOREIGN KEY (department_manager_id) REFERENCES employee(employee_id) ON DELETE SET NULL;


-- ----------------------------------------------------------------------------
--  traveler
--  Extra data for people who book visits. traveler_id == user_id (same string);
--  both columns are kept because the code reads/writes both.
-- ----------------------------------------------------------------------------
CREATE TABLE traveler (
    traveler_id   VARCHAR(20) NOT NULL,
    nationalId    INT         NOT NULL,
    guide         BOOLEAN     NOT NULL DEFAULT FALSE,
    clubMember    BOOLEAN     NOT NULL DEFAULT FALSE,
    user_id       VARCHAR(20) NOT NULL,
    familyMembers INT         NULL,
    creditCard    VARCHAR(30) NULL,
    PRIMARY KEY (traveler_id),
    UNIQUE KEY uq_traveler_nationalId (nationalId),
    CONSTRAINT fk_traveler_user
        FOREIGN KEY (user_id) REFERENCES `user`(user_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;


-- ----------------------------------------------------------------------------
--  employee
--  Staff accounts. The login credentials are in `user`; this table holds the
--  job data (role, salary, which park, and a link to the department manager
--  via requests). role drives which screen the client opens after login.
-- ----------------------------------------------------------------------------
CREATE TABLE employee (
    employee_id INT         NOT NULL,
    user_id     VARCHAR(20) NOT NULL,
    salary      INT         NULL,
    park_id     INT         NULL,
    role        ENUM('park_worker','park_manager','department_manager','service_rep') NOT NULL,
    PRIMARY KEY (employee_id),
    CONSTRAINT fk_employee_user
        FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_employee_park
        FOREIGN KEY (park_id) REFERENCES park(park_id) ON DELETE SET NULL
) ENGINE=InnoDB;
-- ----------------------------------------------------------------------------
--  booking
--  The central table. A booking moves through the statuses listed below as the
--  traveler books, confirms, checks in and checks out (or cancels / expires).
--  The traveler* columns snapshot the contact details at booking time; the
--  notification/lifecycle columns at the bottom drive the reminder scheduler.
-- ----------------------------------------------------------------------------
CREATE TABLE booking (
    booking_id            INT   NOT NULL,
    traveler_id           VARCHAR(20)   NULL,
    travelerName          VARCHAR(100)  NULL,
    travelerEmail         VARCHAR(100)  NULL,
    travelerPhoneNumber   VARCHAR(20)   NULL,
    park_id               INT           NULL,
    numberOfVisitors      INT           NOT NULL,
    visitorTime           DATETIME      NOT NULL,
    status                ENUM('PENDING','CONFIRMED','WAITING_LIST',
                               'PENDING_WAITLIST_CONFIRMATION','PENDING_REMINDER_CONFIRMATION',
                               'CANCELLED','CHECKED_IN','CHECKED_OUT','SYSTEM_CANCEL') NOT NULL,
    organizedBooking      BOOLEAN       NOT NULL DEFAULT FALSE,
    price                 DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    paid                  BOOLEAN       NOT NULL DEFAULT FALSE,
    visitorsInside        INT           NOT NULL DEFAULT 0,
    entryTime             DATETIME      NULL,
    exitTime              DATETIME      NULL,
    -- reminder / waitlist lifecycle bookkeeping
    action_required_at    DATETIME      NULL,
    action_deadline       DATETIME      NULL,
    reminder_sent_at      DATETIME      NULL,
    last_notification_type VARCHAR(50)  NULL,
    PRIMARY KEY (booking_id),
    KEY idx_booking_traveler (traveler_id),
    KEY idx_booking_slot (park_id, visitorTime),
    KEY idx_booking_status (status),
    CONSTRAINT fk_booking_traveler
        FOREIGN KEY (traveler_id) REFERENCES `user`(user_id) ON DELETE SET NULL,
    CONSTRAINT fk_booking_park
        FOREIGN KEY (park_id) REFERENCES park(park_id) ON DELETE SET NULL
) ENGINE=InnoDB;


-- ----------------------------------------------------------------------------
--  WaitingList
--  One row per (park, time slot) that has at least one waiting traveler.
--  status is 'OPEN' while the slot is still managed.
-- ----------------------------------------------------------------------------
CREATE TABLE WaitingList (
    waitingList_id VARCHAR(36) NOT NULL,
    park_id        INT         NOT NULL,
    slot_time      DATETIME    NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (waitingList_id),
    UNIQUE KEY uq_waitlist_slot (park_id, slot_time),
    CONSTRAINT fk_waitlist_park
        FOREIGN KEY (park_id) REFERENCES park(park_id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- ----------------------------------------------------------------------------
--  WaitingListEntry
--  A single booking's place in a slot's waiting list. status goes
--  WAITING -> OFFERED/PENDING -> CONFIRMED or CANCELLED as spots free up.
--  Ordered by registered_at so the earliest waiter is offered a freed spot first.
-- ----------------------------------------------------------------------------
CREATE TABLE WaitingListEntry (
    id             VARCHAR(36) NOT NULL,
    waitingList_id VARCHAR(36) NOT NULL,
    booking_id     INT NOT NULL,
    registered_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status         VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_wle_list (waitingList_id),
    KEY idx_wle_booking (booking_id),
    CONSTRAINT fk_wle_list
        FOREIGN KEY (waitingList_id) REFERENCES WaitingList(waitingList_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_wle_booking
        FOREIGN KEY (booking_id) REFERENCES booking(booking_id)
        ON DELETE CASCADE
) ENGINE=InnoDB;


-- ----------------------------------------------------------------------------
--  report
--  Reports a park manager submits up to the department manager.
-- ----------------------------------------------------------------------------
CREATE TABLE report (
    report_id   INT          NOT NULL AUTO_INCREMENT,
    park_id     INT          NOT NULL,
    reportTitle VARCHAR(150) NOT NULL,
    content     TEXT         NULL,
    employee_id INT          NOT NULL,
    PRIMARY KEY (report_id),
    CONSTRAINT fk_report_park
        FOREIGN KEY (park_id) REFERENCES park(park_id),
    CONSTRAINT fk_report_employee
        FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
) ENGINE=InnoDB;


-- ----------------------------------------------------------------------------
--  managerRequests
--  A park manager's request to change a park parameter (capacity, gap, etc.).
--  approved is NULL while pending, 1 if approved, 0 if rejected.
--  On approval the server copies new_value into the matching park column.
-- ----------------------------------------------------------------------------
CREATE TABLE managerRequests (
    request_Id     VARCHAR(36) NOT NULL,
    employee_id    INT         NOT NULL,
    dep_manager_id INT         NULL,
    requestTitle   VARCHAR(150) NOT NULL,
    parameter_type ENUM('MAX_CAPACITY','GAP','DEFAULT_STAY_TIME','PRICE_PER_PERSON') NOT NULL,
    new_value      INT         NOT NULL,
    park_id        INT         NOT NULL,
    approved       TINYINT(1)  NULL,
    PRIMARY KEY (request_Id),
    KEY idx_mr_pending (park_id, approved),
    CONSTRAINT fk_mr_employee
        FOREIGN KEY (employee_id) REFERENCES employee(employee_id),
    CONSTRAINT fk_mr_depmanager
        FOREIGN KEY (dep_manager_id) REFERENCES employee(employee_id),
    CONSTRAINT fk_mr_park
        FOREIGN KEY (park_id) REFERENCES park(park_id)
) ENGINE=InnoDB;


-- ----------------------------------------------------------------------------
--  promotion
--  An active discount for a park.
-- ----------------------------------------------------------------------------
CREATE TABLE promotion (
    promotion_id INT          NOT NULL AUTO_INCREMENT,
    park_id      INT          NOT NULL,
    promo_code   INT          NOT NULL,
    percentage   INT          NOT NULL,
    endDate      DATETIME     NULL,
    description  VARCHAR(255) NULL,
    PRIMARY KEY (promotion_id),
    KEY idx_promo_park (park_id),
    CONSTRAINT fk_promo_park
        FOREIGN KEY (park_id) REFERENCES park(park_id)
) ENGINE=InnoDB;


-- ----------------------------------------------------------------------------
--  promotion_request
--  A park manager's request to ADD / UPDATE / DELETE a promotion.
--  approved is NULL while pending. On approval the server applies the action
--  to the promotion table. promotion_id is 0 for ADD, the target id otherwise.
-- ----------------------------------------------------------------------------
CREATE TABLE promotion_request (
    request_id   VARCHAR(36)  NOT NULL,
    employee_id  INT          NOT NULL,
    action_type  ENUM('ADD','UPDATE','DELETE') NOT NULL,
    promotion_id INT          NOT NULL DEFAULT 0,
    park_id      INT          NOT NULL,
    promo_code   INT          NULL,
    percentage   INT          NULL,
    endDate      DATETIME     NULL,
    description  VARCHAR(255) NULL,
    approved     TINYINT(1)   NULL,
    PRIMARY KEY (request_id),
    KEY idx_pr_pending (park_id, approved),
    CONSTRAINT fk_pr_employee
        FOREIGN KEY (employee_id) REFERENCES employee(employee_id),
    CONSTRAINT fk_pr_park
        FOREIGN KEY (park_id) REFERENCES park(park_id)
) ENGINE=InnoDB;


-- ============================================================================
--  Sample seed data (handy for testing logins and the booking flow)
-- ============================================================================

INSERT INTO park (park_id, name, currentVisitors, maxCapacity, gap, defaultStayTime, pricePerPerson) VALUES
    (1, 'Banias Nature Reserve',  0, 300, 30, 240, 50.00),
    (2, 'Masada National Park',   0, 500, 50, 240, 65.00),
    (3, 'Ein Gedi Reserve',       0, 250, 25, 180, 45.00);

-- Employees (login via username/password in the user table)
INSERT INTO `user` (user_id, username, password, firstName, lastName, email, phoneNumber) VALUES
    ('1001', 'worker1',  '1234', 'Dana',  'Cohen',  'dana@gonature.local',  '0500000001'),
    ('1002', 'pmanager', '1234', 'Ronen', 'Levi',   'ronen@gonature.local', '0500000002'),
    ('1003', 'depmgr',   '1234', 'Sara',  'Mizrahi','sara@gonature.local',  '0500000003'),
    ('1004', 'service',  '1234', 'Omer',  'Katz',   'omer@gonature.local',  '0500000004');

INSERT INTO employee (employee_id, user_id, salary, park_id, role) VALUES
    (1, '1001', 9000,  1, 'park_worker'),
    (2, '1002', 14000, 1, 'park_manager'),
    (3, '1003', 20000, 1, 'department_manager'),
    (4, '1004', 8500,  1, 'service_rep');

-- Link all parks to the department manager (employee_id=3)
UPDATE park SET department_manager_id = 3;

-- A traveler (logs in with national ID 123456789)
INSERT INTO `user` (user_id, firstName, lastName, email, phoneNumber) VALUES
    ('123456789', 'Yossi', 'Israeli', 'yossi@example.com', '0521234567');

INSERT INTO traveler (traveler_id, nationalId, guide, clubMember, user_id, familyMembers, creditCard) VALUES
    ('123456789', 123456789, FALSE, TRUE, '123456789', 3, NULL);


-- ============================================================================
--  APPENDIX — legacy `Order` table
--  Referenced by getAllOrders / getOrder / updateOrder in ParkServer, but it
--  belongs to the early prototype and is not part of the live booking flow.
--  Kept here only so those queries don't fail if that code path is exercised.
-- ===================-- =========================================================
-- CREATE TABLE `Order` (
--     order_number         INT  NOT NULL,
--     order_date           DATE NULL,
--     number_of_visitors   INT  NULL,
--     confirmation_code    INT  NULL,
--     subscriber_id        INT  NULL,
--     date_of_placing_order DATE NULL,
--     PRIMARY KEY (order_number)
-- ) ENGINE=InnoDB;
