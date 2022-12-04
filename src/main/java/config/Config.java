package config;

import org.checkerframework.checker.units.qual.C;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

    protected static Config instance;

    private Properties properties;
    private final String fileName = "leckerschmecker.cfg";

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
    // ///////////////////////////////////////////////////////////////////////////////////////

}
