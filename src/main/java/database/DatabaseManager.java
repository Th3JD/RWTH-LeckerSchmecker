/*
 * RWTH-LeckerSchmecker
 * Copyright (c) 2023 Th3JD, ekansemit, 3dde
 *
 * This file is part of RWTH-LeckerSchmecker.
 *
 * RWTH-LeckerSchmecker is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with RWTH-LeckerSchmecker.
 * If not, see <https://www.gnu.org/licenses/>.
 */

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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import localization.ResourceManager;
import meal.Canteen;
import meal.DietType;
import meal.LeckerSchmecker;
import meal.MainMeal;
import org.apache.commons.dbcp2.BasicDataSource;
import rating.RatingInfo;
import telegram.ChatContext;
import telegram.LeckerSchmeckerBot;

public class DatabaseManager {


    protected static DatabaseManager instance;
    private BasicDataSource dataSource;
    private final TimeBasedGenerator generator = Generators.timeBasedGenerator(
            new EthernetAddress("00:00:00:00:00:00"));

    // STATEMENTS
    private String LOAD_USER, LOAD_USER_CHAT_IDS, ADD_USER, SET_CANTEEN, SET_DIET_TYPE, SET_LOCALE, SET_COMPACT_LAYOUT, SET_AUTOMATED_QUERY, LOAD_MEAL_BY_ALIAS, LOAD_MEALS_BY_SHORT_ALIAS,
            ADD_NEW_MEAL_ALIAS, ADD_NEW_MEAL_SHORT_ALIAS, LOAD_MEALNAME_BY_ID, ADD_MEAL_ALIAS, LOAD_NUMBER_OF_VOTES,
            RATE_MEAL, DELETE_RATING, LOAD_USER_RATING_BY_DATE, LOAD_GLOBAL_RATING, LOAD_USER_RATING, LOAD_SIMILAR_RATING,
            LOAD_AUTOMATED_QUERY_IDS, LOAD_CHATID_BY_USERID;
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

    public static Set<Long> getUserChatIds() {
        return getInstance()._getUserChatIds();
    }

    public static void setDefaultCanteen(UUID userID, Canteen canteen) {
        getInstance()._setDefaultCanteen(userID, canteen);
    }

    public static void setLanguage(UUID userID, Locale locale) {
        getInstance()._setLanguage(userID, locale);
    }

    public static void setCompactLayout(UUID userID, boolean value) {
        getInstance()._setCompactLayout(userID, value);
    }

    public static void setDefaultDietType(UUID userID, DietType dietType) {
        getInstance()._setDefaultDietType(userID, dietType);
    }

    public static void setAutomatedQuery(UUID userID, LocalTime time) {
        getInstance()._setAutomatedQuery(userID, time);

    }

    public static Integer loadMealID(MainMeal meal) {
        return getInstance()._loadMealID(meal);
    }

