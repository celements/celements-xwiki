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
package com.celements.web.service;

import java.util.List;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.web.XWikiMessageTool;

@ComponentRole
public interface IWebUtilsService {
  
  /**
   * Returns level of hierarchy with level=1 returning root which is null, else
   * corresponding DocumentReference or throws IndexOutOfBoundsException
   * @param level
   * @return DocumentReference of level
   * @throws IndexOutOfBoundsException - if level above root or below lowest
   */
  public DocumentReference getParentForLevel(int level) throws IndexOutOfBoundsException;
  
  public List<DocumentReference> getDocumentParentsList(DocumentReference docRef,
      boolean includeDoc);
  
  public String getDocSectionAsJSON(String regex, DocumentReference docRef, int section 
      ) throws XWikiException;
  
  public String getDocSection(String regex, DocumentReference docRef, int section 
      ) throws XWikiException;

  public int countSections(String regex, DocumentReference docRef) throws XWikiException;

  public XWikiMessageTool getMessageTool(String adminLanguage);

  public XWikiMessageTool getAdminMessageTool();

  public String getAdminLanguage();

  public String getAdminLanguage(String userFullName);

  public String getDefaultLanguage();

  public DocumentReference resolveDocumentReference(String fullName);

  public boolean isAdminUser();

  public boolean isAdvancedAdmin();
  
  public List<Attachment> getAttachmentListSorted(Document doc, String comparator
      ) throws ClassNotFoundException;
  
  public List<Attachment> getAttachmentListSorted(Document doc,
      String comparator, boolean imagesOnly);

  public String getAttachmentListSortedAsJSON(Document doc, String comparator,
      boolean imagesOnly);
}
