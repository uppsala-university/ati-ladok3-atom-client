package se.sunet.ati.ladok.atom;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.abdera.model.Entry;
import org.junit.Test;

public class AtomClientITCase {

	@Test
	public void testAtomClientGetAllEntriesFromStart() throws Exception {
		AtomClient ac = new AtomClient();

		List<Entry> entries = ac.getEntries(null);

		if (entries != null) {
			System.out.println("Found " + entries.size() + " entries");
			for (Entry e : entries) {
				System.out.println("entryid: " + e.getId() + ", baseuri: " + e.getBaseUri());
			}
		} else {
			System.out.println("No entries found.");
		}
	}

	@Test
	public void testAtomClientGetAllEntriesFromGivenId() throws Exception {

		String lastReadEntryId = "https://api.mit.ladok.se/uppfoljning/feed/2;10212d7e-9c73-11e5-865c-257de21b4421";

		AtomClient ac = new AtomClient();

		List<Entry> entries = ac.getEntries(lastReadEntryId);

		if (entries != null) {
			System.out.println("Found " + entries.size() + " entries");
			for (Entry e : entries) {
				System.out.println("entryid: " + e.getId() + ", baseuri: " + e.getBaseUri());
			}
		} else {
			System.out.println("No entries found.");
		}

		assertTrue(!entries.isEmpty());

	}

}
