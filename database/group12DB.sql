-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: localhost    Database: gonaturedb
-- ------------------------------------------------------
-- Server version	8.0.45

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
INSERT INTO `booking` VALUES (1,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,2,'2026-05-01 09:00:00','CHECKED_OUT',0,100.00,1,0,'2026-05-01 09:15:00','2026-05-01 13:15:00',NULL,NULL,NULL,NULL),(2,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,3,'2026-05-05 10:00:00','CHECKED_OUT',0,195.00,1,0,'2026-05-05 10:20:00','2026-05-05 14:20:00',NULL,NULL,NULL,NULL),(3,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,1,'2026-05-10 14:00:00','CHECKED_OUT',0,45.00,1,0,'2026-05-10 14:10:00','2026-05-10 17:10:00',NULL,NULL,NULL,NULL),(4,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,4,'2026-05-15 11:00:00','CHECKED_OUT',1,200.00,1,0,'2026-05-15 11:25:00','2026-05-15 15:25:00',NULL,NULL,NULL,NULL),(5,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-05-20 15:00:00','CHECKED_OUT',0,130.00,1,0,'2026-05-20 15:30:00','2026-05-20 19:30:00',NULL,NULL,NULL,NULL),(6,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,3,'2026-05-25 09:00:00','CHECKED_OUT',0,135.00,1,0,'2026-05-25 09:45:00','2026-05-25 13:45:00',NULL,NULL,NULL,NULL),(7,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,1,'2026-06-01 12:00:00','CHECKED_OUT',0,50.00,1,0,'2026-06-01 12:15:00','2026-06-01 16:15:00',NULL,NULL,NULL,NULL),(8,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-06-05 13:00:00','CHECKED_OUT',0,130.00,1,0,'2026-06-05 13:20:00','2026-06-05 17:20:00',NULL,NULL,NULL,NULL),(9,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,2,'2026-06-22 09:00:00','CHECKED_OUT',0,100.00,1,0,'2026-06-22 09:15:00','2026-06-25 17:42:45',NULL,NULL,NULL,NULL),(10,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,3,'2026-06-22 10:00:00','CHECKED_OUT',0,195.00,1,0,'2026-06-22 10:20:00','2026-06-25 18:02:54',NULL,NULL,NULL,NULL),(11,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,1,'2026-06-23 14:00:00','CHECKED_OUT',0,45.00,1,0,'2026-06-23 14:10:00','2026-06-25 18:02:56',NULL,NULL,NULL,NULL),(12,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,4,'2026-06-24 11:00:00','CHECKED_OUT',1,200.00,1,0,'2026-06-24 11:25:00','2026-06-24 13:14:21',NULL,NULL,NULL,NULL),(13,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-06-25 15:00:00','CHECKED_OUT',0,130.00,1,0,'2026-06-25 15:30:00','2026-06-25 18:02:58',NULL,NULL,NULL,NULL),(14,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,3,'2026-06-26 09:00:00','CHECKED_OUT',0,135.00,1,0,'2026-06-26 09:45:00','2026-06-25 18:03:01',NULL,NULL,NULL,NULL),(15,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,1,'2026-06-27 12:00:00','CHECKED_OUT',0,50.00,1,0,'2026-06-27 12:15:00','2026-06-25 18:03:03',NULL,NULL,NULL,NULL),(16,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-06-22 21:00:00','CANCELLED',0,130.00,1,2,'2026-06-28 13:20:00',NULL,NULL,NULL,NULL,NULL),(17,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,3,'2026-06-22 20:00:00','CHECKED_OUT',0,150.00,1,0,'2026-06-22 20:25:32','2026-06-25 17:42:38',NULL,NULL,'2026-06-22 10:00:00','CONFIRMATION'),(18,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-06-22 10:30:00','CANCELLED',0,130.00,1,0,NULL,NULL,NULL,NULL,'2026-06-22 11:00:00','CONFIRMATION'),(19,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,1,'2026-06-22 13:00:00','CANCELLED',0,45.00,1,0,NULL,NULL,NULL,NULL,'2026-06-22 14:00:00','CONFIRMATION'),(20,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,5,'2026-07-02 08:00:00','CONFIRMED',1,250.00,1,0,NULL,NULL,NULL,NULL,'2026-06-23 09:00:00','CONFIRMATION'),(21,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,4,'2026-07-03 11:00:00','CONFIRMED',0,260.00,1,0,NULL,NULL,NULL,NULL,'2026-06-24 12:00:00','CONFIRMATION'),(22,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,2,'2026-07-04 12:00:00','CANCELLED',0,90.00,1,0,NULL,NULL,NULL,NULL,'2026-06-25 13:00:00','CONFIRMATION'),(23,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,3,'2026-07-05 09:30:00','CANCELLED',0,150.00,1,0,NULL,NULL,NULL,NULL,'2026-06-26 10:30:00','CONFIRMATION'),(24,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,1,'2026-07-06 15:00:00','CANCELLED',0,65.00,1,0,NULL,NULL,NULL,NULL,'2026-06-27 16:00:00','CONFIRMATION'),(25,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,3,'2026-07-07 10:00:00','CONFIRMED',1,135.00,1,0,NULL,NULL,NULL,NULL,'2026-06-28 11:00:00','CONFIRMATION'),(26,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,2,'2026-07-08 14:30:00','CONFIRMED',0,100.00,1,0,NULL,NULL,NULL,NULL,'2026-06-29 15:30:00','CONFIRMATION'),(27,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,2,'2026-06-25 09:00:00','CANCELLED',0,100.00,0,0,NULL,NULL,'2026-06-25 08:30:00','2026-06-25 10:00:00',NULL,NULL),(28,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,3,'2026-06-26 10:00:00','CANCELLED',0,195.00,0,0,NULL,NULL,'2026-06-26 09:30:00','2026-06-26 11:00:00',NULL,NULL),(29,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,1,'2026-06-27 14:00:00','CANCELLED',0,45.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(30,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,4,'2026-06-28 11:00:00','CANCELLED',1,200.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(31,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-06-29 15:00:00','CANCELLED',0,130.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(32,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,2,'2026-06-10 09:00:00','CANCELLED',0,100.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(33,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,3,'2026-06-11 10:00:00','CANCELLED',0,195.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(34,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,1,'2026-06-12 14:00:00','CANCELLED',0,45.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(35,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,4,'2026-06-13 11:00:00','CANCELLED',1,200.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(36,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-06-14 15:00:00','CANCELLED',0,130.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(37,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,3,'2026-06-15 09:00:00','CANCELLED',0,135.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(38,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,1,'2026-06-16 12:00:00','CANCELLED',0,50.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(39,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-06-17 13:00:00','CANCELLED',0,130.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(40,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,4,'2026-06-18 10:00:00','CANCELLED',1,180.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(41,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,3,'2026-06-19 15:00:00','CANCELLED',0,150.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(42,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,2,'2026-07-09 09:00:00','CANCELLED',0,100.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(43,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,3,'2026-07-10 10:00:00','CANCELLED',0,195.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(44,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,1,'2026-07-11 14:00:00','CANCELLED',0,45.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(45,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,4,'2026-07-12 11:00:00','CANCELLED',1,200.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(46,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,2,'2026-07-13 15:00:00','CANCELLED',0,130.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(47,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,2,'2026-06-08 09:00:00','SYSTEM_CANCEL',0,100.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(48,'123456789','Yossi Israeli','yossi@example.com','0521234567',2,3,'2026-06-09 10:00:00','SYSTEM_CANCEL',0,195.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(49,'123456789','Yossi Israeli','yossi@example.com','0521234567',3,1,'2026-06-23 14:00:00','SYSTEM_CANCEL',0,45.00,0,0,NULL,NULL,NULL,NULL,NULL,'WAITLIST_CONFIRMATION_TIMEOUT'),(50,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,2,'2026-06-24 11:00:00','SYSTEM_CANCEL',0,100.00,0,0,NULL,NULL,NULL,NULL,'2026-06-22 14:00:00','REMINDER_CONFIRMATION_TIMEOUT'),(1283673,'58c44a45-0cac-4ffb-8fee-7ca1f5e9a7a3','awlfn','oinwaoifnao@awnfoawf.com','0546139018',2,6,'2026-06-25 08:00:00','CANCELLED',0,331.50,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(1478548,'3cca5919-b47e-41a3-92c5-e744f7a61e75','benan bader','benan@ahlan.com','0503321323',1,6,'2026-06-24 15:00:00','CANCELLED',0,255.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(1542882,'3cca5919-b47e-41a3-92c5-e744f7a61e75','amer bader','benan@gmail.com','0503321323',1,2,'2026-06-26 08:00:00','CANCELLED',0,28.05,1,0,NULL,NULL,NULL,NULL,NULL,NULL),(1584775,'58c44a45-0cac-4ffb-8fee-7ca1f5e9a7a3','aofinawf','waofnoawf@aoinfaw.com','0546712923',1,6,'2026-06-24 08:00:00','CANCELLED',0,255.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(1673603,'b86c7441-0bfe-4c4e-a6a2-bbbfdd48ab50','ameer halavy','ameerhalavy@braude.com','050333333',1,6,'2026-07-01 08:00:00','CONFIRMED',0,255.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(1764172,'b1a3ee61-14ed-4c08-9ef0-95fc760cf559','fauzi','fauzia2@fnwaof.com','0546139013',2,2,'2026-06-24 11:00:00','CONFIRMED',0,110.50,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(2115872,'b86c7441-0bfe-4c4e-a6a2-bbbfdd48ab50','ameer halavy','ameerhalavy@braude.com','050333333',1,2,'2026-06-26 08:00:00','CANCELLED',0,85.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(2394897,'c498e231-3c6a-456f-8141-dd48ff0aefbc','aoiwnfaowf','aoinwfowa@aonwfawf.com','05471239172',1,7,'2026-06-24 08:00:00','CANCELLED',0,191.25,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(2724193,'58c44a45-0cac-4ffb-8fee-7ca1f5e9a7a3','awfawf','awfawf@aownfoawf.com','0541231231',1,6,'2026-06-24 09:00:00','CANCELLED',0,255.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(2743408,'123456789','Yossi Israeli','yossi@example.com','0502211325',1,1,'2026-07-05 16:00:00','CONFIRMED',0,38.25,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(3200858,'ef2109e6-4f17-4525-9b77-34b0ff6ea5c1','benan bader','benan@bader.com','0503321323',1,3,'2026-06-26 08:00:00','CHECKED_OUT',0,114.75,0,0,NULL,'2026-06-25 21:54:17',NULL,NULL,NULL,NULL),(3318245,'3cca5919-b47e-41a3-92c5-e744f7a61e75','jeff','mynameisjeff@gmail.com','0503321323',1,5,'2026-07-23 16:00:00','CONFIRMED',0,212.50,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(3632347,'3cca5919-b47e-41a3-92c5-e744f7a61e75','amer bader','benan@gmail.com','0503321323',3,3,'2026-06-30 08:00:00','CONFIRMED',0,114.75,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(3679253,'ea1596e6-bb9e-4271-ac9c-40c2d29e12b7',NULL,NULL,NULL,1,4,'2026-06-23 21:41:28','CHECKED_OUT',0,200.00,0,0,'2026-06-23 21:41:28','2026-06-23 22:05:56',NULL,NULL,NULL,NULL),(3710804,'3cca5919-b47e-41a3-92c5-e744f7a61e75','amer bader','benan@gmail.com','0503321323',1,6,'2026-07-22 08:00:00','CONFIRMED',0,255.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(3788500,'c498e231-3c6a-456f-8141-dd48ff0aefbc','k awfkawfaownf','aownfaowf@awfawf.com','0546123921',1,6,'2026-06-30 08:00:00','CANCELLED',0,159.38,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(3810234,'c498e231-3c6a-456f-8141-dd48ff0aefbc','awof nawfo','awonfaowf@awofnawf.com','0547129237',2,10,'2026-06-25 08:00:00','CANCELLED',0,328.19,1,0,NULL,NULL,NULL,NULL,NULL,NULL),(3866286,'b15872d7-2f68-49e8-9534-94d7f25e9cfc',NULL,NULL,NULL,1,2,'2026-06-23 21:53:45','CHECKED_OUT',0,100.00,0,0,'2026-06-23 21:53:45','2026-06-23 22:52:08',NULL,NULL,NULL,NULL),(3878962,'3cca5919-b47e-41a3-92c5-e744f7a61e75','benan bader','benan@gmail.com','0503321323',1,3,'2026-06-25 09:00:00','CANCELLED',0,56.10,1,0,NULL,NULL,NULL,NULL,NULL,NULL),(4076468,'b86c7441-0bfe-4c4e-a6a2-bbbfdd48ab50','ameer halavy','ameerhalavy@braude.com','050333333',1,6,'2026-07-05 16:00:00','CONFIRMED',0,255.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(4161674,'b86c7441-0bfe-4c4e-a6a2-bbbfdd48ab50','ameer halavy','ameerhalavy@braude.com','050333333',1,3,'2026-07-03 15:00:00','CONFIRMED',0,127.50,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(4183219,'3cca5919-b47e-41a3-92c5-e744f7a61e75','benan bader','benan@gmail.com','0503321323',1,16,'2026-06-25 12:00:00','CANCELLED',0,478.13,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(4417774,'c498e231-3c6a-456f-8141-dd48ff0aefbc','aowifnoaw','aoinfawo@awonfaowf.com','05412729317',1,12,'2026-07-02 11:00:00','CANCELLED',0,350.63,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(4598397,'c498e231-3c6a-456f-8141-dd48ff0aefbc','awafinaw','waofnoawf@aoinfaw.coma','05461239123',1,16,'2026-06-24 08:00:00','CANCELLED',0,478.13,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(5182579,'3cca5919-b47e-41a3-92c5-e744f7a61e75','amer bader','benan@gmail.com','0503321323',1,3,'2026-06-26 08:00:00','CANCELLED',0,63.75,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(5201990,'3cca5919-b47e-41a3-92c5-e744f7a61e75','amer bader','benan@gmail.com','0503321323',1,1,'2026-06-26 10:00:00','CONFIRMED',0,38.25,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(5566457,'ef2109e6-4f17-4525-9b77-34b0ff6ea5c1','benan bader','benan@bader.com','0503321323',2,3,'2026-06-26 08:00:00','CONFIRMED',0,149.18,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(5843449,'3cca5919-b47e-41a3-92c5-e744f7a61e75','amer bader','benan@gmail.com','0503321323',1,3,'2026-07-16 15:00:00','CONFIRMED',0,127.50,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(6340290,'b1a3ee61-14ed-4c08-9ef0-95fc760cf559','oiawfoawf','awiofba@aiowfoawf.com','0546139013',2,6,'2026-06-25 08:00:00','CONFIRMED',0,331.50,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(6440816,'b86c7441-0bfe-4c4e-a6a2-bbbfdd48ab50','ameer halavy','ameerhalavy@braude.com','050333333',1,1,'2026-07-01 12:00:00','CONFIRMED',0,42.50,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(6471165,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,6,'2026-07-04 16:00:00','SYSTEM_CANCEL',0,229.50,0,0,NULL,NULL,NULL,NULL,NULL,'WAITLIST_CONFIRMATION_TIMEOUT'),(7534927,'c498e231-3c6a-456f-8141-dd48ff0aefbc','awiofnaw','aownfawf@awfonawf.com','0547123012',1,9,'2026-06-28 15:00:00','CONFIRMED',0,224.40,1,0,NULL,NULL,NULL,NULL,NULL,NULL),(7667637,'3cca5919-b47e-41a3-92c5-e744f7a61e75','amer bader','benan@gmail.com','0503321323',1,2,'2026-06-26 08:00:00','CONFIRMED',0,85.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(7712976,'c498e231-3c6a-456f-8141-dd48ff0aefbc','awof nawof','anfowai@aowfnaowf.com','05461239123',1,7,'2026-06-24 09:00:00','CANCELLED',0,191.25,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(7749475,'b86c7441-0bfe-4c4e-a6a2-bbbfdd48ab50','ameer halaby','ameerhalavy@braude.com','0503333333',1,2,'2026-06-26 08:00:00','SYSTEM_CANCEL',0,85.00,0,0,NULL,NULL,NULL,NULL,NULL,'WAITLIST_CONFIRMATION_TIMEOUT'),(7849417,'123456789','Yossi Israeli','yossi@example.com','0521234567',1,1,'2026-07-05 16:00:00','CANCELLED',0,38.25,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(8657590,'58c44a45-0cac-4ffb-8fee-7ca1f5e9a7a3','awfoinaw','aownfa@awonfoawf.com','0546139013',1,6,'2026-06-28 15:00:00','CANCELLED',0,255.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(8913054,'3cca5919-b47e-41a3-92c5-e744f7a61e75','benan bader','benan@gmail.com','0503321323',1,3,'2026-06-24 17:00:00','CANCELLED',0,56.10,1,0,NULL,NULL,NULL,NULL,NULL,NULL),(9206204,'3cca5919-b47e-41a3-92c5-e744f7a61e75','benan bader','benan@gmail.com','0503321323',1,6,'2026-07-29 12:00:00','CONFIRMED',0,255.00,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(9877945,'d5a06d65-634e-4595-a789-3907376f4a8a','Visitor Guest','kfwdwe@ijrge.com','0502131234',2,3,'2026-06-25 11:00:00','CONFIRMED',0,165.75,0,0,NULL,NULL,NULL,NULL,NULL,NULL),(9946939,'b86c7441-0bfe-4c4e-a6a2-bbbfdd48ab50','ameer halavy','ameerhalavy@braude.com','050333333',1,4,'2026-07-04 16:00:00','CHECKED_OUT',0,170.00,0,0,NULL,'2026-06-25 18:07:37',NULL,NULL,NULL,NULL);
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
INSERT INTO `employee` VALUES (1,'1001',9000,1,'park_worker'),(2,'1002',14000,1,'park_manager'),(3,'1003',20000,1,'department_manager'),(4,'1004',8500,1,'service_rep');
/*!40000 ALTER TABLE `employee` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `managerrequests`
--

DROP TABLE IF EXISTS `managerrequests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `managerrequests` (
  `request_Id` varchar(36) NOT NULL,
  `employee_id` int NOT NULL,
  `dep_manager_id` int DEFAULT NULL,
  `requestTitle` varchar(150) NOT NULL,
  `parameter_type` enum('MAX_CAPACITY','GAP','DEFAULT_STAY_TIME','PRICE_PER_PERSON') NOT NULL,
  `new_value` int NOT NULL,
  `park_id` int NOT NULL,
  `request_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
-- Dumping data for table `managerrequests`
--

LOCK TABLES `managerrequests` WRITE;
/*!40000 ALTER TABLE `managerrequests` DISABLE KEYS */;
INSERT INTO `managerrequests` VALUES ('06329b06-e6e9-474a-9827-4cc4b49eeac1',2,3,'Reserve 3 spots for walk-in visitors','GAP',3,1,'2026-06-25 17:56:01',1),('22cf40ba-d5ff-4e46-a1a1-782ee6f2677b',2,3,'Change total park capacity to 3','MAX_CAPACITY',3,1,'2026-06-25 17:39:58',1),('2869633d-d622-420f-afb0-b4ad58caa36b',2,3,'Set default visitor stay duration to 5 hours','DEFAULT_STAY_TIME',5,1,'2026-06-25 15:41:33',1),('33f3207a-c2d3-411a-bb85-c448c9b739ed',2,3,'Change total park capacity to 10','MAX_CAPACITY',10,1,'2026-06-25 17:55:58',1),('589dfc96-df42-4fa6-9c1c-596a37decaa9',2,3,'Reserve 30 spots for walk-in visitors','GAP',30,1,'2026-06-25 12:05:28',1),('60c3af24-6892-404a-a85c-5fe4dfef6a52',2,3,'Set default visitor stay duration to 4 hours','DEFAULT_STAY_TIME',4,1,'2026-06-25 12:05:28',1),('67ed9557-50fe-4faa-b386-56215f2c2bfe',2,3,'Change total park capacity to 500','MAX_CAPACITY',500,1,'2026-06-25 15:40:38',1),('71864128-a905-4f1c-854f-6462cc650d3c',2,3,'Reserve 10 spots for walk-in visitors','GAP',10,1,'2026-06-25 12:05:28',1),('9ba58a4b-07db-4ef4-b1c2-3bfa10ddd057',2,3,'Reserve 40 spots for walk-in visitors','GAP',40,1,'2026-06-25 12:05:28',1),('a8535f36-94e8-4e5e-b2ed-21cabaa8e8fb',2,3,'Reserve 1 spots for walk-in visitors','GAP',1,1,'2026-06-25 17:43:56',1),('b12331aa-54d5-46c0-aec8-c78dbc80c0a6',2,3,'Set default visitor stay duration to 6 hours','DEFAULT_STAY_TIME',6,1,'2026-06-25 12:14:25',1),('c249327e-19b7-4041-8cb7-51f3c59f9d34',2,3,'Set default visitor stay duration to 5 hours','DEFAULT_STAY_TIME',5,1,'2026-06-25 12:05:28',1),('d1440b89-7a7b-4838-9a80-8e4699cfc7a5',2,3,'Change total park capacity to 15','MAX_CAPACITY',15,1,'2026-06-25 12:05:28',0),('dd1f81b5-6bd8-48c2-9506-17a409f6e9fe',2,3,'Change total park capacity to 250','MAX_CAPACITY',250,1,'2026-06-25 12:05:28',0),('e4eaeda9-e26d-407f-a1e4-86c50df0be34',2,3,'Change total park capacity to 290','MAX_CAPACITY',290,1,'2026-06-25 12:05:28',0),('e6b4b474-335d-408d-b85c-17c7b9d9fbc9',2,3,'Reserve 30 spots for walk-in visitors','GAP',30,1,'2026-06-25 12:05:28',0);
/*!40000 ALTER TABLE `managerrequests` ENABLE KEYS */;
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
INSERT INTO `park` VALUES (1,'Banias Nature Reserve',0,10,3,5,50.00,3,2),(2,'Masada National Park',0,500,50,240,65.00,3,NULL),(3,'Ein Gedi Reserve',0,250,25,180,45.00,3,NULL);
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
  `report_type` enum('VISITORS','USAGE') DEFAULT NULL,
  `from_date` date DEFAULT NULL,
  `to_date` date DEFAULT NULL,
  `submitted_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`report_id`),
  UNIQUE KEY `uq_report_period` (`park_id`,`report_type`,`from_date`,`to_date`),
  KEY `fk_report_park` (`park_id`),
  KEY `fk_report_employee` (`employee_id`),
  CONSTRAINT `fk_report_employee` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`employee_id`),
  CONSTRAINT `fk_report_park` FOREIGN KEY (`park_id`) REFERENCES `park` (`park_id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report`
--

LOCK TABLES `report` WRITE;
/*!40000 ALTER TABLE `report` DISABLE KEYS */;
INSERT INTO `report` VALUES (1,1,'Park Visitors Report - JUNE 2026','Visitors Report\n\nPeriod: 2026-06-01 to 2026-06-30\nIndividual Visitors: 12\nOrganized Groups: 4\nTotal Visitors: 16',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(2,1,'Park Usage Report - JUNE 2026','Usage Report\n\nPeriod: 2026-06-01 to 2026-06-30\n\n2026-06-01: 1/300 (0.3%)\n2026-06-22: 5/300 (1.7%)\n2026-06-23: 6/300 (2.0%)\n2026-06-24: 4/300 (1.3%)\n',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(3,1,'Park Visitors Report - JUNE 2026','Visitors Report\n\nPeriod: 2026-06-01 to 2026-06-30\nIndividual Visitors: 12\nOrganized Groups: 4\nTotal Visitors: 16',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(4,1,'Park Visitors Report - JUNE 2026','Visitors Report\n\nPeriod: 2026-06-01 to 2026-06-30\nIndividual Visitors: 12\nOrganized Groups: 4\nTotal Visitors: 16',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(5,1,'Park Visitors Report - OCTOBER 2026','Visitors Report\n\nPeriod: 2026-10-01 to 2026-10-31\nIndividual Visitors: 0\nOrganized Groups: 0\nTotal Visitors: 0',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(6,1,'Park Visitors Report - JUNE 2026','Visitors Report\n\nPeriod: 2026-06-01 to 2026-06-30\nIndividual Visitors: 12\nOrganized Groups: 4\nTotal Visitors: 16',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(7,1,'Park Visitors Report - OCTOBER 2026','Visitors Report\n\nPeriod: 2026-10-01 to 2026-10-31\nIndividual Visitors: 0\nOrganized Groups: 0\nTotal Visitors: 0',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(8,1,'Park Visitors Report - JUNE 2026','Visitors Report\n\nPeriod: 2026-06-01 to 2026-06-30\nIndividual Visitors: 12\nOrganized Groups: 4\nTotal Visitors: 16',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(9,1,'Park Visitors Report - SEPTEMBER 2026','Visitors Report\n\nPeriod: 2026-09-01 to 2026-09-30\nIndividual Visitors: 0\nOrganized Groups: 0\nTotal Visitors: 0',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(10,1,'Park Visitors Report - JULY 2026','Visitors Report\n\nPeriod: 2026-07-01 to 2026-07-31\nIndividual Visitors: 0\nOrganized Groups: 0\nTotal Visitors: 0',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(11,1,'Park Visitors Report - JULY 2026','Visitors Report\n\nPeriod: 2026-07-01 to 2026-07-31\nIndividual Visitors: 0\nOrganized Groups: 0\nTotal Visitors: 0',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(12,1,'Park Visitors Report - JULY 2026','Visitors Report\n\nPeriod: 2026-07-01 to 2026-07-31\nIndividual Visitors: 0\nOrganized Groups: 0\nTotal Visitors: 0',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(13,1,'Park Visitors Report - JUNE 2026','Visitors Report\n\nPeriod: 2026-06-01 to 2026-06-30\nIndividual Visitors: 12\nOrganized Groups: 4\nTotal Visitors: 16',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(14,1,'Park Visitors Report - JUNE 2026','Visitors Report\n\nPeriod: 2026-06-01 to 2026-06-30\nIndividual Visitors: 12\nOrganized Groups: 4\nTotal Visitors: 16',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(15,1,'Park Usage Report - JUNE 2026','Usage Report\n\nPeriod: 2026-06-01 to 2026-06-30\n\n2026-06-01: 1/300 (0.3%)\n2026-06-22: 5/300 (1.7%)\n2026-06-23: 6/300 (2.0%)\n2026-06-24: 4/300 (1.3%)\n',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(16,1,'Park Visitors Report - JUNE 2026','Visitors Report\n\nPeriod: 2026-06-01 to 2026-06-30\nIndividual Visitors: 12\nOrganized Groups: 4\nTotal Visitors: 16',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(17,1,'Park Visitors Report - JUNE 2026','Visitors Report\n\nPeriod: 2026-06-01 to 2026-06-30\nIndividual Visitors: 12\nOrganized Groups: 4\nTotal Visitors: 16',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(18,1,'Park Usage Report - JUNE 2026','Usage Report\n\nPeriod: 2026-06-01 to 2026-06-30\n\n2026-06-01: 1/300 (0.3%)\n2026-06-22: 5/300 (1.7%)\n2026-06-23: 6/300 (2.0%)\n2026-06-24: 4/300 (1.3%)\n',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(19,1,'Park Usage Report - JUNE 2026','Usage Report\n\nPeriod: 2026-06-24 to 2026-06-30\n\n2026-06-24: 4/300 (1.3%)\n',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(20,1,'Park Usage Report - JUNE 2026','Usage Report\n\nPeriod: 2026-06-25 to 2026-06-30\n\n',2,NULL,NULL,NULL,'2026-06-25 14:20:54'),(21,1,'Park Visitors Report - JUNE 2026','Visitors Report\n\nPeriod: 2026-06-01 to 2026-06-30\nIndividual Visitors: 12\nOrganized Groups: 4\nTotal Visitors: 16',2,NULL,NULL,NULL,'2026-06-25 14:44:46'),(22,1,'Park Visitors Report - JUNE 2026','Visitors Report\n\nPeriod: 2026-06-01 to 2026-06-30\nIndividual Visitors: 12\nOrganized Groups: 4\nTotal Visitors: 16',2,NULL,NULL,NULL,'2026-06-25 14:46:07');
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
  `guide` tinyint(1) NOT NULL DEFAULT '0',
  `clubMember` tinyint(1) NOT NULL DEFAULT '0',
  `user_id` varchar(36) NOT NULL,
  `familyMembers` int DEFAULT NULL,
  `creditCard` varchar(30) DEFAULT NULL,
  PRIMARY KEY (`traveler_id`),
  KEY `fk_traveler_user` (`user_id`),
  CONSTRAINT `fk_traveler_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `traveler`
--

LOCK TABLES `traveler` WRITE;
/*!40000 ALTER TABLE `traveler` DISABLE KEYS */;
INSERT INTO `traveler` VALUES ('123456789',0,1,'123456789',3,NULL),('3cca5919-b47e-41a3-92c5-e744f7a61e75',0,0,'7ca059d8-f671-4ba1-a4f7-b03b3af2c02c',NULL,NULL),('50d833f7-d995-4ac7-a38c-0a3f6c796893',0,0,'41e100c6-69a1-4ca0-9318-119fbf7f1f86',NULL,NULL),('55f63fab-fae9-4e6a-95f8-39c0c8dfd6cf',0,0,'e848dce8-06f2-4389-bb72-efc2dffe0bc2',NULL,NULL),('58c44a45-0cac-4ffb-8fee-7ca1f5e9a7a3',0,0,'b682be29-5df5-4c14-aadb-dee385f7037e',NULL,NULL),('7395c6e2-d432-4c52-8164-d3c3a6d8bd30',0,0,'73154b7e-0cdb-4784-8b2f-881b13b38903',NULL,NULL),('865097545',0,0,'617191425',NULL,NULL),('9f3d925f-b7e8-4b50-9b2b-3f130bb4e706',0,0,'6c74844a-43e0-4556-a6b1-147ebaf562b5',NULL,NULL),('b15872d7-2f68-49e8-9534-94d7f25e9cfc',0,0,'30e11d98-800e-42ba-9b05-d5a1d45e51b3',NULL,NULL),('b1a3ee61-14ed-4c08-9ef0-95fc760cf559',0,0,'61944542-5b26-4b4a-bbfc-568588f5b562',NULL,NULL),('b86c7441-0bfe-4c4e-a6a2-bbbfdd48ab50',0,0,'aa36bf4a-8615-4218-988c-5f51aff58b3e',NULL,NULL),('c498e231-3c6a-456f-8141-dd48ff0aefbc',1,0,'00919d0e-9aa6-4dfd-af89-a1bdbb806d93',NULL,NULL),('c75fe90e-f2b0-4bb2-9892-2431b0088702',0,0,'136b59f0-d25e-452b-96aa-3b161592c2cd',NULL,NULL),('cb8afa25-14e9-490d-ad01-798f96bc361c',0,0,'80ca7f76-47c3-4cf1-baef-58743a55bec2',NULL,NULL),('d5a06d65-634e-4595-a789-3907376f4a8a',0,0,'7e4979f1-e73d-4ef3-8aa3-d148c300c337',NULL,NULL),('ea1596e6-bb9e-4271-ac9c-40c2d29e12b7',0,0,'f3330b41-e51b-4bea-bfc0-64eba86255a8',NULL,NULL),('ef2109e6-4f17-4525-9b77-34b0ff6ea5c1',0,1,'5b92fbca-1e51-4e4d-b537-14a873086549',4,NULL),('fdf0a431-abc6-4214-965b-c2e147f2c758',0,0,'f26b3d7e-e13e-4d50-a3dc-6d978a03215f',NULL,NULL);
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
INSERT INTO `user` VALUES ('00919d0e-9aa6-4dfd-af89-a1bdbb806d93',NULL,NULL,'Visitor','Guest',NULL,NULL,NULL),('1001','worker1','1234','Dana','Cohen','dana@gonature.local','0500000001',NULL),('1002','pmanager','1234','Ronen','Levi','ronen@gonature.local','0500000002',NULL),('1003','depmgr','1234','Sara','Mizrahi','sara@gonature.local','0500000003',NULL),('1004','service','1234','Omer','Katz','omer@gonature.local','0500000004',NULL),('123456789',NULL,NULL,'Yossi','Israeli','yossi@example.com','0521234567',NULL),('136b59f0-d25e-452b-96aa-3b161592c2cd',NULL,NULL,'Visitor','Guest',NULL,NULL,NULL),('30e11d98-800e-42ba-9b05-d5a1d45e51b3',NULL,NULL,'Visitor','Guest',NULL,NULL,NULL),('41e100c6-69a1-4ca0-9318-119fbf7f1f86',NULL,NULL,'Visitor','Guest',NULL,NULL,NULL),('5b92fbca-1e51-4e4d-b537-14a873086549',NULL,NULL,'benan','bader','benan@bader.com','0503321323','319117453'),('617191425',NULL,NULL,'Visitor','Guest',NULL,NULL,NULL),('61944542-5b26-4b4a-bbfc-568588f5b562',NULL,NULL,'Visitor','Guest','fbawfbawf@oawnfoawf.com','0546139013',NULL),('6c74844a-43e0-4556-a6b1-147ebaf562b5',NULL,NULL,'Visitor','Guest',NULL,NULL,NULL),('73154b7e-0cdb-4784-8b2f-881b13b38903',NULL,NULL,'Visitor','Guest',NULL,NULL,NULL),('7ca059d8-f671-4ba1-a4f7-b03b3af2c02c',NULL,NULL,'amer','bader','benan@gmail.com','0503321323',NULL),('7e4979f1-e73d-4ef3-8aa3-d148c300c337',NULL,NULL,'Visitor','Guest',NULL,NULL,NULL),('80ca7f76-47c3-4cf1-baef-58743a55bec2',NULL,NULL,'Visitor','Guest',NULL,NULL,NULL),('aa36bf4a-8615-4218-988c-5f51aff58b3e',NULL,NULL,'ameer','halavy','ameerhalavy@braude.com','050333333',NULL),('b682be29-5df5-4c14-aadb-dee385f7037e',NULL,NULL,'Visitor','Guest',NULL,NULL,NULL),('e848dce8-06f2-4389-bb72-efc2dffe0bc2',NULL,NULL,'Visitor','Guest',NULL,NULL,NULL),('f26b3d7e-e13e-4d50-a3dc-6d978a03215f',NULL,NULL,'Visitor','Guest',NULL,NULL,NULL),('f3330b41-e51b-4bea-bfc0-64eba86255a8',NULL,NULL,'Visitor','Guest',NULL,NULL,NULL);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `waitinglist`
--

DROP TABLE IF EXISTS `waitinglist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `waitinglist` (
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
-- Dumping data for table `waitinglist`
--

LOCK TABLES `waitinglist` WRITE;
/*!40000 ALTER TABLE `waitinglist` DISABLE KEYS */;
INSERT INTO `waitinglist` VALUES ('08fe56a9-413b-41d3-975a-7e8b503e9a8a',1,'2026-07-04 16:00:00','OPEN','2026-06-25 18:04:01'),('4019d4d4-9c01-4c34-99c2-5c8bbec22da2',1,'2026-06-24 08:00:00','OPEN','2026-06-23 23:00:08'),('90030344-f671-4818-8966-4d8e16f1ce27',1,'2026-06-24 09:00:00','OPEN','2026-06-23 23:18:29'),('a7a0fe21-e6bf-4c0c-a421-999b75024ab3',1,'2026-06-26 08:00:00','OPEN','2026-06-25 17:42:54'),('f48505c9-a231-4502-ba1b-cd47f3e524e7',1,'2026-06-28 15:00:00','OPEN','2026-06-23 23:20:22');
/*!40000 ALTER TABLE `waitinglist` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `waitinglistentry`
--

DROP TABLE IF EXISTS `waitinglistentry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `waitinglistentry` (
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
  CONSTRAINT `fk_wle_list` FOREIGN KEY (`waitingList_id`) REFERENCES `waitinglist` (`waitingList_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `waitinglistentry`
--

LOCK TABLES `waitinglistentry` WRITE;
/*!40000 ALTER TABLE `waitinglistentry` DISABLE KEYS */;
INSERT INTO `waitinglistentry` VALUES ('1c4c333a-fa47-4093-bb16-40629d6e8366','a7a0fe21-e6bf-4c0c-a421-999b75024ab3',7749475,'2026-06-25 17:49:08','EXPIRED','2026-06-25 17:49:30'),('36396946-98dd-4935-b004-4050e8f6f16d','4019d4d4-9c01-4c34-99c2-5c8bbec22da2',4598397,'2026-06-23 23:00:08','CANCELLED','2026-06-23 23:14:26'),('45246d80-e2bb-4002-9ab4-fe12c13f4e1c','a7a0fe21-e6bf-4c0c-a421-999b75024ab3',7667637,'2026-06-25 17:53:49','CONFIRMED','2026-06-25 17:54:05'),('6b9b7086-93ca-46d9-b346-f73bfee8c42a','08fe56a9-413b-41d3-975a-7e8b503e9a8a',6471165,'2026-06-25 18:04:01','EXPIRED','2026-06-25 18:07:50'),('79d32fe1-afed-4f6f-aa43-0edd2ec9c702','4019d4d4-9c01-4c34-99c2-5c8bbec22da2',2394897,'2026-06-23 23:14:44','CANCELLED','2026-06-23 23:16:36'),('d3680761-06f2-4e7c-9d9e-684504be0d36','a7a0fe21-e6bf-4c0c-a421-999b75024ab3',5182579,'2026-06-25 17:42:54','CANCELLED','2026-06-25 17:45:29'),('d6082be5-36bb-4fee-9477-6f156d95c8a3','90030344-f671-4818-8966-4d8e16f1ce27',7712976,'2026-06-23 23:18:29','CANCELLED','2026-06-23 23:19:32'),('f7cbb1d3-543d-4ebb-936f-97f9d30f2a0b','f48505c9-a231-4502-ba1b-cd47f3e524e7',7534927,'2026-06-23 23:20:22','CONFIRMED','2026-06-23 23:46:30');
/*!40000 ALTER TABLE `waitinglistentry` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-25 22:07:38
