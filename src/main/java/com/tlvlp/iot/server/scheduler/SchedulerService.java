package com.tlvlp.iot.server.scheduler;

import lombok.extern.flogger.Flogger;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import javax.enterprise.context.ApplicationScoped;

import static org.quartz.CronScheduleBuilder.cronSchedule;

/**
 * Event scheduling is done via Quartz scheduler with automatic database persistence.
 */
@Flogger
@ApplicationScoped
public class SchedulerService {

    private final Scheduler scheduler;

    public SchedulerService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void addScheduledEvent(String schedulerGroup, String schedulerName, String cron, String eventAddress, String eventMessage) {
        try {
            JobDetail job = JobBuilder.newJob(EventJob.class)
                    .withIdentity(schedulerName, schedulerGroup)
                    .usingJobData("eventAddress", eventAddress)
                    .usingJobData("eventMessage", eventMessage)
                    .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(schedulerName, schedulerGroup)
                    .withSchedule(cronSchedule(cron))
                    .startNow()
                    .build();

            scheduler.scheduleJob(job, trigger);
        } catch (Exception e) {
            var err = "Unable to add scheduled event: " + e.getMessage();
            log.atSevere().log(err);
            throw new ScheduledEventException(err);
        }

    }

    public void pauseScheduledEvent() {
        //TODO
    }

    public void removeScheduledEvent() {
        //TODO
    }

    public void getAllScheduledEvents() {
        //TODO
    }

    public void getScheduledEventsForUnit(Long unitId) {
        //TODO
    }


}
