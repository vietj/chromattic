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

package org.chromattic.core;

import org.chromattic.api.ChromatticIOException;
import org.chromattic.api.Status;
import org.chromattic.common.CloneableInputStream;
import org.chromattic.common.jcr.Path;
import org.chromattic.core.jcr.type.NodeTypeInfo;
import org.chromattic.core.mapper.ObjectMapper;
import org.chromattic.core.mapper.PropertyMapper;
import org.chromattic.core.mapper.property.JCRPropertyMapper;
import org.chromattic.core.vt2.ValueDefinition;
import org.chromattic.spi.instrument.MethodHandler;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public abstract class ObjectContext<O extends ObjectContext<O>> implements MethodHandler {

  private final AtomicBoolean initialized = new AtomicBoolean();
   
  public abstract ObjectMapper<O> getMapper();

  public abstract Object getObject();

  public abstract EntityContext getEntity();

  /**
   * Returns the type info associated with the context. Null is returned when the context is in transient
   * state, otherwise the type info of the corresponding node is returned.
   *
   * @return the type info
   */
  public abstract NodeTypeInfo getTypeInfo();

  public abstract Status getStatus();

  public abstract DomainSession getSession();

  public final Object invoke(Object o, Method method) throws Throwable {
    MethodInvoker<O> invoker = getMapper().getInvoker(method);
    if (invoker != null) {
      return invoker.invoke((O)this);
    } else {
      throw createCannotInvokeError(method);
    }
  }

  public final Object invoke(Object o, Method method, Object arg) throws Throwable {
    MethodInvoker<O> invoker = getMapper().getInvoker(method);
    if (invoker != null) {
      return invoker.invoke((O)this, arg);
    } else {
      throw createCannotInvokeError(method, arg);
    }
  }

  public final Object invoke(Object o, Method method, Object[] args) throws Throwable {
    MethodInvoker<O> invoker = getMapper().getInvoker(method);
    if (invoker != null) {
      switch (args.length) {
        case 0:
          return invoker.invoke((O)this);
        case 1:
          return invoker.invoke((O)this, args[0]);
        default:
          return invoker.invoke((O)this, args);
      }
    } else {
      throw createCannotInvokeError(method, (Object[])args);
    }
  }

  private AssertionError createCannotInvokeError(Method method, Object... args) {
    StringBuilder msg = new StringBuilder("Cannot invoke method ").append(method.getName()).append("(");
    Class[] parameterTypes = method.getParameterTypes();
    for (int i = 0;i < parameterTypes.length;i++) {
      if (i > 0) {
        msg.append(',');
      }
      msg.append(parameterTypes[i].getName());
    }
    msg.append(") with arguments (");
    for (int i = 0;i < args.length;i++) {
      if (i > 0) {
        msg.append(',');
      }
      msg.append(String.valueOf(args[i]));
    }
    msg.append(")");
    return new AssertionError(msg);
  }

  /**
   * Checks if the entity has been initialized if not it will load all the properties at the same time
   */
  @SuppressWarnings("rawtypes")
  private void checkInitialized() throws RepositoryException {
    if (!initialized.get()) {
      try {
        if (getStatus() == Status.PERSISTENT) {
          EntityContext ctx = getEntity();
          EntityContextState state = ctx.state;
          Domain domain = state.getSession().domain;
          if (!domain.isHasPropertyOptimized()) {
            return;
          }
          ObjectMapper<O> mapper = getMapper();
          Map<String, ValueDefinition<?, ?>> properties = new HashMap<String, ValueDefinition<?, ?>>();
          for (PropertyMapper<?, ?, O, ?> pm : mapper.getPropertyMappers()) {
            if (pm instanceof JCRPropertyMapper) {
              JCRPropertyMapper jpm = ((JCRPropertyMapper)pm);
              String propertyName = jpm.getJCRPropertyName(); 
              propertyName = domain.encodeName(ctx, propertyName, NameKind.PROPERTY);
              properties.put(propertyName, jpm.getValueDefinition());
            }
          }
          if (properties.size() > 1) {
            // We load the properties if and only if we have at least 2 properties to load
            NodeTypeInfo typeInfo = getTypeInfo();
            state.loadProperties(typeInfo, properties);
          }
        }
      } finally {
         initialized.set(true);
      }
    }
  }
  
  public final <V> boolean hasProperty(String propertyName, ValueDefinition<?, V> type) throws RepositoryException {
    checkInitialized();
    EntityContext ctx = getEntity();
    EntityContextState state = ctx.state;

    //
    propertyName = state.getSession().domain.encodeName(ctx, propertyName, NameKind.PROPERTY);
    Path.validateName(propertyName);

    //
    NodeTypeInfo typeInfo = getTypeInfo();
    return state.hasProperty(typeInfo, propertyName, type);
  }

  public final <V> V getPropertyValue(String propertyName, ValueDefinition<?, V> type) throws RepositoryException {
    checkInitialized();
    EntityContext ctx = getEntity();
    EntityContextState state = ctx.state;

    //
    propertyName = state.getSession().domain.encodeName(ctx, propertyName, NameKind.PROPERTY);
    Path.validateName(propertyName);

    //
    NodeTypeInfo typeInfo = getTypeInfo();
    return state.getPropertyValue(typeInfo, propertyName, type);
  }

  public final <L, V> L getPropertyValues(String propertyName, ValueDefinition<?, V> simpleType, ArrayType<L, V> arrayType) throws RepositoryException {
    checkInitialized();
    EntityContext ctx = getEntity();
    EntityContextState state = ctx.state;

    //
    propertyName = state.getSession().domain.encodeName(ctx, propertyName, NameKind.PROPERTY);
    Path.validateName(propertyName);

    //
    NodeTypeInfo typeInfo = getTypeInfo();
    return state.getPropertyValues(typeInfo, propertyName, simpleType, arrayType);
  }

  public final <V> void setPropertyValue(String propertyName, ValueDefinition<?, V> type, V o) throws RepositoryException {
    EntityContext ctx = getEntity();
    EntityContextState state = ctx.state;

    //
    propertyName = state.getSession().domain.encodeName(ctx, propertyName, NameKind.PROPERTY);
    Path.validateName(propertyName);

    //
    Object object = getObject();

    //
    EventBroadcaster broadcaster = state.getSession().broadcaster;

    //
    NodeTypeInfo typeInfo = getTypeInfo();

    //
    if (o instanceof InputStream && broadcaster.hasStateChangeListeners()) {
      CloneableInputStream in;
      try {
        in = new CloneableInputStream((InputStream)o);
      }
      catch (IOException e) {
        throw new ChromatticIOException("Could not read stream", e);
      }
      @SuppressWarnings("unchecked") V v = (V)in;
      state.setPropertyValue(typeInfo, propertyName, type, v);
      broadcaster.propertyChanged(state.getId(), object, propertyName, in.clone());
    } else {
      state.setPropertyValue(typeInfo, propertyName, type, o);
      broadcaster.propertyChanged(state.getId(), object, propertyName, o);
    }
  }

  public final <L, V> void setPropertyValues(String propertyName, ValueDefinition<?, V> type, ArrayType<L, V> arrayType, L propertyValues) throws RepositoryException {
    EntityContext ctx = getEntity();
    EntityContextState state = ctx.state;

    //
    propertyName = state.getSession().domain.encodeName(ctx, propertyName, NameKind.PROPERTY);
    Path.validateName(propertyName);

    //
    NodeTypeInfo typeInfo = getTypeInfo();

    //
    state.setPropertyValues(typeInfo, propertyName, type, arrayType, propertyValues);
  }

  public final void removeChild(String prefix, String localName) {
    if (getStatus() != Status.PERSISTENT) {
      throw new IllegalStateException("Can only insert/remove a child of a persistent object");
    }

    //
    getSession().removeChild(this, prefix, localName);
  }

  public final void orderBefore(EntityContext srcCtx, EntityContext dstCtx) {
    getSession().orderBefore(this, srcCtx, dstCtx);
  }

  public final <T1 extends Throwable, T2 extends Throwable> void addChild(
      ThrowableFactory<T1> thisStateTF,
      ThrowableFactory<T2> childStateTF,
      String prefix,
      EntityContext childCtx) throws T1, T2 {
    String localName = childCtx.getLocalName();
    addChild(thisStateTF, childStateTF, prefix, localName, childCtx);
  }

  public final <T1 extends Throwable, T2 extends Throwable> void addChild(
      ThrowableFactory<T1> thisStateTF,
      ThrowableFactory<T2> childStateTF,
      String prefix,
      String localName,
      EntityContext childCtx) throws T1, T2 {
    if (childCtx.getStatus() == Status.PERSISTENT) {
      getSession().move(childStateTF, thisStateTF, childCtx, this, prefix, localName);
    } else {
      getSession().persist(thisStateTF, childStateTF, ThrowableFactory.NPE, this, childCtx, prefix, localName);
    }
  }

  public final EntityContext getChild(String prefix, String localName) {
    return getSession().getChild(this, prefix, localName);
  }

  public final boolean hasChild(String prefix, String localName) {
    return getSession().hasChild(this, prefix, localName);
  }

  public final <T> Iterator<T> getChildren(Class<T> filterClass) {
    return getSession().getChildren(this, filterClass);
  }
  
  public final boolean hasChildren() {
     return getSession().hasChildren(this);
   }
}
