package se.sunet.ati.ladok.atom;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.model.Link;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AtomUtil {

	private static Log log = LogFactory.getLog(AtomUtil.class);

	public static final String LINK_NAME_PREVIOUS_ARCHIVE = "prev-archive";
	public static final String LINK_NAME_NEXT_ARCHIVE = "next-archive";
	public static final String LINK_NAME_SELF = "self";
	public static final String LINK_NAME_VIA = "via";
	
	public static final String FEED_ENTRY_SEPARATOR = ";";

	/**
	 * Hämtar URL till nästa arkiv i ordningen.
	 * 
	 * @param f Det arkiv man vill basera frågan på.
	 * @return URL till nästa arkiv.
	 */
	public static String getNextArchiveLink(Feed f) {
		return getLinkHref(f, LINK_NAME_NEXT_ARCHIVE);
	}
	
	/**
	 * Hämtar URL till föregående arkiv i ordningen.
	 * 
	 * @param f Det arkiv man vill basera frågan på.
	 * @return URL till föregående arkiv.
	 */
	public static String getPrevArchiveLink(Feed f) {
		return getLinkHref(f, LINK_NAME_PREVIOUS_ARCHIVE);
	}

	/**
	 * Hämtar URL till via-länken.
	 * 
	 * @param f Det arkiv man vill basera frågan på.
	 * @return URL till föregående arkiv.
	 */
	private static String getViaLink(Feed f) {
		return getLinkHref(f, LINK_NAME_VIA);
	}	
	
	/**
	 * Hämtar URL till det egna arkivet.
	 * 
	 * @param f Det arkiv man vill basera frågan på.
	 * @return URL till föregående arkiv.
	 */
	public static String getSelfLink(Feed f) {
		String via = getViaLink(f);
		return via != null ? via : getLinkHref(f, LINK_NAME_SELF);
	}
	
	public static String getSelfLink(Entry e) {
		Feed f = (Feed) e.getDocument().getRoot();
		return getSelfLink(f);
	}

	public static String getFeedIdAndEventId(Entry e) {
		return getSelfLink(e) + FEED_ENTRY_SEPARATOR + e.getId().toString();
	}

	
	/**
	 * Hjälpmetod för at hämta ut länkars värde ur en feed.
	 * 
	 * @param f Den feed man vill extrahera länkar från.
	 * @param linkname Namnet på länken.
	 * @return URL för efterfrågad länk.
	 */
	private static String getLinkHref(Feed f, String linkname) {
		String linkHref = null;
		
		for (Link link : f.getLinks()) {
			if (linkname.equalsIgnoreCase(link.getRel())) {
				linkHref = link.getAttributeValue("href");
				linkHref = linkHref.replaceAll("http://mit[0-9]+-ladok3.its.umu.se:[0-9]+", "https://api.mit.ladok.se");
				break;
			}
		}
		
		log.info("Returning link '" + linkname + "':" + linkHref);
		
		return linkHref;		
	}
	

}
