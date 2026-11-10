-- Upgrades a database created by version 1 (adds passenger details + payment info)
ALTER TABLE tickets ADD COLUMN contact_name   VARCHAR(80) DEFAULT '' NOT NULL;
ALTER TABLE tickets ADD COLUMN phone          VARCHAR(10) DEFAULT '' NOT NULL;
ALTER TABLE tickets ADD COLUMN payment_method VARCHAR(10) DEFAULT 'CASH' NOT NULL;
ALTER TABLE tickets ADD COLUMN status         VARCHAR(20) DEFAULT 'PAID' NOT NULL;
ALTER TABLE tickets ADD COLUMN travel_date    DATE DEFAULT CURRENT_DATE NOT NULL;

CREATE TABLE passengers (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  ticket_id  INT NOT NULL REFERENCES tickets(id),
  name       VARCHAR(60) NOT NULL,
  age        INT NOT NULL CHECK (age BETWEEN 0 AND 120),
  gender     CHAR(1) NOT NULL CHECK (gender IN ('M','F','O')),
  fare       INT NOT NULL
);
