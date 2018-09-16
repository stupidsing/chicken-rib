package chickenrib.net.cluster;

import java.io.IOException;
import java.util.Set;

import suite.streamlet.FunUtil.Fun;
import suite.streamlet.Signal;

public interface Cluster {

	public void start() throws IOException;

	public void stop() throws IOException;

	public Object requestForResponse(String peer, Object request);

	public <I, O> void setOnReceive(Class<I> clazz, Fun<I, O> onReceive);

	public Set<String> getActivePeers();

	public Signal<String> getOnJoined();

	public Signal<String> getOnLeft();

	public String getMe();

}
