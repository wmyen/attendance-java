INSERT IGNORE INTO leave_types (name, code, is_paid, requires_doc) VALUES
('特休', 'ANNUAL', true, false),
('事假', 'PERSONAL', false, false),
('病假', 'SICK', false, true),
('喪假', 'BEREAVEMENT', true, true),
('補休', 'COMPENSATORY', true, false);
