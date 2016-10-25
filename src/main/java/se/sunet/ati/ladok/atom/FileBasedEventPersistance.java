package se.sunet.ati.ladok.atom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.abdera.model.Entry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileBasedEventPersistance implements EventPersistance {
	
	private Log log = LogFactory.getLog(this.getClass());
	
	private static String PROPERTY_STORE_FILENAME = "ladok.log";
	private static String PROPERTY_LAST_READ_FEEDID_AND_ENTRYID = "last";
	
	static {
		File f = new File("ladok.log");
		
		if (!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public synchronized Entry saveEntry(Entry e) throws Exception{
		
		String feedIdAndEventId = AtomUtil.getFeedIdAndEventId(e);
		
		log.info("saveEntry feedIdAndEventId=" + feedIdAndEventId);
		
		Properties prop = new Properties();
		prop.setProperty(PROPERTY_LAST_READ_FEEDID_AND_ENTRYID, feedIdAndEventId);
		
		try {
			prop.store(new FileOutputStream(PROPERTY_STORE_FILENAME), null);
			log.info("Saving message: " + e.getId().toString());
			
			return e;
			
		} catch (Exception e1) {
			
			log.error(e1);
			
			return null;
		}
		
	}

	@Override
	public boolean isUnseenEntry(Entry e) {
		// TODO
		return false;
	}
	
	@Override
	public String getLastReadEntryId() {
		
		Properties prop = new Properties();
		
		try {
			prop.load(new FileInputStream(PROPERTY_STORE_FILENAME));
			if (prop.containsKey(PROPERTY_LAST_READ_FEEDID_AND_ENTRYID)) {
				return (prop.getProperty(PROPERTY_LAST_READ_FEEDID_AND_ENTRYID));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
		
	}

}
