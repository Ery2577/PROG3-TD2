package com.TD2.Repository;

import com.TD2.DBConnection;
import com.TD2.Model.Dish;
import com.TD2.Model.DishTypeEnum;
import com.TD2.Model.Ingredient;
import com.TD2.Model.CategoryEnum;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataRetriever {

    // 6.a) findDishById : Récupère un plat et ses ingrédients
    public Dish findDishById(Integer id) {
        String sqlDish = "SELECT * FROM Dish WHERE id = ?";
        String sqlIngredients = "SELECT * FROM Ingredient WHERE id_dish = ?";

        try (Connection conn = DBConnection.getConnection()) {
            // 1. Récupérer le plat
            PreparedStatement psDish = conn.prepareStatement(sqlDish);
            psDish.setInt(1, id);
            ResultSet rsDish = psDish.executeQuery();

            if (rsDish.next()) {
                Dish dish = new Dish();
                dish.setId(rsDish.getInt("id"));
                dish.setName(rsDish.getString("name"));
                dish.setDishType(DishTypeEnum.valueOf(rsDish.getString("dish_type")));

                // 2. Récupérer les ingrédients du plat
                PreparedStatement psIng = conn.prepareStatement(sqlIngredients);
                psIng.setInt(1, id);
                ResultSet rsIng = psIng.executeQuery();

                List<Ingredient> ingredients = new ArrayList<>();
                while (rsIng.next()) {
                    ingredients.add(mapResultSetToIngredient(rsIng));
                }
                dish.setIngredients(ingredients);
                return dish;
            } else {
                throw new RuntimeException("Plat non trouvé avec l'id : " + id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 6.b) findIngredients : Pagination
    public List<Ingredient> findIngredients(int page, int size) {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT * FROM Ingredient LIMIT ? OFFSET ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, size);
            ps.setInt(2, (page - 1) * size);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ingredients.add(mapResultSetToIngredient(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ingredients;
    }

    // 6.c) createIngredients : Avec principe d'ATOMICITÉ
    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Début de la transaction

            String checkSql = "SELECT COUNT(*) FROM Ingredient WHERE name = ?";
            String insertSql = "INSERT INTO Ingredient (name, price, category) VALUES (?, ?, ?::category_enum)";

            for (Ingredient ing : newIngredients) {
                // Vérifier si l'ingrédient existe déjà
                PreparedStatement checkPs = conn.prepareStatement(checkSql);
                checkPs.setString(1, ing.getName());
                ResultSet rs = checkPs.executeQuery();
                rs.next();
                if (rs.getInt(1) > 0) {
                    conn.rollback(); // Annuler tout si un ingrédient existe déjà
                    throw new RuntimeException("L'ingrédient existe déjà : " + ing.getName());
                }

                // Insertion
                PreparedStatement insertPs = conn.prepareStatement(insertSql);
                insertPs.setString(1, ing.getName());
                insertPs.setDouble(2, ing.getPrice());
                insertPs.setString(3, ing.getCategory().name());
                insertPs.executeUpdate();
            }

            conn.commit(); // Valider tout si aucune erreur
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw new RuntimeException("Erreur lors de la création : " + e.getMessage());
        }
        return newIngredients;
    }

    // 6.d) saveDish : Insertion ou Mise à jour
    public Dish saveDish(Dish dishToSave) {

        return dishToSave;
    }

    // 6.e) findDishsByIngredientName
    public List<Dish> findDishsByIngredientName(String ingredientName) {
        List<Dish> dishes = new ArrayList<>();
        String sql = "SELECT DISTINCT d.* FROM Dish d JOIN Ingredient i ON d.id = i.id_dish WHERE i.name ILIKE ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + ingredientName + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Dish d = new Dish();
                d.setId(rs.getInt("id"));
                d.setName(rs.getString("name"));
                dishes.add(d);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return dishes;
    }

    // 6.f) findIngredientsByCriteria (Recherche dynamique)
    public List<Ingredient> findIngredientsByCriteria(String name, CategoryEnum category, String dishName, int page, int size) {
        List<Ingredient> ingredients = new ArrayList<>();
        // Construction dynamique de la requête SQL...
        return ingredients;
    }

    // Méthode utilitaire pour transformer un ResultSet en objet Ingredient
    private Ingredient mapResultSetToIngredient(ResultSet rs) throws SQLException {
        Ingredient ing = new Ingredient();
        ing.setId(rs.getInt("id"));
        ing.setName(rs.getString("name"));
        ing.setPrice(rs.getDouble("price"));
        ing.setCategory(CategoryEnum.valueOf(rs.getString("category")));
        return ing;
    }
}