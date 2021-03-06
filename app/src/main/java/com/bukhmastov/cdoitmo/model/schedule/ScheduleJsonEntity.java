package com.bukhmastov.cdoitmo.model.schedule;

import com.bukhmastov.cdoitmo.model.JsonEntity;

public abstract class ScheduleJsonEntity extends JsonEntity {

    public abstract long getTimestamp();

    public abstract String getDataSource();

    public abstract boolean isEmptySchedule();
}
