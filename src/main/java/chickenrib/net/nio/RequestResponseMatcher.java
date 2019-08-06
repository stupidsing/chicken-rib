package chickenrib.net.nio;

import java.util.HashMap;
import java.util.Map;

import chickenrib.net.nio.NioplexFactory.RequestResponseNioplex;
import primal.Verbs.Get;
import primal.adt.Mutable;
import primal.adt.Pair;
import primal.primitive.IntPrim.IntSink;
import primal.primitive.adt.Bytes;
import suite.concurrent.Condition;

public class RequestResponseMatcher {

	// tODO clean-up lost requests
	private Map<Integer, Pair<Mutable<Bytes>, Condition>> requests = new HashMap<>();

	public Bytes requestForResponse(RequestResponseNioplex channel, Bytes request) {
		return requestForResponse(token -> channel.send(RequestResponseNioplex.REQUEST, token, request));
	}

	public Bytes requestForResponse(IntSink sink) {
		return requestForResponse(sink, Long.MAX_VALUE);
	}

	public Bytes requestForResponse(IntSink sink, long timeout) {
		var token = Get.temp();
		var holder = Mutable.<Bytes> nil();
		var condition = new Condition();

		return condition.waitTill(() -> {
			return holder.value() != null;
		}, () -> {
			requests.put(token, Pair.of(holder, condition));
			sink.f(token);
		}, () -> {
			requests.remove(token);
			return holder.value();
		}, timeout);
	}

	public void onResponseReceived(int token, Bytes response) {
		requests.get(token).map((holder, condition) -> {
			condition.satisfyOne(() -> holder.set(response));
			return null;
		});
	}

}
