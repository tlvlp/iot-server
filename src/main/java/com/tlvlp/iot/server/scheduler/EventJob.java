package com.tlvlp.iot.server.scheduler;

import io.vertx.mutiny.core.eventbus.EventBus;
import lombok.extern.flogger.Flogger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.enterprise.context.ApplicationScoped;

@Flogger
@ApplicationScoped
public class EventJob implements Job {

    private final EventBus eventBus;

    public EventJob(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            String address = String.valueOf(jobExecutionContext.get("eventAddress"));
            Object message = jobExecutionContext.get("eventMessage");
            eventBus.sendAndForget(address, message);
        } catch (Exception e) {
            var err = String.format("Unable to execute scheduled job: %s", e.getMessage());
            log.atSevere().log(err);
            throw new JobExecutionException(err, false);
        }
    }
}
