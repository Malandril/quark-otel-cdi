package org.acme;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.Duration;

@Path("/hello")
public class GreetingResource {

    @Inject
    Event<Waow> events;
    @Inject
    Tracer tracer;

    @Inject
    EventBus bus;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        Log.info("Hello");
        Waow waow = new Waow("waow", Context.current());
        bus.requestAndForget("vertx", waow);
        events.fireAsync(waow);
        Log.info("Sent");
        return "Hello from Quarkus REST";
    }

    @ConsumeEvent("vertx")
    public Uni<Void> consume(Waow waow) throws InterruptedException {
        Log.infof("Received vertx event %s", waow.message);
        return Uni.createFrom().nullItem().
                onItem().delayIt().by(Duration.ofSeconds(1))
                .invoke(() -> Log.infof("End vertx event %s", waow.message))
                .replaceWithVoid();
    }

    public void asyncData(@ObservesAsync Waow waow) throws InterruptedException {
        Log.infof("Received cdi async %s", waow.message);
        Thread.sleep(1000);
        Log.infof("End cdi async %s", waow.message);
    }

    public record Waow(String message, Context context) {
    }
}
