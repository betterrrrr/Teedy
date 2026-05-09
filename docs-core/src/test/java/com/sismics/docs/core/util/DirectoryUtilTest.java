package com.sismics.docs.core.util;

import com.sismics.util.EnvironmentUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DirectoryUtilTest {
    private String originalOs;
    private String originalTeedyHome;
    private String originalWindowsAppData;
    private String originalMacHome;
    private boolean originalWebappContext;

    @Before
    public void setUp() throws Exception {
        originalOs = (String) getStaticField(EnvironmentUtil.class, "OS");
        originalTeedyHome = (String) getStaticField(EnvironmentUtil.class, "TEEDY_HOME");
        originalWindowsAppData = (String) getStaticField(EnvironmentUtil.class, "WINDOWS_APPDATA");
        originalMacHome = (String) getStaticField(EnvironmentUtil.class, "MAC_OS_USER_HOME");
        originalWebappContext = EnvironmentUtil.isWebappContext();
    }

    @After
    public void tearDown() throws Exception {
        setStaticField(EnvironmentUtil.class, "OS", originalOs);
        setStaticField(EnvironmentUtil.class, "TEEDY_HOME", originalTeedyHome);
        setStaticField(EnvironmentUtil.class, "WINDOWS_APPDATA", originalWindowsAppData);
        setStaticField(EnvironmentUtil.class, "MAC_OS_USER_HOME", originalMacHome);
        EnvironmentUtil.setWebappContext(originalWebappContext);
    }

    @Test
    public void usesTeedyHomeWhenSet() throws Exception {
        Path tempHome = Files.createTempDirectory("teedy_home");
        setStaticField(EnvironmentUtil.class, "TEEDY_HOME", tempHome.toString());
        EnvironmentUtil.setWebappContext(true);

        Path base = DirectoryUtil.getBaseDataDirectory();
        assertEquals(tempHome.toAbsolutePath().normalize(), base.toAbsolutePath().normalize());

        Path storage = DirectoryUtil.getStorageDirectory();
        assertTrue(Files.isDirectory(storage));

        deleteRecursively(tempHome);
    }

    @Test
    public void usesTempDirInUnitTest() throws Exception {
        setStaticField(EnvironmentUtil.class, "TEEDY_HOME", null);
        EnvironmentUtil.setWebappContext(false);

        Path base = DirectoryUtil.getBaseDataDirectory();
        Path expected = Paths.get(System.getProperty("java.io.tmpdir"));
        assertEquals(expected.toAbsolutePath().normalize(), base.toAbsolutePath().normalize());
    }

    @Test
    public void usesWindowsAppDataWhenWebappOnWindows() throws Exception {
        Path appData = Files.createTempDirectory("teedy_appdata");
        setStaticField(EnvironmentUtil.class, "TEEDY_HOME", null);
        setStaticField(EnvironmentUtil.class, "OS", "windows");
        setStaticField(EnvironmentUtil.class, "WINDOWS_APPDATA", appData.toString());
        EnvironmentUtil.setWebappContext(true);

        Path base = DirectoryUtil.getBaseDataDirectory();
        Path expected = Paths.get(appData.toString() + "\\Sismics\\Docs");
        assertEquals(expected.toAbsolutePath().normalize(), base.toAbsolutePath().normalize());

        deleteRecursively(appData);
    }

    @Test
    public void usesMacHomeWhenWebappOnMac() throws Exception {
        Path macHome = Files.createTempDirectory("teedy_machome");
        setStaticField(EnvironmentUtil.class, "TEEDY_HOME", null);
        setStaticField(EnvironmentUtil.class, "OS", "mac");
        setStaticField(EnvironmentUtil.class, "MAC_OS_USER_HOME", macHome.toString());
        EnvironmentUtil.setWebappContext(true);

        Path base = DirectoryUtil.getBaseDataDirectory();
        Path expected = Paths.get(macHome.toString() + "/Library/Sismics/Docs");
        assertEquals(expected.toAbsolutePath().normalize(), base.toAbsolutePath().normalize());

        deleteRecursively(macHome);
    }

    private static Object getStaticField(Class<?> clazz, String name) throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static void setStaticField(Class<?> clazz, String name, Object value) throws Exception {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static void deleteRecursively(Path path) throws Exception {
        if (path == null || !Files.exists(path)) {
            return;
        }
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                        // best-effort cleanup
                    }
                });
    }
}
