-- Insert statements for the "OrderSectors" table
INSERT INTO OpsSvcDb.OrderSectors (order_sector_id, order_sector) VALUES
(1, 'Personal'),
(2, 'Business'),
(3, 'Corporation'),
(4, 'Government'),
(5, 'Other');

-- Insert statements for the "OrderTypes" table
INSERT INTO OpsSvcDb.OrderTypes (order_type_id, order_type) VALUES
(1, 'Intake'),
(2, 'Output'),
(3, 'Demo');

-- Insert statements for the "BatteryStatus" table
INSERT INTO OpsSvcDb.BatteryStatus (battery_status_id, status) VALUES
(1, 'Intake'),
(2, 'Rejected'),
(3, 'Testing'),
(4, 'Refurb'),
(5, 'Storage'),
(6, 'Hold'),
(7, 'Shipping'),
(8, 'Received'),
(9, 'Destroyed'),
(10, 'Lost');

-- Insert statements for the "CustomerData" table
INSERT INTO OpsSvcDb.CustomerData (customer_id, contact_name, email, phone, address, loyalty_id) VALUES
(1, 'John Doe', 'jdoe@example.com', '(555)123-4567', '555 Elm St, Boulder, Colorado, USA', uuid_generate_v4()),
(2, 'Ender', 'endy@example.com', '(555)654-6969', '8754 85th N St, Fishers, Indiana, USA', uuid_generate_v4()),
(3, 'Sarah Jane', 'sjane@otherexample.com', '(555)987-6543', '111 First Ave, Austin, Texas, USA', uuid_generate_v4());