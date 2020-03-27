package com.josephcday.totp;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Props file
 * 
 * @author joseph.day
 */
public class EnvVars {

    private static final Logger logger = LoggerFactory.getLogger(TOTPVerticle.class);

    public static int _appPort = 8080; // application port the service listens for main
    public static int _timeout = 2; // seconds before timeout and hangup

    public EnvVars() {
        _appPort = (Integer) map("TOTP_PORT", _appPort);
        _timeout = (Integer) map("TOTP_TIMEOUT", _timeout);
    }

    public static void init() {
        new EnvVars();
    }

    static Object map(String sysVar, Object variable) {
        String var = System.getenv(sysVar);
        if (var != null) {
            try {
                if (variable instanceof Integer) {
                    variable = Integer.parseInt(var);
                }
                if (variable instanceof Long) {
                    variable = Long.parseLong(var);
                }
                if (variable instanceof Boolean) {
                    variable = Boolean.parseBoolean(var);
                }
                if (variable instanceof String) {
                    variable = var;
                }
                System.out.println(sysVar + "=" + variable);
            } catch (Exception e) {
                String message = "'" + sysVar + "' system variable not " + variable.getClass().getName()
                        + ".  Using default value: " + variable;
                logger.error(message);
            }
        } else {
            String message = "'" + sysVar + "' system variable missing." + "  Using default value: " + variable;
            logger.info(message);
        }
        return variable;
    }
}
