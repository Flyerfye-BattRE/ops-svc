-- Clear the "OrderRecords" TEST table
DELETE FROM OpsSvcSchema.OrderRecords;

-- Insert statements for the "OrderRecords" TEST table
INSERT INTO OpsSvcSchema.OrderRecords (order_id, order_date, order_type_id, order_sector_id, customer_id, shipping_plan_id, completed, notes) VALUES
(1, '2024-04-17 08:15:00', 1, 2, 1, NULL, FALSE, 'Clearing of old stock'),
(2, '2024-04-18 10:30:00', 1, 2, 1, NULL, FALSE, 'Urgent order needed for upcoming event'),
(3, '2024-04-18 11:30:00', 1, 2, 1, NULL, FALSE, 'Low priority order needed for upcoming event'),
(4, '2024-04-19 09:45:00', 2, 3, 1, NULL, TRUE, 'Restocking inventory after clearance sale');
