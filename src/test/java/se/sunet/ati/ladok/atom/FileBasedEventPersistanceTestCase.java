package se.sunet.ati.ladok.atom;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.Parser;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class FileBasedEventPersistanceTestCase {
        private static EventPersistance eventPersistance;
        private static Entry entry;
        private static File file;

        @BeforeClass
        public static void beforeClass() throws IOException {
                eventPersistance = new FileBasedEventPersistance();

                // Read the first entry from a file
                Parser parser = Abdera.getNewParser();
                InputStream in = FileBasedEventPersistanceTestCase.class.getResourceAsStream("/files/ladok/feeds/1");
                Document<Feed> doc = parser.parse(in);
                Feed feed = doc.getRoot();
                List<Entry> entries = feed.getEntries();
                if(!entries.isEmpty()) {
                        entry = entries.get(0);
                }

                // Create a temporary file
                File directory = org.apache.commons.lang3.SystemUtils.getUserDir();
                file = new File(directory, "ladok.log");
                System.out.println("The file is " + file.getCanonicalPath());
                boolean fileCreated = file.createNewFile();
                if(fileCreated) {
                        System.out.println("Created the file " + file.getCanonicalPath());
                }
        }

        @Test(expected = FileNotFoundException.class)
        public void testSaveEntryReadOnly() throws Exception {
                file.setReadOnly();
                Entry returnedEntry = eventPersistance.saveEntry(entry);
        }

        @Test
        public void testSaveEntrySuccess() throws Exception {
                file.setWritable(true);
                Entry returnedEntry = eventPersistance.saveEntry(entry);
                assertNotNull(returnedEntry);
        }
}
