package chickenrib.net.cluster;

import java.io.IOException;
import java.util.Set;

import primal.fp.Funs.Fun;
import suite.streamlet.Pusher;

public interface Cluster {

	public void start() throws IOException;

	public void stop() throws IOException;

	public Object requestForResponse(String peer, Object request);

	public <I, O> void setOnReceive(Class<I> clazz, Fun<I, O> onReceive);

	public Set<String> getActivePeers();

	public Pusher<String> getOnJoined();

	public Pusher<String> getOnLeft();

	public String getMe();

}
