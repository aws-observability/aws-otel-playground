# From https://aws.amazon.com/getting-started/hands-on/boosting-mysql-database-performance-with-amazon-elasticache-for-redis/3/

CREATE TABLE planet
(
    id   INT UNSIGNED AUTO_INCREMENT,
    name VARCHAR(30),
    PRIMARY KEY (id)
);

INSERT INTO planet (name)
VALUES ('Mercury'),
       ('Venus'),
       ('Earth'),
       ('Mars'),
       ('Jupiter'),
       ('Saturn'),
       ('Uranus'),
       ('Neptune');
