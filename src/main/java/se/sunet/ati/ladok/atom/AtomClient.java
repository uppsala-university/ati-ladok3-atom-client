package se.sunet.ati.ladok.atom;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.net.ssl.KeyManagerFactory;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.Response.ResponseType;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.util.ClientAuthSSLProtocolSocketFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import static se.sunet.ati.ladok.atom.AtomUtil.FEED_ENTRY_SEPARATOR;
import static se.sunet.ati.ladok.atom.AtomUtil.getNextArchiveLink;
import static se.sunet.ati.ladok.atom.AtomUtil.getPrevArchiveLink;
import static se.sunet.ati.ladok.atom.AtomUtil.getSelfLink;

public class AtomClient {

	private static final String PROPERTY_CLIENT_CERTIFICATE_FILE = "clientCertificateFile";
	private static final String PROPERTY_CLIENT_CERTIFICATE_PWD = "clientCertificatePwd";
	private static final String PROPERTY_LAST_FEED = "lastFeed";
	private static final String PROPERTY_USE_CERT = "useCert";
	private static final String XPATH_HANDELSEUID_SELECTOR = "/*/events:HandelseUID";
	public static String TOO_MANY_EVENTS_REQUESTED = "Too many events requested :-(";
	private static int MAX_ENTRIES_PER_RUN = 100;

	private AbderaClient client = null;
	private boolean propertiesInitialized = false;
	private String lastFeed = null;
	private String useCert = "false";
	private String clientCertificateFile = null;
	private String clientCertificatePwd = null;

	private Log log = LogFactory.getLog(this.getClass());
	private static String propertyFile = "atomclient.properties";

        private Map<String, Feed> cachedFeeds = new HashMap<>();


  public AtomClient() throws Exception {
		// We don't initialize here, but instead initialize lazily to enable
		// loading properties either from properties file when used stand alone
		// or via dependency injection when used in an OSGi environment.
		// init();
	}

	/**
	 * Finds the last entry added to the feed defined by "lastFeed"
	 * @return the last entry in the lastFeed, if such exists, otherwise null
	 */
	public Entry findLastEntry() {
		Feed feed = getFeed(lastFeed);
		if (feed == null) {
			return null;
		}

		return findLastEntry(feed);
	}

	private Entry findLastEntry(Feed feed) {
		List<Entry> entries = feed.getEntries();
		if (entries != null && entries.size() > 0) {
			return entries.get(0);
		}
		return null;
	}

	private void init() throws Exception {
		if (propertiesInitialized) {
			return; // Already initialized
		}
		loadProperties();
		checkProperties();
		propertiesInitialized = true;
	}

	private void loadProperties() throws Exception {
		if (lastFeed != null) {
			return; // Properties already loaded via setters
		}
		Properties properties = new Properties();
		try {
			InputStream in = this.getClass().getClassLoader().getResourceAsStream(propertyFile);
			if (in == null) {
				throw new Exception("Unable to find atomclient.properties (see atomclient.properties.sample)");
			}
			properties.load(in);
			lastFeed = properties.getProperty(PROPERTY_LAST_FEED);
			if (properties.getProperty(PROPERTY_USE_CERT) != null) {
				useCert = properties.getProperty(PROPERTY_USE_CERT);
			}
			if ("true".equals(useCert)) {
				clientCertificateFile = StrSubstitutor.replaceSystemProperties(properties.getProperty(PROPERTY_CLIENT_CERTIFICATE_FILE));
				clientCertificatePwd = properties.getProperty(PROPERTY_CLIENT_CERTIFICATE_PWD);
			}
		}
		catch (IOException e) {
			log.error("Unable to read atomclient.properties");
			throw e;
		}
	}

	private void checkProperties() throws Exception {
		if (lastFeed == null) {
			throw new Exception("Missing property \"" + PROPERTY_LAST_FEED + "\".");
		}
		log.info(PROPERTY_LAST_FEED + ": " + lastFeed);
		log.info(PROPERTY_USE_CERT + ": " + useCert);
		if ("true".equals(useCert)) {
			if (clientCertificateFile == null || clientCertificateFile.equals("")) {
				throw new Exception("Missing property \"" + PROPERTY_CLIENT_CERTIFICATE_FILE + "\".");
			}
			if (clientCertificatePwd == null || clientCertificatePwd.equals("")) {
				throw new Exception("Missing property \"" + PROPERTY_CLIENT_CERTIFICATE_PWD + "\".");
			}
		}
	}

