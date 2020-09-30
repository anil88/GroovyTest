import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.GroovySystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertNotNull;

public class TestGroovy {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private GroovyShell shell;
    private GroovyClassLoader groovyClassLoader;
    private File scriptsHome;
    private File pkg1;
    private File pkg2;
    private final static String scriptB = "package pkg1\n"
                                          + "\n"
                                          + "import pkg2.A\n"
                                          + "\n"
                                          + "public class B {\n"
                                          + "    public static final String Prop = \"ABCDDD\";\n"
                                          + "\n"
                                          + "    B() {\n"
                                          + "        A a = new A()\n"
                                          + "    }\n"
                                          + "\n"
                                          + "    public static class NestedInB {\n"
                                          + "        public static final String NestedProp = \"ABCD\";\n"
                                          + "    }\n"
                                          + "}";
    private final static String scriptA = "package pkg2\n"
                                          + "\n"
                                          + "import pkg1.B\n"
                                          + "\n"
                                          + "class A {\n"
                                          + "    @SuppressWarnings(value = [B.NestedInB.NestedProp])\n"
                                          + "    A() {\n"
                                          + "        String name = B.NestedInB.class.getName();\n"
                                          + "        System.out.println(name);\n"
                                          + "        System.out.println(B.NestedInB.NestedProp);\n"
                                          + "    }\n"
                                          + "}";

    @Before
    public void setUp() throws Exception {
        shell = new GroovyShell(getClass().getClassLoader());
        groovyClassLoader = shell.getClassLoader();
        scriptsHome = temporaryFolder.newFolder("scripts");
        pkg1 = temporaryFolder.newFolder("scripts", "pkg1");
        pkg2 = temporaryFolder.newFolder("scripts", "pkg2");

        copyResource(scriptB, Paths.get(pkg1.getPath() + File.separator + "B.groovy"));
        copyResource(scriptA, Paths.get(pkg2.getPath() + File.separator + "A.groovy"));
    }

    @Test
    public void scriptCanNotBeLoaded_exception() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        System.out.println("GroovyVersion:" + GroovySystem.getVersion());
        groovyClassLoader.addClasspath(scriptsHome.getPath());
        final Class<?> a = groovyClassLoader.loadClass("pkg2.A");
        a.newInstance();
        assertNotNull(a);
    }

    @Test
    public void scriptCanBeLoaded_success() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        System.out.println("GroovyVersion:" + GroovySystem.getVersion());
        final File scriptA = new File(pkg1 + File.separator + "B.groovy");
        final File scriptB = new File(pkg2 + File.separator + "A.groovy");

        CompilerConfiguration config = new CompilerConfiguration();
        config.setTargetDirectory(scriptsHome);

        final CompilationUnit compilationUnit = new CompilationUnit(config, null, groovyClassLoader);
        File[] scripts = new File[2];
        scripts[0] = scriptB;
        scripts[1] = scriptA;

        compilationUnit.addSources(scripts);
        compilationUnit.compile();

        groovyClassLoader.addClasspath(config.getTargetDirectory().getAbsolutePath());
        final Class<?> a = groovyClassLoader.loadClass("pkg2.A");
        a.newInstance();
        assertNotNull(a);
    }

    public void copyResource(final String storyAsText, final Path folder) throws Exception {
        InputStream s = new ByteArrayInputStream(storyAsText.getBytes());
        Files.copy(s, folder);
    }
}
