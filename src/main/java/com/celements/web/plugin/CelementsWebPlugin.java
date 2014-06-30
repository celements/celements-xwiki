/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.celements.web.plugin;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;

import com.celements.mandatory.CheckMandatoryDocuments;
import com.celements.navigation.cmd.GetMappedMenuItemsForParentCommand;
import com.celements.pagetype.IPageType;
import com.celements.rendering.RenderCommand;
import com.celements.web.plugin.api.CelementsWebPluginApi;
import com.celements.web.plugin.cmd.AddTranslationCommand;
import com.celements.web.plugin.cmd.CelSendMail;
import com.celements.web.plugin.cmd.CheckClassesCommand;
import com.celements.web.plugin.cmd.PasswordRecoveryAndEmailValidationCommand;
import com.celements.web.plugin.cmd.PossibleLoginsCommand;
import com.celements.web.plugin.cmd.SkinConfigObjCommand;
import com.celements.web.plugin.cmd.TokenBasedUploadCommand;
import com.celements.web.plugin.cmd.UserNameForUserDataCommand;
import com.celements.web.service.IPrepareVelocityContext;
import com.celements.web.service.IWebUtilsService;
import com.celements.web.token.NewCelementsTokenForUserCommand;
import com.celements.web.utils.IWebUtils;
import com.celements.web.utils.WebUtils;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiResponse;

public class CelementsWebPlugin extends XWikiDefaultPlugin {

  private static Log LOGGER = LogFactory.getFactory().getInstance(
      CelementsWebPlugin.class);

  private final static IWebUtils util = WebUtils.getInstance();

  final String PARAM_XPAGE = "xpage";
  final String PARAM_CONF = "conf";
  final String PARAM_AJAX_MODE = "ajax_mode";
  final String PARAM_SKIN = "skin";
  final String PARAM_LANGUAGE = "language";
  final String PARAM_XREDIRECT = "xredirect";

  private List<String> supportedAdminLangList;

  private CelSendMail injectedCelSendMail;

  public CelementsWebPlugin(
      String name, String className, XWikiContext context) {
    super(name, className, context);
  }

