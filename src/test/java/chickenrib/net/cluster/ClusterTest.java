package chickenrib.net.cluster;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static primal.statics.Rethrow.ex;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;

import chickenrib.net.cluster.impl.ClusterImpl;
import primal.Verbs.Sleep;

public class ClusterTest {

	private InetAddress localHost = ex(() -> InetAddress.getLocalHost());

	@Test
	public void testCluster() throws IOException {
		var peers = Map.ofEntries( //
				entry("NODE0", new InetSocketAddress(localHost, 3000)), //
				entry("NODE1", new InetSocketAddress(localHost, 3001)));

		var cluster0 = new ClusterImpl("NODE0", peers);
		var cluster1 = new ClusterImpl("NODE1", peers);

		cluster1.setOnReceive(Integer.class, i -> i + 1);

		cluster0.start();
		cluster1.start();

		Sleep.quietly(2 * 1000);

		System.out.println("=== CLUSTER FORMED (" + LocalDateTime.now() + ") ===\n");

		assertEquals(12346, cluster0.requestForResponse("NODE1", 12345));

		cluster0.stop();
		cluster1.stop();

		System.out.println("=== CLUSTER STOPPED (" + LocalDateTime.now() + ") ===\n");
	}

}
