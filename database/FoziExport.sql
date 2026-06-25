-- MySQL dump 10.13  Distrib 8.0.46, for macos15 (arm64)
--
-- Host: 127.0.0.1    Database: gonaturedb
-- ------------------------------------------------------
-- Server version	9.7.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '0e5c32da-657b-11f1-be23-66d0e2fb4155:1-503';

--
-- Table structure for table `booking`
--

DROP TABLE IF EXISTS `booking`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `booking` (
  `booking_id` int NOT NULL,
  `traveler_id` varchar(36) DEFAULT NULL,
  `travelerName` varchar(100) DEFAULT NULL,
  `travelerEmail` varchar(100) DEFAULT NULL,
  `travelerPhoneNumber` varchar(20) DEFAULT NULL,
  `park_id` int DEFAULT NULL,
  `numberOfVisitors` int NOT NULL,
  `visitorTime` datetime NOT NULL,
  `status` enum('PENDING','CONFIRMED','WAITING_LIST','PENDING_WAITLIST_CONFIRMATION','PENDING_REMINDER_CONFIRMATION','CANCELLED','CHECKED_IN','CHECKED_OUT','SYSTEM_CANCEL') NOT NULL,
  `organizedBooking` tinyint(1) NOT NULL DEFAULT '0',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00',
  `paid` tinyint(1) NOT NULL DEFAULT '0',
  `visitorsInside` int NOT NULL DEFAULT '0',
  `entryTime` datetime DEFAULT NULL,
  `exitTime` datetime DEFAULT NULL,
  `action_required_at` datetime DEFAULT NULL,
  `action_deadline` datetime DEFAULT NULL,
  `reminder_sent_at` datetime DEFAULT NULL,
  `last_notification_type` varchar(50) DEFAULT NULL,
  `walk_in` tinyint DEFAULT NULL,
  PRIMARY KEY (`booking_id`),
  KEY `idx_booking_slot` (`park_id`,`visitorTime`),
  KEY `idx_booking_status` (`status`),
  KEY `fk_booking_traveler_idx` (`traveler_id`),
  CONSTRAINT `fk_booking_park` FOREIGN KEY (`park_id`) REFERENCES `park` (`park_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_booking_traveler` FOREIGN KEY (`traveler_id`) REFERENCES `traveler` (`traveler_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `booking`
--

LOCK TABLES `booking` WRITE;
/*!40000 ALTER TABLE `booking` DISABLE KEYS */;
INSERT INTO `booking` VALUES (1,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,2,'2026-05-01 09:00:00','CONFIRMED',0,100.00,1,0,'2026-05-01 09:15:00','2026-05-01 13:15:00',NULL,NULL,NULL,NULL,NULL),(2,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,3,'2026-05-05 10:00:00','CONFIRMED',0,195.00,1,0,'2026-05-05 10:20:00','2026-05-05 14:20:00',NULL,NULL,NULL,NULL,NULL),(3,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,1,'2026-05-10 14:00:00','CONFIRMED',0,45.00,1,0,'2026-05-10 14:10:00','2026-05-10 17:10:00',NULL,NULL,NULL,NULL,NULL),(4,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,4,'2026-05-15 11:00:00','CONFIRMED',1,200.00,1,0,'2026-05-15 11:25:00','2026-05-15 15:25:00',NULL,NULL,NULL,NULL,NULL),(5,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-05-20 15:00:00','CONFIRMED',0,130.00,1,0,'2026-05-20 15:30:00','2026-05-20 19:30:00',NULL,NULL,NULL,NULL,NULL),(6,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,3,'2026-05-25 09:00:00','CONFIRMED',0,135.00,1,0,'2026-05-25 09:45:00','2026-05-25 13:45:00',NULL,NULL,NULL,NULL,NULL),(7,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,1,'2026-06-01 12:00:00','CONFIRMED',0,50.00,1,0,'2026-06-01 12:15:00','2026-06-01 16:15:00',NULL,NULL,NULL,NULL,NULL),(8,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-06-05 13:00:00','CONFIRMED',0,130.00,1,0,'2026-06-05 13:20:00','2026-06-05 17:20:00',NULL,NULL,NULL,NULL,NULL),(9,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,2,'2026-06-22 09:00:00','CONFIRMED',0,100.00,1,0,'2026-06-22 09:15:00','2026-06-25 11:11:19',NULL,NULL,NULL,NULL,NULL),(10,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,3,'2026-06-25 10:45:00','CONFIRMED',0,195.00,1,3,'2026-06-22 10:20:00',NULL,NULL,NULL,NULL,NULL,NULL),(11,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,1,'2026-06-23 14:00:00','CONFIRMED',0,45.00,1,1,'2026-06-23 14:10:00',NULL,NULL,NULL,NULL,NULL,NULL),(12,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,4,'2026-06-24 11:00:00','CONFIRMED',1,200.00,1,0,'2026-06-24 11:25:00','2026-06-24 15:39:07',NULL,NULL,NULL,NULL,NULL),(13,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-06-25 15:00:00','CONFIRMED',0,130.00,1,2,'2026-06-25 15:30:00',NULL,NULL,NULL,NULL,NULL,NULL),(14,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,2,'2026-06-25 10:45:00','CHECKED_IN',0,135.00,1,3,'2026-06-26 09:45:00',NULL,NULL,NULL,NULL,NULL,NULL),(15,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,1,'2026-06-27 12:00:00','CHECKED_IN',0,50.00,1,1,'2026-06-27 12:15:00',NULL,NULL,NULL,NULL,NULL,NULL),(16,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-06-22 21:00:00','CONFIRMED',0,130.00,1,2,'2026-06-28 13:20:00',NULL,NULL,NULL,NULL,NULL,NULL),(17,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,3,'2026-06-22 20:00:00','CONFIRMED',0,150.00,1,0,'2026-06-22 20:25:32','2026-06-25 10:32:27',NULL,NULL,'2026-06-22 10:00:00','CONFIRMATION',NULL),(18,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-06-22 10:30:00','CONFIRMED',0,130.00,1,0,NULL,NULL,NULL,NULL,'2026-06-22 11:00:00','CONFIRMATION',NULL),(19,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,1,'2026-06-22 13:00:00','CONFIRMED',0,45.00,1,0,NULL,NULL,NULL,NULL,'2026-06-22 14:00:00','CONFIRMATION',NULL),(20,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,5,'2026-07-02 08:00:00','CONFIRMED',1,250.00,1,0,NULL,NULL,NULL,NULL,'2026-06-23 09:00:00','CONFIRMATION',NULL),(21,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,4,'2026-07-03 11:00:00','CONFIRMED',0,260.00,1,0,NULL,NULL,NULL,NULL,'2026-06-24 12:00:00','CONFIRMATION',NULL),(22,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,2,'2026-06-24 16:00:00','CONFIRMED',0,90.00,1,0,NULL,NULL,NULL,NULL,'2026-06-25 13:00:00','CONFIRMATION',NULL),(23,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,3,'2026-06-24 16:00:00','CONFIRMED',0,150.00,1,0,NULL,NULL,NULL,NULL,'2026-06-26 10:30:00','CONFIRMATION',NULL),(24,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,1,'2026-06-24 20:00:00','CONFIRMED',0,65.00,1,0,NULL,NULL,NULL,NULL,'2026-06-27 16:00:00','CONFIRMATION',NULL),(25,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,3,'2026-06-24 15:00:00','CONFIRMED',1,135.00,1,0,NULL,NULL,NULL,NULL,'2026-06-28 11:00:00','CONFIRMATION',NULL),(26,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,2,'2026-06-24 17:00:00','CONFIRMED',0,100.00,1,0,'2026-06-24 16:54:00','2026-06-24 17:11:42',NULL,NULL,'2026-06-29 15:30:00','CONFIRMATION',NULL),(27,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,2,'2026-06-25 09:00:00','CONFIRMED',0,100.00,0,0,NULL,NULL,'2026-06-25 08:30:00','2026-06-25 10:00:00',NULL,NULL,NULL),(28,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,3,'2026-06-25 10:00:00','PENDING',0,195.00,0,0,NULL,NULL,'2026-06-26 09:30:00','2026-06-26 11:00:00',NULL,NULL,NULL),(29,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,1,'2026-06-27 14:00:00','PENDING',0,45.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(30,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,4,'2026-06-28 11:00:00','PENDING',1,200.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(31,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-06-29 15:00:00','PENDING',0,130.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(32,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,2,'2026-06-10 09:00:00','CANCELLED',0,100.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(33,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,3,'2026-06-11 10:00:00','CANCELLED',0,195.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(34,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,1,'2026-06-12 14:00:00','CANCELLED',0,45.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(35,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,4,'2026-06-13 11:00:00','CANCELLED',1,200.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(36,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-06-14 15:00:00','CANCELLED',0,130.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(37,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,3,'2026-06-15 09:00:00','CANCELLED',0,135.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(38,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,1,'2026-06-16 12:00:00','CANCELLED',0,50.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(39,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-06-17 13:00:00','CANCELLED',0,130.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(40,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,4,'2026-06-18 10:00:00','CANCELLED',1,180.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(41,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,3,'2026-06-19 15:00:00','CANCELLED',0,150.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(42,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,2,'2026-07-09 09:00:00','WAITING_LIST',0,100.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(43,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,3,'2026-07-10 10:00:00','WAITING_LIST',0,195.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(44,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,1,'2026-07-11 14:00:00','WAITING_LIST',0,45.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(45,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,4,'2026-07-12 11:00:00','WAITING_LIST',1,200.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(46,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-07-13 15:00:00','WAITING_LIST',0,130.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(47,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,2,'2026-06-08 09:00:00','SYSTEM_CANCEL',0,100.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(48,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,3,'2026-06-09 10:00:00','SYSTEM_CANCEL',0,195.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(49,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,1,'2026-06-23 14:00:00','SYSTEM_CANCEL',0,45.00,0,0,NULL,NULL,NULL,NULL,NULL,'WAITLIST_CONFIRMATION_TIMEOUT',NULL),(50,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,2,'2026-06-24 11:00:00','SYSTEM_CANCEL',0,100.00,0,0,NULL,NULL,NULL,NULL,'2026-06-22 14:00:00','REMINDER_CONFIRMATION_TIMEOUT',NULL),(123,'123456789',NULL,NULL,NULL,1,5,'2026-06-24 15:00:00','CONFIRMED',0,123.00,1,0,'2026-06-24 11:25:00','2026-06-25 10:27:55',NULL,NULL,NULL,NULL,NULL),(358591,'88be1b98-0aad-4bbd-ba31-31bbe533d08e','Visitor Guest','awfawfA@afnawf.com','0548123812',1,6,'2026-06-25 11:00:00','CONFIRMED',0,127.50,0,0,'2026-06-25 10:52:15','2026-06-25 10:52:33',NULL,NULL,NULL,NULL,NULL),(1126169,'c38c0081-e587-4e4b-b37f-c00af3ac9e7b',NULL,NULL,NULL,1,10,'2026-06-25 14:27:59','CHECKED_OUT',0,500.00,0,0,'2026-06-25 14:27:59','2026-06-25 14:28:32',NULL,NULL,NULL,NULL,NULL),(1142485,'58c44a45-0cac-4ffb-8fee-7ca1f5e9a7a3',NULL,NULL,NULL,1,6,'2026-06-24 16:31:56','CONFIRMED',0,300.00,0,0,'2026-06-24 16:31:56','2026-06-24 16:32:07',NULL,NULL,NULL,NULL,NULL),(1283673,'58c44a45-0cac-4ffb-8fee-7ca1f5e9a7a3','awlfn','oinwaoifnao@awnfoawf.com','0546139018',2,6,'2026-06-25 08:00:00','CANCELLED',0,331.50,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(1286197,'fe59a1b8-404d-432f-b599-64265efe70b4',NULL,NULL,NULL,1,14,'2026-06-25 14:27:33','CANCELLED',0,700.00,0,0,'2026-06-25 14:27:33','2026-06-25 14:27:54',NULL,NULL,NULL,NULL,NULL),(1495577,'36ee0ebc-8637-4d1b-afd6-2cb9e204a15e','Visitor Guest','ffkneofiwon@oaiwnfioa.com','0541239712',1,6,'2026-06-25 10:00:00','CONFIRMED',0,255.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(1584775,'58c44a45-0cac-4ffb-8fee-7ca1f5e9a7a3','aofinawf','waofnoawf@aoinfaw.com','0546712923',1,6,'2026-06-25 10:45:00','CANCELLED',0,255.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(1764172,'b1a3ee61-14ed-4c08-9ef0-95fc760cf559','fauzi','fauzia2@fnwaof.com','0546139013',2,2,'2026-06-24 17:00:00','CONFIRMED',0,110.50,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(1966800,'c498e231-3c6a-456f-8141-dd48ff0aefbc','Visitor Guests','afionwanf2anwfoawf@awf.com','05712391',1,16,'2026-06-25 16:00:00','CONFIRMED',0,420.75,1,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(2394897,'c498e231-3c6a-456f-8141-dd48ff0aefbc','aoiwnfaowf','aoinwfowa@aonwfawf.com','05471239172',1,7,'2026-06-24 08:00:00','CANCELLED',0,191.25,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(2600258,'58c44a45-0cac-4ffb-8fee-7ca1f5e9a7a3',NULL,NULL,NULL,1,15,'2026-06-25 11:12:25','CHECKED_OUT',0,750.00,0,0,'2026-06-25 11:12:25','2026-06-25 11:15:36',NULL,NULL,NULL,NULL,NULL),(2724193,'58c44a45-0cac-4ffb-8fee-7ca1f5e9a7a3','awfawf','awfawf@aownfoawf.com','0541231231',1,6,'2026-06-24 09:00:00','CANCELLED',0,255.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(3585971,'88be1b98-0aad-4bbd-ba31-31bbe533d08e','Visitor Guest','awfawfA@afnawf.com','0548123812',1,6,'2026-06-25 11:00:00','CONFIRMED',0,127.50,0,0,'2026-06-25 10:52:20','2026-06-25 10:52:35',NULL,NULL,NULL,NULL,NULL),(3679253,'ea1596e6-bb9e-4271-ac9c-40c2d29e12b7',NULL,NULL,NULL,1,4,'2026-06-23 21:41:28','CONFIRMED',0,200.00,0,0,'2026-06-23 21:41:28','2026-06-23 22:05:56',NULL,NULL,NULL,NULL,NULL),(3682266,'c498e231-3c6a-456f-8141-dd48ff0aefbc','Visitor Guests','afionwanf2anwfoawf@awf.com','05712391',1,1,'2026-06-25 15:45:00','CHECKED_OUT',0,38.25,0,0,'2026-06-25 15:45:05','2026-06-25 15:45:18',NULL,NULL,NULL,NULL,NULL),(3743252,'9159d413-f61f-4fb4-b9cf-3d13c7873b49','Visitor Guest','wfoaiwnF@aonwfaw.com','054812391',1,3,'2026-06-25 11:00:00','CONFIRMED',0,127.50,0,0,'2026-06-25 10:52:25','2026-06-25 10:52:38',NULL,NULL,NULL,NULL,NULL),(3788500,'c498e231-3c6a-456f-8141-dd48ff0aefbc','k awfkawfaownf','aownfaowf@awfawf.com','0546123921',1,6,'2026-06-30 08:00:00','CANCELLED',0,159.38,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(3810234,'c498e231-3c6a-456f-8141-dd48ff0aefbc','awof nawfo','awonfaowf@awofnawf.com','0547129237',2,10,'2026-06-25 08:00:00','CANCELLED',0,328.19,1,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(3866286,'b15872d7-2f68-49e8-9534-94d7f25e9cfc',NULL,NULL,NULL,1,2,'2026-06-23 21:53:45','CONFIRMED',0,100.00,0,0,'2026-06-23 21:53:45','2026-06-23 22:52:08',NULL,NULL,NULL,NULL,NULL),(4417774,'c498e231-3c6a-456f-8141-dd48ff0aefbc','aowifnoaw','aoinfawo@awonfaowf.com','05412729317',1,12,'2026-07-02 11:00:00','CANCELLED',0,350.63,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(4483255,'58c44a45-0cac-4ffb-8fee-7ca1f5e9a7a3','Visitor Guest','fauaiwf@iawf.com','9541823121',2,6,'2026-06-24 17:00:00','CONFIRMED',0,331.50,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(4487887,'9159d413-f61f-4fb4-b9cf-3d13c7873b49','Visitor Guest','fawfawfN@awonfawf.com','0547123012',2,4,'2026-06-25 10:00:00','CANCELLED',0,221.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(4598397,'c498e231-3c6a-456f-8141-dd48ff0aefbc','awafinaw','waofnoawf@aoinfaw.coma','05461239123',1,16,'2026-06-24 08:00:00','CANCELLED',0,478.13,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(4841549,'f3e0de25-be37-4968-874a-583e60e13ad6','Visitor Guest','awefawef@awawf.com','05461239012',1,6,'2026-06-25 11:00:00','CHECKED_IN',0,255.00,0,6,'2026-06-25 11:11:30',NULL,NULL,NULL,NULL,NULL,NULL),(5035251,'afd35c32-6db8-4ebc-8cb8-fa3bbdc2d708',NULL,NULL,NULL,1,15,'2026-06-25 14:37:04','CHECKED_IN',0,750.00,1,15,'2026-06-25 14:37:04',NULL,NULL,NULL,NULL,NULL,NULL),(5329572,'58c44a45-0cac-4ffb-8fee-7ca1f5e9a7a3',NULL,NULL,NULL,1,15,'2026-06-25 10:29:02','CONFIRMED',0,750.00,0,0,'2026-06-25 10:29:02','2026-06-25 11:12:41',NULL,NULL,NULL,NULL,NULL),(6041317,'9924e648-f295-4d71-954e-1b68c37976c5',NULL,NULL,NULL,1,15,'2026-06-25 14:24:08','CANCELLED',0,750.00,0,0,'2026-06-25 14:24:08','2026-06-25 14:24:11',NULL,NULL,NULL,NULL,NULL),(6340290,'b1a3ee61-14ed-4c08-9ef0-95fc760cf559','oiawfoawf','awiofba@aiowfoawf.com','0546139013',2,6,'2026-06-25 08:00:00','CONFIRMED',0,331.50,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(7534927,'c498e231-3c6a-456f-8141-dd48ff0aefbc','awiofnaw','aownfawf@awfonawf.com','0547123012',1,9,'2026-06-28 15:00:00','CONFIRMED',0,224.40,1,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(7712976,'c498e231-3c6a-456f-8141-dd48ff0aefbc','awof nawof','anfowai@aowfnaowf.com','05461239123',1,7,'2026-06-24 09:00:00','CANCELLED',0,191.25,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(8657590,'58c44a45-0cac-4ffb-8fee-7ca1f5e9a7a3','awfoinaw','aownfa@awonfoawf.com','0546139013',1,6,'2026-06-28 15:00:00','CANCELLED',0,255.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(9108081,'58c44a45-0cac-4ffb-8fee-7ca1f5e9a7a3',NULL,NULL,NULL,1,6,'2026-06-25 09:06:02','CHECKED_OUT',0,300.00,0,0,'2026-06-25 09:06:02','2026-06-25 09:06:41',NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `booking` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `employee`
--

DROP TABLE IF EXISTS `employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee` (
  `employee_id` int NOT NULL,
  `user_id` varchar(36) NOT NULL,
  `salary` int DEFAULT NULL,
  `park_id` int DEFAULT NULL,
  `role` enum('park_worker','park_manager','department_manager','service_rep') NOT NULL,
  PRIMARY KEY (`employee_id`),
  KEY `fk_employee_user` (`user_id`),
  KEY `fk_employee_park` (`park_id`),
  CONSTRAINT `fk_employee_park` FOREIGN KEY (`park_id`) REFERENCES `park` (`park_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `employee`
--

LOCK TABLES `employee` WRITE;
/*!40000 ALTER TABLE `employee` DISABLE KEYS */;
INSERT INTO `employee` VALUES (1,'1001',9000,1,'park_worker'),(2,'1002',14000,1,'park_manager'),(3,'1003',20000,1,'department_manager'),(4,'1004',8500,1,'service_rep'),(5,'f26b3d7e-e13e-4d50-a3dc-612343215f',9000,1,'park_worker'),(6,'41e100c6-69a1-4ca0-9318-119fbf7f1f86',1231,1,'park_worker');
/*!40000 ALTER TABLE `employee` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `managerRequests`
--

DROP TABLE IF EXISTS `managerRequests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `managerRequests` (
  `request_Id` varchar(36) NOT NULL,
  `employee_id` int NOT NULL,
  `dep_manager_id` int DEFAULT NULL,
  `requestTitle` varchar(150) NOT NULL,
  `parameter_type` enum('MAX_CAPACITY','GAP','DEFAULT_STAY_TIME','PRICE_PER_PERSON') NOT NULL,
  `new_value` int NOT NULL,
  `park_id` int NOT NULL,
  `approved` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`request_Id`),
  KEY `idx_mr_pending` (`park_id`,`approved`),
  KEY `fk_mr_employee` (`employee_id`),
  KEY `fk_mr_depmanager` (`dep_manager_id`),
  CONSTRAINT `fk_mr_depmanager` FOREIGN KEY (`dep_manager_id`) REFERENCES `employee` (`employee_id`),
  CONSTRAINT `fk_mr_employee` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`employee_id`),
  CONSTRAINT `fk_mr_park` FOREIGN KEY (`park_id`) REFERENCES `park` (`park_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `managerRequests`
--

LOCK TABLES `managerRequests` WRITE;
/*!40000 ALTER TABLE `managerRequests` DISABLE KEYS */;
/*!40000 ALTER TABLE `managerRequests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `park`
--

DROP TABLE IF EXISTS `park`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `park` (
  `park_id` int NOT NULL,
  `name` varchar(100) NOT NULL,
  `currentVisitors` int NOT NULL DEFAULT '0',
  `maxCapacity` int NOT NULL,
  `gap` int NOT NULL DEFAULT '0',
  `defaultStayTime` int NOT NULL DEFAULT '240',
  `pricePerPerson` decimal(10,2) NOT NULL DEFAULT '0.00',
  `department_manager_id` int DEFAULT NULL,
  `park_manager_id` int DEFAULT NULL,
  PRIMARY KEY (`park_id`),
  KEY `fk_park_depmgr` (`department_manager_id`),
  KEY `fk_park_parkmanager` (`park_manager_id`),
  CONSTRAINT `fk_park_depmgr` FOREIGN KEY (`department_manager_id`) REFERENCES `employee` (`employee_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_park_parkmanager` FOREIGN KEY (`park_manager_id`) REFERENCES `employee` (`employee_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `park`
--

LOCK TABLES `park` WRITE;
/*!40000 ALTER TABLE `park` DISABLE KEYS */;
INSERT INTO `park` VALUES (1,'Banias Nature Reserve',21,300,1,240,50.00,3,NULL),(2,'Masada National Park',0,500,50,240,65.00,3,NULL),(3,'Ein Gedi Reserve',0,250,25,180,45.00,3,NULL);
/*!40000 ALTER TABLE `park` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `promotion`
--

DROP TABLE IF EXISTS `promotion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion` (
  `promotion_id` int NOT NULL AUTO_INCREMENT,
  `park_id` int NOT NULL,
  `promo_code` int NOT NULL,
  `percentage` int NOT NULL,
  `endDate` datetime DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`promotion_id`),
  KEY `idx_promo_park` (`park_id`),
  CONSTRAINT `fk_promo_park` FOREIGN KEY (`park_id`) REFERENCES `park` (`park_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `promotion`
--

LOCK TABLES `promotion` WRITE;
/*!40000 ALTER TABLE `promotion` DISABLE KEYS */;
/*!40000 ALTER TABLE `promotion` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `promotion_request`
--

DROP TABLE IF EXISTS `promotion_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion_request` (
  `request_id` varchar(36) NOT NULL,
  `employee_id` int NOT NULL,
  `action_type` enum('ADD','UPDATE','DELETE') NOT NULL,
  `promotion_id` int NOT NULL DEFAULT '0',
  `park_id` int NOT NULL,
  `promo_code` int DEFAULT NULL,
  `percentage` int DEFAULT NULL,
  `endDate` datetime DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `approved` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`request_id`),
  KEY `idx_pr_pending` (`park_id`,`approved`),
  KEY `fk_pr_employee` (`employee_id`),
  CONSTRAINT `fk_pr_employee` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`employee_id`),
  CONSTRAINT `fk_pr_park` FOREIGN KEY (`park_id`) REFERENCES `park` (`park_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `promotion_request`
--

LOCK TABLES `promotion_request` WRITE;
/*!40000 ALTER TABLE `promotion_request` DISABLE KEYS */;
/*!40000 ALTER TABLE `promotion_request` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `report`
--

DROP TABLE IF EXISTS `report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report` (
  `report_id` int NOT NULL AUTO_INCREMENT,
  `park_id` int NOT NULL,
  `reportTitle` varchar(150) NOT NULL,
  `content` text,
  `employee_id` int NOT NULL,
  PRIMARY KEY (`report_id`),
  KEY `fk_report_park` (`park_id`),
  KEY `fk_report_employee` (`employee_id`),
  CONSTRAINT `fk_report_employee` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`employee_id`),
  CONSTRAINT `fk_report_park` FOREIGN KEY (`park_id`) REFERENCES `park` (`park_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report`
--

LOCK TABLES `report` WRITE;
/*!40000 ALTER TABLE `report` DISABLE KEYS */;
/*!40000 ALTER TABLE `report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `traveler`
--

DROP TABLE IF EXISTS `traveler`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `traveler` (
  `traveler_id` varchar(36) NOT NULL,
  `nationalId` int NOT NULL,
  `guide` tinyint(1) NOT NULL DEFAULT '0',
  `clubMember` tinyint(1) NOT NULL DEFAULT '0',
  `user_id` varchar(36) NOT NULL,
  `familyMembers` int DEFAULT NULL,
  `creditCard` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`traveler_id`),
  UNIQUE KEY `uq_traveler_nationalId` (`nationalId`),
  KEY `fk_traveler_user` (`user_id`),
  CONSTRAINT `fk_traveler_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `traveler`
--

LOCK TABLES `traveler` WRITE;
/*!40000 ALTER TABLE `traveler` DISABLE KEYS */;
INSERT INTO `traveler` VALUES ('123456789',123456789,0,1,'123456789',3,NULL),('36ee0ebc-8637-4d1b-afd6-2cb9e204a15e',432143212,0,1,'6d8eafed-e09a-4928-9641-ccfbdd938cf0',NULL,NULL),('50d833f7-d995-4ac7-a38c-0a3f6c796893',123123432,0,0,'41e100c6-69a1-4ca0-9318-119fbf7f1f86',NULL,NULL),('58c44a45-0cac-4ffb-8fee-7ca1f5e9a7a3',123123123,1,0,'b682be29-5df5-4c14-aadb-dee385f7037e',NULL,NULL),('7395c6e2-d432-4c52-8164-d3c3a6d8bd30',27364538,0,0,'73154b7e-0cdb-4784-8b2f-881b13b38903',NULL,NULL),('865097545',123321243,0,0,'617191425',NULL,NULL),('88be1b98-0aad-4bbd-ba31-31bbe533d08e',123456765,0,0,'374cd731-3465-43dd-b5ad-997211784c39',NULL,NULL),('9159d413-f61f-4fb4-b9cf-3d13c7873b49',321341231,0,0,'5b8fdf79-f682-4636-80f9-a8068671a282',NULL,NULL),('9924e648-f295-4d71-954e-1b68c37976c5',123124124,0,0,'1331423f-86e0-4a0d-9731-297139912cad',NULL,NULL),('9f3d925f-b7e8-4b50-9b2b-3f130bb4e706',473829371,0,0,'6c74844a-43e0-4556-a6b1-147ebaf562b5',NULL,NULL),('afd35c32-6db8-4ebc-8cb8-fa3bbdc2d708',12421321,0,0,'c54e9d3a-7572-4c22-b7d2-bf765d4554e2',NULL,NULL),('b15872d7-2f68-49e8-9534-94d7f25e9cfc',98923471,0,0,'30e11d98-800e-42ba-9b05-d5a1d45e51b3',NULL,NULL),('b1a3ee61-14ed-4c08-9ef0-95fc760cf559',123123456,0,0,'61944542-5b26-4b4a-bbfc-568588f5b562',NULL,NULL),('c38c0081-e587-4e4b-b37f-c00af3ac9e7b',745324,0,0,'e11f5358-b348-4a7e-ba50-897702ff5f53',NULL,NULL),('c498e231-3c6a-456f-8141-dd48ff0aefbc',209113059,1,0,'00919d0e-9aa6-4dfd-af89-a1bdbb806d93',6,NULL),('c75fe90e-f2b0-4bb2-9892-2431b0088702',292746129,0,0,'136b59f0-d25e-452b-96aa-3b161592c2cd',NULL,NULL),('cb8afa25-14e9-490d-ad01-798f96bc361c',765432112,0,0,'80ca7f76-47c3-4cf1-baef-58743a55bec2',NULL,NULL),('ea1596e6-bb9e-4271-ac9c-40c2d29e12b7',19278365,0,0,'f3330b41-e51b-4bea-bfc0-64eba86255a8',NULL,NULL),('f3e0de25-be37-4968-874a-583e60e13ad6',321232121,0,0,'3a78ddea-275a-4b55-89d6-1933be323d10',NULL,NULL),('fdf0a431-abc6-4214-965b-c2e147f2c758',987654321,0,0,'f26b3d7e-e13e-4d50-a3dc-6d978a03215f',NULL,NULL),('fe59a1b8-404d-432f-b599-64265efe70b4',123412342,0,0,'7927a271-48f0-4e54-a375-df8f4b824cd3',NULL,NULL);
/*!40000 ALTER TABLE `traveler` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `user_id` varchar(36) NOT NULL,
  `username` varchar(50) DEFAULT NULL,
  `password` varchar(100) DEFAULT NULL,
  `firstName` varchar(50) NOT NULL,
  `lastName` varchar(50) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `phoneNumber` varchar(20) DEFAULT NULL,
  `nationalId` varchar(9) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uq_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES ('00919d0e-9aa6-4dfd-af89-a1bdbb806d93',NULL,NULL,'Visitor','Guests','afionwanf2anwfoawf@awf.com','05712391','209113059'),('1001','worker1','1234','Dana','Cohen','dana@gonature.local','0500000001','43212312'),('1002','pmanager','1234','Ronen','Levi','ronen@gonature.local','0500000002',NULL),('1003','depmgr','1234','Sara','Mizrahi','sara@gonature.local','0500000003',NULL),('1004','service','1234','Omer','Katz','omer@gonature.local','0500000004',NULL),('123456789',NULL,NULL,'Yossi','Israeli','yossi@example.com','0521234567','123456789'),('1331423f-86e0-4a0d-9731-297139912cad',NULL,NULL,'Visitor','Guest',NULL,NULL,NULL),('136b59f0-d25e-452b-96aa-3b161592c2cd',NULL,NULL,'Visitor','Guest',NULL,NULL,'292746129'),('30e11d98-800e-42ba-9b05-d5a1d45e51b3',NULL,NULL,'Visitor','Guest',NULL,NULL,'98923471'),('374cd731-3465-43dd-b5ad-997211784c39',NULL,NULL,'Visitor','Guest',NULL,NULL,'123456765'),('3a78ddea-275a-4b55-89d6-1933be323d10',NULL,NULL,'Visitor','Guest',NULL,NULL,'321232121'),('41e100c6-69a1-4ca0-9318-119fbf7f1f86',NULL,NULL,'Visitor','Guest',NULL,NULL,'123123432'),('5b8fdf79-f682-4636-80f9-a8068671a282',NULL,NULL,'Visitor','Guest',NULL,NULL,'321341231'),('617191425',NULL,NULL,'Visitor','Guest',NULL,NULL,'123321243'),('61944542-5b26-4b4a-bbfc-568588f5b562',NULL,NULL,'Visitor','Guest','fbawfbawf@oawnfoawf.com','0546139013','123123456'),('6c74844a-43e0-4556-a6b1-147ebaf562b5',NULL,NULL,'Visitor','Guest',NULL,NULL,'473829371'),('6d8eafed-e09a-4928-9641-ccfbdd938cf0',NULL,NULL,'BENAN','BADER','benan@benan.com','0546128261','432143212'),('73154b7e-0cdb-4784-8b2f-881b13b38903',NULL,NULL,'Visitor','Guest',NULL,NULL,'27364538'),('7927a271-48f0-4e54-a375-df8f4b824cd3',NULL,NULL,'Visitor','Guest',NULL,NULL,NULL),('80ca7f76-47c3-4cf1-baef-58743a55bec2',NULL,NULL,'Visitor','Guest',NULL,NULL,'765432112'),('b682be29-5df5-4c14-aadb-dee385f7037e',NULL,NULL,'Visitor','Guest','fauaiwf@iawf.com','9541823121','123123123'),('c54e9d3a-7572-4c22-b7d2-bf765d4554e2',NULL,NULL,'Visitor','Guest',NULL,NULL,NULL),('e11f5358-b348-4a7e-ba50-897702ff5f53',NULL,NULL,'Visitor','Guest',NULL,NULL,NULL),('f26b3d7e-e13e-4d50-a3dc-612343215f','worker2','1234','Worker','New','wrker@worker.com','0546192736',NULL),('f26b3d7e-e13e-4d50-a3dc-6d978a03215f',NULL,NULL,'Visitor','Guest',NULL,NULL,'987654321'),('f3330b41-e51b-4bea-bfc0-64eba86255a8',NULL,NULL,'Visitor','Guest',NULL,NULL,'19278365');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `WaitingList`
--

DROP TABLE IF EXISTS `WaitingList`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `WaitingList` (
  `waitingList_id` varchar(36) NOT NULL,
  `park_id` int NOT NULL,
  `slot_time` datetime NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'OPEN',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`waitingList_id`),
  UNIQUE KEY `uq_waitlist_slot` (`park_id`,`slot_time`),
  CONSTRAINT `fk_waitlist_park` FOREIGN KEY (`park_id`) REFERENCES `park` (`park_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `WaitingList`
--

LOCK TABLES `WaitingList` WRITE;
/*!40000 ALTER TABLE `WaitingList` DISABLE KEYS */;
INSERT INTO `WaitingList` VALUES ('4019d4d4-9c01-4c34-99c2-5c8bbec22da2',1,'2026-06-24 08:00:00','OPEN','2026-06-23 23:00:08'),('90030344-f671-4818-8966-4d8e16f1ce27',1,'2026-06-24 09:00:00','OPEN','2026-06-23 23:18:29'),('f48505c9-a231-4502-ba1b-cd47f3e524e7',1,'2026-06-28 15:00:00','OPEN','2026-06-23 23:20:22');
/*!40000 ALTER TABLE `WaitingList` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `WaitingListEntry`
--

DROP TABLE IF EXISTS `WaitingListEntry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `WaitingListEntry` (
  `id` varchar(36) NOT NULL,
  `waitingList_id` varchar(36) NOT NULL,
  `booking_id` int NOT NULL,
  `registered_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` varchar(30) NOT NULL DEFAULT 'WAITING',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_wle_list` (`waitingList_id`),
  KEY `idx_wle_booking` (`booking_id`),
  CONSTRAINT `fk_wle_booking` FOREIGN KEY (`booking_id`) REFERENCES `booking` (`booking_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_wle_list` FOREIGN KEY (`waitingList_id`) REFERENCES `WaitingList` (`waitingList_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `WaitingListEntry`
--

LOCK TABLES `WaitingListEntry` WRITE;
/*!40000 ALTER TABLE `WaitingListEntry` DISABLE KEYS */;
INSERT INTO `WaitingListEntry` VALUES ('36396946-98dd-4935-b004-4050e8f6f16d','4019d4d4-9c01-4c34-99c2-5c8bbec22da2',4598397,'2026-06-23 23:00:08','CANCELLED','2026-06-23 23:14:26'),('79d32fe1-afed-4f6f-aa43-0edd2ec9c702','4019d4d4-9c01-4c34-99c2-5c8bbec22da2',2394897,'2026-06-23 23:14:44','CANCELLED','2026-06-23 23:16:36'),('d6082be5-36bb-4fee-9477-6f156d95c8a3','90030344-f671-4818-8966-4d8e16f1ce27',7712976,'2026-06-23 23:18:29','CANCELLED','2026-06-23 23:19:32'),('f7cbb1d3-543d-4ebb-936f-97f9d30f2a0b','f48505c9-a231-4502-ba1b-cd47f3e524e7',7534927,'2026-06-23 23:20:22','CONFIRMED','2026-06-23 23:46:30');
/*!40000 ALTER TABLE `WaitingListEntry` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-25 16:41:54
