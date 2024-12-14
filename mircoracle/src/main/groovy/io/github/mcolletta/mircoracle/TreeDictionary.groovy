/*
 * Copyright (C) 2016-2023 Mirco Colletta
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


class TreeDictionary<K extends List, V> implements Iterable<Map.Entry<K, V>> {
    
    TreeDictionaryNode<V> root
    Comparator comparator
    Closure defaultValue
    
    
    TreeDictionary(Comparator cmp=null) {
        root = new TreeDictionaryNode<V>()
        comparator = cmp
    }

    TreeDictionary(TreeDictionaryNode<V> tree, Comparator cmp=null) {
        root = tree
        comparator = cmp
    }
    
    boolean containsKey(List key) {
        TreeDictionaryNode pointer = root
        boolean retVal = false
        for(Object item: key) {
            TreeDictionaryNode node = pointer.getChild(item, comparator)
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

    V getAt(K key) {
        V val = null
        TreeDictionaryNode<V> pointer = root
        for(Object item: key) {
            TreeDictionaryNode node = pointer.getChild(item, comparator)
            if (node == null) {
                if (defaultValue != null)
                    return defaultValue()
                else
                    throw new Exception("The key $key does not belong to the dictionary")
            }
            pointer = node
        }
        return pointer.value
    }
    
    void putAt(K key, V value) {
        TreeDictionaryNode pointer = root
        for(Object item: key) {
            TreeDictionaryNode node = pointer.getChild(item, comparator)
            if (node == null) {
                node = new TreeDictionaryNode(pointer, item)
                pointer.children << node
            }
            pointer = node
        }
        pointer.value = value
    }

    public Iterator iterator() {
        return new TreeDictionaryIterator<>()
    }
    
    private class TreeDictionaryIterator<K extends List, V> implements Iterator {
   
        List<TreeDictionaryNode> queue
        private List v = []
        
        TreeDictionaryIterator() {
            queue = [root]
            
        }
        
        public boolean hasNext() {
            return queue.size() > 0
        }
        
        public Map.Entry<K, V> next() {
           Map.Entry<K, V> kvp
           boolean isRoot = true
           while (queue.size() > 0) {
                TreeDictionaryNode node = queue.remove(0)
                //println "Node ${node.path} value ${node.value}"
                if (node.children.size() > 0) {
                    queue = node.children + queue
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
        for (Map.Entry<K, V> item: this) {
            clos(item.key, item.value)
        }
    }
    
    void withDefault(Closure clos) {
        defaultValue = clos
    }
    
    String toString() {
        for (Map.Entry<K, V> item: this) {
            println "${item.key} = ${item.value}"
        }
    }
    
}


class TreeDictionaryNode<T> {
    TreeDictionaryNode parent
    List<TreeDictionaryNode> children = []
    def content
    T value
    List<Continuation> continuations = []
    
    TreeDictionaryNode() { }

    TreeDictionaryNode(TreeDictionaryNode parent) {
        this.parent = parent
    }

    TreeDictionaryNode(TreeDictionaryNode parent, Object content) {
        this.parent = parent
        this.content = content
    }

    TreeDictionaryNode(TreeDictionaryNode parent, Object content, T value) {
        this.parent = parent
        this.content = content
        this.value = value
    }
    
    List getPath() {
        List path = [content]
        TreeDictionaryNode pointer = parent
        while (pointer != null) {
            path.add(0,pointer.content)
            pointer = pointer.parent
        }
        return path
    }
    
    TreeDictionaryNode getChild(Object item, Comparator comparator=null) {
        TreeDictionaryNode retVal = null
        boolean found = false
        for(TreeDictionaryNode child: children) {
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

