package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 테스트 전용: SseEmitter에 붙어 보낸 내용을 글자로 모은다.
 * ResponseBodyEmitter.Handler와 initialize가 패키지 안에서만 보여서 같은 패키지에 둔다.
 * send 한 번(이벤트 한 건)이 blocks의 한 칸이 된다.
 */
public final class SseEmitterProbe {

	private final List<String> blocks = new CopyOnWriteArrayList<>();

	private SseEmitterProbe() {
	}

	public static SseEmitterProbe attach(SseEmitter emitter) {
		SseEmitterProbe probe = new SseEmitterProbe();
		try {
			emitter.initialize(probe.new RecordingHandler());
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return probe;
	}

	/** 지금까지 보낸 내용. 한 칸이 event · id · data 줄이 든 한 건이거나 주석(ping) 한 건이다. */
	public List<String> blocks() {
		return List.copyOf(blocks);
	}

	private final class RecordingHandler implements ResponseBodyEmitter.Handler {

		@Override
		public void send(Object data, MediaType mediaType) {
			blocks.add(String.valueOf(data));
		}

		@Override
		public void send(Set<ResponseBodyEmitter.DataWithMediaType> items) {
			StringBuilder block = new StringBuilder();
			for (ResponseBodyEmitter.DataWithMediaType item : items) {
				block.append(item.getData());
			}
			blocks.add(block.toString());
		}

		@Override
		public void complete() {
		}

		@Override
		public void completeWithError(Throwable failure) {
		}

		@Override
		public void onTimeout(Runnable callback) {
		}

		@Override
		public void onError(Consumer<Throwable> callback) {
		}

		@Override
		public void onCompletion(Runnable callback) {
		}

	}

}
