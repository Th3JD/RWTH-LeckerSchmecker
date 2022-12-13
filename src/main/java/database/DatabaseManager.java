package database;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import config.Config;
import meal.Canteen;
import meal.LeckerSchmecker;
import telegram.ChatContext;
import telegram.LeckerSchmeckerBot;

import java.sql.*;
import java.util.UUID;

public class DatabaseManager {

    protected static DatabaseManager instance;

    private Connection connection;
    private final TimeBasedGenerator generator = Generators.timeBasedGenerator(EthernetAddress.fromInterface());

    // STATEMENTS
    private PreparedStatement LOAD_USER, ADD_USER;
    /////////////


    private static DatabaseManager getInstance(){
        if(instance == null){
            instance = new DatabaseManager();
        }
        return instance;
    }


    // Static function declarations //////////////////////////////////////////////////////////
    public static void connect(){
        getInstance()._connect();
        getInstance()._setupStatements();
    }

    public static void disconnect(){
        getInstance()._disconnect();
    }

    public static void setupTables(){
        getInstance()._setupTables();
    }

    public static ChatContext loadUser(LeckerSchmeckerBot bot, long chatID){
        return getInstance()._loadUser(bot, chatID);
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
            LOAD_USER.close();
            ADD_USER.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void _setupTables(){
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            stmt.addBatch("create table if not exists meals2 (" +
                    "meal_id   int auto_increment primary key, " +
                    "meal_name varchar(100) not null" +
                    ");");

            stmt.addBatch("create table if not exists users\n" +
                    "(\n" +
                    "    userID          UUID                                                                                                                        not null,\n" +
                    "    chatID          BIGINT                                                                                                                      not null,\n" +
                    "    default_canteen ENUM ('academica', 'ahornstrasse', 'vita', 'templergraben', 'bayernallee', 'eupenerstrasse', 'kmac', 'juelich', 'suedpark') null,\n" +
                    "    constraint users_pk\n" +
                    "        primary key (userID)\n" +
                    ");\n");

            stmt.addBatch("create unique index if not exists users_chatID_index\n" +
                    "    on users (chatID desc);");

            //TODO: Add table containing information about the user, date, meal and rating

            stmt.executeBatch();
            connection.commit();
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    protected void _setupStatements(){
        try {
            LOAD_USER = connection.prepareStatement("SELECT * FROM users WHERE chatID=?");
            ADD_USER = connection.prepareStatement("INSERT INTO users VALUES (?, ?, ?)");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected ChatContext _loadUser(LeckerSchmeckerBot bot, long chatID){
        try{
            LOAD_USER.clearParameters();
            LOAD_USER.setLong(1, chatID);
            ResultSet rs = LOAD_USER.executeQuery();

            if(!rs.next()){
                // User not yet in database
                LeckerSchmecker.getLogger().info("User with chatID " + chatID + " is not yet registered.");

                UUID userID = generator.generate();

                ADD_USER.clearParameters();
                ADD_USER.setString(1, userID.toString());
                ADD_USER.setLong(2, chatID);
                ADD_USER.setString(3, null);
                ADD_USER.execute();

                return new ChatContext(bot, userID, chatID, null);
            } else {
                UUID userID = UUID.fromString(rs.getString("userID"));

                String defaultCanteenRaw = rs.getString("default_canteen");
                Canteen defaultCanteen = null;
                if(defaultCanteenRaw != null){
                    defaultCanteen = Canteen.getByURLName(rs.getString("default_canteen")).get();
                }

                return new ChatContext(bot, userID, chatID, defaultCanteen);
            }

        } catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    // ///////////////////////////////////////////////////////////////////////////////////////

}