  public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
    return new CelementsWebPluginApi((CelementsWebPlugin) plugin, context);
  }

  public String getName() {
    return getPrepareVelocityContextService().getVelocityName();
  }

  public void init(XWikiContext context) {
    //TODO check if this is really needed for main-wiki or if we get a virtualInit on the
    //TODO main wiki to. (if needed move to ApplicationStartedEvent listener)
    LOGGER.trace("init called database [" + context.getDatabase() + "]");
    if ("1".equals(context.getWiki().Param("celements.classCollections.checkOnStart",
        "1"))) {
      new CheckClassesCommand().checkClasses();
    }
    if ("1".equals(context.getWiki().Param("celements.mandatory.checkOnStart", "1"))) {
      new CheckMandatoryDocuments().checkMandatoryDocuments();
    }
    super.init(context);
  }

  public void virtualInit(XWikiContext context) {
    //TODO move to WikiReadyEvent listener (after migration to xwiki > 4.1-M1
    LOGGER.trace("virtualInit called database [" + context.getDatabase() + "]");
    if ("1".equals(context.getWiki().Param("celements.classCollections.checkOnStart",
        "1"))) {
      new CheckClassesCommand().checkClasses();
    }
    if ("1".equals(context.getWiki().Param("celements.mandatory.checkOnStart", "1"))) {
      new CheckMandatoryDocuments().checkMandatoryDocuments();
    }
    super.virtualInit(context);
  }

  public int queryCount() {
    return util.queryCount();
  }
  
  /**
   * getSubMenuItemsForParent
   * get all submenu items of given parent document (by fullname).
   * 
   * @param parent
   * @param menuSpace (default: $doc.space)
   * @param menuPart 
   * @return (array of menuitems)
   */
  public List<com.xpn.xwiki.api.Object> getSubMenuItemsForParent(
      String parent, String menuSpace, String menuPart, XWikiContext context) {
    return util.getSubMenuItemsForParent(parent, menuSpace, menuPart, context);
  }

  public String getVersionMode(XWikiContext context) {
    String versionMode = context.getWiki().getSpacePreference("celements_version",
        context);
    if ("---".equals(versionMode)) {
      versionMode = context.getWiki().getXWikiPreference("celements_version",
          "celements2", context);
      if ("---".equals(versionMode)) {
        versionMode = "celements2";
      }
    }
    return versionMode;
  }

  /**
   * getUsernameForUserData
   * 
   * @param login
   * @param possibleLogins
   * @param context
   * @return
   * @throws XWikiException
   * 
   * @deprecated since 2.14.0 use UserNameForUserDataCommand instead
   */
  @Deprecated
  public String getUsernameForUserData(String login, String possibleLogins,
      XWikiContext context) throws XWikiException{
    return new UserNameForUserDataCommand().getUsernameForUserData(login, possibleLogins,
        context);
  }

  /**
   * 
   * @param userToken
   * @param context
   * @return
   * @throws XWikiException
   * 
   * @deprecated since 2.14.0 use TokenLDAPAuthServiceImpl instead
   */
  @Deprecated
  public String getUsernameForToken(String userToken, XWikiContext context
      ) throws XWikiException{
    
    String hashedCode = encryptString("hash:SHA-512:", userToken);
    String userDoc = "";
    
    if((userToken != null) && (userToken.trim().length() > 0)){
      
      String hql = ", BaseObject as obj, Classes.TokenClass as token where ";
      hql += "doc.space='XWiki' ";
      hql += "and obj.name=doc.fullName ";
      hql += "and token.tokenvalue=? ";
      hql += "and token.validuntil>=? ";
      hql += "and obj.id=token.id ";
      
      List<Object> parameterList = new Vector<Object>();
      parameterList.add(hashedCode);
      parameterList.add(new Date());
      
      XWikiStoreInterface storage = context.getWiki().getStore();
      List<String> users = storage.searchDocumentsNames(hql, 0, 0, parameterList, context);
      LOGGER.info("searching token and found " + users.size() + " with parameters " + 
          Arrays.deepToString(parameterList.toArray()));
      if(users == null || users.size() == 0) {
        String db = context.getDatabase();
        context.setDatabase("xwiki");
        users = storage.searchDocumentsNames(hql, 0, 0, parameterList, context);
        if(users != null && users.size() == 1) {
          users.add("xwiki:" + users.remove(0));
        }
        context.setDatabase(db);
      }
      int usersFound = 0;
      for (String tmpUserDoc : users) {
        if(!tmpUserDoc.trim().equals("")) {
          usersFound++;
          userDoc = tmpUserDoc;
        }
      }
      if(usersFound > 1){
        LOGGER.warn("Found more than one user for token '" + userToken + "'");
        return null;
      }
    } else {
      LOGGER.warn("No valid token given");
    }    
    return userDoc;
  }

  /**
   * @deprecated since 2.22.0
   * instead use NewCelementsTokenForUserCommand.getNewCelementsTokenForUserWithAutentication
   */
  @Deprecated
  public String getNewCelementsTokenForUser(String accountName,
      Boolean guestPlus, XWikiContext context) throws XWikiException {
    return new NewCelementsTokenForUserCommand(
        ).getNewCelementsTokenForUserWithAuthentication(accountName, guestPlus, context);
  }
  public String encryptString(String encoding, String str) {
    return new PasswordClass().getEquivalentPassword(encoding, str);
  }
  
  public Map<String, String> activateAccount(String activationCode,
      XWikiContext context) throws XWikiException{
    Map<String, String> userAccount = new HashMap<String, String>();
    String hashedCode = encryptString("hash:SHA-512:", activationCode);
    String username = new UserNameForUserDataCommand().getUsernameForUserData(hashedCode,
        "validkey", context);
    
    if((username != null) && !username.equals("")){
      String password = context.getWiki().generateRandomString(24);
      XWikiDocument doc = context.getWiki().getDocument(username, context);
      BaseObject obj = doc.getObject("XWiki.XWikiUsers");

//      obj.set("validkey", "", context);
      obj.set("active", "1", context);
      obj.set("force_pwd_change", "1", context);
      obj.set("password", password, context);
      
      context.getWiki().saveDocument(doc, context);
      
      userAccount.put("username", username);
      userAccount.put("password", password);
    }
    
    return userAccount;
  }

  public String getEmailAdressForUser(String username, XWikiContext context) {
    if (context.getWiki().exists(username, context)) {
      try {
        XWikiDocument doc = context.getWiki().getDocument(username, context);
        BaseObject obj = doc.getObject("XWiki.XWikiUsers");
        return obj.getStringValue("email");
      } catch (XWikiException e) {
        LOGGER.error(e);
      }
    }
    return null;
  }
  
  //TODO Delegation can be removed as soon as latin1 flag can be removed
  /**
   * @deprecated since 2.19.0 instead use CelSendMail class directly.
   */
  @Deprecated
  public int sendMail(
      String from, String replyTo, 
      String to, String cc, String bcc, 
      String subject, String htmlContent, String textContent, 
      List<Attachment> attachments, Map<String, String> others,
      XWikiContext context){
    return sendMail(from, replyTo, to, cc, bcc, subject, htmlContent, textContent,
        attachments, others, false, context);
  }
  
  /**
   * @deprecated since 2.19.0 instead use CelSendMail class directly.
   */
  @Deprecated
  public int sendMail(
        String from, String replyTo, 
        String to, String cc, String bcc, 
        String subject, String htmlContent, String textContent, 
        List<Attachment> attachments, Map<String, String> others, boolean isLatin1,
        XWikiContext context){
    CelSendMail sender = getCelSendMail(context);
    sender.setFrom(from);
    sender.setReplyTo(replyTo);
    sender.setTo(to);
    sender.setCc(cc);
    sender.setBcc(bcc);
    sender.setSubject(subject);
    sender.setHtmlContent(htmlContent, isLatin1);
    sender.setTextContent(textContent);
    sender.setAttachments(attachments);
    sender.setOthers(others);
    return sender.sendMail();
  }

  void injectCelSendMail(CelSendMail celSendMail) {
    this.injectedCelSendMail = celSendMail;
  }
  
  CelSendMail getCelSendMail(XWikiContext context) {
    if(injectedCelSendMail != null) {
      return injectedCelSendMail;
    }
    return new CelSendMail(context);
  }
    
  public List<Attachment> getAttachmentsForDocs(List<String> docsFN, XWikiContext context) {
    List<Attachment> attachments = new ArrayList<Attachment>();
    for(String docFN : docsFN) {
      try {
        LOGGER.info("getAttachmentsForDocs: processing doc " + docFN);
        for(XWikiAttachment xwikiAttachment : context.getWiki().getDocument(
            docFN, context).getAttachmentList()) {
          LOGGER.info("getAttachmentsForDocs: adding attachment " + 
              xwikiAttachment.getFilename() + " to list.");
          attachments.add(new Attachment(context.getWiki().getDocument(
              docFN, context).newDocument(context), xwikiAttachment, context));
        }
      } catch (XWikiException e) {
        LOGGER.error(e);
      }
    }
    return attachments;
  }

  /**
   * @deprecated since 2.11.7 instead use renderCelementsDocument
   *             on celementsweb scriptService
   */
  @Deprecated
  public String renderCelementsPageType(XWikiDocument doc, IPageType pageType,
      XWikiContext context) throws XWikiException{
    XWikiDocument viewTemplate = context.getWiki().getDocument(
        pageType.getRenderTemplate("view"), context);
    return context.getWiki().getRenderingEngine(
        ).renderDocument(viewTemplate, doc, context);
  }

  /**
   * @deprecated since 2.29.0 use SkinConfigObjCommand instead.
   */
  @Deprecated
  public BaseObject getSkinConfigObj(XWikiContext context) {
    return new SkinConfigObjCommand().getSkinConfigObj();
  }

  @Override
  public void beginRendering(XWikiContext context) {
    LOGGER.debug("start beginRendering: language [" + context.getLanguage() + "].");
    try {
      getPrepareVelocityContextService().prepareVelocityContext(context);
    } catch(RuntimeException exp) {
      LOGGER.error("beginRendering", exp);
      throw exp;
    }
    LOGGER.debug("end beginRendering: language [" + context.getLanguage() + "].");
  }

  @Override
  public void beginParsing(XWikiContext context) {
    LOGGER.debug("start beginParsing: language [" + context.getLanguage() + "].");
    try {
      getPrepareVelocityContextService().prepareVelocityContext(context);
    } catch(RuntimeException exp) {
      LOGGER.error("beginParsing", exp);
      throw exp;
    }
    LOGGER.debug("end beginParsing: language [" + context.getLanguage() + "].");
  }

  IPrepareVelocityContext getPrepareVelocityContextService() {
    return Utils.getComponent(IPrepareVelocityContext.class);
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getUniqueNameValueRequestMap(XWikiContext context) {
    Map<String, String[]> params = context.getRequest().getParameterMap();
    Map<String, String> resultMap = new HashMap<String, String>();
    for (String key : params.keySet()) {
      if((params.get(key) != null) && (params.get(key).length > 0)) {
        resultMap.put(key, params.get(key)[0]);
      } else {
        resultMap.put(key, "");
      }
    }
    return resultMap;
  }
  
  public int createUser(boolean validate, XWikiContext context) throws XWikiException{
    String possibleLogins = getPossibleLogins(context);
    return createUser(getUniqueNameValueRequestMap(context), possibleLogins, validate, context);
  }

  /**
   * @deprecated since 2.33.0 instead use PossibleLoginsCommand
   */
  @Deprecated
  public String getPossibleLogins(XWikiContext context) {
    return new PossibleLoginsCommand().getPossibleLogins();
  }
  
  @SuppressWarnings("deprecation")
  public synchronized int createUser(Map<String, String> userData, String possibleLogins,
      boolean validate, XWikiContext context) throws XWikiException {
    String accountName = "";
    if(userData.containsKey("xwikiname")) {
      accountName = userData.get("xwikiname");
      userData.remove("xwikiname");
    } else {
      while(accountName.equals("") || context.getWiki().exists("XWiki." + accountName, context)){
        accountName = context.getWiki().generateRandomString(12);
      }
    }
    String validkey = "";
    int success = -1;
    if(areIdentifiersUnique(userData, possibleLogins, context)) {
      if(!userData.containsKey("password")) {
        String password = context.getWiki().generateRandomString(8);
        userData.put("password", password);
      }
      if(!userData.containsKey("validkey")) {
        validkey = getUniqueValidationKey(context);
        userData.put("validkey", validkey);
      } else {
        validkey = userData.get("validkey");
      }
      if(!userData.containsKey("active")) {
        userData.put("active", "0");
      }
      String content = "#includeForm(\"XWiki.XWikiUserSheet\")";
      
      //TODO as soon as all installations are on xwiki 1.8+ change to new method (using
      //     XWikiDocument.XWIKI10_SYNTAXID as additional parameter
      success = context.getWiki().createUser(accountName, userData, "XWiki.XWikiUsers",
          content, "edit", context);
    }
    
    if(success == 1){
      // Set rights on user doc
      XWikiDocument doc = context.getWiki().getDocument("XWiki." + accountName, context);
      List<BaseObject> rightsObjs = doc.getObjects("XWiki.XWikiRights");
      for (BaseObject rightObj : rightsObjs) {
        if(rightObj.getStringValue("groups").equals("")){
          rightObj.set("users", doc.getFullName(), context);
          rightObj.set("allow", "1", context);
          rightObj.set("levels", "view,edit,delete", context);
          rightObj.set("groups", "", context);
        } else{
          rightObj.set("users", "", context);
          rightObj.set("allow", "1", context);
          rightObj.set("levels", "view,edit,delete", context);
          rightObj.set("groups", "XWiki.XWikiAdminGroup", context);
        }
      }
      context.getWiki().saveDocument(doc, context);
      
      if(validate) {
        LOGGER.info("send account validation mail with data: accountname='" + accountName
            + "', email='" + userData.get("email") + "', validkey='" + validkey + "'");
        try{
          new PasswordRecoveryAndEmailValidationCommand().sendValidationMessage(
              userData.get("email"), validkey, "Tools.AccountActivationMail", context);
        } catch(XWikiException e){
          LOGGER.error("Exception while sending validation mail to '" + 
              userData.get("email") + "'", e);
        }
      }
    }
    return success;
  }

  private boolean areIdentifiersUnique(Map<String, String> userData, 
      String possibleLogins, XWikiContext context) throws XWikiException {
    boolean isUnique = true;
    for (String key : userData.keySet()) {
      if(!"".equals(key.trim()) && (("," + possibleLogins + ",").indexOf("," + key + ",") >= 0)) {
        String user = getUsernameForUserData(userData.get(key), possibleLogins, context);
        if((user == null) || (user.length() > 0)) { //user == "" means there is no such user
          isUnique = false;
        }
      }
    }
    return isUnique;
  }

  /**
   * getUniqueValidationKey
   * 
   * @param context
   * @return
   * @throws XWikiException
   * 
   * @deprecated since 2.14.0 use NewCelementsTokenForUserCommand instead
   */
  @Deprecated
  public String getUniqueValidationKey(XWikiContext context)
      throws XWikiException {
    return new NewCelementsTokenForUserCommand().getUniqueValidationKey(context);
  }

  /**
   * 
   * @param attachToDoc
   * @param fieldName
   * @param userToken
   * @param context
   * @return
   * @throws XWikiException
   * 
   * @deprecated since 2.28.0 use TokenBasedUploadCommand instead
   */
  @Deprecated
  public int tokenBasedUpload(Document attachToDoc, String fieldName, String userToken,
      XWikiContext context) throws XWikiException {
    return new TokenBasedUploadCommand().tokenBasedUpload(attachToDoc, fieldName,
        userToken, context);
  }
  
  /**
   * 
   * @param attachToDocFN
   * @param fieldName
   * @param userToken
   * @param createIfNotExists
   * @param context
   * @return
   * @throws XWikiException
   * 
   * @deprecated since 2.28.0 use TokenBasedUploadCommand instead
   */
  @Deprecated
  public int tokenBasedUpload(String attachToDocFN, String fieldName, String userToken, 
      Boolean createIfNotExists, XWikiContext context) throws XWikiException {
    return new TokenBasedUploadCommand().tokenBasedUpload(attachToDocFN, fieldName,
        userToken, createIfNotExists, context);
  }

  /**
   * 
   * @param userToken
   * @param context
   * @return
   * @throws XWikiException
   * 
   * @deprecated since 2.14.0 use TokenLDAPAuthServiceImpl instead
   */
  @Deprecated
  public XWikiUser checkAuthByToken(String userToken, XWikiContext context
      ) throws XWikiException {
    String username = getUsernameForToken(userToken, context);
    if((username != null) && !username.equals("")){
      LOGGER.info("checkAuthByToken: user " + username + " identified by userToken.");
      context.setUser(username);
      return context.getXWikiUser();
    } else {
      LOGGER.warn("checkAuthByToken: username could not be identified by token");
    }
    return null;
  }

  public XWikiUser checkAuth(String logincredential, String password,
        String rememberme, String possibleLogins, Boolean noRedirect, XWikiContext context
      ) throws XWikiException {
    String loginname = getUsernameForUserData(logincredential, possibleLogins, context);
    if ("".equals(loginname) && possibleLogins.matches("(.*,)?loginname(,.*)?")) {
        loginname = logincredential;
    }
    if (noRedirect != null) {
      context.put("ajax", noRedirect);
    }
    return context.getWiki().getAuthService().checkAuth(loginname, password, rememberme,
        context);
  }

  /**
   * @deprecated since 2.2 use ITreeNodeService instead
   */
  @Deprecated
  public void enableMappedMenuItems(XWikiContext context) {
    GetMappedMenuItemsForParentCommand cmd = new GetMappedMenuItemsForParentCommand();
    cmd.set_isActive(true);
    context.put(GetMappedMenuItemsForParentCommand.CELEMENTS_MAPPED_MENU_ITEMS_KEY, cmd);
  }

  public boolean executeAction(Document actionDoc, Map<String, String[]> request, 
      XWikiDocument includingDoc, XWikiContext context) {
    LOGGER.info("Executing action on doc '" + actionDoc.getFullName() + "'");
    VelocityContext vcontext = ((VelocityContext) context.get("vcontext"));
    vcontext.put("theDoc", actionDoc);
    Boolean debug = (Boolean)vcontext.get("debug");
    vcontext.put("debug", true);
    Boolean hasedit = (Boolean)vcontext.get("hasedit");
    vcontext.put("hasedit", true);
    Object req = vcontext.get("request");
    vcontext.put("request", getApiUsableMap(request));
    XWikiDocument execAct = null;
    try {
      execAct = context.getWiki()
          .getDocument("celements2web:Macros.executeActions", context);
    } catch (XWikiException e) {
      LOGGER.error("Could not get action Macro", e);
    }
    String actionContent = "";
    if(execAct != null) {
      String execContent = execAct.getContent();
      execContent = execContent.replaceAll("\\{(/?)pre\\}", "");
      actionContent = context.getWiki().getRenderingEngine().interpretText(
          execContent, includingDoc, context);
    }
    Object successfulObj = vcontext.get("successful");
    boolean successful = (successfulObj != null)
                          && "true".equals(successfulObj.toString());
    if(!successful) {
      LOGGER.error("Error executing action. Output:" + vcontext.get("actionScriptOutput"));
      LOGGER.error("Rendered Action Script: " + actionContent);
    }
    vcontext.put("debug", debug);
    vcontext.put("hasedit", hasedit);
    vcontext.put("request", req);
    return successful;
  }

  //FIXME Hack to get mail execution to work. The script is not expecting arrays in the
  //      map, since it expects a request. Multiple values with the same name get lost 
  //      in this "quick and dirty" fix
  private Object getApiUsableMap(Map<String, String[]> request) {
    Map<String, String> apiConform = new HashMap<String, String>();
    for (String key : request.keySet()) {
      if((request.get(key) != null) && (request.get(key).length > 0)) {
        apiConform.put(key, request.get(key)[0]);
      } else {
        apiConform.put(key, null);
      }
    }
    return apiConform;
  }

  public List<String> getSupportedAdminLanguages() {
    if (supportedAdminLangList == null) {
      setSupportedAdminLanguages(Arrays.asList(new String[] {"de","fr","en","it"}));
    }
    return supportedAdminLangList;
  }

  public void setSupportedAdminLanguages(List<String> newSupportedAdminLangList) {
    supportedAdminLangList = newSupportedAdminLangList;
  }

  public boolean writeUTF8Response(String filename, String renderDocFullName, 
      XWikiContext context) {
    boolean success = false;
    if(context.getWiki().exists(renderDocFullName, context)) {
      XWikiDocument renderDoc;
      try {
        renderDoc = context.getWiki().getDocument(renderDocFullName, context);
        adjustResponseHeader(filename, context.getResponse(), context);
        setResponseContent(renderDoc, context.getResponse(), context);
      } catch (XWikiException e) {
        LOGGER.error(e);
      }
      context.setFinished(true);
    }
    return success;
  }
  
  void adjustResponseHeader(String filename, XWikiResponse response, 
      XWikiContext context) {
    response.setContentType("text/plain");
    String ofilename = Util.encodeURI(filename, context).replaceAll("\\+", " ");
    response.addHeader("Content-disposition", "attachment; filename=\"" + ofilename + 
        "\"; charset='UTF-8'");
  }

  void setResponseContent(XWikiDocument renderDoc, XWikiResponse response,
      XWikiContext context) throws XWikiException {
    String renderedContent = new RenderCommand().renderDocument(renderDoc);
    byte[] data = {};
    try {
      data = renderedContent.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e1) {
      e1.printStackTrace();
    }
    response.setContentLength(data.length + 3);
    try {
      response.getOutputStream().write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
      response.getOutputStream().write(data);
    } catch (IOException e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
          XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
          "Exception while sending response", e);
    }
  }

  public boolean isFormFilled(Map<String, String[]> parameterMap, 
      Set<String> additionalFields) {
    boolean isFilled = false;
    if(parameterMap.size() > getIsFilledModifier(parameterMap, additionalFields)) {
      isFilled = true;
    }
    return isFilled;
  }
  
  short getIsFilledModifier(Map<String, String[]> parameterMap, 
      Set<String> additionalFields) {
    List<String> standardParams = new ArrayList<String>();
    standardParams.add(PARAM_XPAGE);
    standardParams.add(PARAM_CONF);
    standardParams.add(PARAM_AJAX_MODE);
    standardParams.add(PARAM_SKIN);
    standardParams.add(PARAM_LANGUAGE);
    standardParams.add(PARAM_XREDIRECT);
    short modifier = 0;
    if(parameterMap.containsKey(PARAM_XPAGE) && parameterMap.containsKey(PARAM_CONF) && 
        arrayContains(parameterMap.get(PARAM_XPAGE), "overlay")) {
      modifier += 1;
    }
    if(parameterMap.containsKey(PARAM_XPAGE) && parameterMap.containsKey(PARAM_AJAX_MODE) && 
        arrayContains(parameterMap.get(PARAM_XPAGE), "celements_ajax")) {
      modifier += 1;
      if(parameterMap.containsKey(PARAM_SKIN)) {
        modifier += 1;
      }
    }
    if(parameterMap.containsKey(PARAM_XPAGE)) {
      modifier += 1;
    }
    if(parameterMap.containsKey(PARAM_XREDIRECT)) {
      modifier += 1;
    }
    if(parameterMap.containsKey(PARAM_LANGUAGE)) {
      modifier += 1;
    }
    if((additionalFields != null) && additionalFields.size() > 0) {
      for (String param : additionalFields) {
        if(!standardParams.contains(param) && parameterMap.containsKey(param)) {
          modifier += 1;
        }
      }
    }
    return modifier;
  }
  
  boolean arrayContains(String[] array, String value) {
    Arrays.sort(array);
    return (Arrays.binarySearch(array, value) >= 0);
  }

  /**
   * @deprecated since 2.14.0 use IWebUtilsService instead
   */
  @Deprecated
  public String getDefaultLanguage(XWikiContext context) {
    return getWebService().getDefaultLanguage();
  }

  private IWebUtilsService getWebService() {
    return Utils.getComponent(IWebUtilsService.class);
  }

  /**
   * addTranslation
   * @param fullName
   * @param language
   * @param context
   * @return
   * 
   * @deprecated since 2.14.0 please use the AddTranslationCommand directly
   */
  @Deprecated
  public boolean addTranslation(String fullName, String language, XWikiContext context) {
    return new AddTranslationCommand().addTranslation(fullName, language, context);
  }

}
