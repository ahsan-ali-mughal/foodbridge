-- Each service owns its own schema. Business schemas are migrated by each
-- service's own Flyway migrations on startup; this script only needs to
-- create the empty databases (and the Quartz scheduler schemas, whose
-- QRTZ_* tables are created by Spring Boot's Quartz auto-schema-init).

CREATE DATABASE IF NOT EXISTS foodbridge_auth CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS foodbridge_claims CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS foodbridge_logistics CHARACTER SET utf8mb4;

-- Dedicated Quartz JobStore schemas, one per service that runs a scheduled
-- job — deliberately separate from each service's business schema above.
CREATE DATABASE IF NOT EXISTS foodbridge_listing_quartz CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS foodbridge_notification_quartz CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS foodbridge_document_quartz CHARACTER SET utf8mb4;

CREATE USER IF NOT EXISTS 'foodbridge'@'%' IDENTIFIED BY 'foodbridge';
GRANT ALL PRIVILEGES ON foodbridge_auth.* TO 'foodbridge'@'%';
GRANT ALL PRIVILEGES ON foodbridge_claims.* TO 'foodbridge'@'%';
GRANT ALL PRIVILEGES ON foodbridge_logistics.* TO 'foodbridge'@'%';
GRANT ALL PRIVILEGES ON foodbridge_listing_quartz.* TO 'foodbridge'@'%';
GRANT ALL PRIVILEGES ON foodbridge_notification_quartz.* TO 'foodbridge'@'%';
GRANT ALL PRIVILEGES ON foodbridge_document_quartz.* TO 'foodbridge'@'%';
FLUSH PRIVILEGES;
