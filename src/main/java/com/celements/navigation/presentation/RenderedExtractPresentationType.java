package com.celements.navigation.presentation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

import com.celements.common.classes.IClassCollectionRole;
import com.celements.navigation.INavigation;
import com.celements.rendering.RenderCommand;
import com.celements.web.classcollections.DocumentDetailsClasses;
import com.celements.web.service.IWebUtilsService;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component("renderedExtract")
public class RenderedExtractPresentationType implements IPresentationTypeRole {

  private static Log LOGGER = LogFactory.getFactory().getInstance(
      RenderedExtractPresentationType.class);

  private static final String _CEL_CM_CPT_TREENODE_DEFAULT_CSSCLASS =
    "cel_cm_presentation_treenode";

  RenderCommand renderCmd;

  @Requirement
  IWebUtilsService webUtilsService;

  @Requirement
  Execution execution;

  protected XWikiContext getContext() {
    return (XWikiContext)execution.getContext().getProperty("xwikicontext");
  }

  @Requirement("celements.documentDetails")
  IClassCollectionRole docDetailsClasses;

  private DocumentDetailsClasses getDocDetailsClasses() {
    return (DocumentDetailsClasses) docDetailsClasses;
  }

  public void writeNodeContent(StringBuilder outStream, boolean isFirstItem,
      boolean isLastItem, DocumentReference docRef, boolean isLeaf, int numItem,
      INavigation nav) {
    LOGGER.debug("writeNodeContent for [" + docRef + "].");
    outStream.append("<div ");
    outStream.append(nav.addCssClasses(docRef, true, isFirstItem, isLastItem, isLeaf,
        numItem) + " ");
    outStream.append(nav.addUniqueElementId(docRef) + ">\n");
    try {
      outStream.append(getDocExtract(docRef));
    } catch (XWikiException exp) {
      LOGGER.error("Failed to get document for [" + docRef + "].", exp);
    }
    outStream.append("</div>\n");
  }

  private String getDocExtract(DocumentReference docRef) throws XWikiException {
    XWikiDocument contentDoc = getContext().getWiki().getDocument(docRef, getContext());
    DocumentReference documentExtractClassRef = getDocDetailsClasses(
        ).getDocumentExtractClassRef(docRef.getLastSpaceReference().getParent(
            ).getName());
    BaseObject extractObj = contentDoc.getXObject(documentExtractClassRef,
        DocumentDetailsClasses.FIELD_DOC_EXTRACT_LANGUAGE, getContext().getLanguage(),
        false);
    if (extractObj == null) {
      extractObj = contentDoc.getXObject(documentExtractClassRef,
          DocumentDetailsClasses.FIELD_DOC_EXTRACT_LANGUAGE,
          webUtilsService.getDefaultLanguage(docRef.getLastSpaceReference()), false);
    }
    if (extractObj != null) {
      return extractObj.getStringValue(DocumentDetailsClasses.FIELD_DOC_EXTRACT_CONTENT);
    } else {
      return "";
    }
  }

  RenderCommand getRenderCommand() {
    if (renderCmd == null) {
      renderCmd = new RenderCommand();
    }
    return renderCmd;
  }

  public String getDefaultCssClass() {
    return _CEL_CM_CPT_TREENODE_DEFAULT_CSSCLASS;
  }

  public String getEmptyDictionaryKey() {
    return "cel_nav_empty_presentation";
  }

  public SpaceReference getPageLayoutForDoc(DocumentReference docRef) {
    return null;
  }

}
