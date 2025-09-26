/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fmi.aq.enfuser.ftools;

import java.io.PrintStream;
import java.util.logging.Level;

/**
 *This class is used to funnel logging and error information.
 *Since this is a new feature (11/2020) there is a switch to roll back to
 * the old style of showing information, in which case system.out and printDebug()
 * are used.
 * 
 * Intended Level use-cases:
 * The SEVERE level should only be used when the application really is in trouble.
 * Users are being affected without having a way to work around the issue.
 * 
 * The WARNING level should be used when something bad happened, but the application
 * still has the chance to heal itself or the issue can wait a day or two to be fixed.
 * 
 * The INFO level should be used to document state changes in the application or
 * some entity within the application.
 * 
 * All of FINE, FINER, and FINEST are intended for relatively detailed tracing.
 * The exact meaning of the three levels will vary between subsystems, but in general,
 * FINEST should be used for the most voluminous detailed output, FINER for
 * somewhat less detailed output, and FINE for the lowest volume (and most important) messages.
 * 
 * @author Lasse Johansson
 */
public class EnfuserLogger {

    public static Level LOG_LEVEL = Level.INFO;

    public static void log(Exception e, Level l, Class c, String msg) {
        log(e, l, c, msg, System.out);
    }
    
    public static void log(Level l, Class c, String msg, PrintStream out) {
        log(null, l, c, msg, out);
    }
 
  public static void log(Level l, Class c, String msg) {
     log(null, l, c, msg, System.out);
 }

/**
 * For old style treatment of logging, check the message Level against
 * one set in GlobOptions. If message level is lower then this returns false
 * (no logging activities).
 * @param l
 * @return 
 */  
private static boolean loggable(Level l) {
     if (LOG_LEVEL.intValue()<= l.intValue()) {
         return true;
     } else {
         return false;
     }
}  
 
/**
 * Handle logging of a message and possibly an Exception. The way how this
 * is done is affected by whether or not 'OLD_STYLE' (static boolean) is set
 * on or off. In case it's set as 'true' then the logging behaves a similar
 * fashion that previous model versions did.
 * @param e Exception, will be logged if non-null.
 * @param l Level for the message. Lower level messages can be omitted if lower
 * than set in GlobOptions.
 * @param c The class responsible for the logging activity
 * @param msg the message to be logged 
 * @param out custom printStream for the logging (TODO)
 */
 public static void log(Exception e, Level l, Class c, String msg, PrintStream out) { 
    if (loggable(l)) {//level exceeds of equals set level for logging.
        if (out==null) out = System.out;
        if (l.intValue()>=Level.WARNING.intValue()) {//warning or severe => to err in 'old-style'
            if (l == Level.WARNING) msg = "WARNING: "+msg;
            if (l == Level.SEVERE) msg = "SEVERE: "+msg;

            if (out == System.out) {
                out = System.err;
            }
        }
        //print it
        out.println(msg);

        if (e!=null) {//an Exception to log
            e.printStackTrace(out);
        }
    }//if loggable

 } 

    public static void sleep(int ms, Class c) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            log(ex,Level.SEVERE,c,"Can't get no sleep!");
        }
    }

    public static void log(Level INFO, String string) {
       log(INFO,EnfuserLogger.class,string);
    }

    public static void fineLog(String string) {
        EnfuserLogger.log(Level.FINE, string);
    }

    public static void warning(Exception e, String msg) {
           log(e,Level.WARNING,EnfuserLogger.class,msg,null);
    }
  
    public static void severe(Exception e, String msg) {
           log(e,Level.SEVERE,EnfuserLogger.class,msg,null); 
    }

    public static void infoLog(String msg) {
       log(Level.INFO,EnfuserLogger.class,msg);
    }

    public static void finerLog(String msg) {
       log(Level.FINER,EnfuserLogger.class,msg);
    }
  

  
}
