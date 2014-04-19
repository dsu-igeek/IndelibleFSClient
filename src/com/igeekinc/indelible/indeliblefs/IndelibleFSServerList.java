/*
 * Copyright 2002-2014 iGeek, Inc.
 * All Rights Reserved
 * @Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.@
 */
 
package com.igeekinc.indelible.indeliblefs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.igeekinc.indelible.indeliblefs.proxies.IndelibleFSServerProxy;
import com.igeekinc.indelible.indeliblefs.server.IndelibleFSServerRemote;
import com.igeekinc.util.ChangeModel;
import com.igeekinc.util.CheckCorrectDispatchThread;

public class IndelibleFSServerList extends ChangeModel implements List<IndelibleFSServerProxy>
{
    private ArrayList<IndelibleFSServerProxy>serverList;
    public IndelibleFSServerList(CheckCorrectDispatchThread checker)
    {
        super(checker);
        serverList = new ArrayList<IndelibleFSServerProxy>();
    }

    public boolean add(IndelibleFSServerProxy o)
    {
        boolean added = false;
        synchronized(serverList)
        {
            added = serverList.add(o);
        }
        if (added)
        {
            fireIndelibleFSServerAddedEvent(new IndelibleFSServerAddedEvent(this, o));
        }
        return added;
    }

    public synchronized void add(int index, IndelibleFSServerProxy element)
    {
        synchronized(serverList)
        {
            serverList.add(index, element);
        }

        fireIndelibleFSServerAddedEvent(new IndelibleFSServerAddedEvent(this, element));
    }

    public synchronized boolean addAll(Collection<? extends IndelibleFSServerProxy> addCollection)
    {
        boolean added = false;
        synchronized(serverList)
        {
            serverList.addAll(addCollection);
        }
        for (IndelibleFSServerProxy curServer:addCollection)
            fireIndelibleFSServerAddedEvent(new IndelibleFSServerAddedEvent(this, curServer));
        return added;    // Not too many ways for this to go bad
    }

    public synchronized boolean addAll(int index, Collection<? extends IndelibleFSServerProxy> addCollection)
    {
        boolean added = false;
        synchronized(serverList)
        {
            serverList.addAll(index, addCollection);
        }
        for (IndelibleFSServerProxy curServer:addCollection)
            fireIndelibleFSServerAddedEvent(new IndelibleFSServerAddedEvent(this, curServer));
        return added;    // Not too many ways for this to go bad
    }

    public synchronized void clear()
    {
    	IndelibleFSServerProxy [] removedServers;
        synchronized(serverList)
        {
            removedServers = new IndelibleFSServerProxy[serverList.size()];
            removedServers = serverList.toArray(removedServers);
            serverList.clear();
        }
        for (IndelibleFSServerProxy curServer:removedServers)
            fireIndelibleFSServerRemovedEvent(new IndelibleFSServerRemovedEvent(this, curServer));
    }

    public boolean contains(Object o)
    {
        synchronized(serverList)
        {
            return serverList.contains(o);
        }
    }

    public boolean containsAll(Collection<?> c)
    {
        synchronized(serverList)
        {
            return serverList.containsAll(c);
        }
    }

    public IndelibleFSServerProxy get(int index)
    {
        synchronized(serverList)
        {
            return serverList.get(index);
        }
    }

    public int indexOf(Object o)
    {
        synchronized(serverList)
        {
            return serverList.indexOf(o);
        }
    }

    public synchronized boolean isEmpty()
    {
        synchronized(serverList)
        {
            return serverList.isEmpty();
        }
    }

    public Iterator<IndelibleFSServerProxy> iterator()
    {
        return serverList.iterator();
    }

    public int lastIndexOf(Object o)
    {
        synchronized(serverList)
        {
            return serverList.lastIndexOf(o);
        }
    }

    public ListIterator<IndelibleFSServerProxy> listIterator()
    {
        return serverList.listIterator();
    }

    public ListIterator<IndelibleFSServerProxy> listIterator(int index)
    {
        return serverList.listIterator(index);
    }

    public boolean remove(Object o)
    {
        if( serverList.remove(o))
        {
            fireIndelibleFSServerRemovedEvent(new IndelibleFSServerRemovedEvent(this, (IndelibleFSServerProxy)o));
            return true;
        }
        else
            return false;
    }

    public IndelibleFSServerProxy remove(int index)
    {
    	IndelibleFSServerProxy removedServer = serverList.remove(index);
        fireIndelibleFSServerRemovedEvent(new IndelibleFSServerRemovedEvent(this, removedServer));
        return removedServer;
    }

    public boolean removeAll(Collection<?> c)
    {
        return serverList.removeAll(c);
    }

    public boolean retainAll(Collection<?> c)
    {
        return serverList.retainAll(c);
    }

    public IndelibleFSServerProxy set(int index, IndelibleFSServerProxy element)
    {
        return serverList.set(index, element);
    }

    public int size()
    {
        return serverList.size();
    }

    public List<IndelibleFSServerProxy> subList(int fromIndex, int toIndex)
    {
        IndelibleFSServerList returnList = new IndelibleFSServerList(this.dispatcher);
        returnList.addAll(serverList.subList(fromIndex, toIndex));
        return returnList;
    }

    public Object[] toArray()
    {
        return serverList.toArray();
    }

    public <T> T[] toArray(T[] a)
    {
        return serverList.toArray(a);
    }
    
    public void addIndelibleFSServerListListener(IndelibleFSServerListListener listener)
    {
        addEventListener(listener);
    }
    
    public void removeIndelibleFSServerListListener(IndelibleFSServerListListener listener)
    {
        removeEventListener(listener);
    }
    
    public void fireIndelibleFSServerAddedEvent(IndelibleFSServerAddedEvent fireEvent)
    {
        try
        {
            fireEventOnCorrectThread(fireEvent, IndelibleFSServerListListener.class, IndelibleFSServerListListener.class.getMethod("indelibleFSServerAdded", new Class[]{IndelibleFSServerAddedEvent.class}));
        } catch (SecurityException e)
        {
            throw new InternalError("Got security exception retrieving actionPerformed method from ActionListener class");
        } catch (NoSuchMethodException e)
        {
            throw new InternalError("ActionListener class missing actionPerformed method");
        }
    }
    
    public void fireIndelibleFSServerRemovedEvent(IndelibleFSServerRemovedEvent fireEvent)
    {
        try
        {
            fireEventOnCorrectThread(fireEvent, IndelibleFSServerListListener.class, IndelibleFSServerListListener.class.getMethod("indelibleFSServerRemove", new Class[]{IndelibleFSServerRemovedEvent.class}));
        } catch (SecurityException e)
        {
            throw new InternalError("Got security exception retrieving actionPerformed method from ActionListener class");
        } catch (NoSuchMethodException e)
        {
            throw new InternalError("ActionListener class missing actionPerformed method");
        }
    }
}
