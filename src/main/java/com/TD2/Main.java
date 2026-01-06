package com.TD2;

import com.TD2.Model.Dish;
import com.TD2.Model.Ingredient;
import com.TD2.Model.CategoryEnum;
import com.TD2.Repository.DataRetriever;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        DataRetriever retriever = new DataRetriever();

        System.out.println("--- DÉBUT DES TESTS ---");

        // 7.a) Test findDishById(1)
        try {
            Dish dish = retriever.findDishById(1);
            System.out.println("Test 7.a : " + dish.getName());
            dish.getIngredients().forEach(i -> System.out.println(" - Ingrédient : " + i.getName()));
        } catch (Exception e) {
            System.err.println("Test 7.a échoué : " + e.getMessage());
        }

        // 7.b) Test findDishById(999)
        try {
            System.out.print("Test 7.b (ID 999) : ");
            retriever.findDishById(999);
        } catch (RuntimeException e) {
            System.out.println("OK (Exception levée : " + e.getMessage() + ")");
        }

        // 7.c) Test findIngredients(page=2, size=2)
        System.out.println("Test 7.c (Page 2, Size 2) :");
        List<Ingredient> p2s2 = retriever.findIngredients(2, 2);
        p2s2.forEach(i -> System.out.println(" - " + i.getName()));

        // 7.e) Test findDishsByIngredientName("eur")
        System.out.println("Test 7.e (Recherche 'eur') :");
        List<Dish> dishesByIng = retriever.findDishsByIngredientName("eur");
        dishesByIng.forEach(d -> System.out.println(" - Plat trouvé : " + d.getName()));

        // 7.f) Test findIngredientsByCriteria (Filtre VEGETABLE)
        System.out.println("Test 7.f (Critère VEGETABLE) :");
        List<Ingredient> veggies = retriever.findIngredientsByCriteria(null, CategoryEnum.VEGETABLE, null, 1, 10);
        veggies.forEach(i -> System.out.println(" - " + i.getName()));

        System.out.println("--- FIN DES TESTS ---");
    }
}