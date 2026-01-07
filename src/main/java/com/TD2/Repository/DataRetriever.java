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
        String sqlDish = "SELECT * FROM dish WHERE id = ?";
        String sqlIngredients = "SELECT * FROM ingredient WHERE id_dish = ?";

        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement psDish = conn.prepareStatement(sqlDish);
            psDish.setInt(1, id);
            ResultSet rsDish = psDish.executeQuery();

            if (rsDish.next()) {
                Dish dish = new Dish();
                dish.setId(rsDish.getInt("id"));
                dish.setName(rsDish.getString("name"));
                dish.setDishType(DishTypeEnum.valueOf(rsDish.getString("dish_type")));

                // Récupération des ingrédients
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
                // Point 7.b : Lever une exception si non trouvé
                throw new RuntimeException("Plat non trouvé avec l'id : " + id);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur SQL : " + e.getMessage());
        }
    }

    // 6.b) findIngredients : Pagination
    public List<Ingredient> findIngredients(int page, int size) {
        List<Ingredient> ingredients = new ArrayList<>();
        String sql = "SELECT * FROM ingredient LIMIT ? OFFSET ?";

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

    // 6.c) createIngredients : ATOMICITÉ (Transaction)
    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Début transaction

            String checkSql = "SELECT COUNT(*) FROM ingredient WHERE name = ?";
            String insertSql = "INSERT INTO ingredient (name, price, category) VALUES (?, ?, ?::category_enum)";

            for (Ingredient ing : newIngredients) {
                // Vérification existence
                try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                    checkPs.setString(1, ing.getName());
                    ResultSet rs = checkPs.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        conn.rollback(); // Annulation totale
                        throw new RuntimeException("L'ingrédient existe déjà : " + ing.getName());
                    }
                }

                // Insertion
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    insertPs.setString(1, ing.getName());
                    insertPs.setDouble(2, ing.getPrice());
                    insertPs.setString(3, ing.getCategory().name());
                    insertPs.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw new RuntimeException("Erreur lors de la création atomique : " + e.getMessage());
        }
        return newIngredients;
    }

    // 6.d) saveDish : Upsert (Insertion ou Mise à jour)
    public Dish saveDish(Dish dishToSave) {
        // ON CONFLICT nécessite une contrainte UNIQUE sur l'ID ou le NOM
        String upsertDish = "INSERT INTO dish (id, name, dish_type) VALUES (?, ?, ?::dish_type_enum) " +
                "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, dish_type = EXCLUDED.dish_type";

        String cleanIngredients = "UPDATE ingredient SET id_dish = NULL WHERE id_dish = ?";
        String updateIngredient = "UPDATE ingredient SET id_dish = ? WHERE name = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Sauvegarder le plat
            try (PreparedStatement ps = conn.prepareStatement(upsertDish)) {
                ps.setInt(1, dishToSave.getId());
                ps.setString(2, dishToSave.getName());
                ps.setString(3, dishToSave.getDishType().name());
                ps.executeUpdate();
            }

            // 2. Dissocier les anciens ingrédients (point d du sujet)
            try (PreparedStatement psClean = conn.prepareStatement(cleanIngredients)) {
                psClean.setInt(1, dishToSave.getId());
                psClean.executeUpdate();
            }

            // 3. Associer les nouveaux
            if (dishToSave.getIngredients() != null) {
                try (PreparedStatement psIng = conn.prepareStatement(updateIngredient)) {
                    for (Ingredient ing : dishToSave.getIngredients()) {
                        psIng.setInt(1, dishToSave.getId());
                        psIng.setString(2, ing.getName());
                        psIng.executeUpdate();
                    }
                }
            }
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur saveDish : " + e.getMessage());
        }
        return dishToSave;
    }

    // 6.e) findDishsByIngredientName
    public List<Dish> findDishsByIngredientName(String ingredientName) {
        List<Dish> dishes = new ArrayList<>();
        String sql = "SELECT DISTINCT d.* FROM dish d JOIN ingredient i ON d.id = i.id_dish WHERE i.name ILIKE ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + ingredientName + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Dish d = new Dish();
                d.setId(rs.getInt("id"));
                d.setName(rs.getString("name"));
                d.setDishType(DishTypeEnum.valueOf(rs.getString("dish_type")));
                dishes.add(d);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return dishes;
    }

    // 6.f) findIngredientsByCriteria (Recherche dynamique paginée)
    public List<Ingredient> findIngredientsByCriteria(String name, CategoryEnum category, String dishName, int page, int size) {
        List<Ingredient> ingredients = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT i.* FROM ingredient i LEFT JOIN dish d ON i.id_dish = d.id WHERE 1=1 ");

        if (name != null) sql.append(" AND i.name ILIKE ? ");
        if (category != null) sql.append(" AND i.category = ?::categoryenum ");
        if (dishName != null) sql.append(" AND d.name ILIKE ? ");

        sql.append(" LIMIT ? OFFSET ?");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int idx = 1;
            if (name != null) ps.setString(idx++, "%" + name + "%");
            if (category != null) ps.setString(idx++, category.name());
            if (dishName != null) ps.setString(idx++, "%" + dishName + "%");

            ps.setInt(idx++, size);
            ps.setInt(idx, (page - 1) * size);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ingredients.add(mapResultSetToIngredient(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return ingredients;
    }

    private Ingredient mapResultSetToIngredient(ResultSet rs) throws SQLException {
        Ingredient ing = new Ingredient();
        ing.setId(rs.getInt("id"));
        ing.setName(rs.getString("name"));
        ing.setPrice(rs.getDouble("price"));
        String cat = rs.getString("category");
        if (cat != null) ing.setCategory(CategoryEnum.valueOf(cat));
        return ing;
    }
}