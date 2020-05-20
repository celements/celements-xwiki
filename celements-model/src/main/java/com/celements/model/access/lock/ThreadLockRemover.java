package com.celements.model.access.lock;

import java.util.List;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ActionExecutionEvent;
import org.xwiki.observation.event.Event;

import com.celements.model.context.ModelContext;
import com.google.common.collect.ImmutableList;

@Singleton
@Component("ModelLock")
public class ThreadLockRemover implements EventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(ThreadLockRemover.class);

  @Requirement
  private ModelLockService lockService;

  @Requirement
  private ModelContext context;

  @Override
  public String getName() {
    return "ModelLock";
  }

  @Override
  public List<Event> getEvents() {
    return ImmutableList.of(
        new ActionExecutionEvent("inline"),
        new ActionExecutionEvent("edit"),
        new ActionExecutionEvent("admin"),
        new ActionExecutionEvent("import"));
  }

  @Override
  public void onEvent(Event event, Object source, Object data) {
    LOGGER.debug("unlock all held locks for event [{}], source [{}], data [{}]",
        event, source, data);
    lockService.unlockAllByMe(context.getWikiRef()); // TODO current wiki may not be sufficient
  }

}
