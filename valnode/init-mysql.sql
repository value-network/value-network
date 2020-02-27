/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `account`
--

DROP TABLE IF EXISTS `account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `account` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `creation_height` int(11) NOT NULL,
  `public_key` varbinary(32) DEFAULT NULL,
  `key_height` int(11) DEFAULT NULL,
  `balance` bigint(20) NOT NULL,
  `unconfirmed_balance` bigint(20) NOT NULL,
  `forged_balance` bigint(20) NOT NULL,
  `pledge_reward_balance` bigint(20) NOT NULL DEFAULT 0,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `total_pledged` bigint(20) NOT NULL DEFAULT '0',
  `account_role` INT NOT NULL DEFAULT 0,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `account_id_height_idx` (`id`,`height`),
  KEY `account_id_balance_height_idx` (`id`,`balance`,`height`),
  KEY `account_id_latest_idx` (`id`,`latest`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


-- --
-- -- Table structure for table `transaction`
-- --

DROP TABLE IF EXISTS `transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `transaction` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `deadline` smallint(6) NOT NULL,
  `sender_public_key` varbinary(32) NOT NULL,
  `recipient_id` bigint(20) DEFAULT NULL,
  `amount` bigint(20) NOT NULL,
  `fee` bigint(20) NOT NULL,
  `height` int(11) NOT NULL,
  `block_id` bigint(20) NOT NULL,
  `signature` varbinary(64) DEFAULT NULL,
  `timestamp` int(11) NOT NULL,
  `type` tinyint(4) NOT NULL,
  `subtype` tinyint(4) NOT NULL,
  `sender_id` bigint(20) NOT NULL,
  `block_timestamp` int(11) NOT NULL,
  `full_hash` varbinary(32) NOT NULL,
  `referenced_transaction_fullhash` varbinary(32) DEFAULT NULL,
  `attachment_bytes` blob,
  `version` tinyint(4) NOT NULL,
  `has_message` tinyint(1) NOT NULL DEFAULT '0',
  `has_encrypted_message` tinyint(1) NOT NULL DEFAULT '0',
  `has_public_key_announcement` tinyint(1) NOT NULL DEFAULT '0',
  `ec_block_height` int(11) DEFAULT NULL,
  `ec_block_id` bigint(20) DEFAULT NULL,
  `has_encrypttoself_message` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `transaction_id_idx` (`id`),
  UNIQUE KEY `transaction_full_hash_idx` (`full_hash`),
  KEY `transaction_block_timestamp_idx` (`block_timestamp`),
  KEY `transaction_sender_id_idx` (`sender_id`),
  KEY `transaction_recipient_id_idx` (`recipient_id`),
  KEY `transaction_recipient_id_amount_height_idx` (`recipient_id`,`amount`,`height`),
  KEY `constraint_ff` (`block_id`),
  CONSTRAINT `constraint_ff` FOREIGN KEY (`block_id`) REFERENCES `block` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `unconfirmed_transaction`
--

