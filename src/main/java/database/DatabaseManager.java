package database;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import config.Config;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import localization.ResourceManager;
import meal.*;
import telegram.ChatContext;
import telegram.LeckerSchmeckerBot;

public class DatabaseManager {

    protected static DatabaseManager instance;

    private Connection connection;
    private final TimeBasedGenerator generator = Generators.timeBasedGenerator(
            new EthernetAddress("00:00:00:00:00:00"));

    // STATEMENTS
    private PreparedStatement LOAD_USER, ADD_USER, SET_CANTEEN, SET_MEAL_TYPE, SET_LOCALE, LOAD_MEAL_BY_ALIAS, LOAD_MEALS_BY_SHORT_ALIAS,
            ADD_NEW_MEAL_ALIAS, ADD_NEW_MEAL_SHORT_ALIAS, LOAD_MEALNAME_BY_ID, ADD_MEAL_ALIAS,
            RATE_MEAL, DELETE_RATING, LOAD_USER_RATING_BY_DATE, LOAD_GLOBAL_RATING, LOAD_USER_RATING;
    /////////////


    private static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }


    // Static function declarations //////////////////////////////////////////////////////////
    public static void connect() {
        getInstance()._connect();
        getInstance()._setupStatements();
    }

    public static void disconnect() {
        getInstance()._disconnect();
    }

    public static void setupTables() {
        getInstance()._setupTables();
    }

    public static ChatContext loadUser(LeckerSchmeckerBot bot, long chatID) {
        return getInstance()._loadUser(bot, chatID);
    }

    public static void setDefaultCanteen(UUID userID, Canteen canteen) {
        getInstance()._setDefaultCanteen(userID, canteen);
    }

    public static void setLanguage(UUID userID, Locale locale) {
        getInstance()._setLanguage(userID, locale);
    }

    public static void setDefaultMealType(UUID userID, MealType mealType) {
        getInstance()._setDefaultMealType(userID, mealType);
    }

    public static Integer loadMealID(MainMeal meal) {
        return getInstance()._loadMealID(meal);
    }

    public static Integer loadMealID(String mealName) {
        return getInstance()._loadMealID(mealName);
    }

    public static Set<Integer> loadMealIDsByShortAlias(MainMeal meal) {
        return getInstance()._loadMealIDsByShortAlias(meal);
    }

    public static int addMeal(MainMeal meal) {
        return getInstance()._addMeal(meal);
    }

    public static List<String> getMealAliases(int mealID) {
        return getInstance()._getMealAliases(mealID);
    }

    public static void addAliasToMeal(int mealID, String newAlias) {
        getInstance()._addAliasToMeal(mealID, newAlias);
    }

    public static void rateMeal(ChatContext context, MainMeal meal, int rating) {
        getInstance()._rateMeal(context, meal, rating);
    }

    public static boolean hasRatedToday(ChatContext context) {
        return getInstance()._hasRatedToday(context);
    }

    public static Float getGlobalRating(MainMeal meal) {
        return getInstance()._getGlobalRating(meal);
    }

    public static Float getUserRating(ChatContext context, MainMeal meal) {
        return getInstance()._getUserRating(context, meal);
    }

    // ///////////////////////////////////////////////////////////////////////////////////////


    // protected implementations /////////////////////////////////////////////////////////////
    protected void _connect() {
        try {
            connection = DriverManager.getConnection(Config.getString("database.url"),
                    Config.getString("database.user"),
                    Config.getString("database.password"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void _disconnect() {
        try {
            LOAD_USER.close();
            ADD_USER.close();
            SET_CANTEEN.close();
            SET_MEAL_TYPE.close();
            SET_LOCALE.close();
            LOAD_MEAL_BY_ALIAS.close();
            LOAD_MEALS_BY_SHORT_ALIAS.close();
            ADD_NEW_MEAL_ALIAS.close();
            ADD_NEW_MEAL_SHORT_ALIAS.close();
            LOAD_MEALNAME_BY_ID.close();
            ADD_MEAL_ALIAS.close();
            RATE_MEAL.close();
            DELETE_RATING.close();
            LOAD_USER_RATING_BY_DATE.close();
            LOAD_GLOBAL_RATING.close();
            LOAD_USER_RATING.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void _setupTables() {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();

            // Users
            stmt.addBatch("create table if not exists users\n" +
                    "(\n" +
                    "    userID          UUID                                                                                                                        not null,\n"
                    +
                    "    chatID          BIGINT                                                                                                                      not null,\n"
                    +
                    "    default_canteen ENUM ('academica', 'ahornstrasse', 'vita', 'templergraben', 'bayernallee', 'eupenerstrasse', 'kmac', 'juelich', 'suedpark') null,\n"
                    +
                    "    default_meal_type ENUM ('vegan', 'vegetarian', 'nopork', 'nofish', 'all') null,\n"
                    +
                    "    language        ENUM ('en-GB', 'de-DE') default 'en-GB' not null,\n" +
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
        } catch (SQLException e) {
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

    protected void _setupStatements() {
        try {
            LOAD_USER = connection.prepareStatement("SELECT * FROM users WHERE chatID=?");
            ADD_USER = connection.prepareStatement("INSERT INTO users VALUES (?, ?, ?, ?, ?)");
            SET_CANTEEN = connection.prepareStatement(
                    "UPDATE users SET default_canteen=? WHERE userID like ?");
            SET_MEAL_TYPE = connection.prepareStatement(
                    "UPDATE users SET default_meal_type=? WHERE userID like ?");
            SET_LOCALE = connection.prepareStatement(
                    "UPDATE users SET language=? WHERE userID like ?");
            LOAD_MEAL_BY_ALIAS = connection.prepareStatement(
                    "SELECT mealID FROM meal_name_alias WHERE alias LIKE ?");
            LOAD_MEALS_BY_SHORT_ALIAS = connection.prepareStatement(
                    "SELECT mealID FROM meal_shortname_alias WHERE shortAlias LIKE ?");
            ADD_NEW_MEAL_ALIAS = connection.prepareStatement(
                    "INSERT INTO meal_name_alias (alias) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS);
            ADD_NEW_MEAL_SHORT_ALIAS = connection.prepareStatement(
                    "INSERT INTO meal_shortname_alias VALUES (?,?)");
            LOAD_MEALNAME_BY_ID = connection.prepareStatement(
                    "SELECT alias FROM meal_name_alias WHERE mealID=?");
            ADD_MEAL_ALIAS = connection.prepareStatement(
                    "INSERT INTO meal_name_alias VALUES (?, ?)");
            RATE_MEAL = connection.prepareStatement(
                    "INSERT INTO ratings VALUES (?, ?, ?, ?)");
            DELETE_RATING = connection.prepareStatement(
                    "DELETE FROM ratings WHERE userID=? AND date=?");
            LOAD_USER_RATING_BY_DATE = connection.prepareStatement(
                    "SELECT * FROM ratings WHERE userID=? AND date=?");
            LOAD_GLOBAL_RATING = connection.prepareStatement(
                    "SELECT AVG(rating) as average from (ratings r inner join (\n"
                            + "    select userID, mealID, MAX(date) as MaxDate\n"
                            + "    from ratings\n"
                            + "    WHERE mealID=?\n"
                            + "    group by userID, mealID\n"
                            + ") rmax on r.userID=rmax.userID and r.mealID=rmax.mealID and r.date=MaxDate);");
            LOAD_USER_RATING = connection.prepareStatement(
                    "SELECT rating from (ratings r inner join (\n"
                            + "    select userID, mealID, MAX(date) as MaxDate\n"
                            + "    from ratings\n"
                            + "    WHERE mealID=? and userID LIKE ?\n"
                            + "    group by userID, mealID\n"
                            + ") rmax on r.userID=rmax.userID and r.mealID=rmax.mealID and r.date=MaxDate);");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected ChatContext _loadUser(LeckerSchmeckerBot bot, long chatID) {
        try {
            LOAD_USER.clearParameters();
            LOAD_USER.setLong(1, chatID);
            ResultSet rs = LOAD_USER.executeQuery();

            if (!rs.next()) {
                // User not yet in database
                LeckerSchmecker.getLogger()
                        .info("User with chatID " + chatID + " is not yet registered.");

                UUID userID = generator.generate();

                ADD_USER.clearParameters();
                ADD_USER.setString(1, userID.toString());
                ADD_USER.setLong(2, chatID);
                ADD_USER.setString(3, null);
                ADD_USER.setString(4, null);
                ADD_USER.setString(5, ResourceManager.DEFAULTLOCALE.getLanguage() + "-"
                        + ResourceManager.DEFAULTLOCALE.getCountry());
                ADD_USER.execute();

                return new ChatContext(bot, userID, chatID, null, null, ResourceManager.DEFAULTLOCALE);
            } else {
                UUID userID = UUID.fromString(rs.getString("userID"));

                String defaultCanteenRaw = rs.getString("default_canteen");
                Canteen defaultCanteen = null;
                if (defaultCanteenRaw != null) {
                    defaultCanteen = Canteen.getByURLName(rs.getString("default_canteen")).get();
                }

                String defaultMealTypeRaw = rs.getString("default_meal_type");
                MealType defaultMealType = null;
                if (defaultMealTypeRaw != null) {
                    defaultMealType = MealType.getById(rs.getString("default_meal_type")).get();
                }

                String[] languageInfo = rs.getString("language").split("-");
                Locale locale = new Locale(languageInfo[0], languageInfo[1]);

                return new ChatContext(bot, userID, chatID, defaultCanteen, defaultMealType, locale);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void _setDefaultCanteen(UUID userID, Canteen canteen) {
        try {
            SET_CANTEEN.clearParameters();
            SET_CANTEEN.setString(1, canteen != null ? canteen.getUrlName() : null);
            SET_CANTEEN.setString(2, userID.toString());
            SET_CANTEEN.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    protected void _setDefaultMealType(UUID userID, MealType mealType) {
        try {
            SET_MEAL_TYPE.clearParameters();
            SET_MEAL_TYPE.setString(1, mealType != null ? mealType.getId() : null);
            SET_MEAL_TYPE.setString(2, userID.toString());
            SET_MEAL_TYPE.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    protected void _setLanguage(UUID userID, Locale locale) {
        try {
            SET_LOCALE.clearParameters();
            SET_LOCALE.setString(1, locale.getLanguage() + "-" + locale.getCountry());
            SET_LOCALE.setString(2, userID.toString());
            SET_LOCALE.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected Integer _loadMealID(String mealName) {
        try {
            LOAD_MEAL_BY_ALIAS.clearParameters();
            LOAD_MEAL_BY_ALIAS.setString(1, mealName);
            ResultSet rs = LOAD_MEAL_BY_ALIAS.executeQuery();

            if (!rs.next()) {
                return null;
            } else {
                return rs.getInt("mealID");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected Integer _loadMealID(MainMeal meal) {
        return this._loadMealID(meal.getName());
    }

    protected Set<Integer> _loadMealIDsByShortAlias(MainMeal meal) {
        Set<Integer> ids = new HashSet<>();

        try {
            LOAD_MEALS_BY_SHORT_ALIAS.clearParameters();
            LOAD_MEALS_BY_SHORT_ALIAS.setString(1, meal.getShortAlias());
            ResultSet rs = LOAD_MEALS_BY_SHORT_ALIAS.executeQuery();

            while (rs.next()) {
                ids.add(rs.getInt("mealID"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ids;
    }

    protected int _addMeal(MainMeal meal) {
        int newID = -1;

        try {
            ADD_NEW_MEAL_ALIAS.clearParameters();
            ADD_NEW_MEAL_ALIAS.setString(1, meal.getName());
            ADD_NEW_MEAL_ALIAS.executeUpdate();
            ResultSet rs = ADD_NEW_MEAL_ALIAS.getGeneratedKeys();

            rs.next();
            newID = rs.getInt("insert_id");

            ADD_NEW_MEAL_SHORT_ALIAS.clearParameters();
            ADD_NEW_MEAL_SHORT_ALIAS.setInt(1, newID);
            ADD_NEW_MEAL_SHORT_ALIAS.setString(2, meal.getShortAlias());
            ADD_NEW_MEAL_SHORT_ALIAS.executeUpdate();
            return newID;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return newID;
    }

    protected List<String> _getMealAliases(int mealID) {
        List<String> res = new LinkedList<>();
        try {
            LOAD_MEALNAME_BY_ID.clearParameters();
            LOAD_MEALNAME_BY_ID.setInt(1, mealID);
            ResultSet rs = LOAD_MEALNAME_BY_ID.executeQuery();

            while (rs.next()) {
                res.add(rs.getString("alias"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    protected void _addAliasToMeal(int mealID, String newAlias) {
        try {
            ADD_MEAL_ALIAS.clearParameters();
            ADD_MEAL_ALIAS.setInt(1, mealID);
            ADD_MEAL_ALIAS.setString(2, newAlias);
            ADD_MEAL_ALIAS.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void _rateMeal(ChatContext context, MainMeal meal, int rating) {
        try {
            DELETE_RATING.clearParameters();
            DELETE_RATING.setString(1, context.getUserID().toString());
            DELETE_RATING.setDate(2, Date.valueOf(LocalDate.now()));
            DELETE_RATING.executeUpdate();

            RATE_MEAL.clearParameters();
            RATE_MEAL.setString(1, context.getUserID().toString());
            RATE_MEAL.setInt(2, meal.getId());
            RATE_MEAL.setDate(3, Date.valueOf(LocalDate.now()));
            RATE_MEAL.setInt(4, rating);
            RATE_MEAL.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected boolean _hasRatedToday(ChatContext context) {
        try {
            LOAD_USER_RATING_BY_DATE.clearParameters();
            LOAD_USER_RATING_BY_DATE.setString(1, context.getUserID().toString());
            LOAD_USER_RATING_BY_DATE.setDate(2, Date.valueOf(LocalDate.now()));
            return LOAD_USER_RATING_BY_DATE.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    protected Float _getGlobalRating(MainMeal meal) {
        try {
            LOAD_GLOBAL_RATING.clearParameters();
            LOAD_GLOBAL_RATING.setInt(1, meal.getId());
            ResultSet rs = LOAD_GLOBAL_RATING.executeQuery();

            if (!rs.next()) {
                return null;
            }

            if (rs.getFloat("average") == 0f) {
                return null; // rs.getFloat returns 0f if the value is null
            }

            return rs.getFloat("average");

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Float _getUserRating(ChatContext context, MainMeal meal) {
        try {
            LOAD_USER_RATING.clearParameters();
            LOAD_USER_RATING.setInt(1, meal.getId());
            LOAD_USER_RATING.setString(2, context.getUserID().toString());
            ResultSet rs = LOAD_USER_RATING.executeQuery();

            if (!rs.next()) {
                return null;
            }

            return rs.getFloat("rating");

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ///////////////////////////////////////////////////////////////////////////////////////

}
