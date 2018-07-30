package se.sunet.ati.ladok.atom;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.abdera.model.Entry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.*;

public class AtomClientTest {

	private HttpServer server;
	private AtomClient client;

	@Before
	public void setup() throws Exception {
		server = HttpServer.create(new InetSocketAddress(9001),0);
		server.createContext("/", new GetHandler());
		server.setExecutor(null);
		server.start();

		client = new AtomClient();
		client.setUseCert("false");
		client.setLastFeed("http://localhost:9001/files/ladok/feeds/recent");
	}

	@After
	public void cleanup() {
		server.stop(0);
	}
	/**
	 * This test uses the <code>lastFeed</code> property from the properties
	 * file as a starting point.
	 */
	@Test
	public void testAtomClientGetAllEntriesFromStart() throws Exception {
		List<Entry> entries = client.getEntries(null);

		assertNotNull(entries);
		assertFalse(entries.isEmpty());
		assertEquals(100, entries.size());
	}

	/**
	 * This test uses a hard coded entryId as a starting point. It does not
	 * use the <code>lastFeed</code> property from the properties file.
	 */
	@Test
	public void testAtomClientGetAllEntriesFromGivenId() throws Exception {
		String lastReadEntryId = "http://localhost:9001/files/ladok/feeds/2;1b4dcbae-59f2-4b4c-8e89-a508555393f3";
		List<Entry> entries = client.getEntries(lastReadEntryId);

		assertNotNull(entries);
		assertFalse(entries.isEmpty());
		assertEquals(2, entries.size());
	}

	@Test
	public void testGetLastEntry() {
		Entry lastEntry = client.findLastEntry();

		assertNotNull(lastEntry);
		assertEquals("d2dd4252-fd84-4977-819a-8dae653b24e5", lastEntry.getId().toASCIIString());

		String feedIdAndEntryId = AtomUtil.getFeedIdAndEntryId(lastEntry);
		assertEquals("http://localhost:9001/files/ladok/feeds/2;d2dd4252-fd84-4977-819a-8dae653b24e5", feedIdAndEntryId);
	}

	private class GetHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange httpExchange) throws IOException {
			String path = httpExchange.getRequestURI().getPath().replaceFirst("/","");
			ClassLoader classLoader = getClass().getClassLoader();
			File file = new File(classLoader.getResource(path).getFile());

			StringBuilder stringBuilder = new StringBuilder();
			try (Scanner scanner = new Scanner(file)){
				while (scanner.hasNextLine()) {
					stringBuilder.append(scanner.nextLine());
				}
			}

			byte[] bytes = stringBuilder.toString().getBytes();
			httpExchange.sendResponseHeaders(200,bytes.length);
			OutputStream os = httpExchange.getResponseBody();
			os.write(bytes,0, bytes.length);
			os.close();
		}
	}
}
