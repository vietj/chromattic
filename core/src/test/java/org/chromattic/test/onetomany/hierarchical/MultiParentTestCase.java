/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.chromattic.test.onetomany.hierarchical;

import org.chromattic.core.DomainSession;
import org.chromattic.test.AbstractTestCase;

import javax.jcr.Node;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class MultiParentTestCase extends AbstractTestCase {

  protected void createDomain() {
    addClass(PARENTS_A.class);
    addClass(PARENTS_B.class);
  }

  public void testLoad() throws Exception {
    DomainSession session = login();
    Node rootNode = session.getRoot();
    Node aNode = rootNode.addNode("parents_a", "parents_a");
    String aId = aNode.getUUID();
    Node bNode = aNode.addNode("b", "parents_b");
    String bId = bNode.getUUID();
    Node cNode = bNode.addNode("c", "parents_b");
    String cId = cNode.getUUID();
    rootNode.save();

    //
    session = login();
    PARENTS_A a = session.findById(PARENTS_A.class, aId);
    PARENTS_B b = session.findById(PARENTS_B.class, bId);
    PARENTS_B c = session.findById(PARENTS_B.class, cId);
    assertSame(b, c.getBParent());
    assertNull(c.getAParent());
    assertSame(a, b.getAParent());
    assertNull(b.getBParent());
  }
}
