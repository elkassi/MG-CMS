package com.lear.MGCMS.security;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {

    private static final Logger LOG = LoggerFactory.getLogger(Constants.class);

    public static String folder = "C:\\CMS-WEB\\";


    public static void writeLogs(String log) {
        try {
            // if folder does not exist, create it


            FileWriter writer = new FileWriter(folder+"logs-"+new SimpleDateFormat("dd-MM-yy").format(new Date())+".log", true);
            writer.write(new SimpleDateFormat("dd-MM-yy HH:mm:ss").format(new Date())+":: "+log + "\n");
            writer.close();
        } catch (IOException e) {
            LOG.error("Constants.writeLogs failed to write to {}", folder, e);
        }
    }


}
