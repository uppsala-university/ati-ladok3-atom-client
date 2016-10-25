package se.sunet.ati.ladok.atom;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.List;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.Parser;
import org.junit.Test;

public class AtomUtilTest {
	
	@Test
	public void testGetSelfLink() throws Exception {
		 Parser parser = Abdera.getNewParser();
		 
		 InputStream in = this.getClass().getResourceAsStream("/files/ladok/feeds/1");
		 Document<Feed> doc = parser.parse(in);
		 String selfLink = AtomUtil.getSelfLink(doc.getRoot());
		 
		 assertEquals("http://localhost/ladok/feeds/1", selfLink);
	}
	
	@Test
	public void testFeedIdAndEntryId() throws Exception {
		Parser parser = Abdera.getNewParser();
		InputStream in = this.getClass().getResourceAsStream("/files/ladok/feeds/1");
		Document<Feed> doc = parser.parse(in);
		Feed feed = doc.getRoot();
		List<Entry> entries = feed.getEntries();
		
		for (Entry entry : entries) {
			String feedIdAndEventId = AtomUtil.getFeedIdAndEventId(entry);
			
			assertEquals("http://localhost/ladok/feeds/1;02cb593f-8f06-4263-b5dd-5665a85f618b", feedIdAndEventId);
			break;
		}
	}
	
}
