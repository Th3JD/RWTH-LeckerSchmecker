package config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

public class Config {

    protected static Config instance;

    private Properties properties;
    private final Set<Long> allowedUsers = new HashSet<>();

    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
            instance._init();
        }
        return instance;
    }


    // Static function declarations //////////////////////////////////////////////////////////
    public static String getString(String property){
        return getInstance()._getString(property);
    }

    public static int getInt(String property){
        return getInstance()._getInt(property);
    }

    public static void readAllowedUsers() {
        getInstance()._readAllowedUsers();
    }

    public static boolean isAllowedUser(long chatID) {
        return getInstance()._isAllowedUser(chatID);
    }

    public static boolean isAdmin(long chatID) {
        return getInstance()._isAdmin(chatID);
    }

    public static long getAdminChatID() {
        return getInstance()._getAdminChatID();
    }

    public static int getLogLines() {
        return getInstance()._getMaxLogLines();
    }

    public static int getLogFiles() {
        return getInstance()._getMaxLogFiles();
    }

    // ///////////////////////////////////////////////////////////////////////////////////////

    // protected implementations /////////////////////////////////////////////////////////////
    protected String _getString(String property) {
        return properties.getProperty(property);
    }

    protected int _getInt(String property) {
        return Integer.parseInt(properties.getProperty(property));
    }

    protected void _init(){
        String fileName = "leckerschmecker.cfg";
        try {
            InputStream is = new FileInputStream(fileName);
            properties = new Properties();
            properties.load(is);
        } catch (FileNotFoundException e){
            System.err.println("Config file does not exist!\nPath: " + fileName);
            System.exit(-1);
        } catch (IOException e){
            System.err.println("Failed to read config file! Error:\n");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    protected void _readAllowedUsers(){
        FileInputStream fis;
        try {
            fis = new FileInputStream("users.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        Scanner scanner = new Scanner(fis);

        while (scanner.hasNextLine()) {
            allowedUsers.add(Long.valueOf(scanner.nextLine()));
        }
    }

    protected boolean _isAllowedUser(long chatID) {
        return allowedUsers.contains(chatID);
    }

    protected boolean _isAdmin(long chatID) {
        return Long.parseLong(properties.getProperty("telegram.adminChatID")) == chatID;
    }

    protected long _getAdminChatID() {
        return Long.parseLong(properties.getProperty("telegram.adminChatID"));
    }

    protected int _getMaxLogLines() {
        return Integer.parseInt(properties.getProperty("log.maxLines"));
    }

    protected int _getMaxLogFiles() {
        return Integer.parseInt(properties.getProperty("log.maxFiles"));
    }
    // ///////////////////////////////////////////////////////////////////////////////////////

}
