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
    private final String fileName = "leckerschmecker.cfg";
    private Set<Long> allowedUsers = new HashSet<>();

    public static Config getInstance(){
        if(instance == null){
            instance = new Config();
            instance._init();
        }
        return instance;
    }


    // Static function declarations //////////////////////////////////////////////////////////
    public static String getString(String property){
        return getInstance()._getString(property);
    }

    public static void readAllowedUsers(){
        getInstance()._readAllowedUsers();
    }

    public static boolean isAllowedUser(long chatID){
        return getInstance()._isAllowedUser(chatID);
    }
    // ///////////////////////////////////////////////////////////////////////////////////////

    // protected implementations /////////////////////////////////////////////////////////////
    protected String _getString(String property){
        return properties.getProperty(property);
    }

    protected void _init(){
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
        FileInputStream fis = null;
        try {
            fis = new FileInputStream("users.txt");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        Scanner scanner = new Scanner(fis);

        while(scanner.hasNextLine()){
            allowedUsers.add(Long.valueOf(scanner.nextLine()));
        }

    }

    protected boolean _isAllowedUser(long chatID){
        return allowedUsers.contains(chatID);
    }

    // ///////////////////////////////////////////////////////////////////////////////////////

}
