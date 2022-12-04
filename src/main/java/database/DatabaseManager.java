package database;

import config.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    protected static DatabaseManager instance;

    private Connection connection;

    private static DatabaseManager getInstance(){
        if(instance == null){
            instance = new DatabaseManager();
        }
        return instance;
    }


    // Static function declarations //////////////////////////////////////////////////////////
    public static void connect(){
        getInstance()._connect();
    }

    public static void disconnect(){
        getInstance()._disconnect();
    }

    public static void setupTables(){
        getInstance()._setupTables();
    }
    // ///////////////////////////////////////////////////////////////////////////////////////


    // protected implementations /////////////////////////////////////////////////////////////
    protected void _connect(){
        try {
            connection = DriverManager.getConnection(Config.getString("database.url"),
                    Config.getString("database.user"),
                    Config.getString("database.password"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void _disconnect(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void _setupTables(){
        try {
            Statement stmt = connection.createStatement();
            stmt.addBatch("create table if not exists meals2 (" +
                    "meal_id   int auto_increment primary key, " +
                    "meal_name varchar(100) not null" +
                    ");");

            //TODO: Add table containing information about the user, date, meal and rating

            stmt.executeBatch();
            connection.commit();
            stmt.close();
        } catch (SQLException e){
            e.printStackTrace();
        }

    }
    // ///////////////////////////////////////////////////////////////////////////////////////

}
