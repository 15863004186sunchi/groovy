/*
 $Id$

 Copyright 2003 (C) James Strachan and Bob Mcwhirter. All Rights Reserved.

 Redistribution and use of this software and associated documentation
 ("Software"), with or without modification, are permitted provided
 that the following conditions are met:

 1. Redistributions of source code must retain copyright
    statements and notices.  Redistributions must also contain a
    copy of this document.

 2. Redistributions in binary form must reproduce the
    above copyright notice, this list of conditions and the
    following disclaimer in the documentation and/or other
    materials provided with the distribution.

 3. The name "groovy" must not be used to endorse or promote
    products derived from this Software without prior written
    permission of The Codehaus.  For written permission,
    please contact info@codehaus.org.

 4. Products derived from this Software may not be called "groovy"
    nor may "groovy" appear in their names without prior written
    permission of The Codehaus. "groovy" is a registered
    trademark of The Codehaus.

 5. Due credit should be given to The Codehaus -
    http://groovy.codehaus.org/

 THIS SOFTWARE IS PROVIDED BY THE CODEHAUS AND CONTRIBUTORS
 ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 THE CODEHAUS OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 OF THE POSSIBILITY OF SUCH DAMAGE.

 */
package groovy.util;


import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * An abstract base class for creating arbitrary nested trees of objects
 * or events
 * 
 * @author <a href="mailto:james@coredevelopers.net">James Strachan</a>
 * @version $Revision$
 */
public abstract class BuilderSupport extends GroovyObjectSupport {

    private Object current;
    private Closure nameMappingClosure;
    private BuilderSupport proxyBuilder;
    
    public BuilderSupport() {
        this.proxyBuilder = this;
    }
    
    public BuilderSupport(BuilderSupport proxyBuilder) {
        this(null, proxyBuilder);
    }
    
    public BuilderSupport(Closure nameMappingClosure, BuilderSupport proxyBuilder) {
        this.nameMappingClosure = nameMappingClosure;
        this.proxyBuilder = proxyBuilder;
    }
    
    public Object invokeMethod(String methodName, Object args) {
        Object name = getName(methodName);
        return doInvokeMethod(methodName, name, args);
    }
    
    protected Object doInvokeMethod(String methodName, Object name, Object args) {
        Object node = null;
        Closure closure = null;
        List list = InvokerHelper.asList(args);

        //System.out.println("Called invokeMethod with name: " + name + " arguments: " + list);

        switch (list.size()) {
        		case 0:
        		    break;
        	    	case 1:
        	    	{
        	    	    	Object object = list.get(0);
        	    	    	if (object instanceof Map) {
        	    	    	    node = proxyBuilder.createNode(name, (Map) object);
        	    	    	} else if (object instanceof Closure) {
        	    	    	    closure = (Closure) object;
        	    	    	    node = proxyBuilder.createNode(name);
        	    	    	} else {
        	    	    	    node = proxyBuilder.createNode(name, object);
        	    	    	}
        	    	}
        	    	break;
        	    	case 2:
        	    	{
        	    	    Object object1 = list.get(0);
    	    	        Object object2 = list.get(1);
        	    	    if (object1 instanceof Map) {
        	    	        if (object2 instanceof Closure) {
        	    	            closure = (Closure) object2;
        	    	            node = proxyBuilder.createNode(name, (Map) object1);
        	    	        } else {
        	    	            node = proxyBuilder.createNode(name, (Map) object1, object2);
        	    	        }
        	    	    } else {
        	    	        if (object2 instanceof Closure) {
        	    	            closure = (Closure) object2;
        	    	            node = proxyBuilder.createNode(name, object1);
        	    	        }
        	    	    }
        	    	}
        	    	break;
        	    	case 3:
        	    	{
        	    	    Object attributes = list.get(0);
        	    	    Object value = list.get(1);
        	    	    closure = (Closure) list.get(2);
        	    	    node = proxyBuilder.createNode(name, (Map) attributes, value);
        	    	}
        	    	break;
        }

        	if (node == null) {
	    	    node = proxyBuilder.createNode(name);
        	}
        
        if (current != null) {
            proxyBuilder.setParent(current, node);
        }

        if (closure != null) {
            // push new node on stack
            Object oldCurrent = current;
            current = node;

            // lets register the builder as the delegate
            closure.setDelegate(this);
            closure.call();

            current = oldCurrent;
        }

        proxyBuilder.nodeCompleted(current, node);
        return node;
    }

    protected abstract void setParent(Object parent, Object child);
    protected abstract Object createNode(Object name);
    protected abstract Object createNode(Object name, Object value);
    protected abstract Object createNode(Object name, Map attributes);
    protected abstract Object createNode(Object name, Map attributes, Object value);

    /**
     * A hook to allow names to be converted into some other object
     * such as a QName in XML or ObjectName in JMX
     * @param methodName
     * @return
     */
    protected Object getName(String methodName) {
        if (nameMappingClosure != null) {
            return nameMappingClosure.call(methodName);
        }
        return methodName;
    }


    /**
     * A hook to allow nodes to be processed once they have had all of their
     * children applied
     */
    protected void nodeCompleted(Object parent, Object node) {
    }

    protected Object getCurrent() {
        return current;
    }
    
    protected void setCurrent(Object current) {
        this.current = current;
    }
}
