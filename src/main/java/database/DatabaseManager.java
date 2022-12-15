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
    private final TimeBasedGenerator generator = Generators.timeBasedGenerator(new EthernetAddress("00:00:00:00:00:00"));

    // STATEMENTS
    private PreparedStatement LOAD_USER, ADD_USER, SET_CANTEEN;
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

    public static void setDefaultCanteen(UUID userID, Canteen canteen){
        getInstance()._setDefaultCanteen(userID, canteen);
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
            SET_CANTEEN.close();
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

            // Users
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

            // Meals
            stmt.addBatch("create table if not exists meal_name_alias\n" +
                    "(\n" +
                    "    mealID int auto_increment,\n" +
                    "    alias  VARCHAR(100) not null,\n" +
                    "    constraint meal_name_alias_pk\n" +
                    "        primary key (mealID, alias)\n" +
                    ");");

            stmt.addBatch("create unique index if not exists meal_name_alias_alias_uindex\n" +
                    "    on meal_name_alias (alias);");

            stmt.addBatch("create table if not exists meal_shortname_alias\n" +
                    "(\n" +
                    "    mealID     int          null,\n" +
                    "    shortAlias VARCHAR(100) not null,\n" +
                    "    constraint meal_shortname_alias_pk\n" +
                    "        primary key (mealID, shortAlias),\n" +
                    "    constraint meal_shortname_alias_meal_name_alias_mealID_fk\n" +
                    "        foreign key (mealID) references meal_name_alias (mealID)\n" +
                    ");");

            stmt.addBatch("create index if not exists meal_shortname_alias_shortAlias_index\n" +
                    "    on meal_shortname_alias (shortAlias);");

            // Rating
            stmt.addBatch("create table if not exists ratings\n" +
                    "(\n" +
                    "    userID UUID    not null,\n" +
                    "    mealID int     not null,\n" +
                    "    date   DATE    not null,\n" +
                    "    rating TINYINT not null,\n" +
                    "    constraint ratings_pk\n" +
                    "        primary key (userID, mealID, date),\n" +
                    "    constraint ratings_meal_name_alias_mealID_fk\n" +
                    "        foreign key (mealID) references meal_name_alias (mealID),\n" +
                    "    constraint ratings_users_userID_fk\n" +
                    "        foreign key (userID) references users (userID)\n" +
                    ");\n");

            stmt.addBatch("create index if not exists ratings_mealID_index\n" +
                    "    on leckerschmecker.ratings (mealID desc);");

            stmt.executeBatch();
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
            SET_CANTEEN = connection.prepareStatement("UPDATE users SET default_canteen=? WHERE userID like ?");
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

    protected void _setDefaultCanteen(UUID userID, Canteen canteen){
        try {
            SET_CANTEEN.clearParameters();
            SET_CANTEEN.setString(1, canteen != null ? canteen.getUrlName() : null);
            SET_CANTEEN.setString(2, userID.toString());
            SET_CANTEEN.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }



    // ///////////////////////////////////////////////////////////////////////////////////////

}
