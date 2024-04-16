-- -----------------------------------------------------
-- Schema OpsSvcDb
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS OpsSvcDb;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS OpsSvcDb.OrderSectors (
  order_sector_id SERIAL PRIMARY KEY,
  order_sector VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS OpsSvcDb.OrderTypes (
  order_type_id SERIAL PRIMARY KEY,
  order_type VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS OpsSvcDb.BatteryStatus (
  battery_status_id SERIAL PRIMARY KEY,
  status VARCHAR(45) NOT NULL
);

CREATE TABLE IF NOT EXISTS OpsSvcDb.CustomerData (
  customer_id SERIAL PRIMARY KEY,
  contact_name VARCHAR(45) NOT NULL,
  email VARCHAR(45) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  address VARCHAR(45) NOT NULL,
  loyalty_id UUID NOT NULL
);

CREATE TABLE IF NOT EXISTS OpsSvcDb.HoldRecords (
  hold_id SERIAL PRIMARY KEY,
  hold_start_date TIMESTAMP NOT NULL,
  hold_end_date TIMESTAMP,
  hold_reason VARCHAR(45) NOT NULL
);

CREATE TABLE IF NOT EXISTS OpsSvcDb.OrderRecords (
  order_id SERIAL PRIMARY KEY,
  order_date TIMESTAMP NOT NULL,
  order_type_id INT NOT NULL,
  order_sector_id INT NOT NULL,
  customer_id INT NOT NULL,
  shipping_plan_id INT,
  completed BOOLEAN DEFAULT FALSE,
  notes VARCHAR(45),
  CONSTRAINT order_type_id FOREIGN KEY (order_type_id) REFERENCES OpsSvcDb.OrderTypes(order_type_id) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT order_sector_id FOREIGN KEY (order_sector_id) REFERENCES OpsSvcDb.OrderSectors(order_sector_id) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT customer_id FOREIGN KEY (customer_id) REFERENCES OpsSvcDb.CustomerData(customer_id) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE IF NOT EXISTS OpsSvcDb.BatteryInventory (
  battery_id SERIAL PRIMARY KEY,
  battery_status_id INT NOT NULL,
  battery_type_id INT NOT NULL,
  intake_order_id INT NOT NULL,
  refurb_plan_id INT,
  hold_id INT,
  output_order_id INT,
  CONSTRAINT battery_status_id FOREIGN KEY (battery_status_id) REFERENCES OpsSvcDb.BatteryStatus(battery_status_id) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT intake_order_id FOREIGN KEY (intake_order_id) REFERENCES OpsSvcDb.OrderRecords(order_id) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT output_order_id FOREIGN KEY (output_order_id) REFERENCES OpsSvcDb.OrderRecords(order_id) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT hold_id FOREIGN KEY (hold_id) REFERENCES OpsSvcDb.HoldRecords(hold_id) ON DELETE NO ACTION ON UPDATE NO ACTION
);