\c mini_dish_db
SET ROLE mini_dish_db_manager;

--ENUM types
CREATE TYPE CategoryEnum AS ENUM (
    'VEGETABLE',
    'ANIMAL',
    'MARINE',
    'DAIRY',
    'OTHER'
);

CREATE TYPE DishTypeEnum AS ENUM (
    'START',
    'MAIN',
    'DESSERT'
);

--Dish table
CREATE TABLE Dish (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    dish_type DishTypeEnum NOT NULL
);

--Ingredient table
CREATE TABLE Ingredient (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    category CategoryEnum NOT NULL,
    id_dish INT,
    CONSTRAINT fk_dish
        FOREIGN KEY (id_dish)
        REFERENCES Dish (id)
        ON DELETE SET NULL
);