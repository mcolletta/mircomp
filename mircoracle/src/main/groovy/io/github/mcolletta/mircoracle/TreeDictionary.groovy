/*
 * Copyright (C) 2016-2024 Mirco Colletta
 *
 * This file is part of MirComp.
 *
 * MirComp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MirComp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MirComp.  If not, see <http://www.gnu.org/licenses/>.
*/

/**
 * @author Mirco Colletta
 */

package io.github.mcolletta.mircoracle

import com.xenoage.utils.pdlib.PList


class TreeDictionary<K, V> implements Iterable<Map.Entry<K, V>> {
    
    TreeDictionaryNode<K,V> root
    Comparator comparator
    K defaultValue
    
    
    TreeDictionary(Comparator cmp=null) {
        root = new TreeDictionaryNode<K,V>()
        comparator = cmp
    }

    TreeDictionary(TreeDictionaryNode<K,V> tree, Comparator cmp=null) {
        root = tree
        comparator = cmp
    }
    
    boolean containsKey(List<K> key) {
        TreeDictionaryNode<K,V> pointer = root
        boolean retVal = false
        for(K item: key) {
            TreeDictionaryNode<K,V> node = pointer.getChild(item, comparator)
            if (node != null) {
                pointer = node
                retVal = true
            }
            else {
                retVal = false
            }
        }
        return retVal
    }

    V getAt(List<K> key) {
        V val = null
        TreeDictionaryNode<K,V> pointer = root
        for(K item: key) {
            TreeDictionaryNode<K,V> node = pointer.getChild(item, comparator)
            if (node == null) {
                if (defaultValue != null)
                    return defaultValue
                else
                    throw new Exception("The key $key does not belong to the dictionary")
            }
            pointer = node
        }
        return pointer.value
    }
    
    void putAt(List<K> key, V value) {
        //println "INSERTING ${key[0]} of type ${key[0].getClass().getName()}"
        TreeDictionaryNode pointer = root
        for(K item: key) {
            TreeDictionaryNode<K,V> node = pointer.getChild(item, comparator)
            if (node == null) {
                node = new TreeDictionaryNode<K,V>(pointer, item)
                pointer.children = pointer.children.plus(node)
            }
            pointer = node
        }
        pointer.value = value
    }

    public Iterator iterator() {
        return new TreeDictionaryIterator<>()
    }
    
    private class TreeDictionaryIterator<K, V> implements Iterator {
   
        List<TreeDictionaryNode<K,V>> queue
        private List v = []
        
        TreeDictionaryIterator() {
            queue = [root]
            
        }
        
        public boolean hasNext() {
            return queue.size() > 0
        }
        
        public Map.Entry<List<K>, V> next() {
          Map.Entry<List<K>, V> kvp
          boolean isRoot = true
          while (queue.size() > 0) {
                TreeDictionaryNode<K,V> node = queue.remove(0)
                //println "Node ${node.path} value ${node.value}"
                if (node.children.size() > 0) {
                    queue = node.children.plus(queue)
                }
                List path = node.getPath()
                if (path.size() > 1) {
                    kvp = Map.entry(path[1..-1], node.value)
                    break
                }
          }
          return kvp
        }
        
        public void remove(){}
    }
    
    void each(Closure clos) {
        // for (Map.Entry<List<K>, V> item: this) {
        for(Iterator<Map.Entry<List<K>, V>> iter = this.iterator(); iter.hasNext(); ) {
            var item = iter.next()
            clos(item.key, item.value)
        }
    }
    
    void withDefault(K obj) {
        defaultValue = obj
    }
    
    String toString() {
        // for (Map.Entry<List<K>, V> item: this) {
        for(Iterator<Map.Entry<List<K>, V>> iter = this.iterator(); iter.hasNext(); ) {
            var item = iter.next()
            println "${item.key} = ${item.value}"
        }
    }
    
}


class TreeDictionaryNode<K,V> {
    TreeDictionaryNode parent
    PList<TreeDictionaryNode> children = []
    K content
    V value
    PList<Continuation> continuations = []
    
    TreeDictionaryNode() { }

    TreeDictionaryNode(TreeDictionaryNode<K,V> parent) {
        this.parent = parent
    }

    TreeDictionaryNode(TreeDictionaryNode<K,V> parent, K content) {
        this.parent = parent
        this.content = content
    }

    TreeDictionaryNode(TreeDictionaryNode<K,V> parent, K content, V value) {
        this.parent = parent
        this.content = content
        this.value = value
    }
    
    List<K> getPath() {
        List<K> path = [content]
        TreeDictionaryNode<K,V> pointer = parent
        while (pointer != null) {
            path.add(0,pointer.content)
            pointer = pointer.parent
        }
        return path
    }
    
    TreeDictionaryNode<K,V> getChild(K item, Comparator comparator=null) {
        TreeDictionaryNode<K,V> retVal = null
        boolean found = false
        for(TreeDictionaryNode<K,V> child: children) {
            if (comparator)
                found = (comparator.compare(child.content,item) == 0)
            else
                found = (child.content == item)
            if (found)
                retVal = child
            if (found)
                break
        }
        return retVal
    }
    
    String toString() {
        //return "TreeDictionaryNode<$content, $value>"
        return printTree()
    }

    String printTree(TreeDictionaryNode node=this, String indent='') {
        String str = ""
        if (node.children.size() > 0) {
            str += "$indent${node.content}^${node.continuations}" + "\n"
            node.children.each { TreeDictionaryNode child ->
                str += printTree(child, indent + '----') 
            }
        }
        else
            str += "$indent${node.content}^${node.continuations}" + "\n"
        return str
    }
}
