CREATE DATABASE IF NOT EXISTS moje_banka;
USE moje_banka;


CREATE TABLE IF NOT EXISTS accounts (
    acc_num INT PRIMARY KEY,
    balance BIGINT NOT NULL DEFAULT 0,
    owner_ip VARCHAR(45)
);