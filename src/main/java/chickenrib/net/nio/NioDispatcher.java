package chickenrib.net.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

import chickenrib.net.nio.NioplexFactory.Nioplex;

public interface NioDispatcher<C extends Nioplex> {

	public void start();

	public void stop();

	/**
	 * Establishes connection to other host actively.
	 */
	public C connect(InetSocketAddress address) throws IOException;

	/**
	 * Re-establishes connection using specified listener, if closed or dropped.
	 */
	public void reconnect(C channel, InetSocketAddress address) throws IOException;

	/**
	 * Ends connection.
	 */
	public void disconnect(C channel) throws IOException;

	/**
	 * Waits for incoming connections.
	 *
	 * @return event for switching off the server.
	 */
	public Closeable listen(int port) throws IOException;

}
