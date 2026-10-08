package com.github.matheuscruzsouza.nanospring.logging;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class RotatingFileLoggerTest {

    private File tempLogFile;
    private RotatingFileLogger logger;

    @Before
    public void setUp() throws IOException {
        tempLogFile = File.createTempFile("test-nano-spring", ".log");
        tempLogFile.deleteOnExit();

        logger = new RotatingFileLogger();
        logger.setLogFile(tempLogFile);
        logger.setLevel(RotatingFileLogger.Level.DEBUG);
        logger.setEnabled(true);
        logger.clear();
    }

    @After
    public void tearDown() {
        if (tempLogFile != null && tempLogFile.exists()) {
            tempLogFile.delete();
        }
        for (int i = 1; i <= 5; i++) {
            File backup = new File(tempLogFile.getAbsolutePath() + "." + i);
            if (backup.exists()) backup.delete();
        }
    }

    @Test
    public void testBasicLoggingAndFormat() {
        logger.log(RotatingFileLogger.Level.INFO, "AUTH_TAG", "User authenticated successfully", null);

        String content = logger.readTail(10);
        assertNotNull(content);
        assertTrue(content.contains("INFO"));
        assertTrue(content.contains("AUTH_TAG"));
        assertTrue(content.contains("User authenticated successfully"));

        List<String> memLogs = logger.getRecentMemoryLogs(5);
        assertFalse(memLogs.isEmpty());
        assertTrue(memLogs.get(memLogs.size() - 1).contains("User authenticated successfully"));
    }

    @Test
    public void testLogLevelFiltering() {
        logger.setLevel(RotatingFileLogger.Level.WARN);

        logger.log(RotatingFileLogger.Level.DEBUG, "DEBUG_TAG", "This debug should be filtered out", null);
        logger.log(RotatingFileLogger.Level.INFO, "INFO_TAG", "This info should be filtered out", null);
        logger.log(RotatingFileLogger.Level.WARN, "WARN_TAG", "This warning must be recorded", null);
        logger.log(RotatingFileLogger.Level.ERROR, "ERROR_TAG", "This error must be recorded", null);

        String content = logger.readTail(10);
        assertFalse(content.contains("This debug should be filtered out"));
        assertFalse(content.contains("This info should be filtered out"));
        assertTrue(content.contains("This warning must be recorded"));
        assertTrue(content.contains("This error must be recorded"));
    }

    @Test
    public void testExceptionStackTraceLogged() {
        Exception testEx = new IllegalArgumentException("Invalid POS terminal id: -1");
        logger.log(RotatingFileLogger.Level.ERROR, "POS_ERROR", "Payment failed", testEx);

        String content = logger.readTail(50);
        assertTrue(content.contains("Payment failed"));
        assertTrue(content.contains("IllegalArgumentException: Invalid POS terminal id: -1"));
    }

    @Test
    public void testFileRotationWhenExceedingMaxSize() {
        // Set a small size cap: 300 bytes and max 2 backups
        logger.setMaxSizeBytes(300);
        logger.setMaxHistory(2);

        for (int i = 1; i <= 20; i++) {
            logger.log(RotatingFileLogger.Level.INFO, "BURST", "Log line number " + i + " padding to exceed size", null);
        }

        File backup1 = new File(tempLogFile.getAbsolutePath() + ".1");
        File backup2 = new File(tempLogFile.getAbsolutePath() + ".2");

        assertTrue("Primary log file must exist", tempLogFile.exists());
        assertTrue("Backup file .1 must have been created by rotation", backup1.exists());
        assertTrue("Primary log file size must be kept bounded", tempLogFile.length() <= 1000);
    }
}
