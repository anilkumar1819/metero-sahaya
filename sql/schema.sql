-- Metro Sahaya : database schema (H2 / standard SQL)
CREATE TABLE lines (
  id       INT PRIMARY KEY,
  name     VARCHAR(40) NOT NULL,
  name_hi  VARCHAR(60),
  name_kn  VARCHAR(60),
  color    VARCHAR(9)  NOT NULL
);

CREATE TABLE stations (
  id        INT AUTO_INCREMENT PRIMARY KEY,
  name      VARCHAR(80) NOT NULL UNIQUE,
  landmark  VARCHAR(200) DEFAULT ''
);

-- ordered list of stations on each line (seq = position on the line)
CREATE TABLE line_stations (
  line_id     INT NOT NULL REFERENCES lines(id),
  seq         INT NOT NULL,
  station_id  INT NOT NULL REFERENCES stations(id),
  PRIMARY KEY (line_id, seq),
  UNIQUE (line_id, station_id)
);

-- a station that appears on more than one line is an interchange
CREATE VIEW interchanges AS
  SELECT station_id FROM line_stations GROUP BY station_id HAVING COUNT(*) > 1;

CREATE TABLE fare_slabs (
  min_stops INT PRIMARY KEY,
  max_stops INT NOT NULL,
  fare      INT NOT NULL
);

-- step-by-step "how to use the metro" guide, in 3 languages
CREATE TABLE guide_steps (
  lang     VARCHAR(2) NOT NULL,
  step_no  INT NOT NULL,
  title    VARCHAR(120) NOT NULL,
  body     VARCHAR(500) NOT NULL,
  PRIMARY KEY (lang, step_no)
);

CREATE TABLE tickets (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  code            VARCHAR(12) NOT NULL UNIQUE,
  from_station    INT NOT NULL REFERENCES stations(id),
  to_station      INT NOT NULL REFERENCES stations(id),
  passengers      INT NOT NULL CHECK (passengers BETWEEN 1 AND 6),
  total_fare      INT NOT NULL,
  contact_name    VARCHAR(80) DEFAULT '' NOT NULL,
  phone           VARCHAR(10) DEFAULT '' NOT NULL,
  payment_method  VARCHAR(10) DEFAULT 'CASH' NOT NULL,   -- UPI / CARD / CASH
  status          VARCHAR(20) DEFAULT 'PAID' NOT NULL,   -- PAID / PAY_AT_COUNTER
  travel_date     DATE DEFAULT CURRENT_DATE NOT NULL,
  created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- one row per traveller on a ticket
CREATE TABLE passengers (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  ticket_id  INT NOT NULL REFERENCES tickets(id),
  name       VARCHAR(60) NOT NULL,
  age        INT NOT NULL CHECK (age BETWEEN 0 AND 120),
  gender     CHAR(1) NOT NULL CHECK (gender IN ('M','F','O')),
  fare       INT NOT NULL
);
