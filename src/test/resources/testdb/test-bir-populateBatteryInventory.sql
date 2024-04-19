-- Clear the "BatteryInventory" TEST table
DELETE FROM OpsSvcSchema.BatteryInventory;

-- Insert statements for the "BatteryInventory" TEST table
INSERT INTO OpsSvcSchema.BatteryInventory (battery_id, battery_status_id, battery_type_id, intake_order_id, refurb_plan_id, hold_id, output_order_id) VALUES
---- Intake, no refurb/hold/output order
(1, 3, 1, 1, NULL, NULL, NULL),
(2, 3, 6, 1, 2, NULL, NULL),
(3, 3, 8, 1, 3, NULL, NULL),
(4, 2, 3, 2, 4, NULL, NULL);