/*
 * Copyright (C) 2010 eXo Platform SAS.
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

package org.chromattic.dataobject.runtime;

import org.chromattic.spi.jcr.SessionLifeCycle;
import org.exoplatform.services.jcr.core.ManageableRepository;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class DataObjectSessionLifeCycle implements SessionLifeCycle {

  public Session login() throws RepositoryException {
    ChromatticSessionProvider provider = ChromatticSessionProvider.getCurrent();
    String workspaceName = provider.workspaceName;
    ManageableRepository current = provider.repositoryService.getCurrentRepository();
    if (workspaceName != null) {
      return current.login(workspaceName);
    } else {
      return current.login();
    }
  }

  public Session login(String workspace) throws RepositoryException {
    return login();
  }

  public Session login(Credentials credentials, String workspace) throws RepositoryException {
    return login();
  }

  public Session login(Credentials credentials) throws RepositoryException {
    return login();
  }

  public void save(Session session) throws RepositoryException {
    session.save();
  }

  public void close(Session session) {
    session.logout();
  }
}
