DROP TABLE IF EXISTS ingredient;
DROP TABLE IF EXISTS dish;
DROP TYPE IF EXISTS category_enum;
DROP TYPE IF EXISTS dish_type_enum;


CREATE TYPE dish_type_enum AS ENUM ('START', 'MAIN', 'DESSERT');
CREATE TYPE category_enum AS ENUM ('VEGETABLE', 'ANIMAL', 'MARINE', 'DAIRY', 'OTHER');

CREATE TABLE dish (
                      id SERIAL PRIMARY KEY,
                      name VARCHAR(255) NOT NULL,
                      dish_type dish_type_enum NOT NULL
);

CREATE TABLE ingredient (
                            id SERIAL PRIMARY KEY,
                            name VARCHAR(255) NOT NULL,
                            price DOUBLE PRECISION,
                            category category_enum,
                            id_dish INT REFERENCES dish(id)
);

GRANT ALL PRIVILEGES ON TABLE dish TO mini_dish_db_manager;
GRANT ALL PRIVILEGES ON TABLE ingredient TO mini_dish_db_manager;