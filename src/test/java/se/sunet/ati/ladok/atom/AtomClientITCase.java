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
		while (entries == null || (entries.size() > 0 && lastEntry != null && !AtomUtil.getSelfLink(lastEntry).endsWith("/" + Long.toString(feedsToRead)))) {
			System.out.println("Ska hämta entries...");
			entries = atomClient.getEntries(feedIdAndLastEntryId);

			if(entries != null) {
				System.out.println("Hämtade " + entries.size() + " entries.");
				for(Entry e : entries) {
					System.out.println("entryid: " + e.getId()
									+ ", baseuri: "
									+ e.getBaseUri());
				}
				entryCount += entries.size();
				// Hämta ut och kom ihåg feedId och entryId
				lastEntry = entries.get(entries.size() - 1);
				feedIdAndLastEntryId = AtomUtil.getFeedIdAndEventId(lastEntry);
			}
			else {
				System.out.println("Inga entries hittades.");
			}
		}
		assertTrue(!entries.isEmpty());
		long end  = new Date().getTime();
		System.out.println("Read " + entryCount + " entries in " + (end - start) + " milliseconds.");
	}
}
