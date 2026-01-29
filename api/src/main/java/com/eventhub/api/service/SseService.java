package com.eventhub.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class SseService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        // Timeout muy alto o infinito para mantener la conexión. Por defecto suele ser
        // 30s.
        // Aquí ponemos 1 hora (3600000L). El cliente debe reconectar si se pierde.
        SseEmitter emitter = new SseEmitter(3600000L);

        emitter.onCompletion(() -> {
            log.debug("SseEmitter completed");
            emitters.remove(emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SseEmitter timeout");
            emitter.complete();
            emitters.remove(emitter);
        });

        emitter.onError((e) -> {
            log.debug("SseEmitter error: {}", e.getMessage());
            emitter.completeWithError(e);
            emitters.remove(emitter);
        });

        emitters.add(emitter);
        return emitter;
    }

    public void notifyClients(String eventName, Object data) {
        if (emitters.isEmpty())
            return;

        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.debug("Error sending SSE to client, removing emitter");
                deadEmitters.add(emitter);
            }
        });

        emitters.removeAll(deadEmitters);
    }
}
