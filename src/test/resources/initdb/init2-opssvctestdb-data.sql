-- Insert statements for the "OrderSectors" table
INSERT INTO OpsSvcSchema.OrderSectors (order_sector_id, order_sector)
SELECT * FROM (VALUES
	(1, 'Personal'),
	(2, 'Business'),
	(3, 'Corporation'),
	(4, 'Government'),
	(5, 'Other')
) AS v (order_sector_id, order_sector)
WHERE NOT EXISTS (
    SELECT 1 FROM OpsSvcSchema.OrderSectors
);

-- Insert statements for the "OrderTypes" table
INSERT INTO OpsSvcSchema.OrderTypes (order_type_id, order_type)
SELECT * FROM (VALUES
	(1, 'Intake'),
	(2, 'Output'),
	(3, 'Demo')
) AS v (order_type_id, order_type)
WHERE NOT EXISTS (
    SELECT 1 FROM OpsSvcSchema.OrderTypes
);

-- Insert statements for the "BatteryStatus" table
INSERT INTO OpsSvcSchema.BatteryStatus (battery_status_id, status)
SELECT * FROM (VALUES
	(0, 'UNKNOWN'),
	(1, 'INTAKE'),
	(2, 'REJECTED'),
	(3, 'TESTING'),
	(4, 'REFURB'),
	(5, 'STORAGE'),
	(6, 'HOLD'),
	(7, 'SHIPPING'),
	(8, 'RECEIVED'),
	(9, 'DESTROYED'),
	(10, 'LOST')
) AS v (battery_status_id, status)
WHERE NOT EXISTS (
    SELECT 1 FROM OpsSvcSchema.BatteryStatus
);

-- Insert statements for the "CustomerData" table
INSERT INTO OpsSvcSchema.CustomerData (customer_id, contact_name, email, phone, address, loyalty_id)
SELECT 1, 'Test Customer', 'customer@test.com', '(555)555-5555', '555 Test Place, City, State, USA', RANDOM_UUID()
WHERE NOT EXISTS (
    SELECT 1 FROM OpsSvcSchema.CustomerData
);