    public static Integer loadMealIDByName(String name) {
        return getInstance()._loadMealIDByName(name);
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

    public static void deleteRatingsAtDate(ChatContext context, LocalDate date) {
        getInstance()._deleteRatingsAtDate(context, date);
    }

    public static int numberOfRatingsByDate(ChatContext context, LocalDate date) {
        return getInstance()._numberOfRatingsByDate(context, date);
    }

    public static RatingInfo getGlobalRating(MainMeal meal) {
        return getInstance()._getGlobalRating(meal);
    }

    public static RatingInfo getUserRating(ChatContext context, MainMeal meal) {
        return getInstance()._getUserRating(context, meal);
    }

    public static List<UUID> getAutomatedQueryIds(LocalTime time) {
        return getInstance()._getAutomatedQueryIds(time);
    }

    public static Long getChatIdByUserId(UUID uuid) {
        return getInstance()._getChatIdByUserId(uuid);
    }

    // ///////////////////////////////////////////////////////////////////////////////////////

    private Connection getConnection() throws SQLException {
        if(dataSource == null)
            throw new SQLException("Database not yet connected");
        return dataSource.getConnection();
    }


    // protected implementations /////////////////////////////////////////////////////////////
    protected void _connect() {
        // Initialize datasource
        dataSource = new BasicDataSource();
        dataSource.setUrl(Config.getString("database.url"));
        dataSource.setUsername(Config.getString("database.user"));
        dataSource.setPassword(Config.getString("database.password"));
        dataSource.setMinIdle(5);
        dataSource.setMaxIdle(10);
        dataSource.setMaxOpenPreparedStatements(100);
    }

    protected void _disconnect() {
        try {
            dataSource.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void _setupTables() {
        try (Connection connection = getConnection();
                Statement stmt = connection.createStatement()) {

            // Users
            stmt.addBatch("create table if not exists users\n" +
                    "(\n" +
                    "    userID          UUID                                                                                                                        not null,\n"
                    +
                    "    chatID          BIGINT                                                                                                                      not null,\n"
                    +
                    "    default_canteen ENUM ('academica', 'ahornstrasse', 'vita', 'templergraben', 'bayernallee', 'eupenerstrasse', 'kmac', 'juelich', 'suedpark') null,\n"
                    +
                    "    default_diet_type ENUM ('vegan', 'vegetarian', 'nopork', 'nofish', 'all') default 'all' not null,\n"
                    +
                    "    language        ENUM ('en-GB', 'de-DE', 'es-ES', 'zh-CN') default 'en-GB' not null,\n"
                    +
                    "    compact_layout  TINYINT                                   default 0 not null,\n"
                    +
                    "    automated_query ENUM ('08:00', '09:00', '10:00', '11:00', '12:00') null,\n"
                    +
                    "    constraint users_pk\n" +
                    "        primary key (userID)\n" +
                    ");\n");

            stmt.addBatch("create unique index if not exists users_chatID_index\n" +
                    "    on users (chatID desc);");

            // Meals
            stmt.addBatch("create table if not exists meal_name_alias\n" +
                    "(\n" +
                    "    mealID int auto_increment,\n" +
                    "    alias  VARCHAR(200) not null,\n" +
                    "    constraint meal_name_alias_pk\n" +
                    "        primary key (mealID, alias)\n" +
                    ");");

            stmt.addBatch("create unique index if not exists meal_name_alias_alias_uindex\n" +
                    "    on meal_name_alias (alias);");

            stmt.addBatch("create table if not exists meal_shortname_alias\n" +
                    "(\n" +
                    "    mealID     int          null,\n" +
                    "    shortAlias VARCHAR(200) not null,\n" +
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

            stmt.addBatch("create table if not exists ratings_moving_average\n" +
                    "(\n" +
                    "   userID              UUID            not null, \n" +
                    "   mealID              int             not null, \n" +
                    "   moving_average      DECIMAL(3,1)    not null, \n" +
                    "   constraint ratings_moving_pk\n" +
                    "       primary key (userID, mealID),\n" +
                    "   constraint ratings_moving_meal_name_alias_mealID_fk\n" +
                    "       foreign key (mealID) references meal_name_alias (mealID),\n" +
                    "   constraint ratings_moving_users_userID_fk\n" +
                    "       foreign key (userID) references users (userID)\n" +
                    ");\n");

            stmt.addBatch("create index if not exists ratings_moving_mealID_index\n" +
                    "   on leckerschmecker.ratings_moving_average (mealID desc);");

            stmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void _setupStatements() {
        LOAD_USER = "SELECT * FROM users WHERE chatID=?";
        ADD_USER = "INSERT INTO users VALUES (?, ?, ?, ?, ?, ?, ?)";
        LOAD_USER_CHAT_IDS = "SELECT chatID FROM users";
        LOAD_NUMBER_OF_VOTES = "SELECT COUNT(*) AS amount FROM ratings WHERE userID LIKE ?";
        SET_CANTEEN = "UPDATE users SET default_canteen=? WHERE userID LIKE ?";
        SET_DIET_TYPE = "UPDATE users SET default_diet_type=? WHERE userID LIKE ?";
        SET_LOCALE = "UPDATE users SET language=? WHERE userID LIKE ?";
        SET_COMPACT_LAYOUT = "UPDATE users SET compact_layout=? WHERE userID LIKE ?";
        SET_AUTOMATED_QUERY ="UPDATE users SET automated_query=? WHERE userID LIKE ?";
        LOAD_MEAL_BY_ALIAS = "SELECT mealID FROM meal_name_alias WHERE alias LIKE ?";
        LOAD_MEALS_BY_SHORT_ALIAS = "SELECT mealID FROM meal_shortname_alias WHERE shortAlias LIKE ?";
        ADD_NEW_MEAL_ALIAS = "INSERT INTO meal_name_alias (alias) VALUES (?)";
        ADD_NEW_MEAL_SHORT_ALIAS = "INSERT INTO meal_shortname_alias VALUES (?,?)";
        LOAD_MEALNAME_BY_ID = "SELECT alias FROM meal_name_alias WHERE mealID=?";
        ADD_MEAL_ALIAS = "INSERT INTO meal_name_alias VALUES (?, ?)";
        RATE_MEAL = "INSERT INTO ratings VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE rating=?";
        DELETE_RATING = "DELETE FROM ratings WHERE userID LIKE ? AND date=?";
        LOAD_USER_RATING_BY_DATE = "SELECT * FROM ratings WHERE userID LIKE ? AND date=?";
        LOAD_GLOBAL_RATING = "SELECT AVG(rating) AS average, COUNT(*) AS votes FROM (ratings r INNER JOIN (\n"
                        + "    SELECT userID, mealID, MAX(date) AS MaxDate\n"
                        + "    FROM ratings\n"
                        + "    WHERE mealID=?\n"
                        + "    GROUP BY userID, mealID\n"
                        + ") rmax ON r.userID=rmax.userID AND r.mealID=rmax.mealID AND r.date=MaxDate);";
        LOAD_USER_RATING = "SELECT rating from (ratings r INNER JOIN (\n"
                        + "    SELECT userID, mealID, MAX(date) AS MaxDate\n"
                        + "    FROM ratings\n"
                        + "    WHERE mealID=? AND userID LIKE ?\n"
                        + "    GROUP BY userID, mealID\n"
                        + ") rmax ON r.userID=rmax.userID AND r.mealID=rmax.mealID AND r.date=MaxDate);";
        LOAD_SIMILAR_RATING = "SELECT AVG(rating) AS average, COUNT(*) AS votes FROM ratings r,\n"
                        + "(SELECT userID, lastRatings.mealID AS mealID, MaxDate FROM\n"
                        + "(SELECT mealID FROM meal_shortname_alias WHERE shortAlias LIKE ?) AS similarIDs,\n"
                        + "(SELECT userID, mealID, MAX(date) AS MaxDate FROM ratings WHERE userID LIKE ? GROUP BY userID, mealID) AS lastRatings\n"
                        + "WHERE similarIDs.mealID=lastRatings.mealID) AS similarLastRatings\n"
                        + "WHERE r.userID=similarLastRatings.userID AND r.mealID=similarLastRatings.mealID AND r.date=similarLastRatings.MaxDate;";
        LOAD_AUTOMATED_QUERY_IDS = "SELECT userID FROM users WHERE automated_query LIKE ?";
        LOAD_CHATID_BY_USERID = "SELECT chatID FROM users WHERE userID LIKE ?";
    }

    protected ChatContext _loadUser(LeckerSchmeckerBot bot, long chatID) {
        try (Connection connection = getConnection();
                 PreparedStatement psL = connection.prepareStatement(LOAD_USER);
                 PreparedStatement psA = connection.prepareStatement(ADD_USER);
                 PreparedStatement psV = connection.prepareStatement(LOAD_NUMBER_OF_VOTES)) {

            psL.setLong(1, chatID);
            ResultSet rs = psL.executeQuery();

            if (!rs.next()) {
                // User not yet in database
                LeckerSchmecker.getLogger()
                        .info("User with chatID " + chatID + " is not yet registered.");

                UUID userID = generator.generate();

                psA.setString(1, userID.toString());
                psA.setLong(2, chatID);
                psA.setString(3, null);
                psA.setString(4, DietType.EVERYTHING.getId());
                psA.setString(5, ResourceManager.DEFAULTLOCALE.getLanguage() + "-"
                        + ResourceManager.DEFAULTLOCALE.getCountry());
                psA.setBoolean(6, false);
                psA.setString(7, null);
                psA.execute();

                return new ChatContext(bot, userID, chatID, null, DietType.EVERYTHING, ResourceManager.DEFAULTLOCALE, false, null, 0);
            } else {
                UUID userID = UUID.fromString(rs.getString("userID"));

                String defaultCanteenRaw = rs.getString("default_canteen");
                Canteen defaultCanteen = null;
                if (defaultCanteenRaw != null) {
                    defaultCanteen = Canteen.getByURLName(rs.getString("default_canteen")).get();
                }

                DietType dietType = DietType.getById(rs.getString("default_diet_type")).orElse(DietType.EVERYTHING);

                String[] languageInfo = rs.getString("language").split("-");
                Locale locale = new Locale(languageInfo[0], languageInfo[1]);

                boolean value = rs.getBoolean("compact_layout");

                String automatedQuery = rs.getString("automated_query");
                LocalTime automatedQueryTime = null;
                if (automatedQuery != null) {
                    DateTimeFormatter parser = DateTimeFormatter.ofPattern("HH:mm");
                    automatedQueryTime = LocalTime.parse(automatedQuery, parser);
                }

                psV.setString(1, userID.toString());
                ResultSet rsNOV = psV.executeQuery();

                rsNOV.next();
                int numberOfVotes = rsNOV.getInt("amount");

                return new ChatContext(bot, userID, chatID, defaultCanteen, dietType, locale, value, automatedQueryTime, numberOfVotes);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Set<Long> _getUserChatIds() {
        Set<Long> ids = new HashSet<>();

        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(LOAD_USER_CHAT_IDS)){
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                ids.add(rs.getLong("chatID"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ids;
    }

    protected void _setDefaultCanteen(UUID userID, Canteen canteen) {
        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(SET_CANTEEN)){
            ps.setString(1, canteen != null ? canteen.getUrlName() : null);
            ps.setString(2, userID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    protected void _setDefaultDietType(UUID userID, DietType dietType) {
        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(SET_DIET_TYPE)){
            ps.setString(1, dietType != null ? dietType.getId() : null);
            ps.setString(2, userID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    protected void _setAutomatedQuery(UUID userID, LocalTime time) {
        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(SET_AUTOMATED_QUERY)){
            ps.setString(1, time != null ? time.toString() : null);
            ps.setString(2, userID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void _setLanguage(UUID userID, Locale locale) {
        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(SET_LOCALE)){
            ps.setString(1, locale.getLanguage() + "-" + locale.getCountry());
            ps.setString(2, userID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void _setCompactLayout(UUID userID, boolean value) {
        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(SET_COMPACT_LAYOUT)){
            ps.setBoolean(1, value);
            ps.setString(2, userID.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected Integer _loadMealID(String mealName) {
        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(LOAD_MEAL_BY_ALIAS)){
            ps.setString(1, mealName);
            ResultSet rs = ps.executeQuery();

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

    protected Integer _loadMealIDByName(String name) {
        return this._loadMealID(name);
    }

    protected Set<Integer> _loadMealIDsByShortAlias(MainMeal meal) {
        Set<Integer> ids = new HashSet<>();

        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(LOAD_MEALS_BY_SHORT_ALIAS)){
            ps.setString(1, meal.getShortAlias());
            ResultSet rs = ps.executeQuery();

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

        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(ADD_NEW_MEAL_ALIAS, Statement.RETURN_GENERATED_KEYS);
                PreparedStatement ps2 = connection.prepareStatement(ADD_NEW_MEAL_SHORT_ALIAS)){

            ps.setString(1, meal.getName());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();

            rs.next();
            newID = rs.getInt("insert_id");

            ps2.setInt(1, newID);
            ps2.setString(2, meal.getShortAlias());
            ps2.executeUpdate();
            return newID;
        } catch (SQLException e) {
            LeckerSchmecker.getLogger().warning("???");
            e.printStackTrace();
        }
        return newID;
    }

    protected List<String> _getMealAliases(int mealID) {
        List<String> res = new LinkedList<>();
        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(LOAD_MEALNAME_BY_ID)){
            ps.setInt(1, mealID);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                res.add(rs.getString("alias"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    protected void _addAliasToMeal(int mealID, String newAlias) {
        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(ADD_MEAL_ALIAS)){
            ps.setInt(1, mealID);
            ps.setString(2, newAlias);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void _rateMeal(ChatContext context, MainMeal meal, int rating) {
        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(RATE_MEAL)){
            ps.setString(1, context.getUserID().toString());
            ps.setInt(2, meal.getId());
            ps.setDate(3, Date.valueOf(LocalDate.now()));
            ps.setInt(4, rating);
            ps.setInt(5, rating);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void _deleteRatingsAtDate(ChatContext context, LocalDate date) {
        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(DELETE_RATING)){
            ps.setString(1, context.getUserID().toString());
            ps.setDate(2, Date.valueOf(date));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected int _numberOfRatingsByDate(ChatContext context, LocalDate date) {
        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(LOAD_USER_RATING_BY_DATE)){
            ps.setString(1, context.getUserID().toString());
            ps.setDate(2, Date.valueOf(date));
            ResultSet rs = ps.executeQuery();

            int res = 0;
            while (rs.next()) {
                res++;
            }

            return res;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    protected RatingInfo _getGlobalRating(MainMeal meal) {

        // Cannot return global rating for meals which are still waiting for admin input
        if (meal.getId() == null) {
            return null;
        }

        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(LOAD_GLOBAL_RATING)){
            ps.setInt(1, meal.getId());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                return null;
            }

            if (rs.getFloat("average") == 0f) {
                return null; // rs.getFloat returns 0f if the value is null
            }

            return new RatingInfo(meal, rs.getFloat("average"), rs.getInt("votes"), false);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected RatingInfo _getUserRating(ChatContext context, MainMeal meal) {

        // Cannot return global rating for meals which are still waiting for admin input
        if (meal.getId() == null) {
            return null;
        }

        try (Connection connection = getConnection();
                PreparedStatement psU = connection.prepareStatement(LOAD_USER_RATING);
                PreparedStatement psS = connection.prepareStatement(LOAD_SIMILAR_RATING)) {
            psU.setInt(1, meal.getId());
            psU.setString(2, context.getUserID().toString());
            ResultSet rs = psU.executeQuery();

            // Check if the user did NOT rate this exact meal yet
            if (!rs.next()) {
                psS.setString(1, meal.getShortAlias());
                psS.setString(2, context.getUserID().toString());
                ResultSet rsSimilar = psS.executeQuery();

                if (!rsSimilar.next()) {
                    return null;
                }

                if (rsSimilar.getFloat("average") == 0f) {
                    return null; // rs.getFloat returns 0f if the value is null
                }

                return new RatingInfo(meal, rsSimilar.getFloat("average"), rsSimilar.getInt("votes"), true);
            }

            return new RatingInfo(meal, rs.getFloat("rating"), 1, false);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected List<UUID> _getAutomatedQueryIds(LocalTime time) {

        if (time == null) {
            return List.of();
        }

        List<UUID> res = new LinkedList<>();
        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(LOAD_AUTOMATED_QUERY_IDS)){
            ps.setString(1, time.toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                res.add(UUID.fromString(rs.getString("userID")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;
    }

    protected Long _getChatIdByUserId(UUID uuid) {
        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(LOAD_CHATID_BY_USERID)){
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            rs.next();
            return rs.getLong("chatID");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ///////////////////////////////////////////////////////////////////////////////////////


}
