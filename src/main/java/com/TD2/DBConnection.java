package com.TD2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    public static Connection getConnection() {
        try {
            Class.forName("org.postgresql.Driver");

            String url = System.getenv("JDBC_URL");
            String user = System.getenv("USERNAME");
            String password = System.getenv("PASSWORD");

            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            System.err.println("Driver non trouvé ! Vérifiez votre pom.xml et rechargez Maven.");
            return null;
        } catch (SQLException e) {
            System.err.println("Erreur de connexion : " + e.getMessage());
            return null;
        }
    }
}