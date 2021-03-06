package com.liquidlabs.log.indexer;

import com.liquidlabs.common.file.FileUtil;
import com.liquidlabs.log.index.LogFile;
import com.liquidlabs.log.index.LogFileOps;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

/**
 * Created with IntelliJ IDEA.
 * User: Neiil
 * Date: 08/02/14
 * Time: 20:45
 * To change this template use File | Settings | File Templates.
 */
public class LUFileStoreTest {

    private static final String LOG_FILE = "build/myFile.log";
    String DIR = "build/PIIndexerTest" + System.currentTimeMillis();
    LUFileStore fileStore;
    int count;

    @Before
    public void setUp() throws Exception {
        try {

            DIR = DIR + count++;

            FileUtil.deleteDir(new File(DIR));

            fileStore = new LUFileStore(DIR, Executors.newSingleThreadExecutor());

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @After
    public void tearDown() throws Exception {
        FileUtil.deleteDir(new File(DIR));
    }

    @Test
    public void testLogIdWorks() throws Exception {
        String file = DIR + "/" + "test.log" + System.currentTimeMillis();
        int first = fileStore.nextFileId;

        fileStore.openLogFile(file, true, "basic", "myTags");
        fileStore.sync();
        Thread.sleep(1000);

        fileStore.initFileSeed();
        int next = fileStore.nextFileId;
        assertTrue(next > first);

    }

    @Test
    public void testAddAndFind() throws Exception {
        String file = DIR + "/" + "test.log" + System.currentTimeMillis();
        fileStore.openLogFile(file, true, "basic", "myTags");

        fileStore.sync();
        LogFile logFile = fileStore.openLogFile(file);

        assertNotNull(logFile);

        fileStore.sync();

        LogFile logFile2 = fileStore.openLogFile(logFile.getId());
        assertNotNull(logFile2);
    }

    @Test
    public void testAddAndFindAndRemove() throws Exception {
        String file = DIR + "/" + "testFindRemove.log" + System.currentTimeMillis();
        fileStore.openLogFile(file, true, "basic", "myStuff");
        fileStore.sync();
        LogFile logFile = fileStore.openLogFile(file);
        assertNotNull(logFile);
        fileStore.removeFromIndex(file);
        fileStore.sync();
        assertFalse(fileStore.isIndexed(file));

        LogFile logFile2 = fileStore.openLogFile(file);

        assertNull(logFile2);
    }


    @Test
    public void testAddAndFindMultiple() throws Exception {
        String file = DIR + "/" + "Atest.log1";
        LogFile logFile1 = fileStore.openLogFile(file, true, "basic", "myStuff");
        logFile1.setStartEndTime(new DateTime().minusHours(1), new DateTime());
        fileStore.updateLogFile(logFile1);

        String file2 = DIR + "/" + "Btest.log2";
        LogFile logFile2 = fileStore.openLogFile(file2, true, "basic", "myStuff");
        logFile2.setStartEndTime(new DateTime().minusHours(3), new DateTime().minusHours(1));
        fileStore.updateLogFile(logFile2);

        fileStore.sync();

        LogFile logFile = fileStore.openLogFile(file2);
        System.out.println("Got LogFile:" + logFile);
        assertNotNull(logFile);


        List<LogFile> logFiles = fileStore.indexedFiles(1, System.currentTimeMillis(), true, new LogFileOps.FilterCallback() {
            @Override
            public boolean accept(LogFile logFile) {
                System.out.println("Checking:" + logFile);
                return true;
            }
        });

        assertEquals(2, logFiles.size());
    }
}
