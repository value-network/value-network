mysql -f -u root <<EOF
drop schema if exists val_master;
CREATE DATABASE val_master CHARACTER SET = 'utf8mb4' COLLATE = 'utf8mb4_unicode_ci';
CREATE USER 'val_user'@'localhost' IDENTIFIED BY '';
GRANT ALL PRIVILEGES ON val_master.* TO 'val_user'@'localhost';
use val_master;
source init-mysql.sql;
EOF
