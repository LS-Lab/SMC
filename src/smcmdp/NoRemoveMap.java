/***********************************************************************************************
 * Copyright (C) 2011, 2012  D. Henriques; J. G. Martins; P. Zuliani; A. Platzer; E. M. Clarke.  All rights reserved.
 * By using this software the USER indicates that he or she has read, understood and will comply
 * with the following:
 *
 * 1. The USER is hereby granted non-exclusive permission to use, copy and/or
 * modify this software for internal, non-commercial, research purposes only. Any
 * distribution, including commercial sale or license, of this software, copies of
 * the software, its associated documentation and/or modifications of either is
 * strictly prohibited without the prior consent of the authors. Title to copyright
 * to this software and its associated documentation shall at all times remain with
 * the authors. Appropriated copyright notice shall be placed on all software
 * copies, and a complete copy of this notice shall be included in all copies of
 * the associated documentation. No right is granted to use in advertising,
 * publicity or otherwise any trademark, service mark, or the name of the authors.
 *
 * 2. This software and any associated documentation is provided "as is".
 *
 * THE AUTHORS MAKE NO REPRESENTATIONS OR WARRANTIES, EXPRESSED OR IMPLIED,
 * INCLUDING THOSE OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE, OR THAT
 * USE OF THE SOFTWARE, MODIFICATIONS, OR ASSOCIATED DOCUMENTATION WILL NOT
 * INFRINGE ANY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER INTELLECTUAL PROPERTY
 * RIGHTS OF A THIRD PARTY.
 *
 * The authors shall not be liable under any circumstances for any direct,
 * indirect, special, incidental, or consequential damages with respect to any
 * claim by USER or any third party on account of or arising from the use, or
 * inability to use, this software or its associated documentation, even if the
 * authors have been advised of the possibility of those damages.
 * ***********************************************************************************************/

package smcmdp;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

public class NoRemoveMap<K, V> implements Iterable<Map.Entry<K, V>>{
  protected int counter;
  protected ConcurrentHashMap<K, Integer> map;
  protected ArrayList<V> values;
  
  public NoRemoveMap(int size){
    counter = -1;
    map = new ConcurrentHashMap<K, Integer>(size);
    values = new ArrayList<V>(size);
  }
  
  public synchronized boolean initialise(K key, V value){
    if(map.containsKey(key))
      return false;
    
    int index = ++counter;
    map.put(key, index);
    values.add(value);
    assert(index == values.size() - 1);
    return true;
  }
  
  public V get(K key) {
    Integer index = map.get(key); 
    if (index == null) return null;
    return get(index);
  }
  
  public V get(int index) { return values.get(index); }
  
  public boolean containsKey(K key) { return map.containsKey(key); }
  
  public int size() { return map.size(); }
  
  public void clear() { counter = -1; map.clear(); values.clear(); }

  public Iterator<Entry<K, V>> iterator() {
    return new NoRemoveMapIterator<K, V>(this);
  }
}

class NoRemoveMapIterator<K, V> implements Iterator<Map.Entry<K, V>>{
  protected NoRemoveMap<K, V> map;
  protected Iterator<Map.Entry<K, Integer>> i;
  
  public NoRemoveMapIterator(NoRemoveMap<K, V> map){
    this.map = map;
    this.i = map.map.entrySet().iterator();
  }

  public boolean hasNext() { return i.hasNext(); }

  @Override
  public Map.Entry<K, V> next() {
    Map.Entry<K, Integer> next = i.next();
    return new AbstractMap.SimpleEntry<K, V>(next.getKey(), map.get(next.getValue()));
  }

  // Not implemented
  public void remove() { assert(false); } 
}


