# --- First database schema

# --- !Ups
CREATE TABLE users (
	email VARCHAR( 255 ) NOT NULL PRIMARY KEY,
	firstName VARCHAR( 255 ) NOT NULL,
	lastName VARCHAR( 255 ) NOT NULL,
	password VARCHAR( 255 ) NOT NULL,
	lastFridgeUpdate DATETIME NOT NULL
);

CREATE TABLE shots (
id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
data LONGBLOB NOT NULL,
recipe INT NOT NULL
);

CREATE TABLE ingredients (
id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
name VARCHAR( 255 ) NOT NULL,
quantity DOUBLE 	NOT NULL,
unities VARCHAR( 255 ),
recipe INT NOT NULL
);

CREATE TABLE fridge (
id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
name VARCHAR( 255 ) NOT NULL,
quantity DOUBLE 	NOT NULL,
unities VARCHAR( 255 ),
category VARCHAR( 255 ) NOT NULL,
user VARCHAR( 255 ) NOT NULL,
date DATE NOT NULL
);

CREATE TABLE recipes (
id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
name VARCHAR( 255 ) NOT NULL,
author VARCHAR( 255 ),
preparation TEXT NOT NULL,
date DATE NOT NULL,
nb INT NOT NULL
);

CREATE TABLE grades (
id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
grade INT NOT NULL,
user VARCHAR( 255 ) NOT NULL,
recipe INT NOT NULL
);

# --- !Downs

drop table users;
drop table shots;
drop table ingredients;
drop table fridge;
drop table grades;
drop table recipes;
