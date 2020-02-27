mysql -u val_user val_master <<EOF
set FOREIGN_KEY_CHECKS=0;
truncate TABLE account;
truncate TABLE block;
truncate TABLE global_parameter;
truncate TABLE peer;
truncate TABLE pledges;
truncate TABLE transaction;
truncate TABLE unconfirmed_transaction;
set FOREIGN_KEY_CHECKS=1;
EOF