	private InputStream createKeystoreInputStream() throws Exception {
		InputStream keystoreInputStream = null;
		if (Files.exists(Paths.get(clientCertificateFile))) {
			// Try to find the keystore as a file
			keystoreInputStream = new FileInputStream(clientCertificateFile);
			log.info("Found the client certificate file '" + clientCertificateFile + "' as a file");
		}
		else {
			// Try to find the keystore as a classpath resource
			keystoreInputStream = this.getClass().getClassLoader().getResourceAsStream(clientCertificateFile);
			if (keystoreInputStream == null) {
				String message = "Unable to find the client certificate file '" + clientCertificateFile + "' as a classpath resource";
				log.debug(message);
				throw new Exception(message);
			}
			log.info("Found the client certificate file '" + clientCertificateFile + "' as a classpath resource");
		}
		if (keystoreInputStream == null) {
			throw new Exception("Unable to find the client certificate file '" + clientCertificateFile + "'");
		}
		return keystoreInputStream;
	}

	/**
	 * Hämtar en Abdera-klient för att hämta feeds.
	 *
	 * @return En klient som kan returnera feeds.
	 * @throws Exception Om någonting i certifikatshanteringen fungerar.
 	 */
	private AbderaClient getClient() throws Exception {
		if(client == null) {
			log.info("Using certificate: " + useCert);
			Abdera abdera = new Abdera();
			// Använd en HttpClient som har tråd för att hantera connections
			HttpClient httpClient = new HttpClient(new SimpleHttpConnectionManager());
			client = new AbderaClient(abdera, httpClient);
			// Bevara cookies mellan anrop
			client.getHttpClientParams().setParameter("http.protocol.single-cookie-header", true);
			InputStream keystoreInputStream = null;
			try {
				if ("true".equals(useCert)) {
					KeyStore clientKeystore = KeyStore.getInstance("PKCS12");
					keystoreInputStream = createKeystoreInputStream();
					clientKeystore.load(keystoreInputStream, clientCertificatePwd.toCharArray());
					ClientAuthSSLProtocolSocketFactory factory = new ClientAuthSSLProtocolSocketFactory(clientKeystore, clientCertificatePwd, "TLS", KeyManagerFactory.getDefaultAlgorithm(), null);
					AbderaClient.registerFactory(factory, 443);
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new Exception(e.getMessage());
			} finally {
				if (keystoreInputStream != null) {
					keystoreInputStream.close();
				}
			}
		}
		else {
			log.debug("Returning a previously instantiated instance of AbderaClient.");
		}
		return client;
	}

	/**
	 * Hämtar ett feed-objekt från en given URL.
	 *
	 * @param url URL för den feed som efterfrågas.
	 * @return Efterfrågad feed.
	 */
	private Feed getFeed(String url) {

		log.info("Fetching feed: " + url);

		Feed cachedFeed = getCachedFeed(url);

		if(cachedFeed != null) {
			return cachedFeed;
		}

		Feed f = null;

		if (url != null) {
			try {
				ClientResponse resp = getClient().get(url);

				if (resp.getType() == ResponseType.SUCCESS) {
					Document<Feed> doc = resp.getDocument();
					f = doc.getRoot();
				} else {

					// Only accept success or client error (logical error).
					if (resp.getType() != ResponseType.CLIENT_ERROR) {
						throw new UnexpectedClientResponseException(resp
								.getType().toString());
					}
					else {
						log.error("The client received an error response with status code " + resp.getStatus());
					}

				}
			} catch (Exception e) {
				log.error(e + " :: " + url);
			}
		}

		return f;

	}

	/**
	 * Hämtar det första arkivet med händelser i hela systemet.
	 *
	 * @param f Det arkiv som man man utgår från.
	 * @return Det första arkivet i kedjan av händelsearkivet.
	 */
	private Feed findFirstFeed(Feed f) {

		log.info("Finding first feed from: " + f.getId());
		Feed first = f;
		Feed previous = getFeed(getPrevArchiveLink(f));

		while (previous != null) {
			first = previous;
			previous = getFeed(getPrevArchiveLink(previous));
		}

		return first;

	}

	/**
	 * Hittar den första händelsen i hela arkivet och returnerar det
	 * som en sammanslagning tillsammans med identiferare för det arkiv
	 * som händelsen är dokumenterad i.
	 *
	 * @param f Det arkiv som är utgångspunkten.
	 * @return Idenfifeiraren för den första händelsen i en sammanslagning
	 * med identifieraren för hemvistarkivet. Null om ingen hittades.
	 */
	private String findFirstFeedIdAndFirstEntryId(Feed f) {

		String baseUri = (f != null) ? f.getBaseUri().toString() : null;

		log.info("Finding first feed and entry from: " + baseUri);

		Entry firstEntry = null;
		String selfLink = null;
		String entityId = null;

		Feed firstFeed = findFirstFeed(f);
		if (firstFeed == null) {
			return null;
		}

		List<Entry> entries = null;
		if (firstFeed != null) {
			entries = firstFeed.getEntries();
		}

		if (firstFeed != null && entries != null) {
			firstEntry = entries.get(entries.size() - 1);
			selfLink = getSelfLink(firstFeed);
			entityId = selfLink + FEED_ENTRY_SEPARATOR + firstEntry.getId().toString();
		}

		return entityId;
	}

	/**
	 * Hämta händelser efter den senast lästa men hämtar aldrig fler än
	 * MAX_ENTRIES_PER_RUN händelser per anrop.
	 *
	 * Om ingen utgångspunkt för frågan är definierad försöker man utgå från
	 * det senaste arkivet definierat i klientens egenskapsfil.
	 *
	 * @param feedIdAndLastEntryId Identifierare för den senast lästa händelsen inklusive referens till identifieraren för händelsens hemvistarkiv.
	 * @return En lista av händelser.
	 * @throws Exception Om det inte finns någon riktig utgångspunkt för frågan.
	 */
	public List<Entry> getEntries(String feedIdAndLastEntryId) throws Exception {
		log.info("Attempting to get all events starting from  " + feedIdAndLastEntryId);
		init();
		String[] parsed = null;
		String firstId = null;

		// TODO: Should not be needed: && !feedIdAndLastEntryId.equals("0")
		if (feedIdAndLastEntryId != null && !feedIdAndLastEntryId.equals("0")) {
			parsed = feedIdAndLastEntryId.split(FEED_ENTRY_SEPARATOR);
		} else {
			Feed feed = getFeed(lastFeed);
			if (feed == null) {
				log.warn("Feed " + lastFeed + " not available");
				return new ArrayList<Entry>();
			}
			firstId = findFirstFeedIdAndFirstEntryId(feed);
			if (firstId != null) {
				log.info("Retrieving first id in archive structure: " + firstId);
				parsed = firstId.split(FEED_ENTRY_SEPARATOR);
			}
		}

		if (parsed == null)
			throw new Exception("No proper starting point for the feed found.");

		String feedId = parsed[0];
		String entryId = parsed[1];

		return getEntries(feedId, (firstId == null ? entryId : null));
	}


	/**
	 * Vänder på alla händelseobjekt i ett arkiv så att de kommer i
	 * fallande kronologisk ordning.
	 *
	 * @param f Arkiv som innehåller de händelser som man vill vända på.
	 * @return En lista av händelser i kronologisk fallande ordning.
	 */
	private List<Entry> getSortedEntriesFromFeed(Feed f) {
		List<Entry> entries = new ArrayList<Entry>(f.getEntries());
		Collections.reverse(entries);
		return entries;
	}

	/**
	 * Filtrerar bort de händelser som redan har lästs.
	 *
	 * @param unfilteredEntries Lista av händelser som innehåller både lästa och oläsa händelser.
	 * @param lastReadEntryId Det senast lästa entryt.
	 * @return En lista av olästa händelser.
	 */
	private List<Entry> filterOlderEntries(List<Entry> unfilteredEntries, String lastReadEntryId) {

		// Get the index of the next entry based on entry id of the last read entry.
		int indexOfLastReadEntry = 0;
		for (Entry entry : unfilteredEntries) {
			indexOfLastReadEntry++;

			if (entry.getId().toString().equals(lastReadEntryId)) {
				break;
			}

		}

		// We need to handle the first event to, passed as null.
		if (lastReadEntryId == null)
			indexOfLastReadEntry = 0;

		List<Entry> result = unfilteredEntries.subList(indexOfLastReadEntry, unfilteredEntries.size());
		return result;
	}

	/**
	 * Extract an Ladok event identifier from a event XML source.
	 *
	 * @param xml The event.
	 * @return And identifier or empty string if not found.
	 */
	private String getEntryId(String xml) {
		NamespaceContext nsContext = new NamespaceContext() {

            @Override
            public String getNamespaceURI(String prefix) {
                return "http://schemas.ladok.se/events";
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return "events";
            }

            @Override
            public Iterator getPrefixes(String namespaceURI) {
                Set s = new HashSet();
                s.add("events");
                return s.iterator();
            }

        };

		XPath xPath = XPathFactory.newInstance().newXPath();
		xPath.setNamespaceContext(nsContext);

		String entryId = "";

		try {
			NodeList nList = (NodeList) xPath.evaluate(XPATH_HANDELSEUID_SELECTOR, new InputSource(new StringReader(xml)), XPathConstants.NODESET);
			if (nList.getLength() == 1 && nList.item(0) != null) {
				entryId = nList.item(0).getTextContent();
				log.debug("Extracting entry id: " + nList.item(0).getTextContent() ) ;
			}
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}

		return entryId;
	}

	/**
	 * Hämtar olästa entries från senast lästa entry tillsammans med entryts käll-feed.
	 * Antalet entries som returneras baseras på MAX_ENTRIES_PER_RUN.
	 *
	 * @param feedId Identifierare för feed som senast lästa entry finns i.
	 * @param lastReadEntryId Identifierare för senast lästa entry.
	 * @return En lista av olästa entries.
	 */
	private List<Entry> getEntries(String feedId, String lastReadEntryId) {

		log.info("Attempting to get max " + MAX_ENTRIES_PER_RUN + " events from latest feed " + feedId + " and up.");
		Feed f = getFeed(feedId);
		List<Entry> entries = new ArrayList<Entry>();
		if (f != null) {
			entries.addAll(filterOlderEntries(getSortedEntriesFromFeed(f), lastReadEntryId));
			String nextArchiveLink = getNextArchiveLink(f);
			while (f != null && nextArchiveLink != null && entries.size() < MAX_ENTRIES_PER_RUN) {
				f = getFeed(nextArchiveLink);
				if (f != null) {
					entries.addAll(getSortedEntriesFromFeed(f));
					cacheFeed(nextArchiveLink, f);
				}
			}
			log.info("Started from " +  lastReadEntryId + " in feed " + feedId + " and found " + entries.size()
					+ " entries");
		}
		return entries;
	}

	public String getClientCertificateFile() {
		return clientCertificateFile;
	}

	public void setClientCertificateFile(String certificateFile) {
		this.clientCertificateFile = certificateFile;
	}

	public String getClientCertificatePwd() {
		return clientCertificatePwd;
	}

	public void setClientCertificatePwd(String certificatePwd) {
		this.clientCertificatePwd = certificatePwd;
	}

	public String getLastFeed() {
		return lastFeed;
	}

	public void setLastFeed(String lastFeed) {
		this.lastFeed = lastFeed;
	}

	public String getUseCert() {
		return useCert;
	}

	public void setUseCert(String useCert) {
		this.useCert = useCert;
	}

	private Feed getCachedFeed(String url) {
		return cachedFeeds.get(url);
	}

	private void cacheFeed(String url, Feed feed) {
		cachedFeeds.clear();
		cachedFeeds.put(url, feed);
	}

}