DROP TABLE IF EXISTS `unconfirmed_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `unconfirmed_transaction` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `expiration` int(11) NOT NULL,
  `transaction_height` int(11) NOT NULL,
  `fee_per_byte` bigint(20) NOT NULL,
  `timestamp` int(11) NOT NULL,
  `transaction_bytes` blob NOT NULL,
  `height` int(11) NOT NULL,
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `unconfirmed_transaction_id_idx` (`id`),
  KEY `unconfirmed_transaction_height_fee_timestamp_idx` (`transaction_height`,`fee_per_byte`,`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pledges`
--

DROP TABLE IF EXISTS `pledges`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pledges` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `account_id` bigint(20) NOT NULL,
  `recip_id` bigint(20) NOT NULL,
  `pledge_total` bigint(20) DEFAULT NULL,
  `pledge_latest_time` bigint(20) DEFAULT NULL,
  `unpledge_total` bigint(20) DEFAULT NULL,
  `withdraw_time` bigint(20) DEFAULT NULL,
  `height` int(11) NOT NULL,
  `latest` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `unique_id_height` (`id`,`height`),
  KEY `idx_sender_height` (`account_id`,`height`),
  KEY `idx_reciper_height` (`recip_id`,`height`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `block`
--

DROP TABLE IF EXISTS `block`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `block` (
  `db_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `timestamp` int(11) NOT NULL,
  `previous_block_id` bigint(20) DEFAULT NULL,
  `total_amount` bigint(20) NOT NULL,
  `total_fee` bigint(20) NOT NULL,
  `payload_length` int(11) NOT NULL,
  `generator_public_key` varbinary(32) NOT NULL,
  `previous_block_hash` varbinary(32) DEFAULT NULL,
  `cumulative_difficulty` blob NOT NULL,
  `base_target` bigint(20) NOT NULL,
  `next_block_id` bigint(20) DEFAULT NULL,
  `height` int(11) NOT NULL,
  `generation_signature` varbinary(64) NOT NULL,
  `block_signature` varbinary(64) NOT NULL,
  `payload_hash` varbinary(32) NOT NULL,
  `generator_id` bigint(20) NOT NULL,
  `nonce` bigint(20) NOT NULL,
  `pool_id` bigint(20) NOT NULL,
  `forge_reward` bigint(20) NOT NULL,
  `ats` blob,
  PRIMARY KEY (`db_id`),
  UNIQUE KEY `block_id_idx` (`id`),
  UNIQUE KEY `block_height_idx` (`height`),
  UNIQUE KEY `block_timestamp_idx` (`timestamp`),
  KEY `block_generator_id_idx` (`generator_id`),
  KEY `block_pool_id_idx` (`pool_id`),
  KEY `constraint_3c5` (`next_block_id`),
  KEY `constraint_3c` (`previous_block_id`),
  CONSTRAINT `constraint_3c` FOREIGN KEY (`previous_block_id`) REFERENCES `block` (`id`) ON DELETE CASCADE,
  CONSTRAINT `constraint_3c5` FOREIGN KEY (`next_block_id`) REFERENCES `block` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `peer`
--

DROP TABLE IF EXISTS `peer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `peer` (
  `address` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pool_miner`
--

DROP TABLE IF EXISTS `pool_miner`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pool_miner` (
  `db_id` INT NOT NULL AUTO_INCREMENT,
  `account_id` BIGINT(20) NOT NULL DEFAULT 0 COMMENT 'miner account id',
  `pool_id` BIGINT(20) NOT NULL DEFAULT 0 COMMENT 'pool account id',
  `status` INT NOT NULL DEFAULT 0 COMMENT 'state 0-enable 1- delete',
  `height` INT NOT NULL DEFAULT 0 ,
  `c_time` BIGINT(20) NULL DEFAULT 0 ,
  `m_time` BIGINT(20) NULL DEFAULT 0 ,
  PRIMARY KEY (`db_id`),
  INDEX `idx_pool_list` (`pool_id`, `status`),
  INDEX `idx_pool_query` (`account_id`, `pool_id`, `status`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `global_parameter`
--

DROP TABLE IF EXISTS `global_parameter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `global_parameter` (
  `db_id` INT NOT NULL AUTO_INCREMENT,
  `id` BIGINT(20) NOT NULL DEFAULT 0 ,
  `transaction_id` BIGINT(20) NOT NULL DEFAULT 0,
  `value` VARCHAR(200) NOT NULL DEFAULT '',
  `height` INT(11) NOT NULL DEFAULT 0,
  `latest` TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`db_id`),
  INDEX `idx_global_name` (`id`),
  UNIQUE INDEX `uniq_id_height` (`id` , `height` )
  )ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `version`
--

DROP TABLE IF EXISTS `version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `version` (
  `next_update` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

INSERT INTO `version` (`next_update`) VALUES (1);

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
