INSERT INTO users (id, user_name, password, email)
VALUES (1, 'customer', 'b53e6d2070d285f7d8b0a6b9ac2ec912', 'customer@example.com')
ON DUPLICATE KEY UPDATE password = VALUES(password), email = VALUES(email);

INSERT INTO users (id, user_name, password, email)
VALUES (2, 'admin', '21232f297a57a5a743894a0e4a801fc3', 'admin@example.com')
ON DUPLICATE KEY UPDATE password = VALUES(password), email = VALUES(email);

INSERT INTO users (id, user_name, password, email)
VALUES (3, 'customer2', 'b53e6d2070d285f7d8b0a6b9ac2ec912', 'admin2@example.com')
ON DUPLICATE KEY UPDATE password = VALUES(password), email = VALUES(email);


