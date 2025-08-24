package com.sapphireims.engine.listener;

import com.sapphireims.engine.domain.WfTask;
import java.util.Map;

public interface TaskListener {
    default void onStart(WfTask task) {}
    default void onComplete(WfTask task, Map<String,Object> output) {}
}
