package com.celements.model.util;

import static com.celements.model.util.EntityTypeUtil.*;
import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Preconditions.*;

import java.util.stream.Stream;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.access.ContextExecutor;
import com.celements.model.context.ModelContext;
import com.celements.model.reference.RefBuilder;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;

@Component
public class DefaultModelUtils implements ModelUtils {

  @Requirement
  private ModelContext context;

  @Requirement("explicit")
  private EntityReferenceResolver<String> resolver;

  private final XWiki getXWiki() {
    return context.getXWikiContext().getWiki();
  }

  @Override
  @Deprecated
  public boolean isAbsoluteRef(EntityReference ref) {
    return References.isAbsoluteRef(ref);
  }

  @Override
  @Deprecated
  public EntityReference cloneRef(EntityReference ref) {
    return References.cloneRef(ref);
  }

  @Override
  @Deprecated
  public <T extends EntityReference> T cloneRef(EntityReference ref, Class<T> token) {
    return References.cloneRef(ref, token);
  }

  @Override
  @Deprecated
  public <T extends EntityReference> com.google.common.base.Optional<T> extractRef(
      EntityReference fromRef, Class<T> token) {
    return References.extractRef(fromRef, token);
  }

  @Override
  @Deprecated
  public <T extends EntityReference> T adjustRef(T ref, Class<T> token, EntityReference toRef) {
    return References.adjustRef(ref, token, firstNonNull(toRef, context.getWikiRef()));
  }

  @Override
  public EntityReference resolveRef(String name) {
    return resolveRef(name, (EntityReference) null);
  }

  @Override
  public EntityReference resolveRef(String name, EntityReference baseRef) {
    return identifyEntityTypeFromName(name).toJavaUtil()
        .map(type -> resolveRef(name, getClassForEntityType(type), baseRef))
        .orElseThrow(() -> new IllegalArgumentException(
            "No valid reference class found for '" + name + "'"));
  }

  @Override
  public <T extends EntityReference> T resolveRef(String name, Class<T> token) {
    return resolveRef(name, token, null);
  }

  @Override
  public <T extends EntityReference> T resolveRef(String name, Class<T> token,
      EntityReference baseRef) {
    EntityReference resolvedRef;
    EntityType type = getEntityTypeForClassOrThrow(token);
    if (checkNotNull(name).isEmpty()) {
      throw new IllegalArgumentException("name may not be empty");
    } else if (type == getRootEntityType()) {
      // resolver cannot handle root reference
      resolvedRef = new WikiReference(name);
    } else {
      baseRef = References.combineRef(baseRef, context.getWikiRef()).get();
      resolvedRef = resolver.resolve(name, type, baseRef);
    }
    return References.cloneRef(resolvedRef, token); // effective immutability
  }

  @Override
  public WikiReference getMainWikiRef() {
    return new WikiReference("xwiki");
  }

  @Override
  public Stream<WikiReference> getAllWikis() {
    Stream<WikiReference> stream;
    try {
      stream = getXWiki().getVirtualWikisDatabaseNames(context.getXWikiContext()).stream()
          .map(name -> RefBuilder.create().wiki(name).build(WikiReference.class));
    } catch (XWikiException xwe) {
      stream = Stream.of(context.getWikiRef());
    }
    return Stream.concat(Stream.of(getMainWikiRef()), stream).distinct();
  }

  @Override
  public Stream<SpaceReference> getAllSpaces(WikiReference wikiRef) {
    RefBuilder builder = RefBuilder.from(wikiRef);
    try {
      return ContextExecutor.executeInWikiThrows(wikiRef,
          () -> getXWiki().getSpaces(context.getXWikiContext()).stream())
          .map(name -> builder.space(name).build(SpaceReference.class))
          .distinct();
    } catch (XWikiException exc) {
      return Stream.of();
    }
  }

  @Override
  public String getDatabaseName(WikiReference wikiRef) {
    checkNotNull(wikiRef);
    String database = "";
    if (getMainWikiRef().equals(wikiRef)) {
      database = getXWiki().Param("xwiki.db", "").trim();
    }
    if (database.isEmpty()) {
      database = wikiRef.getName().replace('-', '_');
    }
    return getXWiki().Param("xwiki.db.prefix", "") + database.replace('-', '_');
  }

  @Override
  public String serializeRef(EntityReference ref, ReferenceSerializationMode mode) {
    checkNotNull(ref);
    // strip child from immutable references by creating relative reference
    // for reason see DefaultStringEntityReferenceSerializer#L29
    ref = new RefBuilder().with(ref).buildRelative();
    return getSerializerForMode(mode).serialize(ref);
  }

  @SuppressWarnings("unchecked")
  private EntityReferenceSerializer<String> getSerializerForMode(ReferenceSerializationMode mode) {
    String hint;
    switch (mode) {
      case GLOBAL:
        hint = "default";
        break;
      case LOCAL:
        hint = "local";
        break;
      case COMPACT:
        hint = "compact";
        break;
      case COMPACT_WIKI:
        hint = "compactwiki";
        break;
      default:
        throw new IllegalArgumentException(String.valueOf(mode));
    }
    return Utils.getComponent(EntityReferenceSerializer.class, hint);
  }

  @Override
  public String serializeRef(EntityReference ref) {
    return serializeRef(ref, ReferenceSerializationMode.GLOBAL);
  }

  @Override
  public String serializeRefLocal(EntityReference ref) {
    return serializeRef(ref, ReferenceSerializationMode.LOCAL);
  }

}
