package se.sunet.ati.ladok.atom;

import java.util.Date;
import java.util.List;
import org.apache.abdera.model.Entry;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class AtomClientITCase {
	/**
	 * Det här testet använder <code>lastFeed</code> propertyn från
	 * properties-filen som en startpunkt.
	 */
	@Test
	public void testAtomClientGetAllEntriesFromStart() throws Exception {
		AtomClient atomClient = new AtomClient();
		long entryCount = 0;
		long start  = new Date().getTime();
		/**
		 * Antalet feeds som ska hämtas.
		 */
		long feedsToRead = 100;

		List<Entry> entries = null;
		Entry lastEntry = null;

		String feedIdAndLastEntryId = null;

		System.out.println("Ska hämta entries...");
		do {
			entries = atomClient.getEntries(feedIdAndLastEntryId);

			System.out.println("Hämtade " + entries.size() + " entries.");
			for(Entry e : entries) {
				System.out.println("entryid: " + e.getId()
								+ ", baseuri: "
								+ e.getBaseUri());
			}
			if(entries.size() > 0) {
				// Addera till totalen
				entryCount += entries.size();
				// Hämta ut och kom ihåg feedId och entryId
				lastEntry = entries.get(entries.size() - 1);
				feedIdAndLastEntryId = AtomUtil.getFeedIdAndEventId(lastEntry);
			}
		}
		while(entries.size() > 0 && !AtomUtil.getSelfLink(lastEntry).endsWith("/" + Long.toString(feedsToRead)));

		assertTrue(entryCount > 0);
		long end  = new Date().getTime();
		System.out.println("Read " + entryCount + " entries in " + (end - start) + " milliseconds.");
	}
}
