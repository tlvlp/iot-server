DROP DATABASE IF EXISTS `tlvlp_iot`;
CREATE DATABASE `tlvlp_iot`;
USE `tlvlp_iot`;

CREATE TABLE `units`
(
    `id`            bigint(20)   NOT NULL AUTO_INCREMENT,
    `project`       varchar(255) NOT NULL,
    `name`          varchar(255) NOT NULL,
    `active`        bit(1)       NOT NULL,
    `last_seen_utc` datetime(6)  NOT NULL,
    `control_topic` varchar(255) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_520_ci;

CREATE TABLE `modules`
(
    `id`      bigint(20) NOT NULL AUTO_INCREMENT,
    `unit_id` bigint(20)   DEFAULT NULL,
    `module`  varchar(255) DEFAULT NULL,
    `name`    varchar(255) DEFAULT NULL,
    `value`   double     NOT NULL,
    `active`  bit(1)       NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE `unit_logs`
(
    `id`        bigint(20)   NOT NULL AUTO_INCREMENT,
    `unit_id`   bigint(20)   NOT NULL,
    `log_entry` varchar(255) NOT NULL,
    `time_utc`  datetime(6)  NOT NULL,
    `type`      varchar(50)  NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_520_ci;

# CREATE TABLE `users`
# (
#     `id`         bigint(20)   NOT NULL AUTO_INCREMENT,
#     `active`     bit(1)       NOT NULL,
#     `email`      varchar(255) NOT NULL,
#     `first_name` varchar(255) NOT NULL,
#     `last_name`  varchar(255) NOT NULL,
#     `password`   varchar(255) NOT NULL,
#     `user_name`  varchar(255) NOT NULL,
#     PRIMARY KEY (`id`)
# ) ENGINE = InnoDB
#   DEFAULT CHARSET = utf8mb4
#   COLLATE = utf8mb4_unicode_520_ci;


# CREATE TABLE `scheduled_events`
# (
#     `id`            bigint(20)   NOT NULL AUTO_INCREMENT,
#     `content`       tinyblob     NOT NULL,
#     `cron_schedule` varchar(255) NOT NULL,
#     `info`          varchar(255) NOT NULL,
#     `type`          int(11)      NOT NULL,
#     `unit_id`       bigint(20)   NOT NULL,
#     `updated_at`    datetime(6)  NOT NULL,
#     `updated_by`    bigint(20)   NOT NULL,
#     PRIMARY KEY (`id`)
# ) ENGINE = InnoDB
#   DEFAULT CHARSET = utf8mb4
#   COLLATE = utf8mb4_unicode_520_ci;
