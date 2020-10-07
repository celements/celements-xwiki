package com.celements.rights.function;

import static com.google.common.collect.ImmutableSet.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.auth.user.User;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.context.ModelContext;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.reference.RefBuilder;
import com.celements.model.util.ModelUtils;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.celements.rights.classes.FunctionRightsClass;
import com.google.common.collect.ImmutableList;

@Component
public class DefaultFunctionRightsAccess implements FunctionRightsAccess {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFunctionRightsAccess.class);

  public static final String SPACE_NAME = "FunctionRights";

  @Requirement
  private IRightsAccessFacadeRole rightsAccess;

  @Requirement
  private IModelAccessFacade modelAccess;

  @Requirement
  private ModelUtils modelUtils;

  @Requirement
  private ModelContext context;

  @Override
  public Set<DocumentReference> getGroupsWithAccess(String... functionNames) {
    Set<DocumentReference> groups = streamGroupsWithAccess(functionNames)
        .collect(toImmutableSet());
    LOGGER.debug("getGroupsWithAccess - for function [{}]: [{}]", functionNames, groups);
    return groups;
  }

  private Stream<DocumentReference> streamGroupsWithAccess(String... functionNames) {
    return getFunctionRightDocRefs(functionNames)
        .map(modelAccess::getOrCreateDocument)
        .flatMap(doc -> XWikiObjectFetcher.on(doc)
            .filter(FunctionRightsClass.CLASS_REF)
            .fetchField(FunctionRightsClass.FIELD_GROUP)
            .stream());
  }

  private Stream<DocumentReference> getFunctionRightDocRefs(String... functionNames) {
    List<WikiReference> wikis = ImmutableList.of(context.getWikiRef(), modelUtils.getMainWikiRef());
    return Stream.of(functionNames)
        .flatMap(functionName -> wikis.stream()
            .map(wiki -> RefBuilder.from(wiki).space(SPACE_NAME).doc(functionName)))
        .map(builder -> builder.nullable().build(DocumentReference.class))
        .filter(Objects::nonNull);
  }

  @Override
  public boolean hasGroupAccess(DocumentReference groupDocRef, String... functionNames) {
    return streamGroupsWithAccess(functionNames).anyMatch(group -> group.equals(groupDocRef));
  }

  @Override
  public boolean hasUserAccess(User user, String... functionNames) {
    return streamGroupsWithAccess(functionNames)
        .anyMatch(groupDocRef -> rightsAccess.isInGroup(groupDocRef, user));
  }

  @Override
  public boolean hasCurrentUserAccess(String... functionNames) {
    return context.getCurrentUser().toJavaUtil()
        .map(user -> hasUserAccess(user, functionNames))
        .orElse(false);
  }

}
